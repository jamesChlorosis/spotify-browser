package com.spotifybrowser.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PreferencesRepository(context: Context) {
    private val dataStore = context.appPreferencesDataStore

    val settings: Flow<BrowserSettings> = dataStore.data
        .map { preferences ->
            BrowserSettings(
                useDesktopUserAgent = preferences[Keys.UseDesktopUserAgent] ?: false,
                defaultZoomPercent = preferences[Keys.DefaultZoomPercent] ?: 100,
                javaScriptEnabled = preferences[Keys.JavaScriptEnabled] ?: true,
                autoplayEnabled = preferences[Keys.AutoplayEnabled] ?: true,
                themeMode = preferences[Keys.ThemeMode]?.let(ThemeMode::valueOf) ?: ThemeMode.System
            )
        }
        .distinctUntilChanged()

    val lastProfileId: Flow<String?> = dataStore.data
        .map { it[Keys.LastProfileId] }
        .distinctUntilChanged()

    val extensionSetupCompleted: Flow<Boolean> = dataStore.data
        .map { it[Keys.ExtensionSetupCompleted] ?: false }
        .distinctUntilChanged()

    suspend fun setLastProfileId(profileId: String) {
        dataStore.edit { it[Keys.LastProfileId] = profileId }
    }

    suspend fun setDesktopUserAgent(enabled: Boolean) {
        dataStore.edit { it[Keys.UseDesktopUserAgent] = enabled }
    }

    suspend fun setDefaultZoomPercent(percent: Int) {
        dataStore.edit { it[Keys.DefaultZoomPercent] = percent.coerceIn(75, 150) }
    }

    suspend fun setJavaScriptEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.JavaScriptEnabled] = enabled }
    }

    suspend fun setAutoplayEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AutoplayEnabled] = enabled }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.ThemeMode] = themeMode.name }
    }

    suspend fun setExtensionSetupCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ExtensionSetupCompleted] = completed }
    }

    private object Keys {
        val LastProfileId = stringPreferencesKey("last_profile_id")
        val UseDesktopUserAgent = booleanPreferencesKey("use_desktop_user_agent")
        val DefaultZoomPercent = intPreferencesKey("default_zoom_percent")
        val JavaScriptEnabled = booleanPreferencesKey("javascript_enabled")
        val AutoplayEnabled = booleanPreferencesKey("autoplay_enabled")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val ExtensionSetupCompleted = booleanPreferencesKey("extension_setup_completed")
    }
}
