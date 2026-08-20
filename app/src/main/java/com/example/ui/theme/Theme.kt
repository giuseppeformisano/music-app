package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RadicalDarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = BlackPitch,
    primaryContainer = DarkGraphite,
    onPrimaryContainer = PureWhite,
    secondary = SpotifyGreen,
    onSecondary = BlackPitch,
    secondaryContainer = BlackCard,
    onSecondaryContainer = PureWhite,
    tertiary = ElectricPink,
    background = BlackPitch,
    onBackground = PureWhite,
    surface = BlackPitch,
    onSurface = PureWhite,
    surfaceVariant = BlackSurface,
    onSurfaceVariant = SubtitleGray,
    outline = CharcoalBorder,
    outlineVariant = DarkGraphite
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = RadicalDarkColorScheme,
        typography = Typography,
        content = content
    )
}

