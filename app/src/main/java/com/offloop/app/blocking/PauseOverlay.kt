package com.offloop.app.blocking

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.offloop.app.R

/**
 * Full-screen pause window drawn with TYPE_ACCESSIBILITY_OVERLAY, so no
 * additional overlay permission is needed. It counts down, then calls
 * [onLeave]; the service turns that into a global HOME action.
 */
class PauseOverlay(
    private val service: AccessibilityService,
    private val onLeave: () -> Unit,
    private val onStay: () -> Unit,
    private val countdownSeconds: Int = 5
) {

    private val handler = Handler(Looper.getMainLooper())
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var root: View? = null
    private var countdownView: TextView? = null
    private var remainingSeconds = countdownSeconds

    val isShowing: Boolean get() = root != null

    fun show() {
        if (root != null) return

        val view = LayoutInflater.from(service).inflate(R.layout.overlay_pause, null)
        val countdown = view.findViewById<TextView>(R.id.overlay_countdown)
        countdownView = countdown
        remainingSeconds = countdownSeconds
        countdown.text = remainingSeconds.toString()

        view.findViewById<View>(R.id.overlay_leave).setOnClickListener {
            hide()
            onLeave()
        }
        view.findViewById<View>(R.id.overlay_stay).setOnClickListener {
            hide()
            onStay()
        }
        view.isFocusableInTouchMode = true
        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode != KeyEvent.KEYCODE_BACK) return@setOnKeyListener false
            if (event.action == KeyEvent.ACTION_UP) {
                hide()
                onLeave()
            }
            true
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setFitInsetsTypes(0)
            }
        }

        try {
            windowManager.addView(view, params)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to show pause overlay", error)
            countdownView = null
            return
        }

        root = view
        view.requestFocus()
        handler.postDelayed(countdownRunnable, 1_000L)
    }

    fun hide() {
        handler.removeCallbacks(countdownRunnable)
        root?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        root = null
        countdownView = null
    }

    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (root == null) return

            remainingSeconds--
            if (remainingSeconds <= 0) {
                hide()
                onLeave()
            } else {
                countdownView?.text = remainingSeconds.toString()
                handler.postDelayed(this, 1_000L)
            }
        }
    }

    private companion object {
        private const val TAG = "OffloopOverlay"
    }
}
