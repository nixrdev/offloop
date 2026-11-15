package com.offloop.app.detection

enum class ShortVideoSurface(val label: String) {
    YOUTUBE_SHORTS("YouTube Shorts"),
    INSTAGRAM_REELS("Instagram Reels")
}

data class Detection(
    val surface: ShortVideoSurface,
    val signal: String
)
