package com.spotifybrowser.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.appPreferencesDataStore by preferencesDataStore(name = "spotify_browser_preferences")
