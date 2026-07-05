package com.spotifybrowser.app.data.profile

data class BrowserProfile(
    val id: String,
    val name: String,
    val webViewSuffix: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
