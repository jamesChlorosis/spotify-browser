package com.spotifybrowser.app.data.profile

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.spotifybrowser.app.MainActivity
import kotlin.system.exitProcess

object AppRestarter {
    const val EXTRA_PROFILE_ID = "com.spotifybrowser.app.extra.PROFILE_ID"

    fun restartWithProfile(context: Context, profileId: String) {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_PROFILE_ID, profileId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        context.startActivity(intent)
        Handler(Looper.getMainLooper()).postDelayed({ exitProcess(0) }, 150)
    }
}
