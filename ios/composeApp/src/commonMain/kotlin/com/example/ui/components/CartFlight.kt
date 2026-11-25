package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.model.Product

/**
 * Animación "vuela al carrito": al agregar un producto, una miniatura sale de la tarjeta
 * y cae en la barra roja del carrito. Solo usa transformaciones de capa (barato de dibujar).
 */
@Stable
class CartFlightController {
    internal data class Flight(val id: Long, val product: Product, val from: Offset)

    internal val flights = mutableStateListOf<Flight>()
    private var nextId = 0L

    /** Posición (en la ventana) del ícono de la barra del carrito; la registra CartSummaryBar. */
    var target: Offset? = null

    fun launch(product: Product, from: Offset) {
        if (flights.size >= 4) return // no acumular animaciones si tocan muy rápido
        flights += Flight(nextId++, product, from)
    }

    internal fun finish(id: Long) {
        flights.removeAll { it.id == id }
    }
}

/** Un solo controlador para toda la app. */
val AppCartFlight = CartFlightController()

val LocalCartFlight = compositionLocalOf { AppCartFlight }

/** Capa que dibuja las miniaturas en vuelo. Va encima de todo el contenido. */
@Composable
fun BoxScope.CartFlightOverlay(controller: CartFlightController = AppCartFlight) {
    if (controller.flights.isEmpty()) return
    val density = LocalDensity.current
    val halfPx = with(density) { 28.dp.toPx() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .matchParentSize()
            .onGloballyPositioned {
                origin = it.positionInWindow()
                size = Offset(it.size.width.toFloat(), it.size.height.toFloat())
            }
    )
    val fallbackTarget = origin + Offset(size.x * 0.15f, size.y * 0.86f)
    controller.flights.forEach { flight ->
        key(flight.id) {
            val progress = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                progress.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
                controller.finish(flight.id)
            }
            val to = controller.target ?: fallbackTarget
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        val t = progress.value
                        // Curva en arco: sube un poco antes de caer en el carrito
                        val x = flight.from.x + (to.x - flight.from.x) * t - origin.x
                        val lift = -160f * 4f * t * (1f - t)
                        val y = flight.from.y + (to.y - flight.from.y) * t + lift - origin.y
                        translationX = x - halfPx
                        translationY = y - halfPx
                        val s = 1f - 0.62f * t
                        scaleX = s; scaleY = s
                        alpha = if (t > 0.85f) (1f - t) / 0.15f else 1f
                    }
                    .shadow(6.dp, RoundedCornerShape(14.dp))
            ) {
                ProductArtworkView(product = flight.product, cornerRadius = 14.dp, modifier = Modifier.size(56.dp))
            }
        }
    }
}
