package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NuvPrimaryDark,
    onPrimary = NuvOnPrimaryDark,
    primaryContainer = NuvPrimaryContainerDark,
    onPrimaryContainer = NuvOnPrimaryContainerDark,
    secondary = NuvSecondaryDark,
    onSecondary = NuvOnSecondaryDark,
    secondaryContainer = NuvSecondaryContainerDark,
    onSecondaryContainer = NuvOnSecondaryContainerDark,
    background = NuvBackgroundDark,
    onBackground = NuvOnBackgroundDark,
    surface = NuvSurfaceDark,
    onSurface = NuvOnSurfaceDark,
    surfaceVariant = NuvSurfaceVariantDark,
    onSurfaceVariant = NuvOnSurfaceVariantDark,
    outline = NuvOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = NuvPrimaryLight,
    onPrimary = NuvOnPrimaryLight,
    primaryContainer = NuvPrimaryContainerLight,
    onPrimaryContainer = NuvOnPrimaryContainerLight,
    secondary = NuvSecondaryLight,
    onSecondary = NuvOnSecondaryLight,
    secondaryContainer = NuvSecondaryContainerLight,
    onSecondaryContainer = NuvOnSecondaryContainerLight,
    tertiary = NuvTertiaryLight,
    onTertiary = NuvOnTertiaryLight,
    tertiaryContainer = NuvTertiaryContainerLight,
    onTertiaryContainer = NuvOnTertiaryContainerLight,
    background = NuvBackgroundLight,
    onBackground = NuvOnBackgroundLight,
    surface = NuvSurfaceLight,
    onSurface = NuvOnSurfaceLight,
    surfaceVariant = NuvSurfaceVariantLight,
    onSurfaceVariant = NuvOnSurfaceVariantLight,
    outline = NuvOutlineLight,
    outlineVariant = NuvOutlineVariantLight,
    error = NuvErrorLight,
    errorContainer = NuvErrorContainerLight,
    onErrorContainer = NuvOnErrorContainerLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to preserve DialFlow's signature brand styling
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
