package com.spotifybrowser.app.data.web

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast

class SpotifyDownloadHandler(context: Context) {
    private val appContext = context.applicationContext
    private val downloadManager =
        appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun enqueue(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription("Downloading from Spotify Browser")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        if (!mimeType.isNullOrBlank()) {
            request.setMimeType(mimeType)
        }
        if (!userAgent.isNullOrBlank()) {
            request.addRequestHeader("User-Agent", userAgent)
        }
        CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }?.let { cookies ->
            request.addRequestHeader("Cookie", cookies)
        }
        runCatching {
            downloadManager.enqueue(request)
            Toast.makeText(appContext, "Download started", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(appContext, "Unable to start download", Toast.LENGTH_LONG).show()
        }
    }
}
