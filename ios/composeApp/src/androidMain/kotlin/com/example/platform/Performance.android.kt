package com.example.platform

import android.app.ActivityManager
import android.content.Context
import android.os.Build

actual object DevicePerformance {
    actual val isLowEnd: Boolean by lazy {
        runCatching {
            val am = AppContext.instance.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val totalGb = info.totalMem / (1024.0 * 1024 * 1024)
            am.isLowRamDevice ||
                totalGb < 4.0 ||
                Runtime.getRuntime().availableProcessors() <= 4 ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        }.getOrDefault(false)
    }
}
