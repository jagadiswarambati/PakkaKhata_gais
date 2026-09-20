package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * PakkaKhata Theme Color Tokens.
 * Provides adaptive color values for both Dark (iQOO near-black & electric lime)
 * and Light (soft off-white, crisp white cards, graphite text, lime brand accents).
 *
 * All screens access these tokens to ensure seamless, complete, contrast-checked theming.
 */
data class PakkaColors(
    val isDark: Boolean,

    // Core Canvas & Surfaces
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHigher: Color,
    val surfaceCard: Color,
    val surfaceCardElevated: Color,

    // Borders & Dividers
    val borderSubtle: Color,
    val borderMedium: Color,
    val borderAccent: Color,

    // Typography
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnAccent: Color,

    // Signature Brand Accent
    val limePrimary: Color,
    val limeContainer: Color,
    val onLimePrimary: Color,
    val onLimeContainer: Color,

    // Financial Status Badges & Indicators
    val settledGreen: Color,
    val settledGreenContainer: Color,
    val settledGreenText: Color,

    val partialAmber: Color,
    val partialAmberContainer: Color,
    val partialAmberText: Color,

    val openRed: Color,
    val openRedContainer: Color,
    val openRedText: Color,

    val overpaidBlue: Color,
    val overpaidBlueContainer: Color,
    val overpaidBlueText: Color,

    // Field & Input Surfaces
    val inputBackground: Color,
    val inputBorder: Color
)

/**
 * iQOO-inspired Dark Theme Palette:
 * Near-black OLED canvas, dark graphite cards, electric-lime accent.
 */
val DarkPakkaColors = PakkaColors(
    isDark = true,
    background = Color(0xFF070908),
    surface = Color(0xFF111512),
    surfaceElevated = Color(0xFF171D19),
    surfaceHigher = Color(0xFF202822),
    surfaceCard = Color(0xFF131814),
    surfaceCardElevated = Color(0xFF1A211C),

    borderSubtle = Color(0xFF1F2922),
    borderMedium = Color(0xFF2E3D33),
    borderAccent = Color(0x33B8F34A),

    textPrimary = Color(0xFFF5F7F5),
    textSecondary = Color(0xFF9BA49F),
    textTertiary = Color(0xFF68746D),
    textOnAccent = Color(0xFF091202),

    limePrimary = Color(0xFFB8F34A),
    limeContainer = Color(0xFF1E2D0D),
    onLimePrimary = Color(0xFF091202),
    onLimeContainer = Color(0xFFD8FAA4),

    settledGreen = Color(0xFF34D399),
    settledGreenContainer = Color(0xFF063824),
    settledGreenText = Color(0xFFA7F3D0),

    partialAmber = Color(0xFFFBBF24),
    partialAmberContainer = Color(0xFF3D2702),
    partialAmberText = Color(0xFFFDE68A),

    openRed = Color(0xFFF87171),
    openRedContainer = Color(0xFF381010),
    openRedText = Color(0xFFFECACA),

    overpaidBlue = Color(0xFF60A5FA),
    overpaidBlueContainer = Color(0xFF0C274A),
    overpaidBlueText = Color(0xFFBFDBFE),

    inputBackground = Color(0xFF111512),
    inputBorder = Color(0xFF2E3D33)
)

/**
 * Complete Professional Light Theme Palette:
 * Soft off-white canvas, crisp white container cards, dark graphite typography,
 * deep lime accents and high-contrast status pills for financial legibility.
 */
val LightPakkaColors = PakkaColors(
    isDark = false,
    background = Color(0xFFF4F6F4),       // Soft premium off-white (easy on eyes)
    surface = Color(0xFFFFFFFF),          // Pure white card surface
    surfaceElevated = Color(0xFFECEFEA),  // Subtle elevated chip/surface
    surfaceHigher = Color(0xFFE2E7DF),    // Popover / Sheet headers
    surfaceCard = Color(0xFFFFFFFF),
    surfaceCardElevated = Color(0xFFFFFFFF),

    borderSubtle = Color(0xFFE1E6DF),     // Subtle crisp card border
    borderMedium = Color(0xFFCAD2C7),     // Defined divider/border
    borderAccent = Color(0x4044730A),     // Light accent border

    textPrimary = Color(0xFF121613),      // Dark graphite / near-black
    textSecondary = Color(0xFF4A554E),    // Slate-graphite secondary (4.8:1 contrast)
    textTertiary = Color(0xFF717D75),     // Muted labels
    textOnAccent = Color(0xFFFFFFFF),

    limePrimary = Color(0xFF3E690A),      // Deep electric lime for AAA contrast on light
    limeContainer = Color(0xFFDFF88F),    // Soft lime container
    onLimePrimary = Color(0xFFFFFFFF),
    onLimeContainer = Color(0xFF132302),

    settledGreen = Color(0xFF059669),
    settledGreenContainer = Color(0xFFD1FAE5),
    settledGreenText = Color(0xFF065F46),

    partialAmber = Color(0xFFD97706),
    partialAmberContainer = Color(0xFFFEF3C7),
    partialAmberText = Color(0xFF92400E),

    openRed = Color(0xFFDC2626),
    openRedContainer = Color(0xFFFEE2E2),
    openRedText = Color(0xFF991B1B),

    overpaidBlue = Color(0xFF2563EB),
    overpaidBlueContainer = Color(0xFFDBEAFE),
    overpaidBlueText = Color(0xFF1E40AF),

    inputBackground = Color(0xFFFFFFFF),
    inputBorder = Color(0xFFCAD2C7)
)

val LocalPakkaColors = staticCompositionLocalOf { DarkPakkaColors }

/**
 * Access the active theme colors anywhere in Compose:
 * `PakkaTheme.colors.background`, `PakkaTheme.colors.surface`, etc.
 */
object PakkaTheme {
    val colors: PakkaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPakkaColors.current
}
