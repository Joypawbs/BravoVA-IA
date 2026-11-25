package com.example.platform

import android.provider.Settings

actual fun systemPrefersReducedMotion(): Boolean = runCatching {
    Settings.Global.getFloat(AppContext.instance.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)
