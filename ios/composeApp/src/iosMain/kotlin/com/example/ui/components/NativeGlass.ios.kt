package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.example.platform.IosBridges
import platform.UIKit.UIViewController

/**
 * Puente hacia SwiftUI (iosApp/iosApp/LiquidGlassBridge.swift): en iOS 26 las barras se
 * dibujan con el vidrio líquido nativo de Apple (.glassEffect) encima de Compose.
 */
interface IosGlassHandle {
    fun viewController(): UIViewController
    fun updateTopBar(
        title: String,
        subtitle: String,
        subtitleArgb: Int,
        hasBack: Boolean,
        backLabel: String,
        symbols: List<String>,
        labels: List<String>,
        texts: List<String>,
        badges: List<String>,
        highlighted: List<Boolean>,
        tints: List<Int>,
        accentArgb: Int,
        dark: Boolean,
        onBack: () -> Unit,
        onAction: (Int) -> Unit
    )
    fun updateDock(
        symbols: List<String>,
        labels: List<String>,
        selected: Int,
        accentArgb: Int,
        dark: Boolean,
        onSelect: (Int) -> Unit
    )
}

interface IosGlassBridge {
    fun isLiquidGlassAvailable(): Boolean
    fun makeTopBar(): IosGlassHandle
    fun makeDock(): IosGlassHandle
}

actual fun nativeLiquidGlassAvailable(): Boolean = IosBridges.glass?.isLiquidGlassAvailable() == true

@OptIn(ExperimentalComposeUiApi::class)
private val overlayProps = UIKitInteropProperties(
    isNativeAccessibilityEnabled = true,
    placedAsOverlay = true
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun NativeGlassTopBar(spec: GlassTopBarSpec, onBack: () -> Unit, onAction: (Int) -> Unit, modifier: Modifier) {
    val bridge = IosBridges.glass ?: return
    val handle = remember { bridge.makeTopBar() }
    val back by rememberUpdatedState(onBack)
    val action by rememberUpdatedState(onAction)
    UIKitViewController(
        factory = { handle.viewController() },
        modifier = modifier,
        update = {
            handle.updateTopBar(
                title = spec.title,
                subtitle = spec.subtitle,
                subtitleArgb = spec.subtitleColor.argbInt(),
                hasBack = spec.hasBack,
                backLabel = spec.backLabel,
                symbols = spec.actions.map { it.symbol },
                labels = spec.actions.map { it.label },
                texts = spec.actions.map { it.text.orEmpty() },
                badges = spec.actions.map { it.badge.orEmpty() },
                highlighted = spec.actions.map { it.highlighted },
                tints = spec.actions.map { (it.tint ?: Color.Unspecified).let { c -> if (c == Color.Unspecified) 0 else c.argbInt() } },
                accentArgb = spec.accent.argbInt(),
                dark = spec.darkMode,
                onBack = { back() },
                onAction = { i -> action(i) }
            )
        },
        properties = overlayProps
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun NativeGlassDock(
    symbols: List<String>,
    labels: List<String>,
    selectedIndex: Int,
    accent: Color,
    darkMode: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier
) {
    val bridge = IosBridges.glass ?: return
    val handle = remember { bridge.makeDock() }
    val select by rememberUpdatedState(onSelect)
    UIKitViewController(
        factory = { handle.viewController() },
        modifier = modifier,
        update = {
            handle.updateDock(symbols, labels, selectedIndex, accent.argbInt(), darkMode) { i -> select(i) }
        },
        properties = overlayProps
    )
}
