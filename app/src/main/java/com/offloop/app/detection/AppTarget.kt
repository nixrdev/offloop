package com.offloop.app.detection

enum class AppTarget(
    val packageName: String,
    val displayName: String,
    val homeTabViewIds: List<String>,
    val homeTabContentDescriptions: List<String>
) {
    YOUTUBE(
        packageName = "com.google.android.youtube",
        displayName = "YouTube",
        homeTabViewIds = listOf(
            "com.google.android.youtube:id/pivot_home",
            "com.google.android.youtube:id/bottom_bar_home_item"
        ),
        homeTabContentDescriptions = listOf("Home")
    ),
    INSTAGRAM(
        packageName = "com.instagram.android",
        displayName = "Instagram",
        homeTabViewIds = listOf(
            "com.instagram.android:id/tab_bar_home_button",
            "com.instagram.android:id/feed_tab"
        ),
        homeTabContentDescriptions = listOf("Home")
    );

    companion object {
        fun fromPackage(packageName: CharSequence?): AppTarget? {
            val name = packageName?.toString() ?: return null
            return entries.firstOrNull { it.packageName == name }
        }
    }
}
