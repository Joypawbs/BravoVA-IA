package com.example.platform

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size as CoilSize
import coil3.transform.Transformation
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

actual fun productImageTransformations(): List<Transformation> = listOf(StudioCutoutCenterTransformation)

actual fun ImageRequest.Builder.softwareBitmapsForTransformations(): ImageRequest.Builder = allowHardware(false)

object StudioCutoutCenterTransformation : Transformation() {
    // v7: algoritmo común (ProductCutout.kt): quita fondos lisos de cualquier color, sin recorte redondeado
    override val cacheKey: String = "studio_cutout_center_v8"

    private val cutoutMemoryCache = object : LruCache<Int, Bitmap>(96) {}

    override suspend fun transform(input: Bitmap, size: CoilSize): Bitmap {
        val w = input.width
        val h = input.height
        val pixels = IntArray(w * h)
        input.getPixels(pixels, 0, w, 0, 0, w, h)
        val key = cutoutSignature(pixels, w, h)
        cutoutMemoryCache.get(key)?.let { if (!it.isRecycled) return it }
        val result = cutoutAndCenter(pixels, w, h) ?: return input
        val out = Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888)
        out.setPixels(result.pixels, 0, result.width, 0, 0, result.width, result.height)
        cutoutMemoryCache.put(key, out)
        return out
    }
}
