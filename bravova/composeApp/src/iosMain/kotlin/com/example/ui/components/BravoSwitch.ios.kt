package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISwitch
import platform.darwin.NSObject

/** Recibe el cambio del UISwitch (patrón target-action de UIKit). */
private class SwitchTarget : NSObject() {
    var onChange: (Boolean) -> Unit = {}

    @ObjCAction
    fun valueChanged(sender: UISwitch) {
        onChange(sender.on)
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun BravoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
    modifier: Modifier,
    enabled: Boolean
) {
    val change by rememberUpdatedState(onCheckedChange)
    val target = remember { SwitchTarget() }
    // Si la app no acepta el cambio (p. ej. bloquear la tarjeta pide Face ID primero),
    // "sync" obliga a volver a dibujar el UISwitch con el valor real
    var sync by remember { mutableIntStateOf(0) }
    target.onChange = { change(it); sync++ }
    UIKitView(
        factory = {
            UISwitch().apply {
                addTarget(target, NSSelectorFromString("valueChanged:"), UIControlEventValueChanged)
            }
        },
        // Tamaño fijo del UISwitch de Apple: 51 × 31 pt
        modifier = modifier.size(width = 51.dp, height = 31.dp),
        update = { sw ->
            sync // se lee para que update vuelva a correr tras cada toque
            if (sw.on != checked) sw.setOn(checked, animated = true)
            sw.enabled = enabled
            sw.onTintColor = UIColor.colorWithRed(
                accent.red.toDouble(), accent.green.toDouble(), accent.blue.toDouble(), accent.alpha.toDouble()
            )
        },
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true)
    )
}
