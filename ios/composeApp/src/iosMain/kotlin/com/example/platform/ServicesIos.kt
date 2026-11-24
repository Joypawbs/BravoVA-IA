package com.example.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.AVFAudio.setActive
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAuthenticationFailed
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

/* ------------------------------------------------------------------ */
/*  Text to speech                                                      */
/* ------------------------------------------------------------------ */

private class SpeechDelegate(
    private val onStart: () -> Unit,
    private val onDone: () -> Unit
) : NSObject(), AVSpeechSynthesizerDelegateProtocol {
    @ObjCSignatureOverride
    override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didStartSpeechUtterance: AVSpeechUtterance) = onStart()

    @ObjCSignatureOverride
    override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) = onDone()

    @ObjCSignatureOverride
    override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didCancelSpeechUtterance: AVSpeechUtterance) = onDone()
}

actual class TtsEngine actual constructor(
    context: PlatformContext,
    private val languageTag: String,
    onStart: () -> Unit,
    onDone: () -> Unit
) {
    private val synthesizer = AVSpeechSynthesizer()
    private val delegate = SpeechDelegate(onStart, onDone)

    init {
        synthesizer.delegate = delegate
    }

    actual fun speak(text: String) {
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, mode = AVAudioSessionModeDefault, options = 0u, error = null)
            session.setActive(true, error = null)
        }
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(languageTag)
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("es-MX")
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("es-ES")
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        synthesizer.speakUtterance(utterance)
    }

    actual fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    actual fun shutdown() {
        stop()
        synthesizer.delegate = null
    }
}

/* ------------------------------------------------------------------ */
/*  Speech recognition (Speech framework + AVAudioEngine)               */
/* ------------------------------------------------------------------ */

actual fun isSpeechRecognitionAvailable(context: PlatformContext): Boolean =
    SFSpeechRecognizer(locale = NSLocale(localeIdentifier = "es-DO")).isAvailable() ||
        SFSpeechRecognizer().isAvailable()

@OptIn(ExperimentalForeignApi::class)
actual class SpeechRecognizerEngine actual constructor(context: PlatformContext) {
    private var listener: SpeechListener? = null
    private var audioEngine: AVAudioEngine? = null
    private var request: SFSpeechAudioBufferRecognitionRequest? = null
    private var task: SFSpeechRecognitionTask? = null
    private var finished = false

    actual fun setListener(listener: SpeechListener) {
        this.listener = listener
    }

    actual fun startListening(languageTag: String) {
        cancel()
        finished = false
        val recognizer = SFSpeechRecognizer(locale = NSLocale(localeIdentifier = languageTag))
        val l = listener
        if (!recognizer.isAvailable()) {
            l?.onError(5)
            return
        }
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryRecord,
            mode = AVAudioSessionModeMeasurement,
            options = AVAudioSessionCategoryOptionDuckOthers,
            error = null
        )
        session.setActive(true, withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, error = null)

        val engine = AVAudioEngine()
        val req = SFSpeechAudioBufferRecognitionRequest()
        req.shouldReportPartialResults = true
        val input = engine.inputNode
        val format = input.outputFormatForBus(0u)
        input.installTapOnBus(0u, bufferSize = 1024u, format = format) { buffer, _ ->
            if (buffer != null) req.appendAudioPCMBuffer(buffer)
        }
        engine.prepare()
        if (!engine.startAndReturnError(null)) {
            l?.onError(3)
            return
        }
        audioEngine = engine
        request = req
        l?.onReadyForSpeech()
        l?.onBeginningOfSpeech()

        task = recognizer.recognitionTaskWithRequest(req) { result, error ->
            onMain {
                if (finished) return@onMain
                if (result != null) {
                    val text = result.bestTranscription.formattedString
                    if (result.isFinal()) {
                        finished = true
                        teardownAudio()
                        listener?.onEndOfSpeech()
                        listener?.onResults(listOf(text))
                    } else {
                        listener?.onPartialResults(listOf(text))
                    }
                } else if (error != null) {
                    finished = true
                    teardownAudio()
                    listener?.onEndOfSpeech()
                    listener?.onError(error.code.toInt())
                }
            }
        }
    }

    private fun teardownAudio() {
        audioEngine?.let { e ->
            e.stop()
            e.inputNode.removeTapOnBus(0u)
        }
        audioEngine = null
        request?.endAudio()
        runCatching {
            AVAudioSession.sharedInstance().setActive(false, withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, error = null)
        }
    }

    actual fun stopListening() {
        // Ends audio capture; the task then delivers its final result.
        audioEngine?.let { e ->
            e.stop()
            e.inputNode.removeTapOnBus(0u)
        }
        audioEngine = null
        request?.endAudio()
    }

    actual fun cancel() {
        task?.cancel()
        task = null
        teardownAudio()
        request = null
    }

    actual fun destroy() {
        cancel()
        listener = null
    }
}

