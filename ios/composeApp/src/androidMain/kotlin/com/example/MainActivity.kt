package com.example

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.appcheck.configureAppCheck
import com.example.notifications.BravoPushNotificationCenter
import com.example.platform.AppContext

private fun configureHighRefreshRateAndHdr(activity: ComponentActivity) {
    runCatching {
        val window = activity.window ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.colorMode = ActivityInfo.COLOR_MODE_HDR
        }
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        }
        val supportedModes = display?.supportedModes
        val currentMode = display?.mode
        val highestRateMode = supportedModes
            ?.filter { mode ->
                currentMode == null ||
                    (mode.physicalWidth == currentMode.physicalWidth &&
                        mode.physicalHeight == currentMode.physicalHeight)
            }
            ?.maxByOrNull { it.refreshRate }
            ?: supportedModes?.maxByOrNull { it.refreshRate }

        val params = window.attributes
        if (highestRateMode != null) {
            params.preferredDisplayModeId = highestRateMode.modeId
            params.preferredRefreshRate = highestRateMode.refreshRate
        } else {
            params.preferredRefreshRate = 120f
        }
        window.attributes = params
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppContext.init(applicationContext)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        configureHighRefreshRateAndHdr(this)
        val initialFcmScreen = intent?.getStringExtra(BravoPushNotificationCenter.EXTRA_OPEN_SCREEN)
        val initialFcmSku = intent?.getStringExtra(BravoPushNotificationCenter.EXTRA_PRODUCT_SKU)

        setContent {
            BravoRoot(initialFcmScreen = initialFcmScreen, initialFcmSku = initialFcmSku)
        }
    }
}
