package com.spotifybrowser.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PreferencesRepository(context: Context) {
    private val dataStore = context.appPreferencesDataStore

    val settings: Flow<BrowserSettings> = dataStore.data
        .map { preferences ->
            BrowserSettings(
                useDesktopUserAgent = false,
                defaultZoomPercent = 100,
                javaScriptEnabled = true,
                autoplayEnabled = true,
                themeMode = preferences[Keys.ThemeMode]?.let(ThemeMode::valueOf) ?: ThemeMode.System
            )
        }
        .distinctUntilChanged()

    val lastProfileId: Flow<String?> = dataStore.data
        .map { it[Keys.LastProfileId] }
        .distinctUntilChanged()

    suspend fun setLastProfileId(profileId: String) {
        dataStore.edit { it[Keys.LastProfileId] = profileId }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.ThemeMode] = themeMode.name }
    }

    private object Keys {
        val LastProfileId = stringPreferencesKey("last_profile_id")
        val ThemeMode = stringPreferencesKey("theme_mode")
    }
}
