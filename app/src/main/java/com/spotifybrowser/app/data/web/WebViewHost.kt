package com.spotifybrowser.app.data.web

import android.net.Uri
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

interface WebViewHost {
    fun openExternalUri(uri: Uri)

    fun openFileChooser(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams
    ): Boolean

    fun showFullscreenContent(view: View, callback: WebChromeClient.CustomViewCallback)

    fun hideFullscreenContent()
}