/* ------------------------------------------------------------------ */
/*  Location                                                            */
/* ------------------------------------------------------------------ */

private fun CLLocation.toGeoPoint(): GeoPoint = coordinate.useContents {
    GeoPoint(latitude, longitude, horizontalAccuracy.toFloat())
}

internal class LocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {
    var onLocation: ((GeoPoint) -> Unit)? = null
    var onAuthorization: ((Boolean) -> Unit)? = null

    @ObjCSignatureOverride
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        val cb = onLocation
        onLocation = null
        cb?.invoke(loc.toGeoPoint())
    }

    @ObjCSignatureOverride
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onLocation = null
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = manager.authorizationStatus
        if (status == kCLAuthorizationStatusNotDetermined) return
        val cb = onAuthorization
        onAuthorization = null
        cb?.invoke(status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways)
    }
}

internal object LocationAuthorization {
    val manager: CLLocationManager by lazy {
        CLLocationManager().apply {
            desiredAccuracy = kCLLocationAccuracyHundredMeters
            delegate = locationDelegate
        }
    }
    val locationDelegate = LocationDelegate()

    fun request(onResult: (Boolean) -> Unit) {
        val status = manager.authorizationStatus
        if (status != kCLAuthorizationStatusNotDetermined) {
            onResult(status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways)
            return
        }
        locationDelegate.onAuthorization = onResult
        manager.requestWhenInUseAuthorization()
    }
}

actual object LocationProvider {
    actual fun hasPermission(context: PlatformContext): Boolean =
        hasPermission(context, AppPermission.ACCESS_FINE_LOCATION)

    actual fun lastKnownLocation(context: PlatformContext): GeoPoint? =
        if (hasPermission(context)) LocationAuthorization.manager.location?.toGeoPoint() else null

    actual fun requestSingleUpdate(context: PlatformContext, onLocation: (GeoPoint) -> Unit) {
        if (!hasPermission(context)) return
        LocationAuthorization.locationDelegate.onLocation = onLocation
        LocationAuthorization.manager.requestLocation()
    }
}

/* ------------------------------------------------------------------ */
/*  Connectivity (Network framework)                                    */
/* ------------------------------------------------------------------ */

actual object ConnectivityMonitor {
    private var lastKnownOnline = true

    actual fun isOnline(context: PlatformContext): Boolean = lastKnownOnline

    actual fun observe(context: PlatformContext, onChange: (Boolean) -> Unit): () -> Unit {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            val online = nw_path_get_status(path) == nw_path_status_satisfied
            lastKnownOnline = online
            onChange(online)
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
        return { nw_path_monitor_cancel(monitor) }
    }
}

