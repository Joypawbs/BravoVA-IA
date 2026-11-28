package com.example.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Alto de lo que flota abajo (barra de pestañas + barra del carrito si se ve + zona segura).
 * Lo calcula App.kt una sola vez; todas las listas lo usan como margen inferior para que el
 * último elemento nunca quede tapado. Antes cada pantalla tenía su propio número (172, 184, 188 dp)
 * y ninguno contaba la barra del carrito.
 */
val LocalBottomChromeInset = compositionLocalOf<Dp> { 184.dp }

object BottomChrome {
    /** Barra de pestañas (vidrio nativo en iOS 26). */
    val DockHeight: Dp = 74.dp
    /** Barra del carrito compacta. */
    val CartBarHeight: Dp = 52.dp
    /** Separación fija entre la barra del carrito y la de pestañas. */
    val CartBarGap: Dp = 8.dp
    /** Aire debajo del último elemento de una lista. */
    val ContentGap: Dp = 24.dp
}
