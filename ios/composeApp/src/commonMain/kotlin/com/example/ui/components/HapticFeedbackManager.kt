package com.example.ui.components

import com.example.platform.PlatformView

enum class HapticStyle {
    TICK,
    CONFIRM,
    HEAVY,
    ADD_TO_CART,
    NAVIGATION,
    ASSISTANT
}

/** Rich haptics + short synth chimes on Android; UIFeedbackGenerator on iOS. */
expect object HapticFeedbackManager {
    fun perform(view: PlatformView, style: HapticStyle = HapticStyle.TICK)
    fun onAddToCart(view: PlatformView)
    fun onDecrementOrRemove(view: PlatformView, isFullRemove: Boolean = false)
    fun onNavigationClick(view: PlatformView)
    fun onAssistantTrigger(view: PlatformView)
    fun onCheckoutConfirm(view: PlatformView)
    fun onOrderCelebration(view: PlatformView)
    fun onBarcodeScanned(view: PlatformView)
    fun onSelectionTick(view: PlatformView)
    fun onConfirmAction(view: PlatformView)
}

fun performCrispHaptic(view: PlatformView, style: HapticStyle = HapticStyle.TICK) {
    HapticFeedbackManager.perform(view, style)
}
