package com.spotifybrowser.app.data.gecko

object ExtensionPermissionPolicy {
    private val restrictedPermissions = setOf(
        "webRequest",
        "webRequestBlocking",
        "declarativeNetRequest",
        "declarativeNetRequestWithHostAccess",
        "proxy",
        "privacy"
    )

    fun isAllowed(
        permissions: Array<String>,
        origins: Array<String>
    ): Boolean {
        if (permissions.any { it in restrictedPermissions }) return false
        return origins.none(::coversSpotifyOrAllSites)
    }

    private fun coversSpotifyOrAllSites(origin: String): Boolean {
        val clean = origin.lowercase()
        return clean == "<all_urls>" ||
            clean.contains("*://*/*") ||
            clean.contains("://*/") ||
            clean.contains("spotify.com")
    }
}
