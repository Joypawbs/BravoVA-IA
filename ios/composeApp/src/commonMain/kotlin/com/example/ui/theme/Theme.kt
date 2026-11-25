package com.example.ui.theme

import com.example.platform.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

val LocalActivePalette = staticCompositionLocalOf { FAVORITE_COLOR_PALETTES.first() }

/**
 * Devuelve blanco o casi negro según el fondo, para que el texto siempre se lea
 * (también con los colores dinámicos de Android).
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
    // Android 12+: Material You completo (colores del fondo de pantalla del usuario).
    // iOS y Android antiguos: la paleta oficial de Bravo.
    // Los tokens (Tone) leen este estado para elegir su versión clara u oscura
    if (ThemeState.dark != darkTheme) ThemeState.dark = darkTheme
    val dynamicScheme = platformDynamicColorScheme(darkTheme)
    val baseScheme = when {
        dynamicScheme != null && isArcaStore ->
            dynamicScheme.copy(primary = ensureLegibleAccent(lerp(ArcaTeal, dynamicScheme.primary, 0.45f), darkTheme))
        dynamicScheme != null -> dynamicScheme
        darkTheme -> bravoDarkScheme(if (isArcaStore) ArcaEmerald else Color(0xFFFF5446))
        else -> bravoLightScheme(if (isArcaStore) ArcaTeal else BravoRed)
    }

    val animatedPrimary by animateColorAsState(baseScheme.primary, tween(260), label = "primaryColor")
    val animatedBg by animateColorAsState(baseScheme.background, tween(260), label = "bgColor")
    val animatedSurface by animateColorAsState(baseScheme.surface, tween(260), label = "surfaceColor")
    val colorScheme = baseScheme.copy(
        primary = animatedPrimary,
        background = animatedBg,
        surface = animatedSurface
    )

    val activeDynamicPalette = AppColorPalette(
        id = "material_you",
        title = if (dynamicScheme != null) "Color del dispositivo (Material You)" else "Bravo oficial",
        subtitle = if (dynamicScheme != null) "Se adapta al fondo de pantalla de Android" else "Rojo y naranja de la marca",
        primary = animatedPrimary,
        primaryDark = lerp(animatedPrimary, Color.Black, 0.25f),
        backgroundLight = animatedBg,
        surfaceLight = animatedSurface,
        accent = colorScheme.secondary,
        hexPreview = listOf("M3 Auto", "Dinámico", "Alto Contraste")
    )

    CompositionLocalProvider(LocalActivePalette provides activeDynamicPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private fun bravoLightScheme(primary: Color) = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = BravoRedSoft,
    onPrimaryContainer = Color(0xFF8A130C),
    secondary = BravoOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDE3),
    onSecondaryContainer = Color(0xFF7A2E0B),
    tertiary = ArcaTeal,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF1F2F4),
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFB),
    surfaceContainer = Color(0xFFF4F5F7),
    surfaceContainerHigh = Color(0xFFEEEFF2),
    surfaceContainerHighest = Color(0xFFE8E9ED),
    outline = Color(0xFFD9DCE1),
    outlineVariant = LightBorder,
    error = Color(0xFFB3261E),
    onError = Color.White
)

private fun bravoDarkScheme(primary: Color) = darkColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B1210),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFFF8A57),
    onSecondary = Color(0xFF3A1200),
    secondaryContainer = Color(0xFF3A1D10),
    onSecondaryContainer = Color(0xFFFFDBCB),
    tertiary = ArcaEmerald,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = Color(0xFF0F1012),
    surfaceContainerLow = Color(0xFF141518),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF1F2024),
    surfaceContainerHighest = Color(0xFF292A2F),
    outline = Color(0xFF3A3C43),
    outlineVariant = DarkBorder,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

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
