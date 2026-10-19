package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SpotifyColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyBlack,
    primaryContainer = SpotifyCardBackground,
    onPrimaryContainer = SpotifyTextPrimary,
    secondary = SpotifyGreen,
    onSecondary = SpotifyBlack,
    secondaryContainer = SpotifyElevated,
    onSecondaryContainer = SpotifyTextPrimary,
    background = SpotifyBlack,
    onBackground = SpotifyTextPrimary,
    surface = SpotifyCardBackground,
    onSurface = SpotifyTextPrimary,
    surfaceVariant = SpotifyElevated,
    onSurfaceVariant = SpotifyTextSecondary,
    outline = SpotifyDivider,
    outlineVariant = SpotifyGlassBorder
)

@Composable
fun SongPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpotifyColorScheme,
        typography = Typography,
        content = content
    )
}
