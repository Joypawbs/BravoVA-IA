package com.example.ui.components

import com.example.ui.theme.Tone

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
    PARTICLES(6000), // figuras simples (corazones, picas, copos…) según un [ParticleStyle]
    TV_STATIC(3500), // estática de televisor vieja
}

/** Figuras simples que puede dibujar el motor de partículas. Ninguna es un personaje. */
enum class ParticleShape { HEART, SPADE, CLUB, DIAMOND, SNOW, FLAME, SPARK, DROP, FEATHER, PELLET, SQUARE, SPEAR, STAR }

/** Cómo se mueven las figuras. */
enum class ParticleMotion { FALL, RISE, SPIN_FALL, BURST, JITTER, ORBIT }

data class ParticleStyle(
    val shapes: List<ParticleShape>,
    val colors: List<Color>,
    val motion: ParticleMotion = ParticleMotion.FALL,
    val count: Int = 22,
    val size: Float = 12f,
    /** Velo de color sobre la pantalla (por ejemplo azul triste o rojo de calor). */
    val tint: Color? = null,
    /** Una vuelta completa en milisegundos: más bajo = más rápido. */
    val periodMs: Int = 4500
)

object EasterEffects {
    var active by mutableStateOf<EasterEffect?>(null)
        private set
    var particles by mutableStateOf<ParticleStyle?>(null)
        private set
    private var serial by mutableStateOf(0)
    val trigger get() = serial

    /** Respeta "reducir movimiento": en ese caso solo se muestra el mensaje (y suena el sonido). */
    var reduceMotion = false

    fun start(effect: EasterEffect, style: ParticleStyle? = null) {
        if (reduceMotion) return
        particles = style
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
                EasterEffect.PARTICLES -> EasterEffects.particles?.let { ParticleRain(it) }
                EasterEffect.TV_STATIC -> TvStatic()
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
                drawCircle(Tone.warningBright, 5f * s, c)
                drawCircle(Color(0xFF8A5A00), 5f * s, c, style = Stroke(1.2f))
            }
        }
    }
}


/* ---------- Motor de partículas: figuras simples que caen, suben, giran o estallan ---------- */
private data class Seed(val x: Float, val y: Float, val speed: Float, val phase: Float, val shape: Int, val color: Int, val scale: Float)

