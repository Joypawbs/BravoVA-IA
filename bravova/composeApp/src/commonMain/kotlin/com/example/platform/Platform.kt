package com.example.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow

/* ------------------------------------------------------------------ */
/*  Context / View stand-ins                                           */
/* ------------------------------------------------------------------ */

/** android.content.Context on Android, a lightweight holder on iOS. */
expect abstract class PlatformContext

/** android.view.View on Android (used for haptics), a dummy on iOS. */
expect open class PlatformView

@Composable
expect fun currentPlatformContext(): PlatformContext

@Composable
expect fun currentPlatformView(): PlatformView

/** Application-wide context, set once at start-up by MainActivity / MainViewController. */
object AppContext {
    private var _instance: PlatformContext? = null
    val instance: PlatformContext
        get() = _instance ?: error("AppContext not initialised")

    fun init(context: PlatformContext) {
        if (_instance == null) _instance = context
    }
}

/** Equivalent of android.os.Build.VERSION_CODES for the branches kept in common code. */
object AndroidVersions {
    const val M = 23
    const val N = 24
    const val O = 26
    const val P = 28
    const val Q = 29
    const val R = 30
    const val S = 31
    const val TIRAMISU = 33
}

expect object PlatformInfo {
    val isAndroid: Boolean
    val isIos: Boolean

    /** Android API level. On iOS a high value so "modern API" branches are taken. */
    val sdkInt: Int
    val deviceModel: String
    val hardware: String
    val socModel: String
    val supportsGpuBlur: Boolean
}

/* ------------------------------------------------------------------ */
/*  Key/value preferences (SharedPreferences / NSUserDefaults)         */
/* ------------------------------------------------------------------ */

expect class PlatformKeyValueStore(name: String) {
    fun getString(key: String): String?
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun getInt(key: String, defValue: Int): Int
    fun getLong(key: String, defValue: Long): Long
    fun getFloat(key: String, defValue: Float): Float
    fun contains(key: String): Boolean
    fun putString(key: String, value: String?)
    fun putBoolean(key: String, value: Boolean)
    fun putInt(key: String, value: Int)
    fun putLong(key: String, value: Long)
    fun putFloat(key: String, value: Float)
    fun remove(key: String)
    fun clear()
}

class SharedPrefs internal constructor(private val store: PlatformKeyValueStore) {
    fun getString(key: String, defValue: String?): String? = store.getString(key) ?: defValue
    fun getBoolean(key: String, defValue: Boolean): Boolean = store.getBoolean(key, defValue)
    fun getInt(key: String, defValue: Int): Int = store.getInt(key, defValue)
    fun getLong(key: String, defValue: Long): Long = store.getLong(key, defValue)
    fun getFloat(key: String, defValue: Float): Float = store.getFloat(key, defValue)
    fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        store.getString(key)?.let { raw -> if (raw.isEmpty()) emptySet() else raw.split('\u001F').toSet() } ?: defValues

    fun contains(key: String): Boolean = store.contains(key)
    fun edit(): Editor = Editor(store)

    class Editor internal constructor(private val store: PlatformKeyValueStore) {
        private val ops = mutableListOf<() -> Unit>()
        fun putString(key: String, value: String?): Editor = apply { ops += { store.putString(key, value) } }
        fun putBoolean(key: String, value: Boolean): Editor = apply { ops += { store.putBoolean(key, value) } }
        fun putInt(key: String, value: Int): Editor = apply { ops += { store.putInt(key, value) } }
        fun putLong(key: String, value: Long): Editor = apply { ops += { store.putLong(key, value) } }
        fun putFloat(key: String, value: Float): Editor = apply { ops += { store.putFloat(key, value) } }
        fun putStringSet(key: String, values: Set<String>?): Editor =
            apply { ops += { store.putString(key, values?.joinToString("\u001F")) } }

        fun remove(key: String): Editor = apply { ops += { store.remove(key) } }
        fun clear(): Editor = apply { ops += { store.clear() } }
        fun apply() {
            ops.forEach { it() }; ops.clear()
        }

        fun commit(): Boolean {
            apply(); return true
        }
    }
}

private val prefsCache = mutableMapOf<String, SharedPrefs>()

/** Replacement for context.getSharedPreferences(name, Context.MODE_PRIVATE). */
fun sharedPrefs(name: String): SharedPrefs =
    prefsCache.getOrPut(name) { SharedPrefs(PlatformKeyValueStore(name)) }

