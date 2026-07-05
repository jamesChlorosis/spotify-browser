package com.spotifybrowser.app.data.web

import android.net.Uri

object ExternalLinkPolicy {
    fun shouldOpenExternally(uri: Uri): Boolean {
        return shouldOpenExternally(uri.scheme, uri.host)
    }

    fun shouldOpenExternally(scheme: String?, host: String?): Boolean {
        val cleanScheme = scheme?.lowercase() ?: return true
        if (cleanScheme != "http" && cleanScheme != "https") return true

        val cleanHost = host?.lowercase() ?: return true
        return !isSpotifyHost(cleanHost)
    }

    fun isSpotifyHost(host: String?): Boolean {
        val cleanHost = host?.lowercase() ?: return false
        return cleanHost == "spotify.com" || cleanHost.endsWith(".spotify.com")
    }
}
