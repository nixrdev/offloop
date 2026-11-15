package com.offloop.app.detection

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Recognises the short-form video surfaces inside apps we support.
 *
 * We deliberately key off view IDs rather than text or content descriptions:
 * labels are localised, and text such as "Shorts" or "Reels" is also present
 * on the normal feed. A visible, feature-specific view ID is the strongest
 * signal we can get without screenshots or ML.
 *
 * Instagram calls Stories "reels" internally, so `reel_viewer` IDs belong to
 * the Stories viewer and must not be used here.
 */
class ShortVideoDetector {

    fun detect(target: AppTarget, root: AccessibilityNodeInfo): Detection? = when (target) {
        AppTarget.YOUTUBE ->
            firstVisibleViewId(root, ShortVideoSurface.YOUTUBE_SHORTS, YOUTUBE_SHORTS_VIEW_IDS)

        AppTarget.INSTAGRAM ->
            firstVisibleViewId(root, ShortVideoSurface.INSTAGRAM_REELS, INSTAGRAM_REELS_VIEW_IDS)
    }

    private fun firstVisibleViewId(
        root: AccessibilityNodeInfo,
        surface: ShortVideoSurface,
        viewIds: List<String>
    ): Detection? {
        for (viewId in viewIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
            if (nodes.isEmpty()) continue

            val visible = nodes.any { it.isVisibleToUser }
            nodes.forEach { it.recycle() }

            if (visible) {
                return Detection(surface = surface, signal = "viewId=$viewId")
            }
        }
        return null
    }

    companion object {
        private val YOUTUBE_SHORTS_VIEW_IDS = listOf(
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/reel_watch_fragment_root",
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/reel_watch_player",
            "com.google.android.youtube:id/reel_watch_metadata",
            "com.google.android.youtube:id/reel_watch_engagement_panel"
        )

        private val INSTAGRAM_REELS_VIEW_IDS = listOf(
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/clips_viewer_action_bar",
            "com.instagram.android:id/clips_viewer_root",
            "com.instagram.android:id/clips_viewer_fragment_root"
        )
    }
}
