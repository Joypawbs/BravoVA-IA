package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlin.math.abs

/*
 * Cerrar el teclado como en las apps de iPhone.
 *
 * Para qué: antes, una vez abierto el teclado en una búsqueda no había forma de cerrarlo.
 * Cómo: se pone una sola vez en la raíz de la app y observa los toques sin quitárselos a nadie:
 *  - un toque que ningún botón ni campo atendió (zona vacía, texto, imagen) cierra el teclado;
 *  - arrastrar el dedo para desplazar la pantalla también lo cierra.
 * Tocar el propio campo de texto no lo cierra, porque el campo atiende ese toque.
 */
/** Cierra también el teclado de campos nativos (el buscador de vidrio de iOS). */
expect fun hideNativeKeyboard()

fun Modifier.dismissKeyboardOnOutsideTouch(): Modifier = composed {
    val focusManager: FocusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    this
        // 1) Arrastres reales del dedo (no desplazamientos hechos por código).
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var dismissed = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    val dy = abs(change.position.y - down.position.y)
                    if (!dismissed && dy > viewConfiguration.touchSlop * 2) {
                        hideNativeKeyboard()
                        keyboard?.hide()
                        focusManager.clearFocus()
                        dismissed = true
                    }
                }
            }
        }
        // 2) Toques que nadie atendió: se revisan después de que los hijos tuvieron su turno.
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Final)
                var upUnconsumed = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        upUnconsumed = !change.isConsumed &&
                            (change.position - down.position).getDistance() < viewConfiguration.touchSlop
                        break
                    }
                    if (change.isConsumed) break
                }
                if (upUnconsumed) {
                    hideNativeKeyboard()
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
            }
        }
}
