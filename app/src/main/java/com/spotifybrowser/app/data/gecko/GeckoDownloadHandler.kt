package com.spotifybrowser.app.data.gecko

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import org.mozilla.geckoview.WebResponse

class GeckoDownloadHandler(context: Context) {
    private val appContext = context.applicationContext
    private val downloadManager =
        appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun enqueue(response: WebResponse) {
        val url = response.uri
        val uri = Uri.parse(url)
        if (uri.scheme != "https" && uri.scheme != "http") {
            Toast.makeText(appContext, "Download link is not supported", Toast.LENGTH_SHORT).show()
            return
        }

        val mimeType = response.header("content-type")
        val contentDisposition = response.header("content-disposition")
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)

        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription("Downloading from Spotify Browser")
            .setMimeType(mimeType)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        downloadManager.enqueue(request)
        Toast.makeText(appContext, "Download started", Toast.LENGTH_SHORT).show()
    }

    private fun WebResponse.header(name: String): String? {
        return headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }
}
