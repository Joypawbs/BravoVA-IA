package com.example.ui.theme

import com.example.platform.*

import androidx.compose.ui.graphics.Color

val BravoRed = Color(0xFFD9232A)
val BravoRedDark = Color(0xFF991B1B)
val BravoRedSoft = Color(0xFFFFE4E6)
val BravoAmber = Color(0xFFF59E0B)
val BravoAmberSoft = Color(0xFFFFF7ED)
val BravoAmberText = Color(0xFF9A3412)
val BravoGreen = Color(0xFF16A34A)
val BravoLime = Color(0xFF84CC16)
val BravoEmerald = Color(0xFF10B981)

val ArcaTeal = Color(0xFF0F766E)
val ArcaTealDark = Color(0xFF115E59)
val ArcaTealSoft = Color(0xFFCCFBF1)
val ArcaEmerald = Color(0xFF14B8A6)

val LightBackground = Color(0xFFFFF7ED)
val LightSurface = Color(0xFFFFFFFF)
val LightCardSurface = Color(0xFAFFFFFF)
val LightBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)

val DarkBackground = Color(0xFF090D16)
val DarkSurface = Color(0xFF111827)
val DarkCardSurface = Color(0xFF162032)
val DarkInputSurface = Color(0xFF1E293B)
val DarkBorder = Color(0xFF283548)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

data class AppColorPalette(
    val id: String,
    val title: String,
    val subtitle: String,
    val primary: Color,
    val primaryDark: Color,
    val backgroundLight: Color,
    val surfaceLight: Color,
    val accent: Color,
    val hexPreview: List<String>
)

val FAVORITE_COLOR_PALETTES = listOf(
    AppColorPalette(
        id = "material_you",
        title = "Material Design 3 (Dispositivo)",
        subtitle = "Dinámico Android • Bravo Oficial",
        primary = Color(0xFFD9232A),
        primaryDark = Color(0xFF991B1B),
        backgroundLight = Color(0xFFFFF7ED),
        surfaceLight = Color(0xFFFFFFFF),
        accent = Color(0xFFF59E0B),
        hexPreview = listOf("M3 Auto", "#D9232A", "#F59E0B")
    ),
    AppColorPalette(
        id = "crimson_cream",
        title = "Crimson Red & Warm Cream",
        subtitle = "#BE123C • #FFF7ED • #FDBA74",
        primary = Color(0xFFBE123C),
        primaryDark = Color(0xFF881337),
        backgroundLight = Color(0xFFFFF7ED),
        surfaceLight = Color(0xFFFFFFFF),
        accent = Color(0xFFFDBA74),
        hexPreview = listOf("#BE123C", "#FFF7ED", "#FDBA74")
    ),
    AppColorPalette(
        id = "ocean_coral",
        title = "Ocean Teal & Coral Pink",
        subtitle = "#0E7490 • #ECFEFF • #FB7185",
        primary = Color(0xFF0E7490),
        primaryDark = Color(0xFF155E75),
        backgroundLight = Color(0xFFECFEFF),
        surfaceLight = Color(0xFFFFFFFF),
        accent = Color(0xFFFB7185),
        hexPreview = listOf("#0E7490", "#ECFEFF", "#FB7185")
    ),
    AppColorPalette(
        id = "midnight_lime",
        title = "Midnight Navy & Electric Lime",
        subtitle = "#111827 • #F3F4F6 • #A3E635",
        primary = Color(0xFF111827),
        primaryDark = Color(0xFF030712),
        backgroundLight = Color(0xFFF3F4F6),
        surfaceLight = Color(0xFFFFFFFF),
        accent = Color(0xFFA3E635),
        hexPreview = listOf("#111827", "#F3F4F6", "#A3E635")
    ),
    AppColorPalette(
        id = "teal_ivory",
        title = "Teal & Warm Ivory",
        subtitle = "#0F766E • #FFFBEB • #F97316",
        primary = Color(0xFF0F766E),
        primaryDark = Color(0xFF115E59),
        backgroundLight = Color(0xFFFFFBEB),
        surfaceLight = Color(0xFFFFFFFF),
        accent = Color(0xFFF97316),
        hexPreview = listOf("#0F766E", "#FFFBEB", "#F97316")
    ),
    AppColorPalette(
        id = "navy_tangerine",
        title = "Navy Blue & Tangerine",
        subtitle = "#0F2D56 • #E8DCC9 • #F05A28",
        primary = Color(0xFF0F2D56),
        primaryDark = Color(0xFF091B36),
        backgroundLight = Color(0xFFE8DCC9),
        surfaceLight = Color(0xFFFAF6F0),
        accent = Color(0xFFF05A28),
        hexPreview = listOf("#0F2D56", "#E8DCC9", "#F05A28")
    ),
    AppColorPalette(
        id = "forest_sun",
        title = "Forest Green & Sun Red",
        subtitle = "#0F4D3C • #F8F6EF • #FF2D3F",
        primary = Color(0xFF0F4D3C),
        primaryDark = Color(0xFF083025),
        backgroundLight = Color(0xFFF8F6EF),
        surfaceLight = Color(0xFFFFFFFF),
        accent = Color(0xFFFF2D3F),
        hexPreview = listOf("#0F4D3C", "#F8F6EF", "#FF2D3F")
    ),
    AppColorPalette(
        id = "espresso_sandstone",
        title = "Expresso & Sandstone",
        subtitle = "#4A2E24 • #E9DCC9 • #F05A28",
        primary = Color(0xFF4A2E24),
        primaryDark = Color(0xFF2E1B14),
        backgroundLight = Color(0xFFE9DCC9),
        surfaceLight = Color(0xFFFAF6F0),
        accent = Color(0xFFF05A28),
        hexPreview = listOf("#4A2E24", "#E9DCC9", "#F05A28")
    )
)

fun findPaletteById(id: String): AppColorPalette {
    return FAVORITE_COLOR_PALETTES.find { it.id == id } ?: FAVORITE_COLOR_PALETTES.first()
}
