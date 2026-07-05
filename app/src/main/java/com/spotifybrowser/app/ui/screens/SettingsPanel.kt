package com.spotifybrowser.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spotifybrowser.app.data.preferences.BrowserSettings
import com.spotifybrowser.app.data.preferences.ThemeMode

@Composable
fun SettingsPanel(
    settings: BrowserSettings,
    modifier: Modifier = Modifier,
    onDesktopUserAgentChanged: (Boolean) -> Unit,
    onZoomChanged: (Int) -> Unit,
    onJavaScriptChanged: (Boolean) -> Unit,
    onAutoplayChanged: (Boolean) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onClearCache: () -> Unit,
    onClearCookies: () -> Unit,
    onClearProfile: () -> Unit
) {
    var confirmClearProfile by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        SettingRow(
            title = "Desktop user agent",
            subtitle = "Request Spotify's desktop web layout.",
            trailing = {
                Switch(
                    checked = settings.useDesktopUserAgent,
                    onCheckedChange = onDesktopUserAgentChanged
                )
            }
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Default zoom: ${settings.defaultZoomPercent}%",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settings.defaultZoomPercent.toFloat(),
                onValueChange = { onZoomChanged(it.toInt()) },
                valueRange = 75f..150f,
                steps = 14
            )
        }

        SettingRow(
            title = "JavaScript",
            subtitle = "Spotify Web Player requires JavaScript for normal use.",
            trailing = {
                Switch(
                    checked = settings.javaScriptEnabled,
                    onCheckedChange = onJavaScriptChanged
                )
            }
        )

        SettingRow(
            title = "Autoplay",
            subtitle = "Keep media active when the browser engine allows it.",
            trailing = {
                Switch(
                    checked = settings.autoplayEnabled,
                    onCheckedChange = onAutoplayChanged
                )
            }
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.values().forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onThemeChanged(mode) },
                        label = { Text(mode.name) }
                    )
                }
            }
        }

        Divider()

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClearCache
        ) {
            Text("Clear cache")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClearCookies
        ) {
            Text("Clear cookies")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { confirmClearProfile = true }
        ) {
            Text("Clear current profile")
        }
    }

    if (confirmClearProfile) {
        AlertDialog(
            onDismissRequest = { confirmClearProfile = false },
            title = { Text("Clear current profile?") },
            text = {
                Text("This removes cookies, site storage, history, form data, and cache for the active profile.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmClearProfile = false
                        onClearProfile()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearProfile = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}
