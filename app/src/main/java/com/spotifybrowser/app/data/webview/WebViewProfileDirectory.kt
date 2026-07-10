package com.spotifybrowser.app.data.webview

import android.os.Build
import android.webkit.WebView
import com.spotifybrowser.app.data.profile.BrowserProfile

object WebViewProfileDirectory {
    private var configuredSuffix: String? = null

    fun configure(profile: BrowserProfile): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return true

        val suffix = profile.storageContextId
        val existing = configuredSuffix
        if (existing == suffix) return true
        if (existing != null) return false

        return runCatching {
            WebView.setDataDirectorySuffix(suffix)
            configuredSuffix = suffix
        }.isSuccess
    }
}
