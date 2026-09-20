package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// iQOO Native System Feature - Color Palette
// Dynamic & Adaptive Theme Tokens:
// When accessed in Compose, use `PakkaTheme.colors.*` for full Light/Dark support.
// The constants below are kept for backward compatibility and default values.
// =========================================================================

// Primary iQOO Signature Accent (Electric Lime)
val IQOOLime = Color(0xFFB8F34A)
val IQOOLimeLight = Color(0xFFCEF86C)
val IQOOLimeDim = Color(0xFF86B828)
val IQOOLimeContainer = Color(0xFF1E2D0D)
val IQOOOnLime = Color(0xFF091202)
val IQOOOnLimeContainer = Color(0xFFD8FAA4)

// Background & Graphite Surfaces (Dark Defaults)
val BackgroundDark = Color(0xFF070908)        // Deep near-black background
val SurfaceDark = Color(0xFF111512)           // Base card surface
val SurfaceElevatedDark = Color(0xFF171D19)   // Elevated card surface
val SurfaceHigherDark = Color(0xFF202822)     // Popovers, dialogs, sheets
val SurfaceCard = Color(0xFF131814)
val SurfaceCardElevated = Color(0xFF1A211C)

// Borders & Dividers (Dark Defaults)
val BorderSubtleDark = Color(0xFF1F2922)
val BorderMediumDark = Color(0xFF2E3D33)
val BorderAccentDark = Color(0x33B8F34A)

// Typography Colors (Dark Defaults)
val TextPrimary = Color(0xFFF5F7F5)
val TextSecondary = Color(0xFF9BA49F)
val TextTertiary = Color(0xFF68746D)
val TextOnAccent = Color(0xFF091202)

// Status & Financial Indicators
val SettledGreen = Color(0xFF34D399)
val SettledGreenContainer = Color(0xFF063824)
val PartialAmber = Color(0xFFFBBF24)
val PartialAmberContainer = Color(0xFF3D2702)
val OpenRed = Color(0xFFF87171)
val OpenRedContainer = Color(0xFF381010)
val OverpaidBlue = Color(0xFF60A5FA)
val OverpaidBlueContainer = Color(0xFF0C274A)

// Backward-compatible mappings for existing references across codebase
val EmeraldPrimary = IQOOLime
val EmeraldOnPrimary = IQOOOnLime
val EmeraldPrimaryContainer = IQOOLimeContainer
val EmeraldOnPrimaryContainer = IQOOOnLimeContainer

val GoldSecondary = PartialAmber
val GoldOnSecondary = Color(0xFF1A1300)
val GoldSecondaryContainer = PartialAmberContainer
val GoldOnSecondaryContainer = Color(0xFFFFDF88)

val SlateTertiary = OverpaidBlue
val SlateOnTertiary = Color(0xFF001E36)
val SlateTertiaryContainer = OverpaidBlueContainer
val SlateOnTertiaryContainer = Color(0xFFD1E4FF)

// Light theme definitions
val BackgroundLight = Color(0xFFF4F6F4)
val OnBackgroundLight = Color(0xFF121613)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF121613)
val SurfaceVariantLight = Color(0xFFECEFEA)
val OnSurfaceVariantLight = Color(0xFF4A554E)
val OutlineLight = Color(0xFFCAD2C7)
