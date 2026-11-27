package com.example.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.BravoDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle
import platform.posix.memcpy

/* ------------------------------------------------------------------ */
/*  Basics                                                             */
/* ------------------------------------------------------------------ */

actual abstract class PlatformContext

/** The single iOS "context" (there is no Context concept on iOS). */
object IosPlatformContext : PlatformContext()

actual open class PlatformView

@Composable
actual fun currentPlatformContext(): PlatformContext = IosPlatformContext

@Composable
actual fun currentPlatformView(): PlatformView = remember { PlatformView() }

actual object Log {
    private fun log(level: String, tag: String, msg: String, tr: Throwable? = null): Int {
        NSLog("%@", "[$level/$tag] $msg${tr?.let { " :: ${it.message}" } ?: ""}")
        return 0
    }

    actual fun d(tag: String, msg: String): Int = log("D", tag, msg)
    actual fun i(tag: String, msg: String): Int = log("I", tag, msg)
    actual fun w(tag: String, msg: String): Int = log("W", tag, msg)
    actual fun w(tag: String, msg: String, tr: Throwable?): Int = log("W", tag, msg, tr)
    actual fun e(tag: String, msg: String): Int = log("E", tag, msg)
    actual fun e(tag: String, msg: String, tr: Throwable?): Int = log("E", tag, msg, tr)
}

actual fun platformNanoTime(): Long = (NSDate().timeIntervalSince1970 * 1_000_000_000.0).toLong()

actual object PlatformInfo {
    actual val isAndroid: Boolean = false
    actual val isIos: Boolean = true
    actual val sdkInt: Int = 99
    actual val deviceModel: String = UIDevice.currentDevice.model
    actual val hardware: String = "apple"
    actual val socModel: String = ""
    actual val supportsGpuBlur: Boolean = true
}

actual fun currentDarkModeHint(context: PlatformContext): Boolean =
    UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark

/* ------------------------------------------------------------------ */
/*  Preferences (NSUserDefaults with a per-file key prefix)             */
/* ------------------------------------------------------------------ */

actual class PlatformKeyValueStore actual constructor(name: String) {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val prefix = "$name."
    private fun k(key: String) = prefix + key

    actual fun getString(key: String): String? = defaults.stringForKey(k(key))
    actual fun getBoolean(key: String, defValue: Boolean): Boolean =
        if (contains(key)) defaults.boolForKey(k(key)) else defValue

    actual fun getInt(key: String, defValue: Int): Int =
        if (contains(key)) defaults.integerForKey(k(key)).toInt() else defValue

    actual fun getLong(key: String, defValue: Long): Long =
        if (contains(key)) defaults.integerForKey(k(key)) else defValue

    actual fun getFloat(key: String, defValue: Float): Float =
        if (contains(key)) defaults.floatForKey(k(key)) else defValue

    actual fun contains(key: String): Boolean = defaults.objectForKey(k(key)) != null
    actual fun putString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(k(key)) else defaults.setObject(value, k(key))
    }

    actual fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, k(key))
    actual fun putInt(key: String, value: Int) = defaults.setInteger(value.toLong(), k(key))
    actual fun putLong(key: String, value: Long) = defaults.setInteger(value, k(key))
    actual fun putFloat(key: String, value: Float) = defaults.setFloat(value, k(key))
    actual fun remove(key: String) = defaults.removeObjectForKey(k(key))
    actual fun clear() {
        defaults.dictionaryRepresentation().keys
            .mapNotNull { it as? String }
            .filter { it.startsWith(prefix) }
            .forEach { defaults.removeObjectForKey(it) }
    }
}

/* ------------------------------------------------------------------ */
/*  Database                                                            */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    return requireNotNull(url?.path)
}

actual fun getDatabaseBuilder(context: PlatformContext): RoomDatabase.Builder<BravoDatabase> =
    Room.databaseBuilder<BravoDatabase>(name = documentDirectory() + "/bravova_local.db")

/* ------------------------------------------------------------------ */
/*  NSData <-> ByteArray                                                */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val bytes = ByteArray(size)
    bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, length) }
    return bytes
}

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

/* ------------------------------------------------------------------ */
/*  System actions / UI helpers                                         */
/* ------------------------------------------------------------------ */

actual object PlatformActions {
    private fun open(url: String): Boolean {
        val nsUrl = NSURL.URLWithString(url) ?: return false
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
        return true
    }

    actual fun openUrl(context: PlatformContext, url: String): Boolean = open(url)

    actual fun sendEmail(context: PlatformContext, to: String, subject: String, body: String): Boolean =
        open("mailto:$to?subject=${percentEncode(subject)}&body=${percentEncode(body)}")

    actual fun dial(context: PlatformContext, phone: String): Boolean = open("tel:${phone.filter { it.isDigit() || it == '+' }}")

    private fun percentEncode(value: String): String = URLEncoder.encode(value).replace("+", "%20")
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back button; navigation uses on-screen controls.
}

@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
