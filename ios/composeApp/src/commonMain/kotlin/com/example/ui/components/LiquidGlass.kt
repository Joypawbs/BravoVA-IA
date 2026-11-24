package com.example.ui.components

import com.example.platform.PlatformInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch
import kotlin.math.min

/*
 * Liquid Glass (estilo iOS 26) para la navegación de BravoVa:
 *  - fondo desenfocado y casi transparente (Haze),
 *  - refracción en los bordes con un shader (lente que dobla lo que hay detrás),
 *  - borde especular claro arriba-izquierda y abajo-derecha,
 *  - reacción "líquida" al tocar: se estira con resorte y brilla donde tocas.
 * Solo se usa en barra inferior, cabeceras y botones flotantes; el contenido va encima, nítido.
 */

/** Shader de refracción (SkSL en iOS, AGSL en Android: mismo código). */
internal const val LIQUID_GLASS_SHADER = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float depth;
uniform float dispersion;

float sdRoundRect(float2 p, float2 c, float r) {
    float2 q = abs(p - c) - (c - float2(r, r));
    return length(max(q, float2(0.0, 0.0))) + min(max(q.x, q.y), 0.0) - r;
}

half4 sampleAt(float2 p) {
    return content.eval(clamp(p, float2(0.5, 0.5), size - float2(0.5, 0.5)));
}

half4 main(float2 p) {
    float2 c = size * 0.5;
    float d = sdRoundRect(p, c, radius);
    float t = clamp(1.0 + d / depth, 0.0, 1.0);
    float bend = t * t * t;
    float e = 1.0;
    float2 n = float2(
        sdRoundRect(p + float2(e, 0.0), c, radius) - sdRoundRect(p - float2(e, 0.0), c, radius),
        sdRoundRect(p + float2(0.0, e), c, radius) - sdRoundRect(p - float2(0.0, e), c, radius));
    float len = length(n);
    n = len > 0.0001 ? n / len : float2(0.0, 0.0);
    float2 sp = p - n * bend * depth * 0.55;
    half4 base = sampleAt(sp);
    half r = sampleAt(sp - n * bend * dispersion).r;
    half b = sampleAt(sp + n * bend * dispersion).b;
    return half4(r, base.g, b, base.a);
}
"""

/** Efecto de refracción para el tamaño dado, o null si la plataforma no lo admite. */
expect fun liquidGlassRefractionEffect(
    width: Float,
    height: Float,
    radius: Float,
    depth: Float,
    dispersion: Float
): RenderEffect?

@Composable
private fun rememberLiquidGlassHazeStyle(darkMode: Boolean): HazeStyle = remember(darkMode) {
    HazeStyle(
        backgroundColor = if (darkMode) Color(0xFF0A0F1A) else Color(0xFFFFFAF5),
        tints = listOf(HazeTint(color = (if (darkMode) Color(0xFF0B1120) else Color.White).copy(alpha = if (darkMode) 0.20f else 0.30f))),
        blurRadius = 9.dp,
        noiseFactor = 0f
    )
}

/**
 * Contenedor de vidrio líquido. [cornerRadius] define la forma (usa la mitad de la altura para cápsulas).
 * [content] se dibuja encima sin distorsión.
 */
@Composable
fun LiquidGlass(
    darkMode: Boolean,
    accent: Color,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    elevation: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (PlatformInfo.isAndroid) {
        // En Android usamos superficies Material 3 (colores dinámicos) en lugar de vidrio.
        androidx.compose.material3.Surface(
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            shadowElevation = (elevation / 4)
        ) {
            Box(content = content)
        }
        return
    }
    val density = LocalDensity.current
    val hazeState = LocalTranslucentHazeState.current
    val useHaze = hazeState != null && DeviceSupportsGpuBlur
    val hazeStyle = rememberLiquidGlassHazeStyle(darkMode)
    var glassSize by remember { mutableStateOf(IntSize.Zero) }
    var pressed by remember { mutableStateOf(false) }
    var touch by remember { mutableStateOf(Offset.Unspecified) }
    val glow = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "liquidGlassScale"
    )

    val radiusPx = with(density) { cornerRadius.toPx() }
    val depthPx = with(density) { 14.dp.toPx() }
    val dispersionPx = with(density) { 1.4.dp.toPx() }
    val refraction = remember(glassSize, radiusPx, depthPx) {
        val w = glassSize.width.toFloat()
        val h = glassSize.height.toFloat()
        if (w <= 0f || h <= 0f) null
        else liquidGlassRefractionEffect(w, h, min(radiusPx, min(w, h) / 2f), min(depthPx, min(w, h) / 2f), dispersionPx)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = 1f + (scale - 1f) * 0.6f
            }
            .shadow(elevation, shape, spotColor = accent.copy(alpha = 0.20f), ambientColor = Color.Black.copy(alpha = 0.18f))
            .clip(shape)
            .onSizeChanged { glassSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressed = true
                    touch = down.position
                    scope.launch { glow.animateTo(1f, tween(120)) }
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        touch = ch.position
                        if (!ch.pressed) break
                    }
                    pressed = false
                    scope.launch { glow.animateTo(0f, tween(420)) }
                }
            }
    ) {
        // 1) Lo que hay detrás: desenfocado y refractado en los bordes.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    renderEffect = refraction
                    clip = true
                    this.shape = shape
                }
                .then(
                    if (useHaze) Modifier.hazeEffect(state = hazeState, style = hazeStyle) { blurEnabled = true }
                    else Modifier.background(uniformMenuTranslucentContainerColor(darkMode))
                )
        )
        // 2) Tinte, brillo especular y destello al tocar.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val r = min(radiusPx, size.minDimension / 2f)
                    val cr = CornerRadius(r, r)
                    val rim = 1.2.dp.toPx()
                    val tint = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (darkMode) 0.10f else 0.26f),
                        0.45f to Color.White.copy(alpha = 0f),
                        1f to accent.copy(alpha = if (darkMode) 0.07f else 0.05f)
                    )
                    val rimLight = Brush.linearGradient(
                        0f to Color.White.copy(alpha = if (darkMode) 0.62f else 0.95f),
                        0.32f to Color.White.copy(alpha = 0.05f),
                        0.68f to Color.White.copy(alpha = 0.03f),
                        1f to Color.White.copy(alpha = if (darkMode) 0.38f else 0.70f),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                    val rimShade = if (darkMode) Color.Black.copy(alpha = 0.35f) else Color(0xFF0F172A).copy(alpha = 0.10f)
                    onDrawBehind {
                        drawRoundRect(brush = tint, cornerRadius = cr)
                        drawRoundRect(
                            color = rimShade,
                            topLeft = Offset(rim, rim),
                            size = Size(size.width - rim * 2, size.height - rim * 2),
                            cornerRadius = CornerRadius(r - rim, r - rim),
                            style = Stroke(rim * 0.8f)
                        )
                        drawRoundRect(
                            brush = rimLight,
                            topLeft = Offset(rim / 2, rim / 2),
                            size = Size(size.width - rim, size.height - rim),
                            cornerRadius = CornerRadius(r - rim / 2, r - rim / 2),
                            style = Stroke(rim)
                        )
                        if (glow.value > 0.01f && touch != Offset.Unspecified) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0f to Color.White.copy(alpha = 0.30f * glow.value),
                                    1f to Color.White.copy(alpha = 0f),
                                    center = touch,
                                    radius = size.minDimension * 1.4f
                                ),
                                radius = size.minDimension * 1.4f,
                                center = touch
                            )
                        }
                    }
                }
        )
        // 3) Contenido nítido.
        content()
    }
}