@Composable
private fun ParticleRain(style: ParticleStyle) {
    val clock = rememberClock(style.periodMs)
    val seeds = remember(style) {
        List(style.count) {
            Seed(
                Random.nextFloat(), Random.nextFloat(), 0.6f + Random.nextFloat() * 0.8f, Random.nextFloat(),
                Random.nextInt(style.shapes.size), Random.nextInt(style.colors.size), 0.7f + Random.nextFloat() * 0.7f
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        style.tint?.let { drawRect(it) }
        val center = Offset(size.width / 2, size.height / 2)
        seeds.forEach { sd ->
            val r = style.size * sd.scale
            val t = (sd.y + clock * sd.speed) % 1f
            var alpha = 0.92f
            var angle = 0f
            val pos = when (style.motion) {
                ParticleMotion.FALL -> Offset(sd.x * size.width + sin((clock * 2 + sd.phase) * 2 * PI).toFloat() * 14f, t * size.height)
                ParticleMotion.RISE -> Offset(sd.x * size.width + sin((clock * 3 + sd.phase) * 2 * PI).toFloat() * 10f, (1f - t) * size.height)
                ParticleMotion.SPIN_FALL -> {
                    angle = (clock * 360f * sd.speed + sd.phase * 360f)
                    Offset(sd.x * size.width + sin((clock + sd.phase) * 2 * PI).toFloat() * 26f, t * size.height)
                }
                ParticleMotion.BURST -> {
                    val a = sd.x * 2 * PI
                    val dist = t * size.maxDimension * 0.6f
                    alpha = (1f - t).coerceIn(0f, 1f)
                    center + Offset((kotlin.math.cos(a) * dist).toFloat(), (sin(a) * dist).toFloat())
                }
                ParticleMotion.JITTER -> {
                    val j = sin((clock * 40 + sd.phase * 10) * PI).toFloat() * 7f
                    angle = j * 3f
                    Offset(sd.x * size.width + j, sd.y * size.height - j)
                }
                ParticleMotion.ORBIT -> {
                    val a = (sd.x + clock * sd.speed) * 2 * PI
                    val radius = size.minDimension * (0.22f + sd.y * 0.2f)
                    center + Offset((kotlin.math.cos(a) * radius).toFloat(), (sin(a) * radius).toFloat())
                }
            }
            val color = style.colors[sd.color].copy(alpha = alpha * style.colors[sd.color].alpha)
            rotate(angle, pos) { drawShape(style.shapes[sd.shape], pos, r, color) }
        }
    }
}

private fun DrawScope.drawShape(shape: ParticleShape, c: Offset, r: Float, color: Color) {
    when (shape) {
        ParticleShape.HEART -> drawPath(heartPath(c, r), color)
        ParticleShape.SPADE -> {
            // Corazón al revés con tallo
            rotate(180f, c) { drawPath(heartPath(c, r), color) }
            drawPath(Path().apply { moveTo(c.x, c.y); lineTo(c.x - r * 0.45f, c.y + r * 1.2f); lineTo(c.x + r * 0.45f, c.y + r * 1.2f); close() }, color)
        }
        ParticleShape.CLUB -> {
            drawCircle(color, r * 0.5f, c + Offset(0f, -r * 0.5f))
            drawCircle(color, r * 0.5f, c + Offset(-r * 0.5f, r * 0.15f))
            drawCircle(color, r * 0.5f, c + Offset(r * 0.5f, r * 0.15f))
            drawPath(Path().apply { moveTo(c.x, c.y); lineTo(c.x - r * 0.4f, c.y + r * 1.1f); lineTo(c.x + r * 0.4f, c.y + r * 1.1f); close() }, color)
        }
        ParticleShape.DIAMOND -> drawPath(Path().apply {
            moveTo(c.x, c.y - r); lineTo(c.x + r * 0.7f, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r * 0.7f, c.y); close()
        }, color)
        ParticleShape.SNOW -> for (k in 0 until 3) {
            val a = (k * 60f) * (PI / 180).toFloat()
            val d = Offset(r * kotlin.math.cos(a), r * sin(a))
            drawLine(color, c - d, c + d, strokeWidth = r * 0.22f)
        }
        ParticleShape.FLAME -> {
            drawPath(dropPath(c, r * 1.2f, up = true), color)
            drawPath(dropPath(c + Offset(0f, r * 0.35f), r * 0.6f, up = true), Color(0xFFFFE08A).copy(alpha = color.alpha))
        }
        ParticleShape.SPARK -> drawPath(Path().apply {
            moveTo(c.x, c.y - r); lineTo(c.x + r * 0.22f, c.y - r * 0.22f); lineTo(c.x + r, c.y)
            lineTo(c.x + r * 0.22f, c.y + r * 0.22f); lineTo(c.x, c.y + r); lineTo(c.x - r * 0.22f, c.y + r * 0.22f)
            lineTo(c.x - r, c.y); lineTo(c.x - r * 0.22f, c.y - r * 0.22f); close()
        }, color)
        ParticleShape.DROP -> drawPath(dropPath(c, r, up = true), color)
        ParticleShape.FEATHER -> {
            drawOval(color, topLeft = c - Offset(r * 0.35f, r), size = androidx.compose.ui.geometry.Size(r * 0.7f, r * 2f))
            drawLine(color.copy(alpha = color.alpha * 0.6f), c + Offset(0f, -r), c + Offset(0f, r * 1.3f), strokeWidth = 2f)
        }
        ParticleShape.PELLET -> drawCircle(color, r * 0.45f, c)
        ParticleShape.SQUARE -> drawRect(color, c - Offset(r * 0.5f, r * 0.5f), androidx.compose.ui.geometry.Size(r, r))
        ParticleShape.SPEAR -> {
            drawLine(color, c + Offset(0f, r * 1.6f), c + Offset(0f, -r * 0.6f), strokeWidth = r * 0.22f)
            drawPath(Path().apply { moveTo(c.x, c.y - r * 1.4f); lineTo(c.x - r * 0.4f, c.y - r * 0.5f); lineTo(c.x + r * 0.4f, c.y - r * 0.5f); close() }, color)
        }
        ParticleShape.STAR -> drawStar(c, r, color)
    }
}

private fun heartPath(c: Offset, r: Float) = Path().apply {
    moveTo(c.x, c.y + r * 0.9f)
    cubicTo(c.x - r * 1.4f, c.y - r * 0.1f, c.x - r * 0.6f, c.y - r * 1.2f, c.x, c.y - r * 0.4f)
    cubicTo(c.x + r * 0.6f, c.y - r * 1.2f, c.x + r * 1.4f, c.y - r * 0.1f, c.x, c.y + r * 0.9f)
    close()
}

private fun dropPath(c: Offset, r: Float, up: Boolean) = Path().apply {
    val tip = if (up) -1f else 1f
    moveTo(c.x, c.y + tip * r * 1.3f)
    cubicTo(c.x + r * 0.9f, c.y, c.x + r * 0.7f, c.y - tip * r * 0.9f, c.x, c.y - tip * r * 0.9f)
    cubicTo(c.x - r * 0.7f, c.y - tip * r * 0.9f, c.x - r * 0.9f, c.y, c.x, c.y + tip * r * 1.3f)
    close()
}

/* ---------- Estática de televisor ---------- */
@Composable
private fun TvStatic() {
    val clock = rememberClock(400)
    Canvas(Modifier.fillMaxSize()) {
        val rnd = Random((clock * 1000).toInt())
        val cell = 9f
        drawRect(Color.Black.copy(alpha = 0.18f))
        repeat(900) {
            val g = rnd.nextFloat()
            drawRect(
                Color(g, g, g, 0.32f),
                Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height),
                androidx.compose.ui.geometry.Size(cell, cell * 0.6f)
            )
        }
        // Franja que baja, como una tele mal sintonizada
        val bandY = (clock * size.height)
        drawRect(Color.White.copy(alpha = 0.10f), Offset(0f, bandY), androidx.compose.ui.geometry.Size(size.width, 40f))
    }
}
