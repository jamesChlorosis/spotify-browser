package com.spotifybrowser.app.data.webview

import com.spotifybrowser.app.data.preferences.BrowserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewScalePolicyTest {
    @Test
    fun defaultScaleIsComfortablyAboveNativeSize() {
        assertEquals(125, BrowserSettings().defaultZoomPercent)
    }

    @Test
    fun pageScaleStaysWithinSupportedBounds() {
        assertEquals(100, WebViewScalePolicy.pageScalePercent(80))
        assertEquals(125, WebViewScalePolicy.pageScalePercent(125))
        assertEquals(150, WebViewScalePolicy.pageScalePercent(175))
    }
}
