package com.example.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Recibe pedidos de "abre tal pantalla" que llegan desde fuera de la app:
 * atajos del ícono (mantener presionado), widgets y enlaces bravova://.
 *
 * Android (MainActivity) e iOS (Swift) llaman a [request]; App.kt lo atiende con [consume].
 * Destinos: buscar, repetir, bravisima, ofertas, pedido, bravia.
 */
object ExternalLinks {
    var pending by mutableStateOf<String?>(null)
        private set

    /** Acepta "buscar", "bravova://buscar" o "bravova:buscar". */
    fun request(target: String) {
        val clean = target.substringAfter("://").substringAfter("bravova:").trim('/').lowercase()
        if (clean.isNotEmpty()) pending = clean
    }

    fun consume(): String? = pending.also { pending = null }
}
