package com.spotifybrowser.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spotifybrowser.app.data.gecko.ExtensionInstallHost

@Composable
fun ExtensionSetupScreen(
    extensionInstallHost: ExtensionInstallHost,
    onContinue: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var isInstalling by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val installedExtensions = remember { mutableStateListOf<String>() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Extensions", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Install compatible signed Firefox .xpi extensions now, or continue without extensions.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = url,
                onValueChange = {
                    url = it
                    statusMessage = null
                },
                singleLine = true,
                label = { Text("HTTPS .xpi URL") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Uri
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !isInstalling,
                    onClick = onContinue
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Text("Continue")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !isInstalling && url.isNotBlank(),
                    onClick = {
                        isInstalling = true
                        statusMessage = null
                        extensionInstallHost.installExtensionFromUrl(url) { result ->
                            isInstalling = false
                            result
                                .onSuccess { name ->
                                    installedExtensions += name
                                    url = ""
                                    statusMessage = "Installed $name"
                                }
                                .onFailure { error ->
                                    statusMessage = error.message ?: "Extension could not be installed"
                                }
                        }
                    }
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                    Text("Install")
                }
            }

            AnimatedVisibility(visible = statusMessage != null) {
                Text(
                    text = statusMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (installedExtensions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Installed", style = MaterialTheme.typography.titleMedium)
                    installedExtensions.forEach { name ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(name)
                        }
                    }
                }
            }

            Text(
                "Extensions requesting Spotify, all-site, proxy, or request-blocking permissions are rejected for compliance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
