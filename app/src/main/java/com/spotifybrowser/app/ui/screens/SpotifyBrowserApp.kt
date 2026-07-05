package com.spotifybrowser.app.ui.screens

import androidx.compose.runtime.Composable
import com.spotifybrowser.app.data.gecko.ExtensionInstallHost
import com.spotifybrowser.app.data.gecko.GeckoBrowserHost
import com.spotifybrowser.app.ui.theme.shouldUseDarkTheme
import com.spotifybrowser.app.viewmodel.AppUiState

@Composable
fun SpotifyBrowserApp(
    uiState: AppUiState,
    browserHost: GeckoBrowserHost,
    extensionInstallHost: ExtensionInstallHost,
    onOpenProfile: (com.spotifybrowser.app.data.profile.BrowserProfile) -> Unit,
    onCreateProfile: (String) -> Unit,
    onRenameProfile: (com.spotifybrowser.app.data.profile.BrowserProfile, String) -> Unit,
    onDeleteProfile: (com.spotifybrowser.app.data.profile.BrowserProfile) -> Unit,
    onBrowserChromeChanged: (com.spotifybrowser.app.data.web.BrowserChromeState) -> Unit,
    onExtensionSetupFinished: () -> Unit,
    onDesktopUserAgentChanged: (Boolean) -> Unit,
    onZoomChanged: (Int) -> Unit,
    onJavaScriptChanged: (Boolean) -> Unit,
    onAutoplayChanged: (Boolean) -> Unit,
    onThemeChanged: (com.spotifybrowser.app.data.preferences.ThemeMode) -> Unit
) {
    if (!uiState.extensionSetupCompleted) {
        ExtensionSetupScreen(
            extensionInstallHost = extensionInstallHost,
            onContinue = onExtensionSetupFinished
        )
        return
    }

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
            profiles = uiState.profiles,
            settings = uiState.settings,
            browserChrome = uiState.browserChrome,
            host = browserHost,
            onBrowserChromeChanged = onBrowserChromeChanged,
            onOpenProfile = onOpenProfile,
            onCreateProfile = onCreateProfile,
            onRenameProfile = onRenameProfile,
            onDeleteProfile = onDeleteProfile,
            onDesktopUserAgentChanged = onDesktopUserAgentChanged,
            onZoomChanged = onZoomChanged,
            onJavaScriptChanged = onJavaScriptChanged,
            onAutoplayChanged = onAutoplayChanged,
            onThemeChanged = onThemeChanged
        )
    }
}
