package com.spotifybrowser.app.data.web

import android.content.Context
import android.webkit.WebView
import com.spotifybrowser.app.BuildConfig
import com.spotifybrowser.app.data.preferences.BrowserSettings
import com.spotifybrowser.app.data.profile.BrowserProfile
import com.spotifybrowser.app.data.profile.WebViewProfileDirectory

class SpotifyWebViewFactory(
    private val host: WebViewHost,
    private val downloadHandler: SpotifyDownloadHandler
) {
    fun create(
        context: Context,
        profile: BrowserProfile,
        settings: BrowserSettings,
        isDarkTheme: Boolean,
        onStateChanged: (BrowserChromeState) -> Unit
    ): WebView {
        WebViewProfileDirectory.apply(profile)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        val spotifyClient = SpotifyWebViewClient(host, onStateChanged)
        return WebView(context).apply {
            WebViewSettingsApplier.apply(this, settings, isDarkTheme)
            webViewClient = spotifyClient
            webChromeClient = SpotifyWebChromeClient(host, spotifyClient)
            setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                downloadHandler.enqueue(url, userAgent, contentDisposition, mimeType, contentLength)
            }
            loadUrl(SpotifyUrls.HOME)
        }
    }
}
