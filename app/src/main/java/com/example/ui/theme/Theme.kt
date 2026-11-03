package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ForestUmberPrimary,
    onPrimary = OnForestUmberPrimary,
    primaryContainer = ForestUmberContainer,
    onPrimaryContainer = OnForestUmberContainer,
    secondary = ChanterelleGoldSecondary,
    onSecondary = OnChanterelleGoldSecondary,
    secondaryContainer = ChanterelleGoldContainer,
    onSecondaryContainer = OnChanterelleGoldContainer,
    tertiary = MossGreenTertiary,
    onTertiary = OnMossGreenTertiary,
    tertiaryContainer = MossGreenContainer,
    onTertiaryContainer = OnMossGreenContainer,
    background = ParchmentBackground,
    onBackground = OnParchmentBackground,
    surface = ParchmentSurface,
    onSurface = OnParchmentSurface,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = OnParchmentSurfaceVariant,
    outline = WarmOutline,
    error = StatusUnpaidRed,
    onError = Color.White,
    errorContainer = StatusUnpaidRedBg,
    onErrorContainer = StatusUnpaidRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkChanterellePrimary,
    onPrimary = OnDarkChanterellePrimary,
    primaryContainer = DarkChanterelleContainer,
    onPrimaryContainer = OnDarkChanterelleContainer,
    secondary = DarkGoldSecondary,
    onSecondary = OnDarkGoldSecondary,
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = OnDarkGoldContainer,
    tertiary = DarkMossTertiary,
    onTertiary = OnDarkMossTertiary,
    tertiaryContainer = DarkMossContainer,
    onTertiaryContainer = OnDarkMossContainer,
    background = TruffleBackground,
    onBackground = OnTruffleBackground,
    surface = TruffleSurface,
    onSurface = OnTruffleSurface,
    surfaceVariant = TruffleSurfaceVariant,
    onSurfaceVariant = OnTruffleSurfaceVariant,
    outline = DarkWarmOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
