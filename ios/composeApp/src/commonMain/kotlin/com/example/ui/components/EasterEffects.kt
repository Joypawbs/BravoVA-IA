package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Efectos visuales de los huevos de pascua (Undertale / Deltarune).
 * Duran unos segundos, no bloquean los toques y desaparecen solos.
 * Todo está dibujado a mano con formas simples: no se dibuja ningún personaje.
 */
enum class EasterEffect(val durationMs: Long) {
    SPAMTON(6000),   // carteles de [[OFERTA]] que parpadean como un anuncio viejo
    MUFFET(6500),    // arañitas colgando del borde de arriba
    RALSEI(6500),    // tono verde suave y estrellitas que caen
    ASGORE(7000),    // florecitas doradas que caen
}

object EasterEffects {
    var active by mutableStateOf<EasterEffect?>(null)
        private set
    private var serial by mutableStateOf(0)
    val trigger get() = serial

    /** Respeta "reducir movimiento": en ese caso solo se muestra el mensaje. */
    var reduceMotion = false

    fun start(effect: EasterEffect) {
        if (reduceMotion) return
        active = effect
        serial++
    }

    fun stop() { active = null }
}

@Composable
fun EasterEffectsOverlay() {
    val effect = EasterEffects.active
    val serial = EasterEffects.trigger
    LaunchedEffect(effect, serial) {
        if (effect != null) {
            delay(effect.durationMs)
            EasterEffects.stop()
        }
    }
    AnimatedVisibility(
        visible = effect != null,
        enter = fadeIn(tween(350)),
        exit = fadeOut(tween(600)),
        modifier = Modifier.fillMaxSize()
    ) {
        // Recuerda el último efecto para que se vea mientras se desvanece
        var last by remember { mutableStateOf(effect) }
        if (effect != null) last = effect
        Box(Modifier.fillMaxSize()) {
            when (last) {
                EasterEffect.SPAMTON -> SpamtonAds()
                EasterEffect.MUFFET -> HangingSpiders()
                EasterEffect.RALSEI -> FluffyStars()
                EasterEffect.ASGORE -> FallingFlowers()
                null -> Unit
            }
        }
    }
}

@Composable
private fun rememberClock(periodMs: Int): Float {
    val t = rememberInfiniteTransition(label = "easter")
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart), label = "clock")
    return v
}

/* ---------- Spamton: anuncios que parpadean ---------- */
@Composable
private fun SpamtonAds() {
    val clock = rememberClock(900)
    val measurer = rememberTextMeasurer()
    val words = remember { listOf("[[OFERTA]]", "[[GRAN AHORRO]]", "¡¡1997!!", "[[GRATIS*]]", "[[HOT DEAL]]", "¡¡KROMER!!") }
    val seeds = remember { List(9) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextInt(words.size)) } }
    Canvas(Modifier.fillMaxSize()) {
        val phase = (clock * 6).toInt()
        seeds.forEachIndexed { i, (x, y, w) ->
            if ((i + phase) % 3 == 0) return@forEachIndexed // parpadeo
            val color = if ((i + phase) % 2 == 0) Color(0xFFFFE600) else Color(0xFFFF4FD8)
            val text = words[(w + phase) % words.size]
            val layout = measurer.measure(
                text,
                TextStyle(color = color, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            )
            val pos = Offset(x * (size.width - layout.size.width).coerceAtLeast(1f), 80f + y * (size.height - 260f))
            rotate(if (i % 2 == 0) -8f else 6f, pos) {
                drawRect(Color.Black.copy(alpha = 0.78f), pos - Offset(10f, 6f),
                    androidx.compose.ui.geometry.Size(layout.size.width + 20f, layout.size.height + 12f))
                drawText(layout, topLeft = pos)
            }
        }
    }
}

