package com.spotifybrowser.app.data.web

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

object WebViewMaintenance {
    fun clearCache(webView: WebView) {
        webView.clearCache(true)
    }

    fun clearCookies(onComplete: () -> Unit = {}) {
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            onComplete()
        }
    }

    fun clearCurrentProfile(webView: WebView, onComplete: () -> Unit = {}) {
        webView.stopLoading()
        webView.clearHistory()
        webView.clearFormData()
        webView.clearCache(true)
        WebStorage.getInstance().deleteAllData()
        clearCookies(onComplete)
    }
}
