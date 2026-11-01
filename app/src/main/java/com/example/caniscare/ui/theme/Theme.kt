package com.example.caniscare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = CanisPrimary,
    onPrimary = CanisOnPrimary,
    primaryContainer = CanisPrimaryContainer,
    onPrimaryContainer = CanisOnPrimaryContainer,
    secondary = CanisSecondary,
    onSecondary = CanisOnSecondary,
    secondaryContainer = CanisSecondaryContainer,
    onSecondaryContainer = CanisOnSecondaryContainer,
    tertiary = CanisTertiary,
    onTertiary = CanisOnTertiary,
    background = CanisBackground,
    onBackground = CanisOnBackground,
    surface = CanisSurface,
    onSurface = CanisOnSurface,
    error = CanisError,
    onError = CanisOnError,
    errorContainer = CanisErrorContainer,
    onErrorContainer = CanisOnErrorContainer,
    outline = CanisOutline,
    surfaceVariant = CanisSurfaceVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = CanisPrimary,
    onPrimary = CanisOnPrimary,
    primaryContainer = CanisOnPrimaryContainer,
    onPrimaryContainer = CanisPrimaryContainer,
    secondary = CanisSecondary,
    onSecondary = CanisOnSecondary,
    background = CanisOnBackground,
    surface = CanisOnBackground
)

@Composable
fun CanisCareTheme(
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
