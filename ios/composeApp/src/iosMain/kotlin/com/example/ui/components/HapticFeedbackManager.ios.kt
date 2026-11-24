package com.example.ui.components

import com.example.platform.PlatformView
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

actual object HapticFeedbackManager {
    private fun impact(style: UIImpactFeedbackStyle, intensity: Double = 1.0) {
        val generator = UIImpactFeedbackGenerator(style = style)
        generator.prepare()
        generator.impactOccurredWithIntensity(intensity)
    }

    private fun notify(type: UINotificationFeedbackType) {
        val generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(type)
    }

    actual fun perform(view: PlatformView, style: HapticStyle) {
        when (style) {
            HapticStyle.TICK -> onSelectionTick(view)
            HapticStyle.NAVIGATION -> onNavigationClick(view)
            HapticStyle.CONFIRM -> onConfirmAction(view)
            HapticStyle.ADD_TO_CART -> onAddToCart(view)
            HapticStyle.ASSISTANT -> onAssistantTrigger(view)
            HapticStyle.HEAVY -> onDecrementOrRemove(view, isFullRemove = true)
        }
    }

    actual fun onAddToCart(view: PlatformView) = impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium, 0.9)
    actual fun onDecrementOrRemove(view: PlatformView, isFullRemove: Boolean) =
        if (isFullRemove) impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleRigid, 1.0)
        else impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight, 0.7)

    actual fun onNavigationClick(view: PlatformView) = impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleSoft, 0.6)
    actual fun onAssistantTrigger(view: PlatformView) = impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium, 0.75)
    actual fun onCheckoutConfirm(view: PlatformView) = notify(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    actual fun onOrderCelebration(view: PlatformView) = notify(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    actual fun onBarcodeScanned(view: PlatformView) = impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleRigid, 0.85)
    actual fun onSelectionTick(view: PlatformView) {
        val generator = UISelectionFeedbackGenerator()
        generator.prepare()
        generator.selectionChanged()
    }

    actual fun onConfirmAction(view: PlatformView) = impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium, 1.0)
}
