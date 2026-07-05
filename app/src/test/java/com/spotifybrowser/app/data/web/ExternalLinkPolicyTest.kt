package com.spotifybrowser.app.data.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalLinkPolicyTest {
    @Test
    fun spotifyHostsStayInsideApp() {
        assertFalse(ExternalLinkPolicy.shouldOpenExternally("https", "open.spotify.com"))
        assertFalse(ExternalLinkPolicy.shouldOpenExternally("https", "accounts.spotify.com"))
    }

    @Test
    fun nonSpotifyHostsOpenExternally() {
        assertTrue(ExternalLinkPolicy.shouldOpenExternally("https", "example.com"))
        assertTrue(ExternalLinkPolicy.shouldOpenExternally("spotify", null))
    }
}
