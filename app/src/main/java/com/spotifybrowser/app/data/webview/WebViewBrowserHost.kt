package com.spotifybrowser.app.data.webview

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

interface WebViewBrowserHost {
    fun setActiveWebView(webView: WebView?)
    fun openExternalUri(uri: Uri)
    fun download(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?
    )
    fun setPageFullscreen(enabled: Boolean)
    fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams
    ): Boolean
}
