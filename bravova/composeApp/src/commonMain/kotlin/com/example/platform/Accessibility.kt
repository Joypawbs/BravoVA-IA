package com.example.platform

/**
 * ¿El usuario activó "Reducir movimiento" (iOS) o desactivó las animaciones (Android)?
 * Con esto se apagan el carrusel automático, el vuelo al carrito y los efectos de los huevos de pascua.
 */
expect fun systemPrefersReducedMotion(): Boolean

/** ¿El usuario activó "Reducir transparencia"? (solo iOS; en Android siempre es falso). */
expect fun systemPrefersReducedTransparency(): Boolean

/**
 * Sube el brillo de la pantalla al máximo mientras esté en pantalla (para que el escáner de la caja
 * lea el código a la primera) y lo devuelve como estaba al salir.
 */
@androidx.compose.runtime.Composable
expect fun KeepScreenAtFullBrightness()
