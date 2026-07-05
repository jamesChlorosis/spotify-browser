package com.spotifybrowser.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.spotifybrowser.app.data.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF0B6B3A),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = androidx.compose.ui.graphics.Color(0xFF4D6355),
    tertiary = androidx.compose.ui.graphics.Color(0xFF38656A),
    background = androidx.compose.ui.graphics.Color(0xFFFBFDF8),
    surface = androidx.compose.ui.graphics.Color(0xFFFBFDF8),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFEFF4ED),
    onSurface = androidx.compose.ui.graphics.Color(0xFF191D19)
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF7DDA9F),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF00391C),
    secondary = androidx.compose.ui.graphics.Color(0xFFB6CCBB),
    tertiary = androidx.compose.ui.graphics.Color(0xFFA0D0D5),
    background = androidx.compose.ui.graphics.Color(0xFF101511),
    surface = androidx.compose.ui.graphics.Color(0xFF101511),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF1C211D),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE0E4DD)
)

@Composable
fun SpotifyBrowserTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

@Composable
fun shouldUseDarkTheme(themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
}
