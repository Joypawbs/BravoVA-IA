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
import androidx.compose.ui.unit.em
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

/*
 * Interlineado en em (proporción del tamaño de letra), no en sp fijos.
 * Con sp, un Text que cambiaba fontSize sin cambiar lineHeight heredaba el interlineado del
 * estilo (bodyLarge: 22 sp). Un texto de 12 sp quedaba con líneas separadísimas (login,
 * tarjetas, avisos). En em se escala solo. Con el tamaño propio de cada estilo, el resultado
 * es el mismo que antes (p. ej. bodyLarge: 15 sp × 1.4667 = 22 sp).
 */
val Typography: Typography
    @Composable get() = Typography(
    displayLarge = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 1.25.em,      // 40 sp
        letterSpacing = (-0.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 1.28.em,      // 32 sp
        letterSpacing = (-0.1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 1.3333.em,    // 28 sp
        letterSpacing = (-0.05).sp
    ),
    titleLarge = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 1.3333.em,    // 24 sp
        letterSpacing = 0.05.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 1.4194.em,    // 22 sp
        letterSpacing = 0.08.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 1.4074.em,    // 19 sp
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RoundedBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 1.4667.em,    // 22 sp
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RoundedBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 1.4444.em,    // 19.5 sp
        letterSpacing = 0.14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RoundedBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 1.4167.em,    // 17 sp
        letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 1.3846.em,    // 18 sp
        letterSpacing = 0.18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 1.3333.em,    // 16 sp
        letterSpacing = 0.16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RoundedDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 1.3333.em,    // 16 sp
        letterSpacing = 0.18.sp
    )
)
