package com.spotifybrowser.app.data.webview

internal object WebViewScalePolicy {
    const val TextZoomPercent = 100

    fun pageScalePercent(value: Int): Int = value.coerceIn(100, 150)
}
