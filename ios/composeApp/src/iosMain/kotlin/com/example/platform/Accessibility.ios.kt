package com.example.platform

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

actual fun systemPrefersReducedMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()

actual fun systemPrefersReducedTransparency(): Boolean =
    platform.UIKit.UIAccessibilityIsReduceTransparencyEnabled()

@androidx.compose.runtime.Composable
actual fun KeepScreenAtFullBrightness() {
    androidx.compose.runtime.DisposableEffect(Unit) {
        val screen = platform.UIKit.UIScreen.mainScreen
        val previous = screen.brightness
        screen.brightness = 1.0
        onDispose { screen.brightness = previous }
    }
}