/* ---------- Muffet: arañitas colgando ---------- */
@Composable
private fun HangingSpiders() {
    val clock = rememberClock(2600)
    val spiders = remember { List(6) { Triple(0.08f + it * 0.17f + Random.nextFloat() * 0.04f, 0.12f + Random.nextFloat() * 0.25f, Random.nextFloat()) } }
    Canvas(Modifier.fillMaxSize()) {
        spiders.forEach { (x, len, ph) ->
            val bob = sin((clock + ph) * 2 * PI).toFloat()
            val cx = x * size.width
            val cy = size.height * (len + 0.03f * bob)
            drawLine(Color(0xFFB8A6E0).copy(alpha = 0.8f), Offset(cx, 0f), Offset(cx, cy), strokeWidth = 2f)
            drawSpider(Offset(cx, cy), 13f, bob)
        }
    }
}

private fun DrawScope.drawSpider(c: Offset, r: Float, wiggle: Float) {
    val body = Color(0xFF5B2A86)
    for (side in listOf(-1f, 1f)) for (k in 0 until 4) {
        val a = (-50f + k * 30f + wiggle * 6f) * (PI / 180).toFloat()
        val knee = Offset(c.x + side * r * 1.5f * kotlin.math.cos(a), c.y + r * 1.2f * sin(a) - r * 0.4f)
        val foot = Offset(knee.x + side * r * 0.8f, knee.y + r * 1.1f)
        drawLine(body, c, knee, strokeWidth = 3f)
        drawLine(body, knee, foot, strokeWidth = 3f)
    }
    drawCircle(body, r, c)
    drawCircle(Color(0xFF7E44B3), r * 0.62f, c + Offset(0f, -r * 0.9f))
    drawCircle(Color.White, r * 0.18f, c + Offset(-r * 0.25f, -r * 1.0f))
    drawCircle(Color.White, r * 0.18f, c + Offset(r * 0.25f, -r * 1.0f))
}

/* ---------- Ralsei: tono verde suave y estrellitas ---------- */
@Composable
private fun FluffyStars() {
    val clock = rememberClock(4200)
    val stars = remember { List(22) { Triple(Random.nextFloat(), Random.nextFloat(), 0.6f + Random.nextFloat() * 0.8f) } }
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0xFF3DDC97).copy(alpha = 0.10f))
        stars.forEach { (x, y0, s) ->
            val y = ((y0 + clock * s) % 1f) * size.height
            val c = Offset(x * size.width + sin((clock * 2 + x) * 2 * PI).toFloat() * 12f, y)
            drawStar(c, 9f * s, Color(0xFFFFF3A8).copy(alpha = 0.9f))
        }
    }
}

private fun DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val a = (-90f + i * 36f) * (PI / 180).toFloat()
        val pt = Offset(c.x + rr * kotlin.math.cos(a), c.y + rr * sin(a))
        if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
    }
    p.close()
    drawPath(p, color)
}

/* ---------- Asgore: florecitas doradas ---------- */
@Composable
private fun FallingFlowers() {
    val clock = rememberClock(5200)
    val flowers = remember { List(16) { Triple(Random.nextFloat(), Random.nextFloat(), 0.5f + Random.nextFloat() * 0.7f) } }
    Canvas(Modifier.fillMaxSize()) {
        flowers.forEach { (x, y0, s) ->
            val y = ((y0 + clock * s) % 1f) * size.height
            val c = Offset(x * size.width + sin((clock + x) * 2 * PI).toFloat() * 20f, y)
            rotate(clock * 360f * s, c) {
                for (k in 0 until 5) {
                    val a = (k * 72f) * (PI / 180).toFloat()
                    drawCircle(Color(0xFFFFD233), 7f * s, Offset(c.x + 8f * s * kotlin.math.cos(a), c.y + 8f * s * sin(a)))
                }
                drawCircle(Color(0xFFF59E0B), 5f * s, c)
                drawCircle(Color(0xFF8A5A00), 5f * s, c, style = Stroke(1.2f))
            }
        }
    }
}
