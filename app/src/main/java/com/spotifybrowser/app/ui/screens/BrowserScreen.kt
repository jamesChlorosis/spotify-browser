package com.spotifybrowser.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.spotifybrowser.app.data.preferences.BrowserSettings
import com.spotifybrowser.app.data.profile.BrowserProfile
import com.spotifybrowser.app.data.web.BrowserChromeState
import com.spotifybrowser.app.data.web.SpotifyUrls
import com.spotifybrowser.app.data.webview.WebViewBrowserFactory
import com.spotifybrowser.app.data.webview.WebViewBrowserHandle
import com.spotifybrowser.app.data.webview.WebViewBrowserHost
import com.spotifybrowser.app.data.webview.applyBrowserSettings

@Composable
fun BrowserScreen(
    activeProfile: BrowserProfile,
    settings: BrowserSettings,
    browserChrome: BrowserChromeState,
    host: WebViewBrowserHost,
    onBrowserChromeChanged: (BrowserChromeState) -> Unit
) {
    val browserFactory = remember(host) { WebViewBrowserFactory(host) }

    var browserHandle by remember { mutableStateOf<WebViewBrowserHandle?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }

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
                        settings = settings,
                        onStateChanged = onBrowserChromeChanged,
                        onRendererGone = {
                            browserHandle = null
                            reloadToken += 1
                        }
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
                    if (localHandle?.view === browserHandle?.view) {
                        host.setActiveWebView(null)
                    }
                    localHandle?.dispose()
                    if (browserHandle === localHandle) {
                        browserHandle = null
                    }
                }
            }
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
