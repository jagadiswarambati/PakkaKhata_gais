package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderMediumDark,
    outlineVariant = BorderSubtleDark
)

private val IQOOLightColorScheme = lightColorScheme(
    primary = Color(0xFF436B0B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCEF86C),
    onPrimaryContainer = Color(0xFF132002),
    secondary = PartialAmber,
    onSecondary = GoldOnSecondary,
    secondaryContainer = GoldSecondaryContainer,
    onSecondaryContainer = GoldOnSecondaryContainer,
    tertiary = SlateTertiary,
    onTertiary = SlateOnTertiary,
    tertiaryContainer = SlateTertiaryContainer,
    onTertiaryContainer = SlateOnTertiaryContainer,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

@Composable
fun PakkaKhataTheme(
    darkTheme: Boolean = true, // Dark-first iQOO system aesthetic
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
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
