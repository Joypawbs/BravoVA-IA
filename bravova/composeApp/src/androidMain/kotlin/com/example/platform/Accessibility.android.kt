package com.example.platform

import android.provider.Settings

actual fun systemPrefersReducedMotion(): Boolean = runCatching {
    Settings.Global.getFloat(AppContext.instance.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)

actual fun systemPrefersReducedTransparency(): Boolean = false

@androidx.compose.runtime.Composable
actual fun KeepScreenAtFullBrightness() {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.DisposableEffect(context) {
        var c: android.content.Context? = context
        while (c is android.content.ContextWrapper && c !is android.app.Activity) c = c.baseContext
        val window = (c as? android.app.Activity)?.window
        val previous = window?.attributes?.screenBrightness
        window?.let { w ->
            w.attributes = w.attributes.apply { screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        }
        onDispose {
            window?.let { w ->
                w.attributes = w.attributes.apply {
                    screenBrightness = previous ?: android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }
}
