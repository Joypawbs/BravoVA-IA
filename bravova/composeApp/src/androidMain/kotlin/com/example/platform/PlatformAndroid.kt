package com.example.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.MainActivity
import com.example.R
import com.example.data.local.BravoDatabase
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.math.max

/* ------------------------------------------------------------------ */
/*  Basics                                                             */
/* ------------------------------------------------------------------ */

actual typealias PlatformContext = Context
actual typealias PlatformView = View

@Composable
actual fun currentPlatformContext(): PlatformContext = LocalContext.current

@Composable
actual fun currentPlatformView(): PlatformView = LocalView.current

actual object Log {
    actual fun d(tag: String, msg: String): Int = android.util.Log.d(tag, msg)
    actual fun i(tag: String, msg: String): Int = android.util.Log.i(tag, msg)
    actual fun w(tag: String, msg: String): Int = android.util.Log.w(tag, msg)
    actual fun w(tag: String, msg: String, tr: Throwable?): Int = android.util.Log.w(tag, msg, tr)
    actual fun e(tag: String, msg: String): Int = android.util.Log.e(tag, msg)
    actual fun e(tag: String, msg: String, tr: Throwable?): Int = android.util.Log.e(tag, msg, tr)
}

actual fun platformNanoTime(): Long = java.lang.System.nanoTime()

actual object PlatformInfo {
    actual val isAndroid: Boolean = true
    actual val isIos: Boolean = false
    actual val sdkInt: Int = Build.VERSION.SDK_INT
    actual val deviceModel: String = Build.MODEL.orEmpty()
    actual val hardware: String = Build.HARDWARE.orEmpty()
    actual val socModel: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL.orEmpty() else ""
    actual val supportsGpuBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

actual fun currentDarkModeHint(context: PlatformContext): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/* ------------------------------------------------------------------ */
/*  Preferences                                                        */
/* ------------------------------------------------------------------ */

actual class PlatformKeyValueStore actual constructor(name: String) {
    private val prefs = AppContext.instance.getSharedPreferences(name, Context.MODE_PRIVATE)

    actual fun getString(key: String): String? = runCatching { prefs.getString(key, null) }.getOrNull()
    actual fun getBoolean(key: String, defValue: Boolean): Boolean =
        runCatching { prefs.getBoolean(key, defValue) }.getOrDefault(defValue)

    actual fun getInt(key: String, defValue: Int): Int = runCatching { prefs.getInt(key, defValue) }.getOrDefault(defValue)
    actual fun getLong(key: String, defValue: Long): Long = runCatching { prefs.getLong(key, defValue) }.getOrDefault(defValue)
    actual fun getFloat(key: String, defValue: Float): Float =
        runCatching { prefs.getFloat(key, defValue) }.getOrDefault(defValue)

    actual fun contains(key: String): Boolean = prefs.contains(key)
    actual fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()
    actual fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    actual fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    actual fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    actual fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    actual fun remove(key: String) = prefs.edit().remove(key).apply()
    actual fun clear() = prefs.edit().clear().apply()
}

/* ------------------------------------------------------------------ */
/*  Database                                                            */
/* ------------------------------------------------------------------ */

actual fun getDatabaseBuilder(context: PlatformContext): RoomDatabase.Builder<BravoDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("bravova_local.db")
    return Room.databaseBuilder<BravoDatabase>(context = appContext, name = dbFile.absolutePath)
}

/* ------------------------------------------------------------------ */
/*  Permissions                                                         */
/* ------------------------------------------------------------------ */

private fun AppPermission.manifestName(): String = when (this) {
    AppPermission.CAMERA -> Manifest.permission.CAMERA
    AppPermission.RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
    AppPermission.POST_NOTIFICATIONS ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS
        else "android.permission.POST_NOTIFICATIONS"
    AppPermission.ACCESS_FINE_LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
    AppPermission.ACCESS_COARSE_LOCATION -> Manifest.permission.ACCESS_COARSE_LOCATION
}

