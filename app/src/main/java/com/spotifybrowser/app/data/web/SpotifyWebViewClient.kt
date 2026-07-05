package com.spotifybrowser.app.data.web

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class SpotifyWebViewClient(
    private val host: WebViewHost,
    private val onStateChanged: (BrowserChromeState) -> Unit
) : WebViewClient() {
    private var lastState = BrowserChromeState()

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!request.isForMainFrame) return false
        return handleNavigation(request.url)
    }

    @Suppress("DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return handleNavigation(Uri.parse(url))
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        publish(
            view,
            lastState.copy(
                currentUrl = url,
                isLoading = true,
                progress = 0,
                error = null
            )
        )
    }

    override fun onPageFinished(view: WebView, url: String) {
        publish(
            view,
            lastState.copy(
                currentUrl = url,
                title = view.title.orEmpty(),
                isLoading = false,
                progress = 100,
                error = null
            )
        )
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        if (request.isForMainFrame) {
            publish(
                view,
                lastState.copy(
                    isLoading = false,
                    error = BrowserError(
                        description = error.description?.toString().orEmpty()
                            .ifBlank { "The page could not be loaded." },
                        failingUrl = request.url.toString()
                    )
                )
            )
        }
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        if (request.isForMainFrame && errorResponse.statusCode >= 500) {
            publish(
                view,
                lastState.copy(
                    isLoading = false,
                    error = BrowserError(
                        description = "Server error ${errorResponse.statusCode}",
                        failingUrl = request.url.toString()
                    )
                )
            )
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        publish(
            view,
            lastState.copy(
                isLoading = false,
                error = BrowserError(
                    description = "Secure connection failed.",
                    failingUrl = error.url
                )
            )
        )
    }

    override fun onRenderProcessGone(
        view: WebView,
        detail: android.webkit.RenderProcessGoneDetail
    ): Boolean {
        publish(
            view,
            lastState.copy(
                isLoading = false,
                error = BrowserError(
                    description = "The browser process stopped. Reload to continue.",
                    failingUrl = lastState.currentUrl
                )
            )
        )
        return true
    }

    fun publishProgress(view: WebView, progress: Int) {
        publish(view, lastState.copy(progress = progress, isLoading = progress < 100))
    }

    fun publishTitle(view: WebView, title: String?) {
        publish(view, lastState.copy(title = title.orEmpty()))
    }

    private fun handleNavigation(uri: Uri): Boolean {
        if (ExternalLinkPolicy.shouldOpenExternally(uri)) {
            host.openExternalUri(uri)
            return true
        }
        return false
    }

    private fun publish(view: WebView, state: BrowserChromeState) {
        lastState = state.copy(
            canGoBack = view.canGoBack(),
            canGoForward = view.canGoForward()
        )
        onStateChanged(lastState)
    }
}
