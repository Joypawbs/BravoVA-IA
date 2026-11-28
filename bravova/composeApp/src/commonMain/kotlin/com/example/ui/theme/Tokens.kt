package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tokens de diseño de BravoVa 2.0: un solo lugar para colores con significado,
 * tamaños de texto, espaciado y esquinas.
 *
 * Los colores leen el modo oscuro de [ThemeState], que actualiza BravoVaTheme.
 * Como es estado observable, también funcionan dentro de Canvas y lambdas no
 * @Composable, y la pantalla se redibuja sola al cambiar de modo.
 */
object ThemeState {
    var dark by mutableStateOf(false)
}

private fun pick(light: Long, dark: Long) = if (ThemeState.dark) Color(dark) else Color(light)

/** Colores con significado (no de marca): éxito, aviso, peligro, información. */
object Tone {
    /** Rojo de marca para texto e íconos: en oscuro, el coral del tema (5,6:1; #E1251B daba 3,8:1). */
    val brand: Color get() = pick(0xFFE1251B, 0xFFFF5446)
    /** Verde de confirmación: ahorro, entregado, pagado. */
    val success: Color get() = pick(0xFF059669, 0xFF34D399)
    /** Verde para texto sobre fondos claros de éxito. */
    val successStrong: Color get() = pick(0xFF047857, 0xFF6EE7B7)
    /** Verde vivo para indicadores pequeños. */
    val successBright: Color get() = pick(0xFF10B981, 0xFF34D399)
    val successSoft: Color get() = pick(0xFFD1FAE5, 0xFF0B3B2E)
    val warning: Color get() = pick(0xFFD97706, 0xFFFBBF24)
    val warningBright: Color get() = pick(0xFFF59E0B, 0xFFFBBF24)
    val warningSoft: Color get() = pick(0xFFFFF7ED, 0xFF3A2A10)
    /** Rojo de marca para ofertas y alertas. */
    val danger: Color get() = pick(0xFFE31837, 0xFFFF6B7D)
    val dangerStrong: Color get() = pick(0xFFBE123C, 0xFFFDA4AF)
    val dangerSoft: Color get() = pick(0xFFFFE4E6, 0xFF3B1218)
    val info: Color get() = pick(0xFF0284C7, 0xFF38BDF8)
    /** Texto casi negro / casi blanco con máximo contraste. */
    val ink: Color get() = pick(0xFF0F172A, 0xFFF1F5F9)
    val inkSoft: Color get() = pick(0xFF334155, 0xFFCBD5E1)
    val muted: Color get() = pick(0xFF64748B, 0xFF94A3B8)
    /** Superficie neutra para chips y fondos de tarjeta en la capa de contenido. */
    val surfaceSubtle: Color get() = pick(0xFFF1F5F9, 0xFF1E293B)
}

/** Escala de texto: nada por debajo de 12 sp para que se lea bien en el súper. */
object TypeScale {
    val caption = 12.sp      // etiquetas, insignias, notas
    val footnote = 13.sp     // texto secundario
    val body = 15.sp         // texto normal
    val headline = 17.sp     // títulos de tarjeta
    val title = 22.sp        // títulos de sección
    val largeTitle = 28.sp   // título grande de pantalla
    /** Mínimo permitido en toda la app. */
    val minimum = 12.sp
}

/** Espaciado en múltiplos de 4. */
object Space {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/** Esquinas comunes. */
object Corner {
    val chip = 999.dp
    val small = 10.dp
    val card = 18.dp
    val sheet = 28.dp
}

/** Tamaño mínimo de los botones que se tocan (Apple y Material recomiendan 44–48). */
val MinTouchTarget = 44.dp
