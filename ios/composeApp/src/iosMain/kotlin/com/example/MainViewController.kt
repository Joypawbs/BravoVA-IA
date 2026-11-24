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
fun MainViewController(aiBridge: IosAiBridge?): UIViewController {
    AppContext.init(IosPlatformContext)
    IosBridges.ai = aiBridge
    return ComposeUIViewController { BravoRoot() }
}
