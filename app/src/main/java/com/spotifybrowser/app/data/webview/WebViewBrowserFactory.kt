package com.spotifybrowser.app.data.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewRenderProcess
import android.webkit.WebViewRenderProcessClient
import com.spotifybrowser.app.data.preferences.BrowserSettings
import com.spotifybrowser.app.data.profile.BrowserProfile
import com.spotifybrowser.app.data.web.BrowserChromeState
import com.spotifybrowser.app.data.web.BrowserError
import com.spotifybrowser.app.data.web.ExternalLinkPolicy
import com.spotifybrowser.app.data.web.SpotifyUrls

class WebViewBrowserFactory(
    private val host: WebViewBrowserHost
) {
    @SuppressLint("SetJavaScriptEnabled")
    fun create(
        context: Context,
        profile: BrowserProfile,
        settings: BrowserSettings,
        onStateChanged: (BrowserChromeState) -> Unit,
        onRendererGone: () -> Unit
    ): WebViewBrowserHandle {
        WebViewProfileDirectory.configure(profile)
        WebView.setWebContentsDebuggingEnabled(false)

        val publisher = BrowserStatePublisher(onStateChanged)
        val webView = WebView(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            applySpotifySettings(settings)
            webViewClient = SpotifyWebViewClient(publisher, onRendererGone)
            webChromeClient = SpotifyWebChromeClient(publisher)
            setOnTouchListener { view, _ ->
                if (!view.hasFocus()) view.requestFocus()
                false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setWebViewRenderProcessClient(SpotifyRenderProcessClient(publisher))
            }
            loadUrl(SpotifyUrls.HOME)
        }
        host.setActiveWebView(webView)

        publisher.update {
            copy(
                currentUrl = SpotifyUrls.HOME,
                canGoBack = webView.canGoBack(),
                canGoForward = webView.canGoForward()
            )
        }

        return WebViewBrowserHandle(webView)
    }

    private fun WebView.applySpotifySettings(settings: BrowserSettings) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        this.settings.apply {
            javaScriptEnabled = settings.javaScriptEnabled
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            // Page scale enlarges icons and controls too; textZoom alone does not.
            textZoom = WebViewScalePolicy.TextZoomPercent
            this@applySpotifySettings.setInitialScale(
                WebViewScalePolicy.pageScalePercent(settings.defaultZoomPercent)
            )
            useWideViewPort = false
            loadWithOverviewMode = false
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = true
            userAgentString = SpotifyUserAgent.create(context, settings.useDesktopUserAgent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                offscreenPreRaster = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }
    }

    private inner class SpotifyWebViewClient(
        private val publisher: BrowserStatePublisher,
        private val onRendererGone: () -> Unit
    ) : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            val uri = request.url
            if (request.isForMainFrame && ExternalLinkPolicy.shouldOpenExternally(uri)) {
                host.openExternalUri(uri)
                return true
            }
            return false
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            publisher.update {
                copy(
                    currentUrl = url ?: currentUrl,
                    isLoading = true,
                    progress = 0,
                    error = null,
                    canGoBack = view.canGoBack(),
                    canGoForward = view.canGoForward()
                )
            }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            publisher.update {
                copy(
                    currentUrl = url ?: currentUrl,
                    isLoading = false,
                    progress = 100,
                    canGoBack = view.canGoBack(),
                    canGoForward = view.canGoForward()
                )
            }
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            publisher.update {
                copy(
                    currentUrl = url ?: currentUrl,
                    canGoBack = view.canGoBack(),
                    canGoForward = view.canGoForward()
                )
            }
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            if (!request.isForMainFrame) return

            publisher.update {
                copy(
                    isLoading = false,
                    error = BrowserError(
                        description = error.description?.toString()
                            ?: "Spotify could not load. Check your connection and try again.",
                        failingUrl = request.url?.toString()
                    )
                )
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            if (!request.isForMainFrame) return

            publisher.update {
                copy(
                    isLoading = false,
                    error = BrowserError(
                        description = "Spotify returned HTTP ${errorResponse.statusCode}.",
                        failingUrl = request.url?.toString()
                    )
                )
            }
        }

        override fun onReceivedSslError(
            view: WebView,
            handler: SslErrorHandler,
            error: SslError
        ) {
            handler.cancel()
            publisher.update {
                copy(
                    isLoading = false,
                    error = BrowserError(
                        description = "A secure connection to Spotify could not be verified.",
                        failingUrl = error.url
                    )
                )
            }
        }

        override fun onRenderProcessGone(
            view: WebView,
            detail: RenderProcessGoneDetail
        ): Boolean {
            publisher.update {
                copy(
                    isLoading = false,
                    error = BrowserError(
                        description = if (detail.didCrash()) {
                            "Spotify's renderer crashed and was restarted."
                        } else {
                            "Android stopped Spotify's renderer to free memory; it was restarted."
                        },
                        failingUrl = currentUrl.ifBlank { SpotifyUrls.HOME }
                    )
                )
            }
            view.post { onRendererGone() }
            return true
        }
    }

    private inner class SpotifyWebChromeClient(
        private val publisher: BrowserStatePublisher
    ) : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            publisher.update { copy(progress = newProgress.coerceIn(0, 100)) }
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            publisher.update { copy(title = title.orEmpty()) }
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: android.webkit.ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            return host.onShowFileChooser(filePathCallback, fileChooserParams)
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            val protectedMediaResources = request.resources
                .filter { it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID }
                .toTypedArray()
            val originHost = request.origin?.host

            if (
                protectedMediaResources.isNotEmpty() &&
                ExternalLinkPolicy.isTrustedSpotifyMediaHost(originHost)
            ) {
                request.grant(protectedMediaResources)
            } else {
                request.deny()
            }
        }

        override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
            host.setPageFullscreen(true)
            callback?.onCustomViewHidden()
        }

        override fun onHideCustomView() {
            host.setPageFullscreen(false)
        }
    }

    private class SpotifyRenderProcessClient(
        private val publisher: BrowserStatePublisher
    ) : WebViewRenderProcessClient() {
        override fun onRenderProcessUnresponsive(
            view: WebView,
            renderer: WebViewRenderProcess?
        ) {
            publisher.update {
                copy(
                    error = BrowserError(
                        description = "Spotify stopped responding. Tap Retry to reload it.",
                        failingUrl = currentUrl.ifBlank { SpotifyUrls.HOME }
                    )
                )
            }
        }

        override fun onRenderProcessResponsive(
            view: WebView,
            renderer: WebViewRenderProcess?
        ) {
            publisher.update { copy(error = null) }
        }
    }

    private class BrowserStatePublisher(
        private val onStateChanged: (BrowserChromeState) -> Unit
    ) {
        private var state = BrowserChromeState()

        fun update(change: BrowserChromeState.() -> BrowserChromeState) {
            state = state.change()
            onStateChanged(state)
        }
    }
}

fun WebView.applyBrowserSettings(settings: BrowserSettings) {
    this.settings.apply {
        javaScriptEnabled = settings.javaScriptEnabled
        mediaPlaybackRequiresUserGesture = false
        textZoom = WebViewScalePolicy.TextZoomPercent
        this@applyBrowserSettings.setInitialScale(
            WebViewScalePolicy.pageScalePercent(settings.defaultZoomPercent)
        )
        useWideViewPort = false
        loadWithOverviewMode = false
        userAgentString = SpotifyUserAgent.create(context, settings.useDesktopUserAgent)
    }
}
