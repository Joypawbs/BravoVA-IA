package com.example.ui.components

import platform.UIKit.UIApplication
import platform.objc.sel_registerName

/** Pide a quien tenga el teclado (incluido un TextField de SwiftUI) que lo suelte. */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual fun hideNativeKeyboard() {
    UIApplication.sharedApplication.sendAction(sel_registerName("resignFirstResponder"), to = null, from = null, forEvent = null)
}
