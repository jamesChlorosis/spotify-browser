package com.spotifybrowser.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spotifybrowser.app.data.profile.BrowserProfile
import com.spotifybrowser.app.ui.components.DeleteProfileDialog
import com.spotifybrowser.app.ui.components.ProfileList
import com.spotifybrowser.app.ui.components.ProfileNameDialog

@Composable
fun ProfileSelectorScreen(
    isReady: Boolean,
    profiles: List<BrowserProfile>,
    lastProfileId: String?,
    onOpenProfile: (BrowserProfile) -> Unit,
    onCreateProfile: (String) -> Unit,
    onRenameProfile: (BrowserProfile, String) -> Unit,
    onDeleteProfile: (BrowserProfile) -> Unit
) {
    var createDialogOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<BrowserProfile?>(null) }
    var deleteTarget by remember { mutableStateOf<BrowserProfile?>(null) }
    var selectedProfileId by remember { mutableStateOf(lastProfileId) }

    LaunchedEffect(profiles, lastProfileId) {
        selectedProfileId = lastProfileId ?: profiles.firstOrNull()?.id
    }

    Scaffold { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val maxContentWidth = if (maxWidth > 720.dp) 620.dp else maxWidth

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isReady) {
                        CircularProgressIndicator()
                    } else {
                        Column(
                            modifier = Modifier
                                .widthIn(max = maxContentWidth)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = "Spotify Browser",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose a profile to continue.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(28.dp))

                            ProfileList(
                                profiles = profiles,
                                selectedProfileId = selectedProfileId,
                                activeProfileId = null,
                                contentPadding = PaddingValues(bottom = 24.dp),
                                onOpenProfile = { profile ->
                                    selectedProfileId = profile.id
                                    onOpenProfile(profile)
                                },
                                onCreateProfile = { createDialogOpen = true },
                                onRenameProfile = { renameTarget = it },
                                onDeleteProfile = { deleteTarget = it }
                            )
                        }
                    }
                }
            }
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
