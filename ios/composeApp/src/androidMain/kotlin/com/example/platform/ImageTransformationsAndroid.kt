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
    override val cacheKey: String = "studio_cutout_center_v6"

    private val cutoutMemoryCache = object : LruCache<Int, Bitmap>(72) {}

    override suspend fun transform(input: Bitmap, size: CoilSize): Bitmap {
        val cx = input.width / 2
        val cy = input.height / 2
        val qx = (input.width / 4).coerceAtLeast(1)
        val qy = (input.height / 4).coerceAtLeast(1)
        val signature = (((((input.width * 31 + input.height) * 31 +
            input.getPixel(cx, cy)) * 31 +
            input.getPixel(qx, qy)) * 31 +
            input.getPixel(input.width - qx - 1, qy)) * 31 +
            input.getPixel(qx, input.height - qy - 1))
        cutoutMemoryCache.get(signature)?.let { cached ->
            if (!cached.isRecycled) return cached
        }
        val src = input.copy(Bitmap.Config.ARGB_8888, true)
        val w = src.width
        val h = src.height
        if (w <= 4 || h <= 4) return src

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        var transparentBorderCount = 0
        var borderSampleCount = 0
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var lightBorderCount = 0

        fun sampleBorderPixel(px: Int) {
            borderSampleCount++
            val a = (px ushr 24) and 0xFF
            if (a < 180) {
                transparentBorderCount++
                return
            }
            val r = (px ushr 16) and 0xFF
            val g = (px ushr 8) and 0xFF
            val b = px and 0xFF
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            if (r >= 172 && g >= 168 && b >= 160 && (maxC - minC) <= 52) {
                lightBorderCount++
                sumR += r
                sumG += g
                sumB += b
            }
        }

        val insetX = min(3, w / 6)
        val insetY = min(3, h / 6)
        for (x in 0 until w) {
            sampleBorderPixel(pixels[x])
            sampleBorderPixel(pixels[(h - 1) * w + x])
            sampleBorderPixel(pixels[insetY * w + x])
            sampleBorderPixel(pixels[(h - 1 - insetY) * w + x])
        }
        for (y in 1 until h - 1) {
            sampleBorderPixel(pixels[y * w])
            sampleBorderPixel(pixels[y * w + (w - 1)])
            sampleBorderPixel(pixels[y * w + insetX])
            sampleBorderPixel(pixels[y * w + (w - 1 - insetX)])
        }

        val alreadyTransparent = transparentBorderCount > (borderSampleCount / 4)

        if (!alreadyTransparent && lightBorderCount > (borderSampleCount / 10)) {
            val bgR = (sumR / lightBorderCount).toInt()
            val bgG = (sumG / lightBorderCount).toInt()
            val bgB = (sumB / lightBorderCount).toInt()

            val visited = BooleanArray(w * h)
            val queue = IntArray(w * h)
            var head = 0
            var tail = 0

            fun isBackgroundCandidate(idx: Int): Boolean {
                val px = pixels[idx]
                val a = (px ushr 24) and 0xFF
                if (a < 32) return true
                val r = (px ushr 16) and 0xFF
                val g = (px ushr 8) and 0xFF
                val b = px and 0xFF
                val diff = abs(r - bgR) + abs(g - bgG) + abs(b - bgB)
                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                val isNearWhite = r >= 215 && g >= 215 && b >= 210 && (maxC - minC) <= 36
                return diff <= 92 || isNearWhite
            }

            fun enqueueIfBg(x: Int, y: Int) {
                val idx = y * w + x
                if (!visited[idx] && isBackgroundCandidate(idx)) {
                    visited[idx] = true
                    queue[tail++] = idx
                }
            }

            for (x in 0 until w) {
                enqueueIfBg(x, 0)
                enqueueIfBg(x, h - 1)
                enqueueIfBg(x, insetY)
                enqueueIfBg(x, h - 1 - insetY)
            }
            for (y in 1 until h - 1) {
                enqueueIfBg(0, y)
                enqueueIfBg(w - 1, y)
                enqueueIfBg(insetX, y)
                enqueueIfBg(w - 1 - insetX, y)
            }

            while (head < tail) {
                val idx = queue[head++]
                pixels[idx] = 0x00000000
                val x = idx % w
                val y = idx / w
                if (x > 0) enqueueIfBg(x - 1, y)
                if (x + 1 < w) enqueueIfBg(x + 1, y)
                if (y > 0) enqueueIfBg(x, y - 1)
                if (y + 1 < h) enqueueIfBg(x, y + 1)
            }

            // Soften anti-aliased edge pixels adjacent to removed background
            for (y in 1 until h - 1) {
                val rowOffset = y * w
                for (x in 1 until w - 1) {
                    val idx = rowOffset + x
                    if (visited[idx]) continue
                    val touchesBg =
                        visited[idx - 1] || visited[idx + 1] || visited[idx - w] || visited[idx + w]
                    if (touchesBg) {
                        val px = pixels[idx]
                        val r = (px ushr 16) and 0xFF
                        val g = (px ushr 8) and 0xFF
                        val b = px and 0xFF
                        val diff = abs(r - bgR) + abs(g - bgG) + abs(b - bgB)
                        if (diff < 115) {
                            val newAlpha = ((diff * 255) / 115).coerceIn(40, 230)
                            pixels[idx] = (newAlpha shl 24) or (px and 0x00FFFFFF)
                        }
                    }
                }
            }
        }

        // Compute foreground bounding box to center the product cleanly
        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1

        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                val a = (pixels[rowOffset + x] ushr 24) and 0xFF
                if (a > 24) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        src.setPixels(pixels, 0, w, 0, 0, w, h)

        if (maxX <= minX || maxY <= minY) {
            return src
        }

        val contentW = maxX - minX + 1
        val contentH = maxY - minY + 1
        val targetSide = max(w, h).coerceIn(240, 420)
        val outBitmap = Bitmap.createBitmap(targetSide, targetSide, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(outBitmap)

        val clipPath = AndroidPath().apply {
            val cornerRadiusPx = targetSide * 0.14f
            addRoundRect(
                RectF(0f, 0f, targetSide.toFloat(), targetSide.toFloat()),
                cornerRadiusPx,
                cornerRadiusPx,
                AndroidPath.Direction.CW
            )
        }
        canvas.clipPath(clipPath)

        val padFraction = 0.05f
        val availableSide = targetSide * (1f - padFraction * 2f)
        val scale = min(availableSide / contentW.toFloat(), availableSide / contentH.toFloat())
        val drawW = contentW * scale
        val drawH = contentH * scale
        val left = (targetSide - drawW) / 2f
        val top = (targetSide - drawH) / 2f

        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
        val srcRect = Rect(minX, minY, maxX + 1, maxY + 1)
        val dstRect = RectF(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(src, srcRect, dstRect, paint)

        cutoutMemoryCache.put(signature, outBitmap)
        return outBitmap
    }
}

