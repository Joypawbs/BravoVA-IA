package com.example.ui.components

/**
 * Código de barras Code 128 (juego B) real, para que el escáner de la caja lea la tarjeta Bravísima.
 *
 * Antes las barras eran decorativas (salían de un cálculo al azar) y ningún lector las entendía.
 * Tabla de patrones: https://en.wikipedia.org/wiki/Code_128. Cada símbolo son 6 anchos (barra, espacio, …)
 * que suman 11 módulos; se probó contra un lector real (zxing-cpp) con "BRV-8842-9901-7234".
 */
object Code128 {
    private val PATTERNS = arrayOf(
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213", "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132", "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211", "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313", "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331", "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111", "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214", "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111", "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141", "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141", "114131", "311141", "411131", "211412", "211214", "211232", "233111"
    )
    private const val START_B = 104
    private const val STOP = "2331112"

    /** Devuelve los módulos de izquierda a derecha: true = barra negra. Sin zona en blanco a los lados. */
    fun modules(text: String): BooleanArray {
        val values = mutableListOf(START_B)
        text.forEach { ch -> values += (ch.code - 32).coerceIn(0, 95) } // juego B: ASCII 32–127
        var sum = START_B
        values.drop(1).forEachIndexed { i, v -> sum += v * (i + 1) }
        values += sum % 103
        val out = ArrayList<Boolean>(values.size * 11 + 13)
        for (v in values) appendWidths(out, PATTERNS[v])
        appendWidths(out, STOP)
        return out.toBooleanArray()
    }

    private fun appendWidths(out: MutableList<Boolean>, widths: String) {
        var bar = true
        widths.forEach { w -> repeat(w - '0') { out += bar }; bar = !bar }
    }
}
