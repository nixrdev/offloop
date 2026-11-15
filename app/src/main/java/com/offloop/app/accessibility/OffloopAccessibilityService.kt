package com.offloop.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.offloop.app.MainActivity
import com.offloop.app.blocking.PauseOverlay
import com.offloop.app.detection.AppTarget
import com.offloop.app.detection.Detection
import com.offloop.app.detection.ShortVideoDetector

class OffloopAccessibilityService : AccessibilityService() {

    private val detector = ShortVideoDetector()
    private val handler = Handler(Looper.getMainLooper())

    private var activeDetection: Detection? = null
    private var lastTarget: AppTarget? = null
    private var lastEvaluationAt = 0L
    private var lastDebugDumpAt = 0L

    private var pauseOverlay: PauseOverlay? = null
    private var interventionRunnable: Runnable? = null
    private var snoozeUntil = 0L
    private var lastExitAt = 0L

    private var periodicCheckRunnable: Runnable? = null

    private val isDebuggable by lazy {
        applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Offloop accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val isWindowChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val isContentChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        if (!isWindowChange && !isContentChange) return

        // Our own overlay windows are accessibility windows too. Ignore them so
        // they do not look like a jump to another app, but opening Offloop's
        // own screen is a real departure from the feed.
        if (event.packageName?.toString() == packageName) {
            if (isWindowChange &&
                pauseOverlay?.isShowing != true &&
                event.className?.toString() == MainActivity::class.java.name
            ) {
                leaveTarget()
            }
            return
        }

        val target = AppTarget.fromPackage(event.packageName)

        if (pauseOverlay?.isShowing == true) {
            if (isWindowChange && isInCall()) {
                hideOverlay()
                scheduleIntervention(CALL_RETRY_MS)
            }
            return
        }

        if (target == null) {
            return
        }

        val now = SystemClock.uptimeMillis()
        if (isContentChange && now - lastEvaluationAt < EVALUATION_THROTTLE_MS) return
        lastEvaluationAt = now

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != target.packageName) {
            root.recycle()
            return
        }

