package com.spotifybrowser.app.ui.screens

import androidx.compose.runtime.Composable
import com.spotifybrowser.app.data.webview.WebViewBrowserHost
import com.spotifybrowser.app.viewmodel.AppUiState

@Composable
fun SpotifyBrowserApp(
    uiState: AppUiState,
    browserHost: WebViewBrowserHost,
    onOpenProfile: (com.spotifybrowser.app.data.profile.BrowserProfile) -> Unit,
    onCreateProfile: (String) -> Unit,
    onRenameProfile: (com.spotifybrowser.app.data.profile.BrowserProfile, String) -> Unit,
    onDeleteProfile: (com.spotifybrowser.app.data.profile.BrowserProfile) -> Unit,
    onBrowserChromeChanged: (com.spotifybrowser.app.data.web.BrowserChromeState) -> Unit,
    onThemeChanged: (com.spotifybrowser.app.data.preferences.ThemeMode) -> Unit
) {
    val activeProfile = uiState.activeProfile
    if (activeProfile == null) {
        ProfileSelectorScreen(
            isReady = uiState.isReady,
            profiles = uiState.profiles,
            lastProfileId = uiState.lastProfileId,
            onOpenProfile = onOpenProfile,
            onCreateProfile = onCreateProfile,
            onRenameProfile = onRenameProfile,
            onDeleteProfile = onDeleteProfile
        )
    } else {
        BrowserScreen(
            activeProfile = activeProfile,
            settings = uiState.settings,
            browserChrome = uiState.browserChrome,
            host = browserHost,
            onBrowserChromeChanged = onBrowserChromeChanged
        )
    }
}
