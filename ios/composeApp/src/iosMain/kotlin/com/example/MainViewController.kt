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
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun MainViewController(
    aiBridge: IosAiBridge?,
    glassBridge: com.example.ui.components.IosGlassBridge?,
    authBridge: com.example.platform.IosAuthBridge?
): UIViewController {
    AppContext.init(IosPlatformContext)
    IosBridges.ai = aiBridge
    IosBridges.auth = authBridge
    IosBridges.glass = glassBridge
    // Rendimiento en iPhone:
    // - parallelRendering: Compose prepara los comandos de Metal en un hilo aparte, así el hilo
    //   principal queda libre para gestos y animaciones (clave a 120 Hz con ProMotion).
    return ComposeUIViewController(configure = { parallelRendering = true }) { BravoRoot() }
}
