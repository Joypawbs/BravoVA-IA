package com.example.platform

import coil3.Bitmap
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.transform.Transformation
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

actual fun productImageTransformations(): List<Transformation> = listOf(IosCutoutCenterTransformation)

actual fun ImageRequest.Builder.softwareBitmapsForTransformations(): ImageRequest.Builder = this

/** iOS: misma limpieza de fondo que en Android (ProductCutout.kt), usando los píxeles de Skia. */
object IosCutoutCenterTransformation : Transformation() {
    override val cacheKey: String = "studio_cutout_center_v7"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = input.width
        val h = input.height
        if (w < 8 || h < 8) return input
        val info = ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
        val bytes = input.readPixels(info, w * 4, 0, 0) ?: return input
        val pixels = IntArray(w * h)
        for (i in 0 until w * h) {
            val o = i * 4
            val b = bytes[o].toInt() and 0xFF
            val g = bytes[o + 1].toInt() and 0xFF
            val r = bytes[o + 2].toInt() and 0xFF
            val a = bytes[o + 3].toInt() and 0xFF
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        val result = cutoutAndCenter(pixels, w, h) ?: return input
        val outBytes = ByteArray(result.width * result.height * 4)
        for (i in result.pixels.indices) {
            val px = result.pixels[i]
            val o = i * 4
            outBytes[o] = (px and 0xFF).toByte()
            outBytes[o + 1] = ((px ushr 8) and 0xFF).toByte()
            outBytes[o + 2] = ((px ushr 16) and 0xFF).toByte()
            outBytes[o + 3] = ((px ushr 24) and 0xFF).toByte()
        }
        val outInfo = ImageInfo(result.width, result.height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
        val out = Bitmap()
        if (!out.allocPixels(outInfo)) return input
        out.installPixels(outInfo, outBytes, result.width * 4)
        return out
    }
}
