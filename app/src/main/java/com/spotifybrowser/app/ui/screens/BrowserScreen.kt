package com.spotifybrowser.app.ui.screens

import android.net.Uri
import android.widget.Toast
import android.webkit.CookieManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.spotifybrowser.app.data.preferences.BrowserSettings
import com.spotifybrowser.app.data.preferences.ThemeMode
import com.spotifybrowser.app.data.profile.BrowserProfile
import com.spotifybrowser.app.data.web.BrowserChromeState
import com.spotifybrowser.app.data.web.SpotifyUrls
import com.spotifybrowser.app.data.webview.WebViewBrowserFactory
import com.spotifybrowser.app.data.webview.WebViewBrowserHandle
import com.spotifybrowser.app.data.webview.WebViewBrowserHost
import com.spotifybrowser.app.data.webview.applyBrowserSettings
import com.spotifybrowser.app.ui.components.DeleteProfileDialog
import com.spotifybrowser.app.ui.components.ProfileList
import com.spotifybrowser.app.ui.components.ProfileNameDialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    activeProfile: BrowserProfile,
    profiles: List<BrowserProfile>,
    settings: BrowserSettings,
    browserChrome: BrowserChromeState,
    host: WebViewBrowserHost,
    onBrowserChromeChanged: (BrowserChromeState) -> Unit,
    onOpenProfile: (BrowserProfile) -> Unit,
    onCreateProfile: (String) -> Unit,
    onRenameProfile: (BrowserProfile, String) -> Unit,
    onDeleteProfile: (BrowserProfile) -> Unit,
    onDesktopUserAgentChanged: (Boolean) -> Unit,
    onZoomChanged: (Int) -> Unit,
    onJavaScriptChanged: (Boolean) -> Unit,
    onAutoplayChanged: (Boolean) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val browserFactory = remember(host) { WebViewBrowserFactory(host) }

    var browserHandle by remember { mutableStateOf<WebViewBrowserHandle?>(null) }
    var controlsVisible by remember { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(false) }
    var profilesVisible by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableIntStateOf(0) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(controlsVisible, settingsVisible, profilesVisible) {
        if (controlsVisible && !settingsVisible && !profilesVisible) {
            delay(4_000)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        key(activeProfile.id, reloadToken) {
            var localHandle by remember { mutableStateOf<WebViewBrowserHandle?>(null) }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { androidContext ->
                    val handle = browserFactory.create(
                        context = androidContext,
                        profile = activeProfile,
                        settings = settings,
                        onStateChanged = onBrowserChromeChanged
                    )
                    browserHandle = handle
                    localHandle = handle
                    handle.view
                },
                update = {
                    localHandle?.view?.applyBrowserSettings(settings)
                }
            )

            DisposableEffect(Unit) {
                onDispose {
                    localHandle?.dispose()
                    if (browserHandle === localHandle) {
                        browserHandle = null
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(Unit) {
                    detectTapGestures { controlsVisible = true }
                }
        )

        if (browserChrome.isLoading && !controlsVisible) {
            LinearProgressIndicator(
                progress = { browserChrome.progress / 100f },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )
        }

        browserChrome.error?.let { error ->
            BrowserErrorOverlay(
                message = error.description,
                onRetry = {
                    val handle = browserHandle
                    if (handle == null) {
                        reloadToken += 1
                    } else {
                        handle.loadUri(error.failingUrl ?: SpotifyUrls.HOME)
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible || browserChrome.error != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            BrowserControlDock(
                browserChrome = browserChrome,
                onBack = { browserHandle?.goBack() },
                onForward = { browserHandle?.goForward() },
                onRefresh = { browserHandle?.reload() },
                onOpenSpotify = {
                    val url = browserChrome.currentUrl.takeIf { it.isNotBlank() } ?: SpotifyUrls.HOME
                    host.openSpotifyInCompatibleBrowser(Uri.parse(url))
                },
                onSignIn = { host.openSpotifyInCompatibleBrowser(Uri.parse(SpotifyUrls.LOGIN)) },
                onExtensions = { host.openExtensionUrl(SpotifyUrls.FIREFOX_ADDONS) },
                onProfiles = {
                    profilesVisible = true
                    controlsVisible = true
                },
                onSettings = {
                    settingsVisible = true
                    controlsVisible = true
                }
            )
        }
    }

    if (settingsVisible) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { settingsVisible = false }
        ) {
            SettingsPanel(
                settings = settings,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                onDesktopUserAgentChanged = onDesktopUserAgentChanged,
                onZoomChanged = onZoomChanged,
                onJavaScriptChanged = onJavaScriptChanged,
                onAutoplayChanged = onAutoplayChanged,
                onThemeChanged = onThemeChanged,
                onClearCache = {
                    browserHandle?.clearCache()
                    Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                },
                onClearCookies = {
                    CookieManager.getInstance().removeAllCookies {
                        CookieManager.getInstance().flush()
                        Toast.makeText(context, "Cookies cleared", Toast.LENGTH_SHORT).show()
                    }
                },
                onClearProfile = {
                    browserHandle?.clearBrowsingData {
                        Toast.makeText(context, "Profile cleared", Toast.LENGTH_SHORT).show()
                    }
                    reloadToken += 1
                }
            )
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (profilesVisible) {
        ProfileManagerSheet(
            profiles = profiles,
            activeProfile = activeProfile,
            sheetState = sheetState,
            onDismiss = { profilesVisible = false },
            onOpenProfile = onOpenProfile,
            onCreateProfile = onCreateProfile,
            onRenameProfile = onRenameProfile,
            onDeleteProfile = { profile ->
                if (profile.id == activeProfile.id) {
                    browserHandle = null
                }
                onDeleteProfile(profile)
            }
        )
    }
}

@Composable
private fun BrowserControlDock(
    browserChrome: BrowserChromeState,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSpotify: () -> Unit,
    onSignIn: () -> Unit,
    onExtensions: () -> Unit,
    onProfiles: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column {
            if (browserChrome.isLoading) {
                LinearProgressIndicator(
                    progress = { browserChrome.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    enabled = browserChrome.canGoBack,
                    onClick = onBack
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                IconButton(
                    enabled = browserChrome.canGoForward,
                    onClick = onForward
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = onOpenSpotify) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in compatible browser")
                }
                IconButton(onClick = onSignIn) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Sign in")
                }
                IconButton(onClick = onExtensions) {
                    Icon(Icons.Default.Extension, contentDescription = "Extensions")
                }
                IconButton(onClick = onProfiles) {
                    Icon(Icons.Default.Groups, contentDescription = "Profiles")
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    }
}

@Composable
private fun BrowserErrorOverlay(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Spotify could not load", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onRetry) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Retry")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileManagerSheet(
    profiles: List<BrowserProfile>,
    activeProfile: BrowserProfile,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onOpenProfile: (BrowserProfile) -> Unit,
    onCreateProfile: (String) -> Unit,
    onRenameProfile: (BrowserProfile, String) -> Unit,
    onDeleteProfile: (BrowserProfile) -> Unit
) {
    var createDialogOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<BrowserProfile?>(null) }
    var deleteTarget by remember { mutableStateOf<BrowserProfile?>(null) }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text("Profiles", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Manage saved Spotify browser profiles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            ProfileList(
                profiles = profiles,
                selectedProfileId = activeProfile.id,
                activeProfileId = activeProfile.id,
                onOpenProfile = {
                    onDismiss()
                    onOpenProfile(it)
                },
                onCreateProfile = { createDialogOpen = true },
                onRenameProfile = { renameTarget = it },
                onDeleteProfile = { deleteTarget = it }
            )
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (createDialogOpen) {
        ProfileNameDialog(
            title = "Create profile",
            confirmLabel = "Create",
            onDismiss = { createDialogOpen = false },
            onConfirm = {
                createDialogOpen = false
                onCreateProfile(it)
            }
        )
    }

    renameTarget?.let { profile ->
        ProfileNameDialog(
            title = "Rename profile",
            initialName = profile.name,
            confirmLabel = "Save",
            onDismiss = { renameTarget = null },
            onConfirm = {
                renameTarget = null
                onRenameProfile(profile, it)
            }
        )
    }

    deleteTarget?.let { profile ->
        DeleteProfileDialog(
            profile = profile,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteProfile(profile)
            }
        )
    }
}
