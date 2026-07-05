package com.spotifybrowser.app.data.profile

import android.webkit.WebView

/**
 * Android WebView data-directory suffixes isolate cookies, storage, and cache.
 * The platform requires the suffix to be set before the first WebView instance
 * is created in the process, so switching profiles after launch restarts the app.
 */
object WebViewProfileDirectory {
    private var appliedSuffix: String? = null

    @Synchronized
    fun apply(profile: BrowserProfile) {
        val current = appliedSuffix
        if (current == profile.webViewSuffix) return

        check(current == null) {
            "WebView profile is already locked to $current. Restart before opening ${profile.webViewSuffix}."
        }

        WebView.setDataDirectorySuffix(profile.webViewSuffix)
        appliedSuffix = profile.webViewSuffix
    }
}
