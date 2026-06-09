package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

// Set of Material typography styles to start with
val defaultTypography = Typography()
val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = PoppinsFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = PoppinsFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = PoppinsFontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = PoppinsFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = PoppinsFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = PoppinsFontFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = PoppinsFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = PoppinsFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = PoppinsFontFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = PoppinsFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = PoppinsFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = PoppinsFontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = PoppinsFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = PoppinsFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = PoppinsFontFamily)
)
