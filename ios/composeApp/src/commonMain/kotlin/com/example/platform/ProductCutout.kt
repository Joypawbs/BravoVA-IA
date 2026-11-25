package com.example.platform

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Quita el fondo liso de las fotos de producto y las deja centradas.
 *
 * 1. Mira el borde de la foto: si es de un color parejo (blanco, gris, de color),
 *    lo toma como fondo.
 * 2. Rellena desde los bordes hacia adentro todo lo que se parezca a ese fondo y lo
 *    vuelve transparente (no toca el interior del producto aunque sea del mismo color).
 * 3. Suaviza el contorno y recorta al producto, centrándolo en un lienzo cuadrado
 *    con un margen parejo.
 *
 * Trabaja con píxeles ARGB (0xAARRGGBB), así que la usan Android e iOS por igual.
 */
class CutoutResult(val pixels: IntArray, val width: Int, val height: Int)

fun cutoutAndCenter(input: IntArray, w: Int, h: Int, padding: Float = 0.06f): CutoutResult? {
    if (w < 8 || h < 8) return null
    val pixels = input.copyOf()

    // --- 1. Color del borde --------------------------------------------------
    var n = 0; var transparent = 0
    var sr = 0.0; var sg = 0.0; var sb = 0.0
    var sr2 = 0.0; var sg2 = 0.0; var sb2 = 0.0
    fun sample(px: Int) {
        n++
        if ((px ushr 24) and 0xFF < 160) { transparent++; return }
        val r = (px ushr 16) and 0xFF; val g = (px ushr 8) and 0xFF; val b = px and 0xFF
        sr += r; sg += g; sb += b; sr2 += r * r; sg2 += g * g; sb2 += b * b
    }
    for (x in 0 until w) { sample(pixels[x]); sample(pixels[(h - 1) * w + x]) }
    for (y in 1 until h - 1) { sample(pixels[y * w]); sample(pixels[y * w + w - 1]) }
    val opaque = n - transparent
    val alreadyTransparent = transparent > n / 3

    if (!alreadyTransparent && opaque > 0) {
        val mr = sr / opaque; val mg = sg / opaque; val mb = sb / opaque
        val spread = sqrt(max(0.0, sr2 / opaque - mr * mr) + max(0.0, sg2 / opaque - mg * mg) + max(0.0, sb2 / opaque - mb * mb))
        // Solo si el borde es parejo: una foto con escena de fondo se deja como está
        if (spread < 38.0) {
            val bgR = mr.toInt(); val bgG = mg.toInt(); val bgB = mb.toInt()
            val light = bgR > 200 && bgG > 200 && bgB > 200
            val threshold = if (light) 70 else 48

            fun isBg(idx: Int): Boolean {
                val px = pixels[idx]
                if ((px ushr 24) and 0xFF < 32) return true
                val r = (px ushr 16) and 0xFF; val g = (px ushr 8) and 0xFF; val b = px and 0xFF
                return abs(r - bgR) + abs(g - bgG) + abs(b - bgB) <= threshold
            }

            // --- 2. Relleno desde los bordes ---------------------------------
            val visited = BooleanArray(w * h)
            val queue = IntArray(w * h)
            var head = 0; var tail = 0
            fun push(x: Int, y: Int) {
                val idx = y * w + x
                if (!visited[idx] && isBg(idx)) { visited[idx] = true; queue[tail++] = idx }
            }
            for (x in 0 until w) { push(x, 0); push(x, h - 1) }
            for (y in 0 until h) { push(0, y); push(w - 1, y) }
            while (head < tail) {
                val idx = queue[head++]
                pixels[idx] = 0
                val x = idx % w; val y = idx / w
                if (x > 0) push(x - 1, y)
                if (x + 1 < w) push(x + 1, y)
                if (y > 0) push(x, y - 1)
                if (y + 1 < h) push(x, y + 1)
            }

            // Contorno suave: los píxeles del borde del producto se vuelven semitransparentes
            for (y in 1 until h - 1) for (x in 1 until w - 1) {
                val idx = y * w + x
                if (visited[idx]) continue
                if (visited[idx - 1] || visited[idx + 1] || visited[idx - w] || visited[idx + w]) {
                    val px = pixels[idx]
                    val r = (px ushr 16) and 0xFF; val g = (px ushr 8) and 0xFF; val b = px and 0xFF
                    val diff = abs(r - bgR) + abs(g - bgG) + abs(b - bgB)
                    val limit = threshold * 1.8
                    if (diff < limit) {
                        val alpha = ((diff / limit) * 255).toInt().coerceIn(60, 235)
                        pixels[idx] = (alpha shl 24) or (px and 0x00FFFFFF)
                    }
                }
            }
        }
    }

    // --- 3. Recorte y centrado --------------------------------------------------
    var minX = w; var minY = h; var maxX = -1; var maxY = -1
    for (y in 0 until h) {
        val row = y * w
        for (x in 0 until w) {
            if ((pixels[row + x] ushr 24) and 0xFF > 24) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }
    if (maxX <= minX || maxY <= minY) return null
    val cw = maxX - minX + 1
    val ch = maxY - minY + 1
    val side = (max(cw, ch) * (1f + padding * 2)).toInt()
    val out = IntArray(side * side)
    val offX = (side - cw) / 2
    val offY = (side - ch) / 2
    for (y in 0 until ch) {
        val srcRow = (minY + y) * w + minX
        val dstRow = (offY + y) * side + offX
        for (x in 0 until cw) out[dstRow + x] = pixels[srcRow + x]
    }
    return CutoutResult(out, side, side)
}

/** Clave de caché barata a partir de unos pocos píxeles. */
fun cutoutSignature(pixels: IntArray, w: Int, h: Int): Int {
    val idx = intArrayOf(0, w / 2, (h / 2) * w + w / 2, (h / 4) * w + w / 4, (h * 3 / 4) * w + w * 3 / 4, w * h - 1)
    var s = w * 31 + h
    for (i in idx) s = s * 31 + pixels[i.coerceIn(0, pixels.lastIndex)]
    return s
}
