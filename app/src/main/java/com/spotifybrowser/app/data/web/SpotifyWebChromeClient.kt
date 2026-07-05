package com.spotifybrowser.app.data.web

import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Message
import android.view.View

class SpotifyWebChromeClient(
    private val host: WebViewHost,
    private val webViewClient: SpotifyWebViewClient
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        webViewClient.publishProgress(view, newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        webViewClient.publishTitle(view, title)
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        return host.openFileChooser(filePathCallback, fileChooserParams)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        host.showFullscreenContent(view, callback)
    }

    override fun onHideCustomView() {
        host.hideFullscreenContent()
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        val transport = resultMsg.obj as WebView.WebViewTransport
        val popup = WebView(view.context)
        popup.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                popupView: WebView,
                request: WebResourceRequest
            ): Boolean {
                host.openExternalUri(request.url)
                popupView.destroy()
                return true
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(popupView: WebView, url: String): Boolean {
                host.openExternalUri(Uri.parse(url))
                popupView.destroy()
                return true
            }
        }
        transport.webView = popup
        resultMsg.sendToTarget()
        return true
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        val originHost = request.origin?.host
        val protectedMediaOnly = request.resources.filter {
            it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
        }.toTypedArray()

        if (ExternalLinkPolicy.isSpotifyHost(originHost) && protectedMediaOnly.isNotEmpty()) {
            request.grant(protectedMediaOnly)
        } else {
            request.deny()
        }
    }
}
