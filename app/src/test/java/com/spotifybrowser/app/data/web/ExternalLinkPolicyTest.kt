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
    fun webLinksStayInsideApp() {
        assertFalse(ExternalLinkPolicy.shouldOpenExternally("https", "example.com"))
        assertFalse(ExternalLinkPolicy.shouldOpenExternally("http", "example.com"))
    }

    @Test
    fun nonWebSchemesOpenExternally() {
        assertTrue(ExternalLinkPolicy.shouldOpenExternally("spotify", null))
        assertTrue(ExternalLinkPolicy.shouldOpenExternally("mailto", "example.com"))
    }
}
