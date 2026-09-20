package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val IQOODarkColorScheme = darkColorScheme(
    primary = IQOOLime,
    onPrimary = IQOOOnLime,
    primaryContainer = IQOOLimeContainer,
    onPrimaryContainer = IQOOOnLimeContainer,
    secondary = PartialAmber,
    onSecondary = GoldOnSecondary,
    secondaryContainer = PartialAmberContainer,
    onSecondaryContainer = GoldOnSecondaryContainer,
    tertiary = SettledGreen,
    onTertiary = Color(0xFF003822),
    tertiaryContainer = SettledGreenContainer,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = DarkPakkaColors.background,
    onBackground = DarkPakkaColors.textPrimary,
    surface = DarkPakkaColors.surface,
    onSurface = DarkPakkaColors.textPrimary,
    surfaceVariant = DarkPakkaColors.surfaceElevated,
    onSurfaceVariant = DarkPakkaColors.textSecondary,
    outline = DarkPakkaColors.borderMedium,
    outlineVariant = DarkPakkaColors.borderSubtle
)

private val IQOOLightColorScheme = lightColorScheme(
    primary = LightPakkaColors.limePrimary,
    onPrimary = LightPakkaColors.onLimePrimary,
    primaryContainer = LightPakkaColors.limeContainer,
    onPrimaryContainer = LightPakkaColors.onLimeContainer,
    secondary = LightPakkaColors.partialAmber,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LightPakkaColors.partialAmberContainer,
    onSecondaryContainer = LightPakkaColors.partialAmberText,
    tertiary = LightPakkaColors.settledGreen,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = LightPakkaColors.settledGreenContainer,
    onTertiaryContainer = LightPakkaColors.settledGreenText,
    background = LightPakkaColors.background,
    onBackground = LightPakkaColors.textPrimary,
    surface = LightPakkaColors.surface,
    onSurface = LightPakkaColors.textPrimary,
    surfaceVariant = LightPakkaColors.surfaceElevated,
    onSurfaceVariant = LightPakkaColors.textSecondary,
    outline = LightPakkaColors.borderMedium,
    outlineVariant = LightPakkaColors.borderSubtle
)

@Composable
fun PakkaKhataTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep signature electric-lime & graphite palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> IQOODarkColorScheme
        else -> IQOOLightColorScheme
    }

    val pakkaColors = if (darkTheme) DarkPakkaColors else LightPakkaColors

    CompositionLocalProvider(
        LocalPakkaColors provides pakkaColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Backward compatibility alias for existing template tests.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PakkaKhataTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