/* ------------------------------------------------------------------ */
/*  Runtime permissions                                                */
/* ------------------------------------------------------------------ */

enum class AppPermission {
    CAMERA,
    RECORD_AUDIO,
    POST_NOTIFICATIONS,
    ACCESS_FINE_LOCATION,
    ACCESS_COARSE_LOCATION
}

expect fun hasPermission(context: PlatformContext, permission: AppPermission): Boolean

interface PermissionLauncher {
    fun launch(permission: AppPermission)
}

interface MultiplePermissionsLauncher {
    fun launch(permissions: Array<AppPermission>)
}

@Composable
expect fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): PermissionLauncher

@Composable
expect fun rememberMultiplePermissionsLauncher(onResult: (Map<AppPermission, Boolean>) -> Unit): MultiplePermissionsLauncher

/* ------------------------------------------------------------------ */
/*  Images                                                             */
/* ------------------------------------------------------------------ */

interface ImagePickerLauncher {
    fun launch()
}

/** Opens the system photo picker; returns a URI/path string readable by [loadImageBytes]. */
@Composable
expect fun rememberImagePicker(onResult: (String?) -> Unit): ImagePickerLauncher

/** Raw bytes of an image picked by the user (content:// on Android, file path on iOS). */
expect suspend fun loadImageBytes(context: PlatformContext, uri: String): ByteArray?

/** Image scaled so its longest side is <= [maxDim] and encoded as JPEG (for Gemini multimodal). */
expect suspend fun loadScaledJpeg(context: PlatformContext, uri: String, maxDim: Int = 1024): ByteArray?

/* ------------------------------------------------------------------ */
/*  System actions                                                      */
/* ------------------------------------------------------------------ */

expect object PlatformActions {
    fun openUrl(context: PlatformContext, url: String): Boolean
    fun sendEmail(context: PlatformContext, to: String, subject: String, body: String): Boolean
    fun dial(context: PlatformContext, phone: String): Boolean
}

@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/** Android 12+ Material You colors; null where not supported (iOS). */
@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** Live camera preview for the barcode scanner. */
@Composable
expect fun CameraPreview(torchEnabled: Boolean, modifier: Modifier = Modifier)

/* ------------------------------------------------------------------ */
/*  Speech                                                              */
/* ------------------------------------------------------------------ */

expect class TtsEngine(
    context: PlatformContext,
    languageTag: String,
    onStart: () -> Unit,
    onDone: () -> Unit
) {
    fun speak(text: String)
    fun stop()
    fun shutdown()
}

interface SpeechListener {
    fun onReadyForSpeech()
    fun onBeginningOfSpeech()
    fun onEndOfSpeech()
    fun onError(error: Int)
    fun onResults(matches: List<String>?)
    fun onPartialResults(matches: List<String>?)
}

expect fun isSpeechRecognitionAvailable(context: PlatformContext): Boolean

expect class SpeechRecognizerEngine(context: PlatformContext) {
    fun setListener(listener: SpeechListener)
    fun startListening(languageTag: String)
    fun stopListening()
    fun cancel()
    fun destroy()
}

/* ------------------------------------------------------------------ */
/*  Location & connectivity                                             */
/* ------------------------------------------------------------------ */

data class GeoPoint(val latitude: Double, val longitude: Double, val accuracyMeters: Float = 0f)

expect object LocationProvider {
    fun hasPermission(context: PlatformContext): Boolean
    fun lastKnownLocation(context: PlatformContext): GeoPoint?

    /** Single fresh fix; calls [onLocation] at most once. */
    fun requestSingleUpdate(context: PlatformContext, onLocation: (GeoPoint) -> Unit)
}

expect object ConnectivityMonitor {
    fun isOnline(context: PlatformContext): Boolean

    /** Registers a callback; returns an unregister function. */
    fun observe(context: PlatformContext, onChange: (Boolean) -> Unit): () -> Unit
}

/* ------------------------------------------------------------------ */
/*  Biometrics                                                          */
/* ------------------------------------------------------------------ */

interface BiometricCancelHandle {
    fun cancel()
}

expect object BiometricPlatform {
    fun isSupported(context: PlatformContext): Boolean
    fun authenticate(
        context: PlatformContext,
        title: String,
        subtitle: String,
        description: String,
        onSuccess: (cipherHash: Int, hardwareBacked: Boolean) -> Unit,
        onFailed: () -> Unit,
        onFallback: () -> Unit
    ): BiometricCancelHandle?
}

