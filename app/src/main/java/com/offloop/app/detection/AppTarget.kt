package com.offloop.app.detection

enum class AppTarget(
    val packageName: String,
    val displayName: String
) {
    YOUTUBE("com.google.android.youtube", "YouTube"),
    INSTAGRAM("com.instagram.android", "Instagram");

    companion object {
        fun fromPackage(packageName: CharSequence?): AppTarget? {
            val name = packageName?.toString() ?: return null
            return entries.firstOrNull { it.packageName == name }
        }
    }
}
