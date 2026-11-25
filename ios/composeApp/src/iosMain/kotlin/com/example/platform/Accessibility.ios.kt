package com.example.platform

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

actual fun systemPrefersReducedMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
