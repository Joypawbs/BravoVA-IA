package com.example.security

import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Critical checkout and payment operations protected by Android BiometricPrompt.
 */
enum class BiometricCriticalOperation(
    val code: String,
    val tokenPrefix: String,
    val badgeTitle: String,
    val defaultTitle: String
) {
    CONFIRM_PAYMENT_AND_ORDER(
        code = "CHECKOUT_PAY",
        tokenPrefix = "PAY",
        badgeTitle = "AUTORIZACIÓN DE PAGO BIOMÉTRICA",
        defaultTitle = "Confirmar Pago y Pedido"
    ),
    AUTHORIZE_BRAVISIMA_REDEMPTION(
        code = "BRAVISIMA_REDEEM",
        tokenPrefix = "BRV",
        badgeTitle = "AUTORIZACIÓN DE CANJE BRAVÍSIMA",
        defaultTitle = "Autorizar Canje de Saldo"
    ),
    UNLOCK_BRAVISIMA_CARD(
        code = "BRAVISIMA_SECURITY",
        tokenPrefix = "ULK",
        badgeTitle = "SEGURIDAD TARJETA BRAVÍSIMA",
        defaultTitle = "Desbloquear / Proteger Tarjeta"
    ),
    CHANGE_PAYMENT_METHOD(
        code = "PAYMENT_METHOD_AUTH",
        tokenPrefix = "CRD",
        badgeTitle = "SEGURIDAD DE MÉTODO DE PAGO",
        defaultTitle = "Autorizar Cambio de Tarjeta"
    ),
    CONFIRM_AI_ORDER(
        code = "BRAVIA_ORDER_PAY",
        tokenPrefix = "BIA",
        badgeTitle = "AUTORIZACIÓN PEDIDO BRAV IA",
        defaultTitle = "Confirmar Pedido Asistido"
    )
}

/**
 * Request descriptor for any critical checkout or payment action requiring BiometricPrompt.
 */
data class BiometricPromptRequest(
    val operation: BiometricCriticalOperation,
    val title: String = operation.defaultTitle,
    val subtitle: String,
    val description: String,
    val badgeTitle: String = operation.badgeTitle,
    val amountRd: Int,
    val itemCount: Int = 0,
    val cashbackRd: Int = 0,
    val paymentMethodLabel: String = "Tarjeta Visa •••• 4821",
    val deliveryOrBranchLabel: String = "Delivery Express",
    val onSuccess: (BiometricVerificationResult) -> Unit = {}
)

/**
 * Result emitted when BiometricPrompt (hardware CryptoObject or interactive sensor fallback) succeeds.
 */
data class BiometricVerificationResult(
    val operation: BiometricCriticalOperation,
    val transactionToken: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val hardwareBacked: Boolean = false
)

object BiometricCheckoutAuthenticator {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "bravova_checkout_biometric_aes_key"
    private const val TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"

    /**
     * Checks whether the device reports biometric hardware support.
     */
    fun isBiometricHardwareSupported(context: Context): Boolean {
        return try {
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
    }

    /**
     * Creates a hardware-backed CryptoObject using AndroidKeyStore when supported by the device.
     */
    fun createHardwareCryptoObject(): BiometricPrompt.CryptoObject? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    KEYSTORE_PROVIDER
                )
                val specBuilder = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    specBuilder.setInvalidatedByBiometricEnrollment(true)
                }
                keyGenerator.init(specBuilder.build())
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

    /**
     * Generates a deterministic SHA-256 verification token for audit and receipt display.
     */
    fun generateVerificationToken(
        operation: BiometricCriticalOperation,
        amountRd: Int,
        cipherHash: Int = 0
    ): String {
        val now = System.currentTimeMillis()
        return try {
            val payload = "${operation.code}:$amountRd:$cipherHash:$now"
            val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
            val hex = digest.take(3).joinToString("") { "%02X".format(Locale.US, it) }
            "BIO-${operation.tokenPrefix}-$hex"
        } catch (_: Throwable) {
            "BIO-${operation.tokenPrefix}-${(now % 1000000).toString().padStart(6, '0')}"
        }
    }

    /**
     * Launches Android's native BiometricPrompt (with CryptoObject when available) and returns a
     * CancellationSignal so the caller can cancel it when the Composable disposes.
     */
    fun launchSystemBiometricPrompt(
        context: Context,
        request: BiometricPromptRequest,
        onHardwareSucceeded: (BiometricVerificationResult) -> Unit,
        onHardwareFailed: () -> Unit = {},
        onFallbackRequested: () -> Unit = {}
    ): CancellationSignal? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onFallbackRequested()
            return null
        }
        if (!isBiometricHardwareSupported(context)) {
            onFallbackRequested()
            return null
        }

        return try {
            val signal = CancellationSignal()
            val promptBuilder = BiometricPrompt.Builder(context)
                .setTitle(request.title)
                .setSubtitle(request.subtitle)
                .setDescription(request.description)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                promptBuilder.setConfirmationRequired(true)
            }

            promptBuilder.setNegativeButton(
                "Usar sensor en pantalla",
                context.mainExecutor,
                DialogInterface.OnClickListener { _, _ ->
                    onFallbackRequested()
                }
            )

            val biometricPrompt = promptBuilder.build()
            val cryptoObject = createHardwareCryptoObject()

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    val cipherHash = result?.cryptoObject?.cipher?.hashCode() ?: 0
                    val token = generateVerificationToken(
                        operation = request.operation,
                        amountRd = request.amountRd,
                        cipherHash = cipherHash
                    )
                    onHardwareSucceeded(
                        BiometricVerificationResult(
                            operation = request.operation,
                            transactionToken = token,
                            timestampMillis = System.currentTimeMillis(),
                            hardwareBacked = result?.cryptoObject != null
                        )
                    )
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onHardwareFailed()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    onFallbackRequested()
                }
            }

            if (cryptoObject != null) {
                try {
                    biometricPrompt.authenticate(cryptoObject, signal, context.mainExecutor, callback)
                } catch (_: Throwable) {
                    biometricPrompt.authenticate(signal, context.mainExecutor, callback)
                }
            } else {
                biometricPrompt.authenticate(signal, context.mainExecutor, callback)
            }
            signal
        } catch (_: Throwable) {
            onFallbackRequested()
            null
        }
    }
}
