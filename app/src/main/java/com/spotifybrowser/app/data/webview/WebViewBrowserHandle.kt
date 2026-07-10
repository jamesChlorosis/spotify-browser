package com.spotifybrowser.app.data.webview

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient

class WebViewBrowserHandle(val view: WebView) {
    fun loadUri(uri: String) {
        view.loadUrl(uri)
    }

    fun goBack() {
        if (view.canGoBack()) view.goBack()
    }

    fun goForward() {
        if (view.canGoForward()) view.goForward()
    }

    fun reload() {
        view.reload()
    }

    fun clearCache() {
        view.clearCache(true)
    }

    fun clearBrowsingData(onComplete: () -> Unit) {
        view.clearCache(true)
        view.clearHistory()
        view.clearFormData()
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            onComplete()
        }
    }

    fun dispose() {
        view.stopLoading()
        view.webChromeClient = null
        view.webViewClient = WebViewClient()
        view.removeAllViews()
        view.destroy()
    }
}
