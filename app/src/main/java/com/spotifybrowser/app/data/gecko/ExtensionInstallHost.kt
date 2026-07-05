package com.spotifybrowser.app.data.gecko

interface ExtensionInstallHost {
    fun installExtensionFromUrl(
        url: String,
        onResult: (Result<String>) -> Unit
    )
}
