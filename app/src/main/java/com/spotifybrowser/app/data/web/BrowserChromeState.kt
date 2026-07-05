package com.spotifybrowser.app.data.web

data class BrowserChromeState(
    val currentUrl: String = SpotifyUrls.HOME,
    val title: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val error: BrowserError? = null
)

data class BrowserError(
    val description: String,
    val failingUrl: String?
)
