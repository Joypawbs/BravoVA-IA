package com.example.platform

/**
 * ¿El usuario activó "Reducir movimiento" (iOS) o desactivó las animaciones (Android)?
 * Con esto se apagan el carrusel automático, el vuelo al carrito y los efectos de los huevos de pascua.
 */
expect fun systemPrefersReducedMotion(): Boolean
