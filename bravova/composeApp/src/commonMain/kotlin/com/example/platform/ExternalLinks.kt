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

    /** Acepta "buscar", "bravova://buscar" o "bravova:buscar"; también "bravia?q=como+hago+mangu". */
    fun request(target: String) {
        val clean = target.substringAfter("://").substringAfter("bravova:").trim('/')
        // Solo el destino va en minúsculas; el texto de ?q= se respeta tal cual.
        val normalized = clean.substringBefore('?').trim('/').lowercase() + clean.drop(clean.substringBefore('?').length)
        if (normalized.isNotEmpty()) pending = normalized
    }

    fun consume(): String? = pending.also { pending = null }

    /** Destino sin parámetros: "bravia?q=hola" → "bravia". */
    fun targetOf(link: String): String = link.substringBefore('?')

    /** Valor de un parámetro, ya decodificado: "bravia?q=como%20hago" → "como hago". */
    fun param(link: String, name: String): String? {
        val query = link.substringAfter('?', "")
        val raw = query.split('&').firstOrNull { it.startsWith("$name=") }?.substringAfter('=') ?: return null
        return percentDecode(raw).trim().ifEmpty { null }
    }

    private fun percentDecode(text: String): String {
        val out = ArrayList<Byte>(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val hex = if (c == '%' && i + 2 < text.length) text.substring(i + 1, i + 3).toIntOrNull(16) else null
            when {
                c == '+' -> { out.add(' '.code.toByte()); i++ }
                hex != null -> { out.add(hex.toByte()); i += 3 }
                else -> { c.toString().encodeToByteArray().forEach { out.add(it) }; i++ }
            }
        }
        return out.toByteArray().decodeToString()
    }
}
