package com.example.ui.theme

import com.example.platform.*
import com.example.resources.*
import org.jetbrains.compose.resources.painterResource

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Poppins es la tipografía geométrica que usa la app oficial de BravoVa.
 */
private val PoppinsFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.poppins_regular, weight = FontWeight.Normal),
        Font(Res.font.poppins_medium, weight = FontWeight.Medium),
        Font(Res.font.poppins_semi_bold, weight = FontWeight.SemiBold),
        Font(Res.font.poppins_bold, weight = FontWeight.Bold),
        Font(Res.font.poppins_extra_bold, weight = FontWeight.ExtraBold),
        Font(Res.font.poppins_extra_bold, weight = FontWeight.Black)
    )

val RoundedDisplayFont: FontFamily
    @Composable get() = PoppinsFamily

val RoundedBodyFont: FontFamily
    @Composable get() = PoppinsFamily

// Nombres antiguos que siguen usando las pantallas; todos apuntan a Poppins.
val PlusJakartaSans: FontFamily
    @Composable get() = RoundedDisplayFont
val InterFont: FontFamily
    @Composable get() = RoundedBodyFont

val Typography: Typography
    @Composable get() = Typography(
    displayLarge = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.05).sp
    ),
    titleLarge = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.05.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.08.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RoundedBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RoundedBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 19.5.sp,
        letterSpacing = 0.14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RoundedBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.18.sp
    )
)
