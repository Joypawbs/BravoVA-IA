package com.example.platform

import platform.Foundation.NSProcessInfo

actual object DevicePerformance {
    actual val isLowEnd: Boolean
        get() {
            val info = NSProcessInfo.processInfo
            val totalGb = info.physicalMemory.toDouble() / (1024.0 * 1024 * 1024)
            return totalGb < 3.8 || info.lowPowerModeEnabled
        }
}