/* ------------------------------------------------------------------ */
/*  Biometrics (Face ID / Touch ID)                                     */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalForeignApi::class)
actual object BiometricPlatform {
    actual fun isSupported(context: PlatformContext): Boolean =
        LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)

    actual fun authenticate(
        context: PlatformContext,
        title: String,
        subtitle: String,
        description: String,
        onSuccess: (cipherHash: Int, hardwareBacked: Boolean) -> Unit,
        onFailed: () -> Unit,
        onFallback: () -> Unit
    ): BiometricCancelHandle? {
        val la = LAContext()
        if (!la.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)) {
            onFallback()
            return null
        }
        la.localizedFallbackTitle = "Usar sensor en pantalla"
        la.evaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, localizedReason = "$title — $subtitle") { success, error ->
            onMain {
                when {
                    success -> onSuccess(la.hashCode(), true)
                    error?.code == LAErrorAuthenticationFailed -> onFailed()
                    else -> onFallback()
                }
            }
        }
        return object : BiometricCancelHandle {
            override fun cancel() = la.invalidate()
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Push: local notifications (FCM for iOS needs APNs setup, see README) */
/* ------------------------------------------------------------------ */

private object ForegroundNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or UNNotificationPresentationOptionSound
        )
    }
}

actual object PushPlatform {
    actual fun fetchToken(onToken: (String?) -> Unit) = onToken(null)
    actual fun subscribe(topic: String) = Unit
    actual fun unsubscribe(topic: String) = Unit

    actual fun hasNotificationPermission(context: PlatformContext): Boolean =
        hasPermission(context, AppPermission.POST_NOTIFICATIONS)

    actual fun ensureChannels(context: PlatformContext) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        if (center.delegate == null) center.delegate = ForegroundNotificationDelegate
        NotificationPermissionCache.refresh()
    }

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
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(if (progressPct != null) "$body ($progressPct%)" else body)
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(
                mapOf<Any?, Any?>(
                    "openScreen" to openScreen,
                    "orderId" to (targetOrderId ?: ""),
                    "sku" to (targetSku ?: "")
                )
            )
        }
        val request = UNNotificationRequest.requestWithIdentifier(id, content, null)
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
    }
}

/* ------------------------------------------------------------------ */
/*  Auth: Firebase Auth for iOS is not wired yet -> local session        */
/* ------------------------------------------------------------------ */

actual object AuthPlatform {
    actual val isSignInAvailable: Boolean = false
    actual fun currentUser(): AuthUser? = null
    actual fun userFlow(): Flow<AuthUser?> = flowOf(null)
    actual suspend fun attemptSilentSignIn(context: PlatformContext): Result<AuthUser?> = Result.success(null)

    actual suspend fun launchSignInSheet(context: PlatformContext, onFallbackSession: () -> Unit): Result<AuthUser?> {
        // Same behaviour as Android devices without Google Play accounts: a verified local session.
        onFallbackSession()
        return Result.success(null)
    }

    actual suspend fun signOut(context: PlatformContext) = Unit
}

/* ------------------------------------------------------------------ */
/*  Generative AI via Swift bridge (Firebase AI Logic iOS SDK)           */
/* ------------------------------------------------------------------ */

/**
 * Implemented in Swift (iosApp/iosApp/FirebaseAiBridge.swift) with the official
 * Firebase AI Logic SDK and handed to [com.example.MainViewController].
 */
interface IosAiBridge {
    fun generate(
        model: String,
        prompt: String,
        imageJpeg: platform.Foundation.NSData?,
        onChunk: (String) -> Unit,
        onComplete: (String?) -> Unit
    )
}

object IosBridges {
    var ai: IosAiBridge? = null
}

private class BridgedGenerativeModel(private val modelName: String) : GenerativeModelCompat {
    override fun generateContentStream(content: AiContent): Flow<AiResponseChunk> = callbackFlow {
        val bridge = IosBridges.ai ?: throw IllegalStateException("Firebase AI no está configurado en iOS")
        bridge.generate(
            model = modelName,
            prompt = content.text,
            imageJpeg = content.image?.toNSData(),
            onChunk = { text -> trySend(AiResponseChunk(text)) },
            onComplete = { error ->
                if (error == null) close() else close(IllegalStateException(error))
            }
        )
        awaitClose { }
    }

    override suspend fun generateContent(content: AiContent): AiResponseChunk {
        val sb = StringBuilder()
        generateContentStream(content).collect { chunk -> chunk.text?.let { sb.append(it) } }
        return AiResponseChunk(sb.toString())
    }
}

actual fun createGenerativeModel(modelName: String): GenerativeModelCompat = BridgedGenerativeModel(modelName)
