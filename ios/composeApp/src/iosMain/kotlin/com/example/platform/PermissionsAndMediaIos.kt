package com.example.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.hasTorch
import platform.AVFoundation.requestAccessForMediaType
import platform.AVFoundation.torchMode
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.QuartzCore.CATransaction
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import kotlin.math.max

internal fun onMain(block: () -> Unit) = dispatch_async(dispatch_get_main_queue()) { block() }

/** Top-most view controller, used to present system pickers. */
internal fun topViewController(): UIViewController? {
    @Suppress("DEPRECATION")
    var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

/* ------------------------------------------------------------------ */
/*  Permissions                                                         */
/* ------------------------------------------------------------------ */

internal object NotificationPermissionCache {
    var granted: Boolean = false

    fun refresh() {
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            granted = status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional
        }
    }
}

private val sharedLocationManager by lazy { CLLocationManager() }

actual fun hasPermission(context: PlatformContext, permission: AppPermission): Boolean = when (permission) {
    AppPermission.CAMERA -> AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
    AppPermission.RECORD_AUDIO ->
        AVAudioSession.sharedInstance().recordPermission == AVAudioSessionRecordPermissionGranted &&
            SFSpeechRecognizer.authorizationStatus() == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized
    AppPermission.POST_NOTIFICATIONS -> {
        NotificationPermissionCache.refresh()
        NotificationPermissionCache.granted
    }
    AppPermission.ACCESS_FINE_LOCATION, AppPermission.ACCESS_COARSE_LOCATION -> {
        val status = sharedLocationManager.authorizationStatus
        status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways
    }
}

private fun requestPermission(permission: AppPermission, onResult: (Boolean) -> Unit) {
    when (permission) {
        AppPermission.CAMERA -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            onMain { onResult(granted) }
        }
        AppPermission.RECORD_AUDIO -> AVAudioSession.sharedInstance().requestRecordPermission { micGranted ->
            if (!micGranted) {
                onMain { onResult(false) }
            } else {
                SFSpeechRecognizer.requestAuthorization { status ->
                    onMain {
                        onResult(status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized)
                    }
                }
            }
        }
        AppPermission.POST_NOTIFICATIONS -> UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { granted, _ ->
                NotificationPermissionCache.granted = granted
                onMain { onResult(granted) }
            }
        AppPermission.ACCESS_FINE_LOCATION, AppPermission.ACCESS_COARSE_LOCATION ->
            LocationAuthorization.request { granted -> onResult(granted) }
    }
}

@Composable
actual fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): PermissionLauncher {
    val callback = rememberUpdatedState(onResult)
    return remember {
        object : PermissionLauncher {
            override fun launch(permission: AppPermission) {
                if (hasPermission(IosPlatformContext, permission)) callback.value(true)
                else requestPermission(permission) { callback.value(it) }
            }
        }
    }
}

@Composable
actual fun rememberMultiplePermissionsLauncher(onResult: (Map<AppPermission, Boolean>) -> Unit): MultiplePermissionsLauncher {
    val callback = rememberUpdatedState(onResult)
    return remember {
        object : MultiplePermissionsLauncher {
            override fun launch(permissions: Array<AppPermission>) {
                val results = mutableMapOf<AppPermission, Boolean>()
                fun next(index: Int) {
                    if (index >= permissions.size) {
                        callback.value(AppPermission.entries.associateWith { results[it] ?: false })
                        return
                    }
                    val p = permissions[index]
                    if (results.containsKey(p)) {
                        next(index + 1); return
                    }
                    requestPermission(p) { granted ->
                        results[p] = granted
                        // Fine and coarse location share a single iOS authorization
                        if (p == AppPermission.ACCESS_FINE_LOCATION) results[AppPermission.ACCESS_COARSE_LOCATION] = granted
                        if (p == AppPermission.ACCESS_COARSE_LOCATION) results[AppPermission.ACCESS_FINE_LOCATION] = granted
                        next(index + 1)
                    }
                }
                next(0)
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Images                                                              */
/* ------------------------------------------------------------------ */

private fun saveJpegToTemp(image: UIImage): String? {
    val data = UIImageJPEGRepresentation(image, 0.9) ?: return null
    val path = NSTemporaryDirectory() + "bravia_" + NSUUID().UUIDString + ".jpg"
    return if (data.writeToFile(path, atomically = true)) path else null
}

private class ImagePickerDelegate(private val onResult: (String?) -> Unit) :
    NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult(image?.let { saveJpegToTemp(it) })
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult(null)
    }
}

@Composable
actual fun rememberImagePicker(onResult: (String?) -> Unit): ImagePickerLauncher {
    val callback = rememberUpdatedState(onResult)
    return remember {
        object : ImagePickerLauncher {
            private var delegate: ImagePickerDelegate? = null
            override fun launch() {
                val picker = UIImagePickerController()
                picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                val d = ImagePickerDelegate { path ->
                    callback.value(path)
                    delegate = null
                }
                delegate = d
                picker.delegate = d
                topViewController()?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

private fun pathFromUri(uri: String): String = uri.removePrefix("file://")

actual suspend fun loadImageBytes(context: PlatformContext, uri: String): ByteArray? = withContext(Dispatchers.IO) {
    NSData.dataWithContentsOfFile(pathFromUri(uri))?.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun loadScaledJpeg(context: PlatformContext, uri: String, maxDim: Int): ByteArray? =
    withContext(Dispatchers.IO) {
        val image = UIImage.imageWithContentsOfFile(pathFromUri(uri)) ?: return@withContext null
        val (w, h) = image.size.useContents { width to height }
        val largest = max(w, h)
        val scaled = if (largest <= maxDim) {
            image
        } else {
            val factor = maxDim / largest
            val nw = w * factor
            val nh = h * factor
            UIGraphicsBeginImageContextWithOptions(CGSizeMake(nw, nh), false, 1.0)
            image.drawInRect(CGRectMake(0.0, 0.0, nw, nh))
            val out = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            out ?: image
        }
        UIImageJPEGRepresentation(scaled, 0.88)?.toByteArray()
    }

/* ------------------------------------------------------------------ */
/*  Camera preview (AVFoundation)                                        */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalForeignApi::class)
private class CameraPreviewView : UIView(frame = CGRectZero.readValue()) {
    private val session = AVCaptureSession()
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)
    private var device: AVCaptureDevice? = null

    init {
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(previewLayer)
        session.sessionPreset = AVCaptureSessionPresetHigh
        val camera = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        device = camera
        if (camera != null) {
            val input = AVCaptureDeviceInput.deviceInputWithDevice(camera, error = null) as? AVCaptureDeviceInput
            if (input != null && session.canAddInput(input)) session.addInput(input)
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
                session.startRunning()
            }
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        previewLayer.frame = bounds
        CATransaction.commit()
    }

    fun setTorch(on: Boolean) {
        val d = device ?: return
        if (!d.hasTorch) return
        if (d.lockForConfiguration(null)) {
            d.torchMode = if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff
            d.unlockForConfiguration()
        }
    }

    fun stop() {
        setTorch(false)
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            session.stopRunning()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(torchEnabled: Boolean, modifier: Modifier) {
    UIKitView(
        factory = { CameraPreviewView() },
        modifier = modifier,
        update = { view -> view.setTorch(torchEnabled) },
        onRelease = { view -> view.stop() }
    )
}

