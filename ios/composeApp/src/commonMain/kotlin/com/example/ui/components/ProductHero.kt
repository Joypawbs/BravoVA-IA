package com.example.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset

/**
 * Transición "héroe": la foto del producto viaja de la tarjeta a la pantalla de detalle.
 * App.kt envuelve el cambio de pantallas en un SharedTransitionLayout y deja aquí los ámbitos.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
class ProductHeroScopes(
    val shared: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope
)

val LocalProductHeroScopes = compositionLocalOf<ProductHeroScopes?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.productHeroImage(sku: String): Modifier {
    val scopes = LocalProductHeroScopes.current ?: return this
    return with(scopes.shared) {
        this@productHeroImage.sharedElement(
            sharedContentState = rememberSharedContentState(key = "product_image_$sku"),
            animatedVisibilityScope = scopes.visibility,
            boundsTransform = { _: Rect, _: Rect ->
                spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
            }
        )
    }
}