/* ------------------------------------------------------------------ */
/*  Push notifications                                                  */
/* ------------------------------------------------------------------ */

expect object PushPlatform {
    fun fetchToken(onToken: (String?) -> Unit)
    fun subscribe(topic: String)
    fun unsubscribe(topic: String)
    fun hasNotificationPermission(context: PlatformContext): Boolean
    fun ensureChannels(context: PlatformContext)
    fun postSystemNotification(
        context: PlatformContext,
        id: String,
        isOrderUpdate: Boolean,
        title: String,
        body: String,
        openScreen: String,
        targetOrderId: String?,
        targetSku: String?,
        progressPct: Int?
    )
}

/* ------------------------------------------------------------------ */
/*  Auth                                                                */
/* ------------------------------------------------------------------ */

data class AuthUser(val uid: String, val displayName: String?, val email: String?)

/** Token de depuración de App Check en instalaciones sin App Attest (Sideloadly), o null. */
expect fun appCheckDebugTokenOrNull(): String?

expect object AuthPlatform {
    val isSignInAvailable: Boolean
    fun currentUser(): AuthUser?
    fun userFlow(): Flow<AuthUser?>
    suspend fun attemptSilentSignIn(context: PlatformContext): Result<AuthUser?>

    /** Returns success(null) when the sheet was dismissed or a local fallback session was used. */
    suspend fun launchSignInSheet(context: PlatformContext, onFallbackSession: () -> Unit): Result<AuthUser?>
    suspend fun signOut(context: PlatformContext)
}

/* ------------------------------------------------------------------ */
/*  Generative AI (Firebase AI Logic on Android, Swift bridge on iOS)   */
/* ------------------------------------------------------------------ */

class AiContent(val text: String, val image: ByteArray?)

class AiContentBuilder {
    private var text = StringBuilder()
    private var image: ByteArray? = null
    fun text(value: String) {
        if (text.isNotEmpty()) text.append('\n'); text.append(value)
    }

    fun image(jpeg: ByteArray) {
        image = jpeg
    }

    fun build() = AiContent(text.toString(), image)
}

fun content(block: AiContentBuilder.() -> Unit): AiContent = AiContentBuilder().apply(block).build()

class AiResponseChunk(val text: String?)

interface GenerativeModelCompat {
    fun generateContentStream(content: AiContent): Flow<AiResponseChunk>
    suspend fun generateContent(content: AiContent): AiResponseChunk
    fun generateContentStream(prompt: String): Flow<AiResponseChunk> = generateContentStream(AiContent(prompt, null))
    suspend fun generateContent(prompt: String): AiResponseChunk = generateContent(AiContent(prompt, null))
}

/**
 * Cómo se comporta el modelo. [systemInstruction] va aparte del mensaje (el modelo la trata como
 * reglas, no como conversación), [jsonOutput] obliga a responder en JSON y [temperature] baja hace
 * las respuestas más predecibles. Un valor negativo deja la temperatura por defecto del modelo.
 */
data class AiModelConfig(
    val systemInstruction: String? = null,
    val jsonOutput: Boolean = false,
    val temperature: Double = -1.0
)

expect fun createGenerativeModel(modelName: String, config: AiModelConfig = AiModelConfig()): GenerativeModelCompat

/* ------------------------------------------------------------------ */
/*  Misc                                                                */
/* ------------------------------------------------------------------ */

/** Whether the system UI is currently in dark mode. */
expect fun currentDarkModeHint(context: PlatformContext): Boolean

/** Platform-specific database file location (Room KMP). */
expect fun getDatabaseBuilder(context: PlatformContext): androidx.room.RoomDatabase.Builder<com.example.data.local.BravoDatabase>


/** Tipo de biometría del equipo, para usar el nombre e ícono correctos (Face ID, Touch ID o huella). */
enum class BiometryKind(val displayName: String, val instruction: String, val sensorHint: String) {
    FACE_ID("Face ID", "Usa Face ID", "Toca para usar Face ID"),
    TOUCH_ID("Touch ID", "Coloca tu dedo en Touch ID", "Toca para usar Touch ID"),
    OPTIC_ID("Optic ID", "Usa Optic ID", "Toca para usar Optic ID"),
    FINGERPRINT("huella", "Coloca tu huella digital", "Toca el sensor de huella para autorizar")
}

expect fun deviceBiometryKind(): BiometryKind
