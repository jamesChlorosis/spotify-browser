package com.spotifybrowser.app.data.webview

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

interface WebViewBrowserHost {
    fun openExternalUri(uri: Uri)
    fun openSpotifyInCompatibleBrowser(uri: Uri)
    fun openExtensionUrl(url: String)
    fun setPageFullscreen(enabled: Boolean)
    fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams
    ): Boolean
}