        try {
            val detection = detector.detect(target, root)
            updateDetection(target, detection)
            if (detection == null && isDebuggable) {
                maybeDumpDebugTree(target, root, now)
            }
        } finally {
            root.recycle()
        }
    }

    private fun updateDetection(target: AppTarget, detection: Detection?) {
        if (target != lastTarget) {
            lastTarget = target
            activeDetection = null
        }

        val previous = activeDetection
        activeDetection = detection

        if (detection == null) {
            cancelIntervention()
            stopPeriodicCheck()
            if (previous != null) {
                Log.i(TAG, "Left ${target.displayName} short-form surface")
            }
            return
        }

        if (previous?.surface != detection.surface) {
            Log.i(TAG, "Detected ${detection.surface.label} (${detection.signal})")
        }
        ensureIntervention()
        ensurePeriodicCheck()
    }

    /**
     * Keeps exactly one intervention pending while a short-form surface is on
     * screen. Snooze and cooldown postpone it rather than cancelling it, so
     * re-entering the feed straight after an exit is still interrupted.
     */
    private fun ensureIntervention() {
        if (pauseOverlay?.isShowing == true || interventionRunnable != null) return

        val now = SystemClock.elapsedRealtime()
        val fireAt = maxOf(
            now + INTERVENTION_DELAY_MS,
            snoozeUntil,
            lastExitAt + EXIT_COOLDOWN_MS
        )
        scheduleIntervention(fireAt - now)
    }

    private fun scheduleIntervention(delayMs: Long) {
        cancelIntervention()
        val runnable = Runnable {
            interventionRunnable = null
            fireIntervention()
        }
        interventionRunnable = runnable
        handler.postDelayed(runnable, delayMs)
        ensurePeriodicCheck()
    }

    private fun fireIntervention() {
        if (pauseOverlay?.isShowing == true) return

        // The overlay sits above heads-up notifications, so it could cover the
        // answer button of an incoming call.
        if (isInCall()) {
            Log.i(TAG, "Intervention postponed: call in progress")
            scheduleIntervention(CALL_RETRY_MS)
            return
        }

        // Remembered state can be stale if a window change was missed, so
        // confirm the feed is really on screen before covering it.
        val detection = detectOnScreen()
        if (detection == null) {
            Log.i(TAG, "Intervention skipped: no short-form surface on screen")
            activeDetection = null
            lastTarget = null
            return
        }

        activeDetection = detection
        showIntervention(detection)
    }

    private fun detectOnScreen(): Detection? {
        val root = rootInActiveWindow ?: return null
        return try {
            AppTarget.fromPackage(root.packageName)?.let { detector.detect(it, root) }
        } finally {
            root.recycle()
        }
    }

    /**
     * Keyboards, the notification shade and other overlays send window events
     * from foreign packages while the target app stays in front. Only treat it
     * as leaving when the active window really belongs to something else.
     */
    private fun hasLeftTarget(): Boolean {
        val target = lastTarget ?: return true
        val root = rootInActiveWindow ?: return true
        return try {
            val activePackage = root.packageName?.toString()
            activePackage != target.packageName && activePackage != packageName
        } finally {
            root.recycle()
        }
    }

    private fun showIntervention(detection: Detection) {
        if (pauseOverlay?.isShowing == true) return

        Log.i(TAG, "Pause overlay shown for ${detection.surface.label}")
        pauseOverlay = PauseOverlay(
            service = this,
            onLeave = { exitToHome("overlay") },
            onStay = { stayOneMoreMinute() }
        ).also { it.show() }
        ensurePeriodicCheck()
    }

    private fun isInCall(): Boolean {
        val mode = getSystemService(AudioManager::class.java)?.mode ?: return false
        return mode == AudioManager.MODE_RINGTONE ||
            mode == AudioManager.MODE_IN_CALL ||
            mode == AudioManager.MODE_IN_COMMUNICATION ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                mode == AudioManager.MODE_CALL_SCREENING)
    }

    private fun exitToHome(reason: String) {
        if (isInCall()) {
            Log.i(TAG, "Auto-exit skipped: call in progress")
            hideOverlay()
            scheduleIntervention(CALL_RETRY_MS)
            return
        }

        // Before pressing Home, confirm a short-form surface is still on screen.
        // If the user moved to another app (e.g. payment/UPI via notification or
        // Instagram shop link) and the leave event was missed, we must not close
        // that app mid-transaction.
        val detection = detectOnScreen()
        if (detection == null) {
            Log.i(TAG, "Auto-exit cancelled: no short-form surface on screen (user may have switched apps)")
            hideOverlay()
            cancelIntervention()
            activeDetection = null
            lastTarget = null
            snoozeUntil = 0
            return
        }

        val surface = detection.surface.label
        Log.i(TAG, "Auto-exit to home ($reason) from $surface")

        hideOverlay()
        cancelIntervention()

        activeDetection = null
        lastTarget = null
        snoozeUntil = 0
        lastExitAt = SystemClock.elapsedRealtime()

        if (!performGlobalAction(GLOBAL_ACTION_HOME)) {
            Log.w(TAG, "Global HOME action was rejected")
        }
    }

    private fun stayOneMoreMinute() {
        hideOverlay()
        cancelIntervention()

        snoozeUntil = SystemClock.elapsedRealtime() + SNOOZE_MS
        Log.i(TAG, "Stayed for ${SNOOZE_MS / 1_000}s; overlay will re-arm")
        ensureIntervention()
    }

    private fun cancelIntervention() {
        interventionRunnable?.let(handler::removeCallbacks)
        interventionRunnable = null
    }

    private fun hideOverlay() {
        pauseOverlay?.hide()
        pauseOverlay = null
    }

    private fun leaveTarget() {
        val surface = activeDetection?.surface?.label
        if (surface != null) {
            Log.i(TAG, "Left $surface")
        }

        cancelIntervention()
        hideOverlay()
        stopPeriodicCheck()

        snoozeUntil = 0
        lastTarget = null
        activeDetection = null
    }

    /**
     * Periodically verifies that the active window still belongs to the target
     * app and the short-form surface is still detected. Runs only while an
     * intervention is scheduled or the overlay is showing, so there is no
     * battery cost when idle.
     */
    private fun ensurePeriodicCheck() {
        if (periodicCheckRunnable != null) return

        val shouldRun = interventionRunnable != null || pauseOverlay?.isShowing == true
        if (!shouldRun) return

        val runnable = object : Runnable {
            override fun run() {
                periodicCheckRunnable = null
                performPeriodicCheck()
            }
        }
        periodicCheckRunnable = runnable
        handler.postDelayed(runnable, PERIODIC_CHECK_INTERVAL_MS)
    }

    private fun performPeriodicCheck() {
        val target = lastTarget
        if (target == null) {
            stopPeriodicCheck()
            return
        }

        val root = rootInActiveWindow
        if (root == null) {
            Log.i(TAG, "Periodic check: no active window, leaving target")
            leaveTarget()
            return
        }

        val activePackage = root.packageName?.toString()
        val stillInTarget = activePackage == target.packageName

        if (!stillInTarget) {
            root.recycle()
            Log.i(TAG, "Periodic check: user switched to $activePackage, leaving target")
            leaveTarget()
            return
        }

        val detection = try {
            detector.detect(target, root)
        } finally {
            root.recycle()
        }

        if (detection == null) {
            Log.i(TAG, "Periodic check: surface no longer detected, leaving target")
            leaveTarget()
            return
        }

        ensurePeriodicCheck()
    }

    private fun stopPeriodicCheck() {
        periodicCheckRunnable?.let(handler::removeCallbacks)
        periodicCheckRunnable = null
    }

    /**
     * When we do not recognise a surface we log the visible view IDs and class
     * names containing words like "reel", "short" or "clip". This is how we
     * tune selectors against the real app versions on the test phone.
     */
    private fun maybeDumpDebugTree(
        target: AppTarget,
        root: AccessibilityNodeInfo,
        now: Long
    ) {
        if (now - lastDebugDumpAt < DEBUG_DUMP_INTERVAL_MS) return
        lastDebugDumpAt = now

        Log.d(TAG, "No match in ${target.displayName}. Candidate nodes:")
        dumpCandidates(root)
    }

    private fun dumpCandidates(root: AccessibilityNodeInfo) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        var visited = 0
        while (queue.isNotEmpty() && visited < DEBUG_NODE_LIMIT) {
            val node = queue.removeFirst()
            visited++

            val viewId = node.viewIdResourceName
            val className = node.className?.toString()
            val interesting = listOfNotNull(viewId, className).any { value ->
                DEBUG_KEYWORDS.any { keyword -> value.contains(keyword, ignoreCase = true) }
            }

            if (interesting && node.isVisibleToUser) {
                Log.d(TAG, "  id=${viewId ?: "-"} class=${className ?: "-"}")
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }

            if (node !== root) node.recycle()
        }

        while (queue.isNotEmpty()) {
            queue.removeFirst().recycle()
        }
    }

    override fun onInterrupt() {
        leaveTarget()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopBlocking()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopBlocking()
        super.onDestroy()
    }

    private fun stopBlocking() {
        cancelIntervention()
        hideOverlay()
        stopPeriodicCheck()
        activeDetection = null
        lastTarget = null
    }

    companion object {
        private const val TAG = "OffloopDetect"
        private const val EVALUATION_THROTTLE_MS = 500L
        private const val DEBUG_DUMP_INTERVAL_MS = 5_000L
        private const val DEBUG_NODE_LIMIT = 300

        private const val INTERVENTION_DELAY_MS = 2_000L
        private const val SNOOZE_MS = 60_000L
        private const val EXIT_COOLDOWN_MS = 5_000L
        private const val CALL_RETRY_MS = 10_000L
        private const val PERIODIC_CHECK_INTERVAL_MS = 2_500L

        private val DEBUG_KEYWORDS = listOf("reel", "short", "clip", "viewer", "player", "pager", "recycler")
    }
}
