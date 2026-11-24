package com.example.ui.theme

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Formato único de precios: "RD$1,597.00" (o "RD$1,597" sin decimales).
 * Así todas las pantallas muestran el dinero igual.
 */
fun formatRd(amount: Double, withCents: Boolean = true): String {
    val cents = (abs(amount) * 100).roundToLong()
    val whole = (cents / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    val sign = if (amount < 0) "-" else ""
    return if (withCents) "${sign}RD$$whole.${(cents % 100).toString().padStart(2, '0')}" else "${sign}RD$$whole"
}

fun formatRd(amount: Int): String = formatRd(amount.toDouble(), withCents = false)
