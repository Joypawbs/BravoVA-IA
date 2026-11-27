package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
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
        minimized: Boolean,
        onSelect: (Int) -> Unit,
        onSearch: () -> Unit
    )
    fun updateCartBar(count: Int, total: String, accentArgb: Int, dark: Boolean, onTap: () -> Unit)
    fun updateToast(text: String, symbol: String, accentArgb: Int, dark: Boolean)
    fun updateSearchField(
        text: String,
        placeholder: String,
        accentArgb: Int,
        dark: Boolean,
        trailingSymbol: String,
        onChange: (String) -> Unit,
        onSubmit: () -> Unit,
        onFocus: (Boolean) -> Unit,
        onTrailing: () -> Unit
    )
    fun focusSearchField()
    /** Muestra u oculta la barra con un fundido nativo (SwiftUI); oculta no recibe toques. */
    fun setVisible(visible: Boolean)
}

interface IosGlassBridge {
    fun isLiquidGlassAvailable(): Boolean
    fun makeTopBar(): IosGlassHandle
    fun makeDock(): IosGlassHandle
    fun makeCartBar(): IosGlassHandle
    fun makeToast(): IosGlassHandle
    fun makeSearchField(): IosGlassHandle
    fun presentQuickView(
        title: String,
        subtitle: String,
        price: String,
        imageUrl: String,
        isFavorite: Boolean,
        accentArgb: Int,
        onAdd: () -> Unit,
        onFavorite: () -> Unit,
        onShare: () -> Unit
    ): Boolean
    fun startDeliveryActivity(orderId: String, branch: String, stage: String, etaMinutes: Int, progress: Double)
    fun updateDeliveryActivity(stage: String, etaMinutes: Int, progress: Double)
    fun endDeliveryActivity()
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
    val glassVisible = rememberNativeGlassVisible()
    SideEffect { handle.setVisible(glassVisible) }
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
    modifier: Modifier,
    minimized: Boolean,
    onSearch: () -> Unit
) {
    val bridge = IosBridges.glass ?: return
    val handle = remember { bridge.makeDock() }
    val glassVisible = rememberNativeGlassVisible()
    SideEffect { handle.setVisible(glassVisible) }
    val select by rememberUpdatedState(onSelect)
    val search by rememberUpdatedState(onSearch)
    UIKitViewController(
        factory = { handle.viewController() },
        modifier = modifier,
        update = {
            handle.updateDock(symbols, labels, selectedIndex, accent.argbInt(), darkMode, minimized, { i -> select(i) }, { search() })
        },
        properties = overlayProps
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun NativeGlassCartBar(count: Int, total: String, accent: Color, darkMode: Boolean, onTap: () -> Unit, modifier: Modifier) {
    val bridge = IosBridges.glass ?: return
    val handle = remember { bridge.makeCartBar() }
    val glassVisible = rememberNativeGlassVisible()
    SideEffect { handle.setVisible(glassVisible) }
    val tap by rememberUpdatedState(onTap)
    UIKitViewController(
        factory = { handle.viewController() },
        modifier = modifier,
        update = { handle.updateCartBar(count, total, accent.argbInt(), darkMode) { tap() } },
        properties = overlayProps
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun NativeGlassToast(text: String, symbol: String, accent: Color, darkMode: Boolean, modifier: Modifier) {
    val bridge = IosBridges.glass ?: return
    val handle = remember { bridge.makeToast() }
    val glassVisible = rememberNativeGlassVisible()
    SideEffect { handle.setVisible(glassVisible) }
    UIKitViewController(
        factory = { handle.viewController() },
        modifier = modifier,
        update = { handle.updateToast(text, symbol, accent.argbInt(), darkMode) },
        properties = overlayProps
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun NativeGlassSearchField(
    text: String,
    placeholder: String,
    accent: Color,
    darkMode: Boolean,
    trailingSymbol: String,
    focusSignal: Int,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFocus: (Boolean) -> Unit,
    onTrailing: () -> Unit,
    modifier: Modifier
) {
    val bridge = IosBridges.glass ?: return
    val handle = remember { bridge.makeSearchField() }
    val glassVisible = rememberNativeGlassVisible()
    SideEffect { handle.setVisible(glassVisible) }
    val change by rememberUpdatedState(onChange)
    val submit by rememberUpdatedState(onSubmit)
    val focus by rememberUpdatedState(onFocus)
    val trailing by rememberUpdatedState(onTrailing)
    androidx.compose.runtime.LaunchedEffect(focusSignal) { if (focusSignal > 0) handle.focusSearchField() }
    UIKitViewController(
        factory = { handle.viewController() },
        modifier = modifier,
        update = {
            handle.updateSearchField(
                text, placeholder, accent.argbInt(), darkMode, trailingSymbol,
                { change(it) }, { submit() }, { focus(it) }, { trailing() }
            )
        },
        properties = overlayProps
    )
}

actual object PlatformExtras {
    actual fun presentQuickView(
        title: String, subtitle: String, price: String, imageUrl: String, isFavorite: Boolean, accentArgb: Int,
        onAdd: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit
    ): Boolean = IosBridges.glass?.presentQuickView(title, subtitle, price, imageUrl, isFavorite, accentArgb, onAdd, onFavorite, onShare) == true

    actual fun startDeliveryActivity(orderId: String, branch: String, stage: String, etaMinutes: Int, progress: Double) {
        IosBridges.glass?.startDeliveryActivity(orderId, branch, stage, etaMinutes, progress)
    }

    actual fun updateDeliveryActivity(stage: String, etaMinutes: Int, progress: Double) {
        IosBridges.glass?.updateDeliveryActivity(stage, etaMinutes, progress)
    }

    actual fun endDeliveryActivity() {
        IosBridges.glass?.endDeliveryActivity()
    }
}
