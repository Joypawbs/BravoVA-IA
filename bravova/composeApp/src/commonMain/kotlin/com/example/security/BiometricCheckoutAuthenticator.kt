package com.example.security

import com.example.platform.BiometricCancelHandle
import com.example.platform.BiometricPlatform
import com.example.platform.PlatformContext
import com.example.platform.System

/**
 * Critical checkout and payment operations protected by BiometricPrompt (Android) / Face ID (iOS).
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

    fun isBiometricHardwareSupported(context: PlatformContext): Boolean = BiometricPlatform.isSupported(context)

    /**
     * Generates a deterministic verification token for audit and receipt display.
     */
    fun generateVerificationToken(
        operation: BiometricCriticalOperation,
        amountRd: Int,
        cipherHash: Int = 0
    ): String {
        val now = System.currentTimeMillis()
        return try {
            val payload = "${operation.code}:$amountRd:$cipherHash:$now"
            // FNV-1a 32-bit digest (portable replacement for SHA-256 truncated to 3 bytes)
            var hash = 0x811C9DC5.toInt()
            for (b in payload.encodeToByteArray()) {
                hash = hash xor (b.toInt() and 0xFF)
                hash *= 0x01000193
            }
            val hex = (hash.toLong() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
            "BIO-${operation.tokenPrefix}-$hex"
        } catch (_: Throwable) {
            "BIO-${operation.tokenPrefix}-${(now % 1000000).toString().padStart(6, '0')}"
        }
    }

    /**
     * Launches the native biometric prompt and returns a handle so the caller can cancel it
     * when the Composable disposes.
     */
    fun launchSystemBiometricPrompt(
        context: PlatformContext,
        request: BiometricPromptRequest,
        onHardwareSucceeded: (BiometricVerificationResult) -> Unit,
        onHardwareFailed: () -> Unit = {},
        onFallbackRequested: () -> Unit = {}
    ): BiometricCancelHandle? {
        return BiometricPlatform.authenticate(
            context = context,
            title = request.title,
            subtitle = request.subtitle,
            description = request.description,
            onSuccess = { cipherHash, hardwareBacked ->
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
                        hardwareBacked = hardwareBacked
                    )
                )
            },
            onFailed = onHardwareFailed,
            onFallback = onFallbackRequested
        )
    }
}
