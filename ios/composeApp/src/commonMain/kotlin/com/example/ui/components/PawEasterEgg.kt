package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.platform.System

/**
 * Huevo de pascua: tocar el logo de Bravo 5 veces seguidas.
 * Aparecen huellitas de perro caminando por la pantalla y un mensaje; el siguiente toque lo cierra.
 * (Solo huellas genéricas y texto: el personaje de Toby Fox no se dibuja.)
 */
object PawEasterEgg {
    var visible by mutableStateOf(false)
        private set
    private val taps = ArrayDeque<Long>()

    fun registerLogoTap(): Boolean {
        val now = System.currentTimeMillis()
        taps.addLast(now)
        while (taps.isNotEmpty() && now - taps.first() > 2500) taps.removeFirst()
        if (taps.size >= 5) {
            taps.clear()
            visible = true
            return true
        }
        return false
    }

    fun dismiss() { visible = false }
}

@Composable
fun PawEasterEggOverlay() {
    if (!PawEasterEgg.visible) return
    val walk = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        walk.animateTo(1f, infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart))
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { PawEasterEgg.dismiss() }
    ) {
        val steps = 9
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        repeat(steps) { i ->
            val t = i / (steps - 1f)
            // Cada huella aparece cuando "pasa" el perro y se va desvaneciendo
            val phase = ((walk.value - t * 0.7f) * 3f).coerceIn(0f, 1f)
            val alpha = if (phase <= 0f) 0f else (1f - (phase - 0.15f).coerceAtLeast(0f)).coerceIn(0f, 1f)
            Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = null,
                tint = Color(0xFF3A2E2A).copy(alpha = 0.75f),
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer {
                        translationX = wPx * (0.08f + 0.78f * t) + if (i % 2 == 0) -18f else 18f
                        translationY = hPx * (0.82f - 0.6f * t)
                        rotationZ = 35f
                        this.alpha = alpha
                    }
            )
        }
        Text(
            text = "* (Un perro molesto se echó a dormir sobre el logo.)\n* Toca para despertarlo.",
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = 120.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black)
                .padding(16.dp)
        )
    }
}
