package com.spotifybrowser.app.data.profile

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.spotifybrowser.app.data.preferences.appPreferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class ProfileManager(private val context: Context) {
    private val dataStore = context.appPreferencesDataStore

    val profiles: Flow<List<BrowserProfile>> = dataStore.data
        .map { preferences -> decodeProfiles(preferences[Keys.ProfilesJson]) }
        .distinctUntilChanged()

    suspend fun ensureDefaultProfile(): BrowserProfile {
        val existing = profiles.first()
        if (existing.isNotEmpty()) return existing.first()

        val profile = newProfile(name = "Default")
        dataStore.edit { it[Keys.ProfilesJson] = encodeProfiles(listOf(profile)) }
        return profile
    }

    suspend fun createProfile(name: String): BrowserProfile {
        val cleanName = name.trim().ifEmpty { "New Profile" }
        val profile = newProfile(cleanName)
        dataStore.edit { preferences ->
            val updated = decodeProfiles(preferences[Keys.ProfilesJson]) + profile
            preferences[Keys.ProfilesJson] = encodeProfiles(updated)
        }
        return profile
    }

    suspend fun renameProfile(profileId: String, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return

        dataStore.edit { preferences ->
            val now = System.currentTimeMillis()
            val updated = decodeProfiles(preferences[Keys.ProfilesJson]).map { profile ->
                if (profile.id == profileId) profile.copy(name = cleanName, updatedAtMillis = now) else profile
            }
            preferences[Keys.ProfilesJson] = encodeProfiles(updated)
        }
    }

    suspend fun deleteProfile(profileId: String): List<BrowserProfile> {
        var remainingProfiles = emptyList<BrowserProfile>()

        dataStore.edit { preferences ->
            val updated = decodeProfiles(preferences[Keys.ProfilesJson]).filterNot { it.id == profileId }
            remainingProfiles = if (updated.isEmpty()) listOf(newProfile("Default")) else updated
            preferences[Keys.ProfilesJson] = encodeProfiles(remainingProfiles)
        }

        return remainingProfiles
    }

    private fun newProfile(name: String): BrowserProfile {
        val id = UUID.randomUUID().toString().replace("-", "")
        val now = System.currentTimeMillis()
        return BrowserProfile(
            id = id,
            name = name,
            storageContextId = "profile_$id",
            createdAtMillis = now,
            updatedAtMillis = now
        )
    }

    private fun decodeProfiles(json: String?): List<BrowserProfile> {
        if (json.isNullOrBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        BrowserProfile(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            storageContextId = item.optString(
                                "storageContextId",
                                item.optString("webViewSuffix", "profile_${item.getString("id")}")
                            ),
                            createdAtMillis = item.getLong("createdAtMillis"),
                            updatedAtMillis = item.getLong("updatedAtMillis")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun encodeProfiles(profiles: List<BrowserProfile>): String {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(
                JSONObject()
                    .put("id", profile.id)
                    .put("name", profile.name)
                    .put("storageContextId", profile.storageContextId)
                    .put("createdAtMillis", profile.createdAtMillis)
                    .put("updatedAtMillis", profile.updatedAtMillis)
            )
        }
        return array.toString()
    }

    private object Keys {
        val ProfilesJson = stringPreferencesKey("profiles_json")
    }
}
