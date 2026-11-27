package com.example.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Visibilidad del vidrio nativo (iOS 26).
 *
 * Para qué: las barras de vidrio de iOS son vistas nativas que se dibujan ENCIMA de Compose.
 * Por eso Compose no puede desvanecerlas ni taparlas: en una transición o con una hoja abierta
 * quedaban superpuestas o desaparecían de golpe.
 * Cómo: cada barra nativa calcula si debe verse ([rememberNativeGlassVisible]) y se lo pasa a
 * SwiftUI, que hace el fundido. Se oculta cuando:
 *  - su pantalla está saliendo en una transición ([GlassPresence] / [glassPresenceFrom]), o
 *  - hay una hoja, diálogo o pantalla completa abierta ([SuppressNativeGlass]).
 */
val LocalGlassPresence = compositionLocalOf { true }

object NativeGlassSuppression {
    var count by mutableIntStateOf(0)
}

/** Llamar dentro de diálogos, hojas y capas a pantalla completa: oculta el vidrio nativo mientras estén abiertos. */
@Composable
fun SuppressNativeGlass() {
    DisposableEffect(Unit) {
        NativeGlassSuppression.count++
        onDispose { NativeGlassSuppression.count = (NativeGlassSuppression.count - 1).coerceAtLeast(0) }
    }
}

/** El contenido solo muestra su vidrio nativo si [visible] y si su contenedor también lo muestra. */
@Composable
fun GlassPresence(visible: Boolean, content: @Composable () -> Unit) {
    val parent = LocalGlassPresence.current
    CompositionLocalProvider(LocalGlassPresence provides (parent && visible), content = content)
}

/** Dentro de AnimatedContent / AnimatedVisibility: el vidrio de lo que está saliendo se desvanece ya. */
@Composable
fun AnimatedVisibilityScope.GlassPresenceFromTransition(content: @Composable () -> Unit) {
    GlassPresence(transition.targetState == EnterExitState.Visible, content)
}

@Composable
fun rememberNativeGlassVisible(requested: Boolean = true): Boolean =
    requested && LocalGlassPresence.current && NativeGlassSuppression.count == 0
