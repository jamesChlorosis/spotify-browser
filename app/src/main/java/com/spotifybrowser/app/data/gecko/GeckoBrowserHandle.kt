package com.spotifybrowser.app.data.gecko

import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

data class GeckoBrowserHandle(
    val view: GeckoView,
    val session: GeckoSession,
    val runtime: GeckoRuntime
)
