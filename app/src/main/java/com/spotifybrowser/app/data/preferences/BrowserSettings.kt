package com.spotifybrowser.app.data.preferences

data class BrowserSettings(
    val useDesktopUserAgent: Boolean = true,
    val defaultZoomPercent: Int = 100,
    val javaScriptEnabled: Boolean = true,
    val autoplayEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System
)

enum class ThemeMode {
    System,
    Light,
    Dark
}
