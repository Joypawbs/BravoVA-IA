package com.example

import androidx.compose.ui.window.ComposeUIViewController
import com.example.platform.AppContext
import com.example.platform.IosAiBridge
import com.example.platform.IosBridges
import com.example.platform.IosPlatformContext
import platform.UIKit.UIViewController

/**
 * Entry point called from Swift (ContentView.swift):
 *   MainViewControllerKt.MainViewController(aiBridge: FirebaseAiBridge())
 */
fun MainViewController(
    aiBridge: IosAiBridge?,
    glassBridge: com.example.ui.components.IosGlassBridge?,
    authBridge: com.example.platform.IosAuthBridge?
): UIViewController {
    AppContext.init(IosPlatformContext)
    IosBridges.ai = aiBridge
    IosBridges.auth = authBridge
    IosBridges.glass = glassBridge
    return ComposeUIViewController { BravoRoot() }
}
