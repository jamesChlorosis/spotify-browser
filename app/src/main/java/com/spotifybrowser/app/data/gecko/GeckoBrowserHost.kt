package com.spotifybrowser.app.data.gecko

import android.net.Uri
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

interface GeckoBrowserHost {
    fun openExternalUri(uri: Uri)
    fun setPageFullscreen(enabled: Boolean)
    fun onFilePrompt(prompt: GeckoSession.PromptDelegate.FilePrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>
}
