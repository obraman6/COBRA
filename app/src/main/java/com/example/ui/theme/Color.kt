package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Custom Palette from user
val PaletteCream = Color(0xFFFDF0D5)
val PaletteNavy = Color(0xFF003049)
val PaletteLightBlue = Color(0xFF669BBC)

// Premium Light Theme
val md_theme_light_primary = PaletteNavy
val md_theme_light_onPrimary = PaletteCream
val md_theme_light_secondary = PaletteLightBlue
val md_theme_light_onSecondary = Color(0xFF1E1E1E)
val md_theme_light_background = PaletteCream
val md_theme_light_onBackground = PaletteNavy

// Premium Dark Theme
val md_theme_dark_primary = PaletteLightBlue
val md_theme_dark_onPrimary = PaletteNavy
val md_theme_dark_secondary = PaletteNavy
val md_theme_dark_onSecondary = PaletteCream
val md_theme_dark_background = Color(0xFF121212)
val md_theme_dark_onBackground = PaletteCream

// Board Options
val BoardLight = PaletteCream
val BoardDark = PaletteNavy
val BoardHighlight = PaletteLightBlue.copy(alpha = 0.6f)
val BoardValidMove = PaletteLightBlue.copy(alpha = 0.8f)

val PieceRed = PaletteLightBlue
val PieceWhite = Color(0xFFFDFEFE)
val KingGold = Color(0xFFFFD700)