actual fun hasPermission(context: PlatformContext, permission: AppPermission): Boolean {
    if (permission == AppPermission.POST_NOTIFICATIONS && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    return ContextCompat.checkSelfPermission(context, permission.manifestName()) == PackageManager.PERMISSION_GRANTED
}

@Composable
actual fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): PermissionLauncher {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onResult(granted)
    }
    return remember(launcher) {
        object : PermissionLauncher {
            override fun launch(permission: AppPermission) = launcher.launch(permission.manifestName())
        }
    }
}

@Composable
actual fun rememberMultiplePermissionsLauncher(onResult: (Map<AppPermission, Boolean>) -> Unit): MultiplePermissionsLauncher {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onResult(AppPermission.entries.associateWith { p -> result[p.manifestName()] == true })
    }
    return remember(launcher) {
        object : MultiplePermissionsLauncher {
            override fun launch(permissions: Array<AppPermission>) =
                launcher.launch(permissions.map { it.manifestName() }.toTypedArray())
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Images                                                              */
/* ------------------------------------------------------------------ */

@Composable
actual fun rememberImagePicker(onResult: (String?) -> Unit): ImagePickerLauncher {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        onResult(uri?.toString())
    }
    return remember(launcher) {
        object : ImagePickerLauncher {
            override fun launch() =
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

actual suspend fun loadImageBytes(context: PlatformContext, uri: String): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
    }.getOrNull()
}

actual suspend fun loadScaledJpeg(context: PlatformContext, uri: String, maxDim: Int): ByteArray? =
    withContext(Dispatchers.IO) {
        try {
            val parsed = Uri.parse(uri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val largest = max(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (largest / sample > maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@withContext null
            ByteArrayOutputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.toByteArray()
            }
        } catch (e: Exception) {
            Log.e("BravoImages", "Failed to decode attached image for multimodal AI", e)
            null
        }
    }

/* ------------------------------------------------------------------ */
/*  System actions / UI helpers                                         */
/* ------------------------------------------------------------------ */

actual object PlatformActions {
    actual fun openUrl(context: PlatformContext, url: String): Boolean = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    actual fun sendEmail(context: PlatformContext, to: String, subject: String, body: String): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$to")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.isSuccess

    actual fun dial(context: PlatformContext, phone: String): Boolean = runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }
}

@Composable
actual fun CameraPreview(torchEnabled: Boolean, modifier: Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    val torch by rememberUpdatedState(torchEnabled)
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                runCatching {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                    camera?.cameraControl?.enableTorch(torch)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
    LaunchedEffect(torchEnabled, camera) {
        runCatching { camera?.cameraControl?.enableTorch(torchEnabled) }
    }
}

/* ------------------------------------------------------------------ */
/*  Speech                                                              */
/* ------------------------------------------------------------------ */

actual class TtsEngine actual constructor(
    context: PlatformContext,
    languageTag: String,
    onStart: () -> Unit,
    onDone: () -> Unit
) {
    private var engine: TextToSpeech? = null

    init {
        var created: TextToSpeech? = null
        created = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                created?.language = java.util.Locale.forLanguageTag(languageTag)
                created?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = onStart()
                    override fun onDone(utteranceId: String?) = onDone()

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = onDone()
                })
            }
        }
        engine = created
    }

    actual fun speak(text: String) {
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bravia_tts")
    }

    actual fun stop() {
        engine?.stop()
    }

    actual fun shutdown() {
        engine?.stop()
        engine?.shutdown()
    }
}

actual fun isSpeechRecognitionAvailable(context: PlatformContext): Boolean =
    SpeechRecognizer.isRecognitionAvailable(context)

