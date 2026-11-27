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
    installCrashReporter()
    IosBridges.ai = aiBridge
    IosBridges.auth = authBridge
    IosBridges.glass = glassBridge
    // Rendimiento en iPhone:
    // - parallelRendering: Compose prepara los comandos de Metal en un hilo aparte, así el hilo
    //   principal queda libre para gestos y animaciones (clave a 120 Hz con ProMotion).
    return ComposeUIViewController(configure = { parallelRendering = true }) { BravoRoot() }
}

/**
 * Diagnóstico: si una excepción de Kotlin llega sin capturar, se escribe completa (tipo, mensaje y
 * pila) en el registro del sistema con la etiqueta BRAVO_CRASH y se guarda para mostrarla en el
 * siguiente arranque. Así un cierre inesperado deja rastro en lugar de solo volver a la pantalla de inicio.
 */
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
private fun installCrashReporter() {
    val previous = setUnhandledExceptionHook(null)
    setUnhandledExceptionHook { t ->
        val report = t.stackTraceToString().take(6000)
        platform.Foundation.NSLog(("BRAVO_CRASH " + report).replace("%", "%%"))
        platform.Foundation.NSUserDefaults.standardUserDefaults.setObject(report, forKey = "bravo_last_crash")
        platform.Foundation.NSUserDefaults.standardUserDefaults.synchronize()
        previous?.invoke(t)
    }
}
