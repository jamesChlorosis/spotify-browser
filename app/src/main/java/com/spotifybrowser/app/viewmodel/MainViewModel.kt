package com.spotifybrowser.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spotifybrowser.app.data.preferences.BrowserSettings
import com.spotifybrowser.app.data.preferences.PreferencesRepository
import com.spotifybrowser.app.data.preferences.ThemeMode
import com.spotifybrowser.app.data.profile.BrowserProfile
import com.spotifybrowser.app.data.profile.ProfileManager
import com.spotifybrowser.app.data.web.BrowserChromeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val isReady: Boolean = false,
    val profiles: List<BrowserProfile> = emptyList(),
    val lastProfileId: String? = null,
    val activeProfile: BrowserProfile? = null,
    val settings: BrowserSettings = BrowserSettings(),
    val browserChrome: BrowserChromeState = BrowserChromeState()
)

private data class AppBaseState(
    val profiles: List<BrowserProfile>,
    val lastProfileId: String?,
    val settings: BrowserSettings
)

class MainViewModel(
    application: Application,
    private val profileManager: ProfileManager,
    private val preferencesRepository: PreferencesRepository,
    initialProfileId: String?
) : AndroidViewModel(application) {
    private val activeProfileId = MutableStateFlow(initialProfileId)
    private val browserChrome = MutableStateFlow(BrowserChromeState())

    private val appBaseState = combine(
        profileManager.profiles,
        preferencesRepository.lastProfileId,
        preferencesRepository.settings
    ) { profiles, lastProfileId, settings ->
        AppBaseState(
            profiles = profiles,
            lastProfileId = lastProfileId,
            settings = settings
        )
    }

    val uiState: StateFlow<AppUiState> = combine(
        appBaseState,
        activeProfileId,
        browserChrome
    ) { base, activeId, chrome ->
        AppUiState(
            isReady = base.profiles.isNotEmpty(),
            profiles = base.profiles,
            lastProfileId = base.lastProfileId,
            activeProfile = base.profiles.firstOrNull { it.id == activeId },
            settings = base.settings,
            browserChrome = chrome
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState()
    )

    init {
        viewModelScope.launch {
            profileManager.ensureDefaultProfile()
            initialProfileId?.let { profileId ->
                profileManager.profiles.first()
                    .firstOrNull { it.id == profileId }
                    ?.let { profile ->
                        activeProfileId.value = profile.id
                        preferencesRepository.setLastProfileId(profile.id)
                    }
            }
        }
    }

    fun openProfile(profile: BrowserProfile) {
        viewModelScope.launch {
            preferencesRepository.setLastProfileId(profile.id)
            activeProfileId.value = profile.id
        }
    }

    fun updateBrowserChrome(state: BrowserChromeState) {
        browserChrome.value = state
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            profileManager.createProfile(name)
        }
    }

    fun renameProfile(profile: BrowserProfile, newName: String) {
        viewModelScope.launch {
            profileManager.renameProfile(profile.id, newName)
        }
    }

    fun deleteProfile(profile: BrowserProfile) {
        viewModelScope.launch {
            val deletingActive = activeProfileId.value == profile.id
            val deletingLastOpened = preferencesRepository.lastProfileId.first() == profile.id
            val remaining = profileManager.deleteProfile(profile.id)
            val replacement = remaining.first()

            if (deletingLastOpened) {
                preferencesRepository.setLastProfileId(replacement.id)
            }

            if (deletingActive) {
                activeProfileId.value = replacement.id
            }
        }
    }

    fun setDesktopUserAgent(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDesktopUserAgent(enabled) }
    }

    fun setDefaultZoomPercent(percent: Int) {
        viewModelScope.launch { preferencesRepository.setDefaultZoomPercent(percent) }
    }

    fun setJavaScriptEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setJavaScriptEnabled(enabled) }
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoplayEnabled(enabled) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(themeMode) }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val profileManager: ProfileManager,
    private val preferencesRepository: PreferencesRepository,
    private val initialProfileId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                application = application,
                profileManager = profileManager,
                preferencesRepository = preferencesRepository,
                initialProfileId = initialProfileId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
