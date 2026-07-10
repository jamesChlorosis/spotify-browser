package com.spotifybrowser.app.data.webview

import android.content.Context
import android.os.Build
import android.webkit.WebSettings

internal object SpotifyUserAgent {
    fun create(context: Context, desktop: Boolean): String {
        val chromeVersion = Regex("Chrome/([0-9.]+)")
            .find(WebSettings.getDefaultUserAgent(context))
            ?.groupValues
            ?.getOrNull(1)
            ?: "126.0.0.0"

        return if (desktop) {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
        } else {
            "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; Mobile) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Mobile Safari/537.36"
        }
    }
}