actual class SpeechRecognizerEngine actual constructor(context: PlatformContext) {
    private val recognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

    actual fun setListener(listener: SpeechListener) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = listener.onReadyForSpeech()
            override fun onBeginningOfSpeech() = listener.onBeginningOfSpeech()
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() = listener.onEndOfSpeech()
            override fun onError(error: Int) = listener.onError(error)
            override fun onResults(results: Bundle?) =
                listener.onResults(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))

            override fun onPartialResults(partialResults: Bundle?) =
                listener.onPartialResults(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    actual fun startListening(languageTag: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    actual fun stopListening() = recognizer.stopListening()
    actual fun cancel() = recognizer.cancel()
    actual fun destroy() = recognizer.destroy()
}

/* ------------------------------------------------------------------ */
/*  Location & connectivity                                             */
/* ------------------------------------------------------------------ */

private fun Location.toGeoPoint() = GeoPoint(latitude, longitude, accuracy)

actual object LocationProvider {
    actual fun hasPermission(context: PlatformContext): Boolean =
        hasPermission(context, AppPermission.ACCESS_FINE_LOCATION) ||
            hasPermission(context, AppPermission.ACCESS_COARSE_LOCATION)

    @SuppressLint("MissingPermission")
    actual fun lastKnownLocation(context: PlatformContext): GeoPoint? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        var best: Location? = null
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            runCatching {
                if (lm.isProviderEnabled(provider)) {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null && (best == null || loc.time > best!!.time)) best = loc
                }
            }
        }
        return best?.toGeoPoint()
    }

    @SuppressLint("MissingPermission")
    actual fun requestSingleUpdate(context: PlatformContext, onLocation: (GeoPoint) -> Unit) {
        if (!hasPermission(context)) return
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return
        }
        @Suppress("DEPRECATION")
        lm.requestSingleUpdate(provider, object : LocationListener {
            override fun onLocationChanged(location: Location) = onLocation(location.toGeoPoint())

            @Deprecated("Deprecated in Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }, Looper.getMainLooper())
    }
}

actual object ConnectivityMonitor {
    actual fun isOnline(context: PlatformContext): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    actual fun observe(context: PlatformContext, onChange: (Boolean) -> Unit): () -> Unit {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return {}
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onChange(true)
            override fun onLost(network: Network) = onChange(isOnline(context))
        }
        cm.registerDefaultNetworkCallback(callback)
        return { runCatching { cm.unregisterNetworkCallback(callback) } }
    }
}

/* ------------------------------------------------------------------ */
/*  Biometrics (BiometricPrompt + AndroidKeyStore CryptoObject)         */
/* ------------------------------------------------------------------ */

