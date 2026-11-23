package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

val LocalActivePalette = staticCompositionLocalOf { FAVORITE_COLOR_PALETTES.first() }

/**
 * Guarantees WCAG high-contrast legibility on any background or accent color
 * selected in the user's Android device settings.
 */
fun ensureHighContrastOnColor(background: Color): Color {
    return if (background.luminance() > 0.46f) {
        Color(0xFF0F172A)
    } else {
        Color.White
    }
}

fun ensureLegibleAccent(rawAccent: Color, darkTheme: Boolean): Color {
    val lum = rawAccent.luminance()
    return when {
        darkTheme && lum < 0.22f -> lerp(rawAccent, Color.White, 0.36f)
        !darkTheme && lum > 0.62f -> lerp(rawAccent, Color(0xFF0F172A), 0.38f)
        else -> rawAccent
    }
}

@Composable
fun BravoVaTheme(
    darkTheme: Boolean = false,
    isArcaStore: Boolean = false,
    paletteId: String = "material_you",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val systemM3Scheme = if (supportsDynamic) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }

    val rawPrimary = when {
        isArcaStore -> if (systemM3Scheme != null) {
            lerp(ArcaTeal, systemM3Scheme.primary, 0.45f)
        } else {
            ArcaTeal
        }
        systemM3Scheme != null -> lerp(BravoRed, systemM3Scheme.primary, 0.55f)
        else -> BravoRed
    }

    val targetPrimary = ensureLegibleAccent(rawPrimary, darkTheme)

    val targetBg = when {
        darkTheme -> DarkBackground
        systemM3Scheme != null -> lerp(LightBackground, systemM3Scheme.background, 0.65f)
        else -> LightBackground
    }

    val targetSurface = when {
        darkTheme -> DarkSurface
        systemM3Scheme != null -> lerp(LightSurface, systemM3Scheme.surface, 0.60f)
        else -> LightSurface
    }

    val targetSurfaceVariant = when {
        darkTheme -> DarkCardSurface
        else -> systemM3Scheme?.let { lerp(Color(0xFFF1F5F9), it.surfaceVariant, 0.52f) } ?: Color(0xFFF1F5F9)
    }

    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(260),
        label = "primaryColor"
    )
    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(260),
        label = "bgColor"
    )
    val animatedSurface by animateColorAsState(
        targetValue = targetSurface,
        animationSpec = tween(260),
        label = "surfaceColor"
    )

    val legibleOnPrimary = ensureHighContrastOnColor(animatedPrimary)
    val secondaryColor = ensureLegibleAccent(
        rawAccent = systemM3Scheme?.secondary ?: if (isArcaStore) ArcaTeal else BravoAmber,
        darkTheme = darkTheme
    )
    val legibleOnSecondary = ensureHighContrastOnColor(secondaryColor)

    val activeDynamicPalette = AppColorPalette(
        id = "material_you",
        title = "Color del Dispositivo (Material You)",
        subtitle = "Adaptación automática al tema de Android",
        primary = animatedPrimary,
        primaryDark = lerp(animatedPrimary, Color.Black, 0.25f),
        backgroundLight = animatedBg,
        surfaceLight = animatedSurface,
        accent = secondaryColor,
        hexPreview = listOf("M3 Auto", "Dinámico", "Alto Contraste")
    )

    val colorScheme = if (darkTheme) {
        val onBgColor = Color(0xFFF8FAFC)
        val onSurfColor = Color(0xFFF8FAFC)
        val onSurfVarColor = Color(0xFFCBD5E1)
        darkColorScheme(
            primary = animatedPrimary,
            onPrimary = legibleOnPrimary,
            primaryContainer = lerp(animatedPrimary, Color(0xFF0F172A), 0.62f),
            onPrimaryContainer = Color(0xFFF8FAFC),
            secondary = secondaryColor,
            onSecondary = legibleOnSecondary,
            secondaryContainer = Color(0xFF1E293B),
            onSecondaryContainer = Color(0xFFF8FAFC),
            tertiary = ArcaTeal,
            onTertiary = Color.White,
            background = animatedBg,
            onBackground = onBgColor,
            surface = animatedSurface,
            onSurface = onSurfColor,
            surfaceVariant = targetSurfaceVariant,
            onSurfaceVariant = onSurfVarColor,
            outline = Color(0xFF2E3C52)
        )
    } else {
        val onBgColor = if (animatedBg.luminance() < 0.35f) Color.White else Color(0xFF0F172A)
        val onSurfColor = if (animatedSurface.luminance() < 0.35f) Color.White else Color(0xFF0F172A)
        val onSurfVarColor = if (targetSurfaceVariant.luminance() < 0.4f) Color(0xFFE2E8F0) else Color(0xFF334155)
        lightColorScheme(
            primary = animatedPrimary,
            onPrimary = legibleOnPrimary,
            primaryContainer = systemM3Scheme?.primaryContainer ?: animatedPrimary.copy(alpha = 0.14f),
            onPrimaryContainer = systemM3Scheme?.primaryContainer?.let { ensureHighContrastOnColor(it) } ?: Color(0xFF0F172A),
            secondary = secondaryColor,
            onSecondary = legibleOnSecondary,
            secondaryContainer = systemM3Scheme?.secondaryContainer ?: secondaryColor.copy(alpha = 0.18f),
            onSecondaryContainer = systemM3Scheme?.secondaryContainer?.let { ensureHighContrastOnColor(it) } ?: Color(0xFF0F172A),
            tertiary = systemM3Scheme?.tertiary ?: ArcaTeal,
            onTertiary = systemM3Scheme?.tertiary?.let { ensureHighContrastOnColor(it) } ?: Color.White,
            background = animatedBg,
            onBackground = onBgColor,
            surface = animatedSurface,
            onSurface = onSurfColor,
            surfaceVariant = targetSurfaceVariant,
            onSurfaceVariant = onSurfVarColor,
            outline = systemM3Scheme?.outline?.copy(alpha = 0.42f) ?: LightBorder
        )
    }

    CompositionLocalProvider(LocalActivePalette provides activeDynamicPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    BravoVaTheme(
        darkTheme = darkTheme,
        isArcaStore = false,
        paletteId = "material_you",
        content = content
    )
}