actual object BiometricPlatform {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "bravova_checkout_biometric_aes_key"
    private const val TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"

    actual fun isSupported(context: PlatformContext): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(BiometricManager::class.java)
            val res = bm?.canAuthenticate()
            res == BiometricManager.BIOMETRIC_SUCCESS ||
                res == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        } else {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        }
    } catch (_: Throwable) {
        false
    }

    private fun createHardwareCryptoObject(): BiometricPrompt.CryptoObject? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                keyGenerator.init(spec.build())
                keyGenerator.generateKey()
            }
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            BiometricPrompt.CryptoObject(cipher)
        } catch (_: Throwable) {
            null
        }
    }

    actual fun authenticate(
        context: PlatformContext,
        title: String,
        subtitle: String,
        description: String,
        onSuccess: (cipherHash: Int, hardwareBacked: Boolean) -> Unit,
        onFailed: () -> Unit,
        onFallback: () -> Unit
    ): BiometricCancelHandle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !isSupported(context)) {
            onFallback()
            return null
        }
        return try {
            val signal = CancellationSignal()
            val builder = BiometricPrompt.Builder(context)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) builder.setConfirmationRequired(true)
            builder.setNegativeButton(
                "Usar sensor en pantalla",
                context.mainExecutor,
                DialogInterface.OnClickListener { _, _ -> onFallback() }
            )
            val prompt = builder.build()
            val crypto = createHardwareCryptoObject()
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess(result?.cryptoObject?.cipher?.hashCode() ?: 0, result?.cryptoObject != null)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    onFallback()
                }
            }
            if (crypto != null) {
                try {
                    prompt.authenticate(crypto, signal, context.mainExecutor, callback)
                } catch (_: Throwable) {
                    prompt.authenticate(signal, context.mainExecutor, callback)
                }
            } else {
                prompt.authenticate(signal, context.mainExecutor, callback)
            }
            object : BiometricCancelHandle {
                override fun cancel() = signal.cancel()
            }
        } catch (_: Throwable) {
            onFallback()
            null
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Push (FCM + NotificationCompat)                                     */
/* ------------------------------------------------------------------ */

actual object PushPlatform {
    private const val CHANNEL_ORDER_UPDATES = "bravova_order_updates"
    private const val CHANNEL_EXCLUSIVE_OFFERS = "bravova_exclusive_offers"
    private const val EXTRA_OPEN_SCREEN = "extra_fcm_open_screen"
    private const val EXTRA_PRODUCT_SKU = "extra_fcm_product_sku"
    private const val EXTRA_ORDER_ID = "extra_fcm_order_id"

    actual fun fetchToken(onToken: (String?) -> Unit) {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                onToken(if (task.isSuccessful) task.result else null)
            }
        } catch (e: Exception) {
            onToken(null)
        }
    }

    actual fun subscribe(topic: String) {
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic(topic) }
    }

    actual fun unsubscribe(topic: String) {
        runCatching { FirebaseMessaging.getInstance().unsubscribeFromTopic(topic) }
    }

    actual fun hasNotificationPermission(context: PlatformContext): Boolean =
        hasPermission(context, AppPermission.POST_NOTIFICATIONS)

    actual fun ensureChannels(context: PlatformContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val orders = NotificationChannel(
                CHANNEL_ORDER_UPDATES,
                "Estado de Pedidos en Tiempo Real",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas en vivo de confirmación, preparación y ubicación del repartidor BravoVa"
                enableVibration(true)
            }
            val offers = NotificationChannel(
                CHANNEL_EXCLUSIVE_OFFERS,
                "Ofertas Exclusivas Bravo y El Arca",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Promociones exclusivas, precios especiales en RD$ y bonos de Tarjeta Bravísima"
                enableVibration(true)
            }
            manager.createNotificationChannels(listOf(orders, offers))
        }
    }

    @SuppressLint("MissingPermission")
    actual fun postSystemNotification(
        context: PlatformContext,
        id: String,
        isOrderUpdate: Boolean,
        title: String,
        body: String,
        openScreen: String,
        targetOrderId: String?,
        targetSku: String?,
        progressPct: Int?
    ) {
        ensureChannels(context)
        val channelId = if (isOrderUpdate) CHANNEL_ORDER_UPDATES else CHANNEL_EXCLUSIVE_OFFERS
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SCREEN, openScreen)
            targetOrderId?.let { putExtra(EXTRA_ORDER_ID, it) }
            targetSku?.let { putExtra(EXTRA_PRODUCT_SKU, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFD9232A.toInt())
        if (progressPct != null) builder.setProgress(100, progressPct.coerceIn(0, 100), false)
        try {
            NotificationManagerCompat.from(context).notify(
                (java.lang.System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                builder.build()
            )
        } catch (se: SecurityException) {
            Log.d("BravoFCMService", "POST_NOTIFICATIONS permission not granted yet: ${se.message}")
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Generative AI (Firebase AI Logic)                                   */
/* ------------------------------------------------------------------ */

private class FirebaseGenerativeModel(private val model: GenerativeModel) : GenerativeModelCompat {
    private fun AiContent.toFirebase() = com.google.firebase.ai.type.content {
        image?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { image(it) } }
        text(this@toFirebase.text)
    }

    override fun generateContentStream(content: AiContent): Flow<AiResponseChunk> =
        if (content.image == null) {
            model.generateContentStream(content.text).map { AiResponseChunk(it.text) }
        } else {
            model.generateContentStream(content.toFirebase()).map { AiResponseChunk(it.text) }
        }

    override suspend fun generateContent(content: AiContent): AiResponseChunk {
        val resp = if (content.image == null) model.generateContent(content.text) else model.generateContent(content.toFirebase())
        return AiResponseChunk(resp.text)
    }
}

actual fun createGenerativeModel(modelName: String, config: AiModelConfig): GenerativeModelCompat =
    FirebaseGenerativeModel(
        Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = com.google.firebase.ai.type.generationConfig {
                if (config.jsonOutput) responseMimeType = "application/json"
                if (config.temperature >= 0) temperature = config.temperature.toFloat()
            },
            systemInstruction = config.systemInstruction?.let { rules -> com.google.firebase.ai.type.content { text(rules) } }
        )
    )


actual fun deviceBiometryKind(): BiometryKind = BiometryKind.FINGERPRINT
