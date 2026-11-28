package com.example.ui.screens

import com.example.ui.components.BravoSwitch
import com.example.ui.theme.Tone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

import com.example.ui.components.SwitchRow
import androidx.compose.runtime.derivedStateOf
import com.example.ui.components.LargeTitle
import com.example.ui.components.GlassBarAction
import com.example.ui.components.GlassTopBar
import com.example.platform.*
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlinx.coroutines.IO

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.platform.Log
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import com.example.model.AccountSubScreen
import com.example.model.CartTotals
import com.example.model.ChatMessage
import com.example.model.Recipe
import com.example.model.BRAV_IA_RECIPES
import com.example.ai.StoreFacts
import com.example.ui.components.nativeLiquidGlassAvailable
import com.example.ui.components.NativeGlassBudgetPanel
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.ai.BravIaEngine
import com.example.model.findRecipeById
import com.example.model.FcmTopicSubscription
import com.example.model.FrequentProductStat
import com.example.model.NpuHardwareStatus
import com.example.model.OrderDeliveryStage
import com.example.model.OrderRecord
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.PushNotificationRecord
import com.example.model.ShoppingList
import com.example.model.StoreMode
import com.example.model.SuggestionMailPayload
import com.example.model.UserAddress
import com.example.model.UserProfile
import com.example.ui.components.AnimatedPurchaseButton
import com.example.ui.components.BravoAiPlasmaOrb
import com.example.ui.components.BravoUnderline
import com.example.ui.components.bravoAiAura
import com.example.ui.components.BravoUnifiedIcon
import com.example.ui.components.DiagonalMetadataStrip
import com.example.ui.components.FloatingHeaderCapsule
import com.example.ui.components.FormattedChatText
import com.example.ui.components.HapticFeedbackManager
import com.example.ui.components.HapticStyle
import com.example.ui.components.LocalTranslucentHazeState
import com.example.ui.components.ProductArtworkView
import com.example.ui.components.TranslucentCard
import com.example.ui.components.performCrispHaptic
import com.example.ui.components.translucentBackdropEffect
import com.example.ui.components.translucentBackdropSource
import com.example.ui.theme.ArcaTeal
import com.example.ui.theme.ArcaTealDark
import com.example.ui.theme.BravoAmber
import com.example.ui.theme.BravoRedDark
import com.example.ui.theme.FAVORITE_COLOR_PALETTES
import com.example.ui.theme.PlusJakartaSans
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private fun formatPesos(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount)
}

@Composable
private fun ChatAttachedImagePreview(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = currentPlatformContext()
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = uriString) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                loadImageBytes(context, uriString)?.decodeToImageBitmap()
            }.getOrNull()
        }
    }

    if (imageBitmap != null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
        ) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = "Imagen adjunta por el cliente",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SuggestionMailDispatchedCard(
    payload: SuggestionMailPayload,
    primaryColor: Color
) {
    val context = currentPlatformContext()
    val view = currentPlatformView()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Tone.success.copy(alpha = 0.12f),
        border = BorderStroke(1.3.dp, Tone.success),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("suggestion_mail_payload_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Tone.success,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "SUGGESTION_MAIL_PAYLOAD • Enviado",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Tone.successStrong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Tone.success.copy(alpha = 0.20f)
                ) {
                    Text(
                        text = "${payload.category} • ${payload.priority}",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Tone.successStrong,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Destino oficial: ${payload.toEmail}",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Asunto: ${payload.subject}",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DiagonalMetadataStrip(
                items = listOf(payload.activeBranch, payload.activeStore.uppercase(), payload.fromUser),
                slashColor = Tone.success,
                fontSizeSp = 12f
            )

            OutlinedButton(
                onClick = {
                    performCrispHaptic(view, HapticStyle.CONFIRM)
                    runCatching {
                        PlatformActions.sendEmail(
                            context,
                            payload.toEmail,
                            payload.subject,
                            """
                                Reporte / Sugerencia Oficial — Brav IA Soporte
                                ---------------------------------------------
                                Destino: ${payload.toEmail}
                                Cliente: ${payload.fromUser} (${payload.userContact})
                                Sucursal Activa: ${payload.activeBranch}
                                Tienda Activa: ${payload.activeStore}
                                Categoría: ${payload.category} | Prioridad: ${payload.priority}
                                Fecha ISO: ${payload.timestampIso}

                                Resumen Ejecutivo:
                                ${payload.summaryBody}
                                """.trimIndent()
                        )
                    }
                },
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.1.dp, Tone.success),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Tone.successStrong,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Abrir copia en cliente de correo (${payload.toEmail})",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Tone.successStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val SoftAiPaletteColors = listOf(
    Color(0xFFF38B9B), // Soft Rose Blush
    Color(0xFFE86A7C), // Gentle Pastel Crimson
    Color(0xFFF69E98), // Warm Salmon Coral
    Color(0xFFD9657B), // Soft Velvet Ruby
    Color(0xFFF2828B)  // Delicate Rose Red
)

private fun sampleSoftAiRedColor(normalizedPhase: Float): Color {
    val clean = ((normalizedPhase % 1f) + 1f) % 1f
    val scaled = clean * SoftAiPaletteColors.size
    val idx = scaled.toInt() % SoftAiPaletteColors.size
    val nextIdx = (idx + 1) % SoftAiPaletteColors.size
    val fraction = scaled - scaled.toInt()
    return lerp(SoftAiPaletteColors[idx], SoftAiPaletteColors[nextIdx], fraction)
}

/**
 * Estrellas animadas con los colores del logo (rojo y ámbar)
 * que aparecen junto al texto de "pensando…" de Brav IA.
 */
@Composable
private fun BravIaLogoSparklingStars(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bravia_logo_stars")
    val masterPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starsMasterPhase"
    )
    val badgePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starsBadgePulse"
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .testTag("bravia_sparkling_stars_indicator"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Aura suave con los colores del logo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Tone.danger.copy(alpha = 0.28f * badgePulse),
                        Color(0xFFFFB800).copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.54f
                ),
                radius = w * 0.54f,
                center = Offset(w * 0.5f, h * 0.5f)
            )

            // Cuatro estrellas que se encienden por turnos
            val starConfigs = listOf(
                floatArrayOf(0.42f, 0.52f, 10.5f, 0.00f),
                floatArrayOf(0.76f, 0.26f, 6.8f, 0.28f),
                floatArrayOf(0.22f, 0.22f, 5.6f, 0.55f),
                floatArrayOf(0.78f, 0.75f, 6.2f, 0.78f)
            )

            starConfigs.forEachIndexed { idx, spec ->
                val cx = w * spec[0]
                val cy = h * spec[1]
                val baseR = spec[2].dp.toPx()
                val angle = masterPhase + spec[3] * (2f * PI).toFloat()
                val twinkle = ((sin(angle) + 1f) * 0.5f).coerceIn(0f, 1f)
                val r = baseR * (0.55f + 0.65f * twinkle)
                val innerR = r * 0.24f

                val starColor = when (idx) {
                    0 -> Color(0xFFFFB800).copy(alpha = 0.65f + 0.35f * twinkle)
                    1 -> Color(0xFFFFD54F).copy(alpha = 0.50f + 0.50f * twinkle)
                    2 -> Color(0xFFFF9800).copy(alpha = 0.45f + 0.55f * twinkle)
                    else -> Color(0xFFFFF59D).copy(alpha = 0.50f + 0.50f * twinkle)
                }

                // Halo de la estrella
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            starColor.copy(alpha = 0.55f * twinkle),
                            Tone.danger.copy(alpha = 0.20f * twinkle),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = r * 1.5f
                    ),
                    radius = r * 1.5f,
                    center = Offset(cx, cy)
                )

                val starPath = Path().apply {
                    moveTo(cx, cy - r)
                    quadraticTo(cx + innerR * 0.36f, cy - innerR * 0.36f, cx + r, cy)
                    quadraticTo(cx + innerR * 0.36f, cy + innerR * 0.36f, cx, cy + r)
                    quadraticTo(cx - innerR * 0.36f, cy + innerR * 0.36f, cx - r, cy)
                    quadraticTo(cx - innerR * 0.36f, cy - innerR * 0.36f, cx, cy - r)
                    close()
                }
                drawPath(path = starPath, color = starColor)

                // Centro blanco dorado
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f * twinkle),
                    radius = max(1.2f, r * 0.18f),
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

/**
 * Frases de "pensando…" que cambian según lo que escribió el usuario,
 * mientras Brav IA responde.
 */
@Composable
private fun BravIaPersonalizedThinkingRow(
    userQuery: String,
    initialText: String,
    activeBranch: String,
    messageSeed: Long
) {
    val phrases = remember(userQuery, initialText, activeBranch, messageSeed) {
        val cleanQuery = userQuery.trim().replace("\n", " ")
        val snippet = cleanQuery.take(28).ifBlank { "tu consulta" }
        val lower = cleanQuery.lowercase()
        when {
            lower.contains("pide") || lower.contains("pedir") || lower.contains("orden") || lower.contains("compra") -> listOf(
                initialText,
                "Verificando existencias de «$snippet» en $activeBranch...",
                "Tomando los precios del catálogo de $activeBranch...",
                "Preparando tu resumen de confirmación manual en el chat..."
            )
            lower.contains("receta") || lower.contains("paso") || lower.contains("cocinar") || lower.contains("hacer") || lower.contains("mangu") || lower.contains("sancocho") || lower.contains("pasta") || lower.contains("pastelon") -> listOf(
                initialText,
                "Buscando «$snippet» en el recetario verificado...",
                "Uniendo los ingredientes con productos del catálogo...",
                "Sumando el total con los precios de $activeBranch..."
            )
            lower.contains("precio") || lower.contains("cuanto") || lower.contains("cuesta") -> listOf(
                initialText,
                "Consultando precios en RD$ para «$snippet» en los 16 pasillos...",
                "Comparando presentaciones Marca Exclusiva Bravo...",
                "Preparando tu respuesta personalizada al detalle..."
            )
            else -> {
                val variants = listOf(
                    listOf(
                        initialText,
                        "Buscando coincidencias para «$snippet» entre los 750 artículos...",
                        "Verificando disponibilidad inmediata en $activeBranch...",
                        "Redactando una respuesta personalizada para ti..."
                    ),
                    listOf(
                        "Revisando nuestros 16 pasillos oficiales para «$snippet»...",
                        initialText,
                        "Consultando precios actualizados en RD$ y detalles...",
                        "Afinando las mejores recomendaciones de Bravo para ti..."
                    )
                )
                variants[( kotlin.math.abs(messageSeed) % variants.size ).toInt()]
            }
        }.distinct()
    }

    var phraseIndex by remember(messageSeed) { mutableIntStateOf(0) }
    LaunchedEffect(phrases, messageSeed) {
        while (true) {
            delay(1350L)
            phraseIndex = (phraseIndex + 1) % phrases.size
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BravIaLogoSparklingStars()

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Brav IA está pensando...",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Tone.danger
            )
            AnimatedContent(
                targetState = phrases[phraseIndex % phrases.size],
                transitionSpec = {
                    fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "thinking_phrase_transition"
            ) { currentPhrase ->
                Text(
                    text = currentPhrase,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dibuja una línea de luz suave que recorre el borde del mensaje de la IA
 * e ilumina un poco lo que tiene alrededor.
 */
@Composable
private fun Modifier.glowingRedAiContour(
    isActivelyResponding: Boolean,
    cornerShape: RoundedCornerShape
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_single_line_contour")
    // La línea recorre el borde despacio
    val lineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isActivelyResponding) 5600 else 9200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "singleLineTraversalProgress"
    )
    // Cambia muy despacio entre rosa, coral y rojo
    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 11000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "softRedColorPhase"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = if (isActivelyResponding) 0.78f else 0.58f,
        targetValue = if (isActivelyResponding) 1.0f else 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "softGlowPulse"
    )

    // Rendimiento: antes se medía el contorno y se recortaban dos trazos en cada cuadro, pero solo se
    // usaba la posición de la punta. La punta está a (avance + 26 % del perímetro), así que basta esa cuenta.
    // Las animaciones se leen al dibujar: no se reconstruye el mensaje en cada cuadro.
    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val ambientRadius = max(w, h) * 0.72f

        onDrawWithContent {
            val headFraction = (lineProgress + 0.26f) % 1f
            val headAngle = headFraction * (2f * PI).toFloat()
            val headCenter = Offset(
                x = (w * 0.5f + cos(headAngle) * (w * 0.48f)).coerceIn(0f, w),
                y = (h * 0.5f + sin(headAngle) * (h * 0.48f)).coerceIn(0f, h)
            )
            val activeSoftColor = sampleSoftAiRedColor(colorPhase)
            val trailingSoftColor = sampleSoftAiRedColor(colorPhase + 0.22f)
            // Luz suave dentro de la tarjeta del mensaje
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeSoftColor.copy(alpha = (if (isActivelyResponding) 0.14f else 0.08f) * glowPulse),
                        trailingSoftColor.copy(alpha = (if (isActivelyResponding) 0.07f else 0.04f) * glowPulse),
                        Color.Transparent
                    ),
                    center = headCenter,
                    radius = ambientRadius
                ),
                radius = ambientRadius,
                center = headCenter
            )

            drawContent()
        }
    }
}

@Composable
fun BravIaChatScreen(
    storeMode: StoreMode,
    darkMode: Boolean,
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    budgetAmount: Int,
    cartTotals: CartTotals,
    activeBranch: String = "Bravo Winston Churchill",
    estimatedEtaMinutes: Int = 12,
    areMenusVisible: Boolean = true,
    npuStatus: NpuHardwareStatus = NpuHardwareStatus(),
    onToggleNpuMode: () -> Unit = {},
    pendingImageUri: String? = null,
    onSelectImageUri: (String?) -> Unit = {},
    productLookup: (String) -> Product?,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenMyLists: () -> Unit = {},
    onBudgetChange: (Int) -> Unit,
    onBuildCartForBudget: () -> Unit,
    onAdjustCartToBudget: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onPauseGeneration: () -> Unit = {},
    onAddBundleForMessage: (Long) -> Unit,
    onSaveBundleAsList: (Long) -> Unit = {},
    onRequestOrderConfirmation: (Long) -> Unit = {},
    onConfirmOrderFromChat: (Long) -> Unit = {},
    onCancelOrderConfirmation: (Long) -> Unit = {},
    onAddSingleProduct: (String) -> Unit,
    onOpenProduct: (String) -> Unit
) {
    val context = currentPlatformContext()
    val view = currentPlatformView()
    val isArca = storeMode == StoreMode.ARCA
    val primaryColor = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary
    val darkPrimaryColor = if (isArca) ArcaTealDark else BravoRedDark
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberImagePicker { uri: String? ->
        if (uri != null) {
            onSelectImageUri(uri)
        }
    }

    var isLiveVoiceModeActive by remember { mutableStateOf(false) }
    var isVoicePaused by remember { mutableStateOf(false) }
    var isListeningMic by remember { mutableStateOf(false) }
    var isSpeakingTts by remember { mutableStateOf(false) }
    var voiceStatusLabel by remember { mutableStateOf("Micrófono listo • Habla libremente sin interrupciones") }
    var lastSpokenMessageId by remember { mutableStateOf(messages.lastOrNull()?.id ?: -1L) }

    var ttsEngine by remember { mutableStateOf<TtsEngine?>(null) }
    var activeRecognizer by remember { mutableStateOf<SpeechRecognizerEngine?>(null) }

    DisposableEffect(context) {
        val engine: TtsEngine? = runCatching {
            TtsEngine(
                context = context,
                languageTag = "es-DO",
                onStart = { isSpeakingTts = true },
                onDone = { isSpeakingTts = false }
            )
        }.getOrNull()
        ttsEngine = engine
        onDispose {
            runCatching {
                activeRecognizer?.cancel()
                activeRecognizer?.destroy()
            }
            engine?.stop()
            engine?.shutdown()
        }
    }

    fun stopAllVoiceOutputAndRecording() {
        ttsEngine?.stop()
        isSpeakingTts = false
        runCatching {
            activeRecognizer?.stopListening()
            activeRecognizer?.cancel()
        }
        isListeningMic = false
    }

    fun speakOutLoud(textToSpeak: String) {
        if (isListeningMic || isVoicePaused) return
        val cleanSpeech = textToSpeak
            .replace("**", "")
            .replace("*", "")
            .replace("#", "")
            .trim()
        if (cleanSpeech.isNotEmpty()) {
            isSpeakingTts = true
            ttsEngine?.speak(cleanSpeech)
        }
    }

    val latestMessage = messages.lastOrNull()
    LaunchedEffect(latestMessage?.id, latestMessage?.isLoading, isGenerating, isLiveVoiceModeActive, isVoicePaused, isListeningMic) {
        if (isLiveVoiceModeActive &&
            !isVoicePaused &&
            !isListeningMic &&
            latestMessage != null &&
            latestMessage.sender == "ai" &&
            !latestMessage.isLoading &&
            !isGenerating &&
            latestMessage.id != lastSpokenMessageId
        ) {
            lastSpokenMessageId = latestMessage.id
            speakOutLoud(latestMessage.text)
        }
    }

    fun startSpeechRecognition() {
        ttsEngine?.stop()
        isSpeakingTts = false
        isVoicePaused = false
        lastSpokenMessageId = messages.lastOrNull()?.id ?: -1L

        if (!isSpeechRecognitionAvailable(context)) {
            voiceStatusLabel = "Modo voz activo • Elige o escribe tu consulta"
            return
        }
        try {
            runCatching {
                activeRecognizer?.cancel()
                activeRecognizer?.destroy()
            }
            val recognizer = SpeechRecognizerEngine(context)
            activeRecognizer = recognizer
            recognizer.setListener(object : SpeechListener {
                override fun onReadyForSpeech() {
                    ttsEngine?.stop()
                    isSpeakingTts = false
                    isListeningMic = true
                    voiceStatusLabel = "Escuchando tu voz... (puedes hablar ahora)"
                }
                override fun onBeginningOfSpeech() {
                    ttsEngine?.stop()
                    isSpeakingTts = false
                    isListeningMic = true
                }
                override fun onEndOfSpeech() {
                    isListeningMic = false
                }
                override fun onError(error: Int) {
                    isListeningMic = false
                    voiceStatusLabel = if (isVoicePaused) {
                        "Modo voz en pausa • Toca Reanudar cuando gustes"
                    } else {
                        "Listo para escucharte • Toca Hablar o un chip rápido"
                    }
                }
                override fun onResults(matches: List<String>?) {
                    isListeningMic = false
                    val spoken = matches?.firstOrNull()?.trim().orEmpty()
                    if (spoken.isNotEmpty() && !isVoicePaused) {
                        voiceStatusLabel = "Escuchado: \"$spoken\""
                        onSendPrompt(spoken)
                    }
                }
                override fun onPartialResults(matches: List<String>?) {
                    val partial = matches
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotEmpty()) {
                        voiceStatusLabel = "Escuchando: \"$partial...\""
                    }
                }
            })
            recognizer.startListening("es-DO")
        } catch (_: Exception) {
            isListeningMic = false
            voiceStatusLabel = "Voz en vivo lista • Dicta tu receta o consulta"
        }
    }

    val audioPermissionLauncher = rememberPermissionLauncher { granted ->
        lastSpokenMessageId = messages.lastOrNull()?.id ?: -1L
        ttsEngine?.stop()
        isSpeakingTts = false
        isLiveVoiceModeActive = true
        isVoicePaused = false
        if (granted) {
            startSpeechRecognition()
        } else {
            voiceStatusLabel = "Modo Voz Activo • Toca Hablar o elige una consulta"
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    val quickChips = remember(isArca) {
        if (isArca) {
            listOf(
                "Pedir por mí (Confirmación)" to "Pide por mí un combo esencial para mi mascota en El Arca Petshop con confirmación manual en el chat.",
                "Dieta perro adulto" to "Recomiéndame el mejor alimento y snacks para un perro adulto de 15 kg bajo mi presupuesto.",
                "Combo gatos" to "Arma un combo mensual para mi gato con alimento de salmón, arena sanitaria y paté bajo mi presupuesto.",
                "Salud y parásitos" to "¿Qué productos de farmacia veterinaria me recomiendas para prevención de pulgas, garrapatas e higiene dental?",
                "Enviar sugerencia" to "Sugiero agregar más variedad de juguetes interactivos hipoalergénicos para cachorros en El Arca."
            )
        } else {
            listOf(
                "Sancocho" to "¿Cómo hago un sancocho dominicano?",
                "Mangú con los tres golpes" to "Receta de mangú con los tres golpes",
                "Sin lactosa" to "¿Qué productos sin lactosa tienen? Tengo intolerancia.",
                "Comida con RD$1,000" to "Arma un almuerzo para 4 personas con 1,000 pesos",
                "Alta en proteína" to "Recomiéndame productos altos en proteína para mis comidas de la semana",
                "Pedir por mí (Confirmación)" to "Hazme el pedido de mi compra habitual del supermercado con confirmación manual aquí en el chat.",
                "Facturación NCF y Horarios" to "Explícame cómo funcionan los comprobantes B01 y B02, los horarios de tienda y el retorno del 1% de la Tarjeta Bravísima.",
                "Enviar sugerencia" to "Sugiero agregar más opciones de postres artesanales sin azúcar marca Bravo en la sucursal."
            )
        }
    }

    // Luz roja suave de fondo en la pantalla de Brav IA
    val uiAmbientTransition = rememberInfiniteTransition(label = "bravia_interface_ambient_glow")
    val uiColorPhase by uiAmbientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "uiAmbientColorPhase"
    )
    val uiOrbitAngle by uiAmbientTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "uiAmbientOrbitAngle"
    )
    // Rendimiento: los colores que cambian con la animación se calculan DENTRO del dibujo
    // (no al componer), así solo se repinta el fondo y no se reconstruye todo el chat en cada cuadro.

    val reduceMotionBorder = remember { com.example.platform.systemPrefersReducedMotion() }
    val hazeState = LocalTranslucentHazeState.current
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    var isImeVisible by remember { mutableStateOf(false) }
    var topOverlayHeightPx by remember { mutableIntStateOf(0) }
    val topOverlayPadding by animateDpAsState(
        targetValue = with(density) { topOverlayHeightPx.toDp() },
        animationSpec = tween(durationMillis = 200),
        label = "bravia_top_overlay_padding"
    )
    // Barra de escritura justo encima de la barra inferior
    val dynamicInputBottomPadding by animateDpAsState(
        targetValue = when {
            isImeVisible -> 4.dp
            // Con el vidrio nativo de iOS la barra inferior no se esconde: el campo siempre va encima.
            areMenusVisible || nativeLiquidGlassAvailable() -> if (PlatformInfo.isIos) 78.dp else 66.dp
            else -> 6.dp
        },
        animationSpec = tween(durationMillis = 180),
        label = "bravia_input_bottom_padding"
    )

    LaunchedEffect(imeInsets, density) {
        snapshotFlow { imeInsets.getBottom(density) > 0 }
            .distinctUntilChanged()
            .collectLatest { visible ->
                delay(90L)
                isImeVisible = visible
                if (visible && messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("bravia_chat_screen")
    ) {
        // Fondo sólido fijo (sin auras): mismo color que el resto de la app
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .translucentBackdropSource(hazeState, zIndex = 0f)
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .translucentBackdropSource(hazeState, zIndex = 1f),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = topOverlayPadding + 6.dp,
                    // Con el teclado abierto solo hace falta dejar sitio al campo de escritura;
                    // los 148 dp de antes dejaban un hueco vacío y empujaban la respuesta bajo el panel
                    bottom = dynamicInputBottomPadding + (if (isImeVisible) 60.dp else 148.dp)
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // En iOS 26+ el panel de presupuesto va fijo bajo la barra superior (ver más abajo),
                // así nunca pasa por detrás de las cápsulas de vidrio
                if (!nativeLiquidGlassAvailable()) item(key = "budget_planner_card", contentType = "budget_header") {
                    TranslucentCard(
                    darkMode = darkMode,
                    isArca = isArca,
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Presupuesto Meta en RD$",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            BravoUnderline(isArca = isArca)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                                Text(
                                    text = "RD$${formatPesos(budgetAmount)}",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = primaryColor
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        HapticFeedbackManager.onNavigationClick(view)
                                        onOpenMyLists()
                                    },
                                    border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .testTag("bravia_open_lists_pill")
                                ) {
                                    Text("Mis listas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                }

                                OutlinedButton(
                                    onClick = {
                                        performCrispHaptic(view, HapticStyle.CONFIRM)
                                        onBuildCartForBudget()
                                    },
                                    enabled = !isGenerating,
                                    border = BorderStroke(1.5.dp, primaryColor),
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .testTag("budget_build_button")
                                ) {
                                    Text("Armar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                }

                                OutlinedButton(
                                    onClick = {
                                        performCrispHaptic(view, HapticStyle.CONFIRM)
                                        onAdjustCartToBudget()
                                    },
                                    enabled = !isGenerating,
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .testTag("budget_adjust_button")
                                ) {
                                    Text("Ajustar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                }
                            }
                        }

                        Slider(
                            value = budgetAmount.toFloat(),
                            onValueChange = {
                                val stepped = ((it / 50f).roundToInt() * 50).coerceIn(400, 3500)
                                onBudgetChange(stepped)
                            },
                            valueRange = 400f..3500f,
                            colors = SliderDefaults.colors(
                                thumbColor = primaryColor,
                                activeTrackColor = primaryColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .testTag("budget_slider")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    if (!isGenerating) {
                                        HapticFeedbackManager.onAssistantTrigger(view)
                                        photoPickerLauncher.launch()
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = primaryColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.2.dp, primaryColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Soporte Visual (Foto / NCF)",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = primaryColor
                                    )
                                }
                            }

                            quickChips.forEach { (label, prompt) ->
                                Surface(
                                    onClick = {
                                        if (!isGenerating) {
                                            HapticFeedbackManager.onAssistantTrigger(view)
                                            onSendPrompt(prompt)
                                        }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = primaryColor.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.28f))
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = primaryColor,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(
                items = messages,
                key = { it.id },
                contentType = { "chat_message" }
            ) { msg ->
                val isUser = msg.sender == "user"
                val isLatestAiResponding = !isUser && (msg.isLoading || (isGenerating && msg.id == messages.lastOrNull()?.id))
                val msgIndex = messages.indexOfFirst { it.id == msg.id }
                val precedingUserQuery = remember(msg.id, messages.size) {
                    if (msgIndex > 0) {
                        messages.subList(0, msgIndex).lastOrNull { it.sender == "user" }?.text.orEmpty()
                    } else {
                        ""
                    }
                }
                val recommendedProducts: List<Product> = remember(msg.skus) {
                    msg.skus.mapNotNull { sku -> productLookup(sku) }
                }
                // Solo las recetas del recetario verificado se muestran como pasos; nunca pasos escritos por el modelo.
                val verifiedRecipe = remember(msg.recipeId) { findRecipeById(msg.recipeId) }
                val parsedRecipeSteps = remember(verifiedRecipe) { verifiedRecipe?.steps ?: emptyList() }
                fun qtyOf(sku: String) = msg.quantities[sku] ?: 1
                val bubbleShape = RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomStart = if (isUser) 22.dp else 8.dp,
                    bottomEnd = if (isUser) 8.dp else 22.dp
                )

                val entryAlpha = remember(msg.id) { Animatable(0f) }
                val entryOffsetY = remember(msg.id) { Animatable(18f) }
                LaunchedEffect(msg.id) {
                    entryAlpha.animateTo(1f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                }
                LaunchedEffect(msg.id) {
                    entryOffsetY.animateTo(0f, animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = entryAlpha.value
                            translationY = entryOffsetY.value
                        },
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (isUser) 0.85f else 0.96f)
                            .clip(bubbleShape)
                            .background(
                                if (isUser) {
                                    Brush.linearGradient(listOf(primaryColor, darkPrimaryColor))
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                                        )
                                    )
                                }
                            )
                            .then(
                                if (isUser) {
                                    Modifier
                                } else {
                                    Modifier.glowingRedAiContour(
                                        isActivelyResponding = isLatestAiResponding,
                                        cornerShape = bubbleShape
                                    )
                                }
                            )
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!msg.attachedImageUri.isNullOrBlank()) {
                                ChatAttachedImagePreview(
                                    uriString = msg.attachedImageUri,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                )
                            }

                            if (msg.isLoading) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        BravIaPersonalizedThinkingRow(
                                            userQuery = precedingUserQuery,
                                            initialText = msg.text,
                                            activeBranch = activeBranch,
                                            messageSeed = msg.id
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        onClick = {
                                            performCrispHaptic(view, HapticStyle.CONFIRM)
                                            stopAllVoiceOutputAndRecording()
                                            onPauseGeneration()
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        color = Tone.danger.copy(alpha = 0.14f),
                                        border = BorderStroke(1.2.dp, Tone.danger),
                                        modifier = Modifier.testTag("bravia_pause_card_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Pause,
                                                contentDescription = "Pausar respuesta de IA",
                                                tint = Tone.danger,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Pausar",
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Tone.danger
                                            )
                                        }
                                    }
                                }
                            } else {
                                if (isLatestAiResponding) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            BravIaLogoSparklingStars(modifier = Modifier.size(28.dp))
                                            Text(
                                                text = "Respondiendo de forma personalizada para ti...",
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Tone.danger,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Surface(
                                            onClick = {
                                                performCrispHaptic(view, HapticStyle.CONFIRM)
                                                stopAllVoiceOutputAndRecording()
                                                onPauseGeneration()
                                            },
                                            shape = RoundedCornerShape(999.dp),
                                            color = Tone.danger.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, Tone.danger)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Pause,
                                                    contentDescription = "Pausar",
                                                    tint = Tone.danger,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "Pausar",
                                                    fontFamily = PlusJakartaSans,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Tone.danger
                                                )
                                            }
                                        }
                                    }
                                }
                                FormattedChatText(
                                    rawText = msg.text,
                                    textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                )

                                // Transparencia: si la IA no respondió y salió la respuesta del catálogo,
                                // se dice (antes no se distinguía de una respuesta real de la IA)
                                if (!isUser && !msg.isLoading && msg.modelLabel?.let {
                                        "sin conexión" in it || "parcial" in it
                                    } == true) {
                                    Text(
                                        text = if ("parcial" in msg.modelLabel.orEmpty()) "Respuesta parcial: la IA tardó demasiado"
                                        else "Respuesta sin IA: sin conexión con el asistente",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!isUser && msg.suggestionPayload != null) {
                                    SuggestionMailDispatchedCard(
                                        payload = msg.suggestionPayload,
                                        primaryColor = primaryColor
                                    )
                                }

                                if (!isUser && verifiedRecipe != null) {
                                    VerifiedRecipeCard(
                                        recipe = verifiedRecipe,
                                        multiplier = msg.recipeMultiplier,
                                        productLookup = productLookup,
                                        primaryColor = primaryColor,
                                        onOpenProduct = onOpenProduct
                                    )
                                }

                                if (!isUser && parsedRecipeSteps.isNotEmpty()) {
                                    RecipeStepsVisualCard(
                                        steps = parsedRecipeSteps,
                                        primaryColor = primaryColor,
                                        isSpeaking = isSpeakingTts,
                                        onSpeakSteps = {
                                            performCrispHaptic(view, HapticStyle.CONFIRM)
                                            isVoicePaused = false
                                            speakOutLoud(parsedRecipeSteps.mapIndexed { idx, s -> "Paso ${idx + 1}: $s" }.joinToString(". "))
                                        },
                                        onPauseSpeech = {
                                            performCrispHaptic(view, HapticStyle.TICK)
                                            isVoicePaused = true
                                            stopAllVoiceOutputAndRecording()
                                        }
                                    )
                                }

                                if (!isUser && msg.text.length > 20) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSpeakingTts) {
                                            Surface(
                                                onClick = {
                                                    performCrispHaptic(view, HapticStyle.TICK)
                                                    isVoicePaused = true
                                                    stopAllVoiceOutputAndRecording()
                                                },
                                                shape = RoundedCornerShape(999.dp),
                                                color = Tone.danger.copy(alpha = 0.12f),
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Pause,
                                                        contentDescription = "Pausar narración",
                                                        tint = Tone.danger,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "Pausar voz",
                                                        fontFamily = PlusJakartaSans,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Tone.danger
                                                    )
                                                }
                                            }
                                        }
                                        Surface(
                                            onClick = {
                                                performCrispHaptic(view, HapticStyle.TICK)
                                                isVoicePaused = false
                                                speakOutLoud(msg.text)
                                            },
                                            shape = RoundedCornerShape(999.dp),
                                            color = primaryColor.copy(alpha = 0.10f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Escuchar respuesta en voz alta",
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Escuchar en voz",
                                                    fontFamily = PlusJakartaSans,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = primaryColor
                                                )
                                            }
                                        }
                                    }
                                }

                                // Guía de IA generativa de Apple: Reintentar y Copiar junto a la
                                // respuesta, y una valoración voluntaria que no interrumpe el chat.
                                if (!isUser && !isLatestAiResponding && msg.text.isNotBlank()) {
                                    AiReplyActions(
                                        messageId = msg.id,
                                        text = msg.text,
                                        accent = primaryColor,
                                        canRetry = precedingUserQuery.isNotBlank() && !isGenerating,
                                        onRetry = { onSendPrompt(precedingUserQuery) },
                                        showDisclaimer = msg.id == messages.lastOrNull()?.id
                                    )
                                }
                            }

                            if (!isUser && recommendedProducts.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Artículos del catálogo (${recommendedProducts.size})",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    BravoUnderline(isArca = isArca)
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    recommendedProducts.forEach { prod ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                                .clickable { onOpenProduct(prod.sku) }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(modifier = Modifier.size(36.dp)) {
                                                    ProductArtworkView(product = prod)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = prod.name,
                                                        fontFamily = PlusJakartaSans,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val q = qtyOf(prod.sku)
                                                    DiagonalMetadataStrip(
                                                        items = if (q > 1) listOf("$q × RD$${formatPesos(prod.price)}", prod.unit)
                                                        else listOf("RD$${formatPesos(prod.price)}", prod.unit, prod.category),
                                                        slashColor = primaryColor,
                                                        fontSizeSp = 12f
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, primaryColor, CircleShape)
                                                    .clickable {
                                                        HapticFeedbackManager.onAddToCart(view)
                                                        onAddSingleProduct(prod.sku)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Agregar ${prod.name}",
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Total con precios del catálogo × cantidades sugeridas: nunca un número del modelo.
                                val totalCombo = recommendedProducts.sumOf { it.price * qtyOf(it.sku) }
                                val cashbackCombo = totalCombo / 100

                                if (msg.budgetExceededBy != null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Tone.warning.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, Tone.warning),
                                        modifier = Modifier.fillMaxWidth().testTag("budget_warning_${msg.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Tone.warning, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = "Esta selección pasa tu presupuesto por RD$${formatPesos(msg.budgetExceededBy)}. Quita productos con el botón de cada uno o pídeme una versión más económica.",
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (msg.orderConfirmed) {
                                    Surface(
                                        onClick = {
                                            HapticFeedbackManager.onConfirmAction(view)
                                            onConfirmOrderFromChat(msg.id)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Tone.success.copy(alpha = 0.14f),
                                        border = BorderStroke(1.4.dp, Tone.success),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("chat_order_confirmed_badge_${msg.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Pedido confirmado",
                                                    tint = Tone.success,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "Pedido Confirmado #${msg.confirmedOrderId ?: "ORD"}",
                                                        fontFamily = PlusJakartaSans,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Tone.successStrong
                                                    )
                                                    Text(
                                                        text = "Toca para ver seguimiento GPS en tiempo real",
                                                        fontFamily = PlusJakartaSans,
                                                        fontWeight = FontWeight.Normal,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "RD$${formatPesos(totalCombo)}",
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Tone.successStrong
                                            )
                                        }
                                    }
                                } else if (msg.requiresOrderConfirmation) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = Tone.success.copy(alpha = 0.10f),
                                        border = BorderStroke(1.5.dp, Tone.success),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("manual_order_confirmation_card_${msg.id}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "Confirmación Manual de Pedido",
                                                    fontFamily = PlusJakartaSans,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Tone.successStrong
                                                )
                                                BravoUnderline(isArca = isArca)
                                            }

                                            DiagonalMetadataStrip(
                                                items = listOf(
                                                    activeBranch,
                                                    "${recommendedProducts.size} artículos",
                                                    "ETA GPS ~$estimatedEtaMinutes min"
                                                ),
                                                slashColor = Tone.success,
                                                fontSizeSp = 12f
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Total a autorizar (+RD$$cashbackCombo Bravísima)",
                                                    fontFamily = PlusJakartaSans,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "RD$${formatPesos(totalCombo)}",
                                                    fontFamily = PlusJakartaSans,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Tone.successStrong
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        performCrispHaptic(view, HapticStyle.TICK)
                                                        onCancelOrderConfirmation(msg.id)
                                                    },
                                                    shape = RoundedCornerShape(999.dp),
                                                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                                    modifier = Modifier
                                                        .weight(0.75f)
                                                        .testTag("cancel_chat_order_button_${msg.id}")
                                                ) {
                                                    Text(
                                                        text = "Cancelar",
                                                        fontFamily = PlusJakartaSans,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Button(
                                                    onClick = {
                                                        HapticFeedbackManager.onCheckoutConfirm(view)
                                                        onConfirmOrderFromChat(msg.id)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF059669),
                                                        contentColor = Color.White
                                                    ),
                                                    shape = RoundedCornerShape(999.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                                    modifier = Modifier
                                                        .weight(1.45f)
                                                        .testTag("confirm_chat_order_button_${msg.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Confirmar Pedido • RD$${formatPesos(totalCombo)}",
                                                        fontFamily = PlusJakartaSans,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Una acción principal y el resto en el menú "⋯" para no saturar la tarjeta
                                    var bundleMenuOpen by remember(msg.id) { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AnimatedPurchaseButton(
                                            text = "Agregar todo • RD$${formatPesos(totalCombo)}",
                                            icon = Icons.Default.ShoppingCart,
                                            primaryColor = primaryColor,
                                            onClick = {
                                                HapticFeedbackManager.onAddToCart(view)
                                                onAddBundleForMessage(msg.id)
                                            },
                                            modifier = Modifier.weight(1f),
                                            testTag = "add_all_recommended_${msg.id}"
                                        )
                                        Box {
                                            Surface(
                                                onClick = { bundleMenuOpen = true },
                                                shape = CircleShape,
                                                color = primaryColor.copy(alpha = 0.12f),
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .testTag("bundle_more_${msg.id}")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.MoreHoriz,
                                                        contentDescription = "Más opciones de la receta",
                                                        tint = primaryColor
                                                    )
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = bundleMenuOpen,
                                                onDismissRequest = { bundleMenuOpen = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Guardar como lista", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold) },
                                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatListBulleted, contentDescription = null) },
                                                    onClick = {
                                                        bundleMenuOpen = false
                                                        HapticFeedbackManager.onConfirmAction(view)
                                                        onSaveBundleAsList(msg.id)
                                                    },
                                                    modifier = Modifier.testTag("save_as_list_${msg.id}")
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Pedir por mí (tú confirmas)", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold) },
                                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tone.success) },
                                                    onClick = {
                                                        bundleMenuOpen = false
                                                        HapticFeedbackManager.onConfirmAction(view)
                                                        onRequestOrderConfirmation(msg.id)
                                                    },
                                                    modifier = Modifier.testTag("request_order_confirmation_button_${msg.id}")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { topOverlayHeightPx = it.height }
                // Banda sólida: lo que se desplaza por debajo no se ve a través del encabezado
                .background(MaterialTheme.colorScheme.background)
        ) {
            GlassTopBar(
                darkMode = darkMode,
                accent = primaryColor,
                title = "Brav IA",
                subtitle = if (isGenerating) "Pensando…" else if (isArca) "Asesor de El Arca" else "Chef y compras",
                onBack = {
                    HapticFeedbackManager.onNavigationClick(view)
                    onBack()
                },
                backTestTag = "bravia_back_button",
                actions = listOf(
                    GlassBarAction(
                        symbol = "waveform", icon = Icons.Default.GraphicEq, label = "Conversación por voz",
                        text = "Voz", highlighted = isLiveVoiceModeActive,
                        testTag = "bravia_voice_mode_toggle",
                        onClick = {
                                HapticFeedbackManager.onAssistantTrigger(view)
                                if (!isLiveVoiceModeActive) {
                                    lastSpokenMessageId = messages.lastOrNull()?.id ?: -1L
                                    ttsEngine?.stop()
                                    isSpeakingTts = false
                                    isVoicePaused = false
                                    val hasPerm = hasPermission(context, AppPermission.RECORD_AUDIO)
                                    if (hasPerm) {
                                        isLiveVoiceModeActive = true
                                        startSpeechRecognition()
                                    } else {
                                        audioPermissionLauncher.launch(AppPermission.RECORD_AUDIO)
                                    }
                                } else {
                                    stopAllVoiceOutputAndRecording()
                                    isVoicePaused = false
                                    isLiveVoiceModeActive = false
                                }
                            }
                    ),
                    GlassBarAction(
                        symbol = "cart", icon = Icons.Default.ShoppingCart, label = "Ver carrito",
                        badge = if (cartTotals.count > 0) cartTotals.count.toString() else null,
                        testTag = "bravia_cart_pill",
                        onClick = {
                            HapticFeedbackManager.onNavigationClick(view)
                            onOpenCart()
                        }
                    )
                )
            )

            if (nativeLiquidGlassAvailable()) {
                // Panel de presupuesto fijo bajo la barra: monto, Listas / Armar / Ajustar,
                // deslizador de vidrio y sugerencias (desplazables con márgenes, sin cortes contra el borde)
                NativeGlassBudgetPanel(
                    value = budgetAmount,
                    min = 400,
                    max = 3500,
                    step = 50,
                    chips = quickChips.map { it.first },
                    accent = primaryColor,
                    darkMode = darkMode,
                    busy = isGenerating,
                    onChange = { onBudgetChange(it) },
                    onAction = { action ->
                        when (action) {
                            0 -> onOpenMyLists()
                            1 -> onBuildCartForBudget()
                            2 -> onAdjustCartToBudget()
                            3 -> if (!isGenerating) photoPickerLauncher.launch()
                            else -> quickChips.getOrNull(action - 10)?.let { (_, prompt) ->
                                if (!isGenerating) onSendPrompt(prompt)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 8.dp)
                        // Más alto: ahora lleva la etiqueta "Presupuesto", el mínimo/máximo y una línea de ayuda
                        .height(192.dp)
                        .testTag("budget_glass_panel")
                )
            }

            AnimatedVisibility(
                visible = isLiveVoiceModeActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LiveVoiceConversationBanner(
                    primaryColor = primaryColor,
                    darkMode = darkMode,
                    isListening = isListeningMic,
                    isSpeaking = isSpeakingTts,
                    isPaused = isVoicePaused,
                    statusText = voiceStatusLabel,
                    onTogglePause = {
                        HapticFeedbackManager.onConfirmAction(view)
                        if (!isVoicePaused) {
                            isVoicePaused = true
                            stopAllVoiceOutputAndRecording()
                            voiceStatusLabel = "Modo voz en pausa • Toca Reanudar para continuar"
                        } else {
                            isVoicePaused = false
                            voiceStatusLabel = "Reanudando escucha..."
                            val hasPerm = hasPermission(context, AppPermission.RECORD_AUDIO)
                            if (hasPerm) {
                                startSpeechRecognition()
                            } else {
                                audioPermissionLauncher.launch(AppPermission.RECORD_AUDIO)
                            }
                        }
                    },
                    onTapSpeakAgain = {
                        HapticFeedbackManager.onAssistantTrigger(view)
                        val hasPerm = hasPermission(context, AppPermission.RECORD_AUDIO)
                        if (hasPerm) {
                            startSpeechRecognition()
                        } else {
                            audioPermissionLauncher.launch(AppPermission.RECORD_AUDIO)
                        }
                    },
                    onStopVoice = {
                        HapticFeedbackManager.onNavigationClick(view)
                        stopAllVoiceOutputAndRecording()
                        isVoicePaused = false
                        isLiveVoiceModeActive = false
                    },
                    onQuickVoicePrompt = { spokenPrompt ->
                        HapticFeedbackManager.onAssistantTrigger(view)
                        ttsEngine?.stop()
                        isSpeakingTts = false
                        isVoicePaused = false
                        voiceStatusLabel = "Consulta enviada: \"$spokenPrompt\""
                        onSendPrompt(spokenPrompt)
                    }
                )
            }
        }

        // Fondo sólido detrás del campo de chat: los mensajes no se transparentan bajo la cápsula
        val dockedInputFade = MaterialTheme.colorScheme.background

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(dockedInputFade)
                .then(if (isImeVisible) Modifier else Modifier.navigationBarsPadding())
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    bottom = dynamicInputBottomPadding,
                    top = 8.dp
                )
                .testTag("bravia_docked_input_bar"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnimatedVisibility(
                visible = !pendingImageUri.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (!pendingImageUri.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 540.dp)
                            .testTag("bravia_pending_image_strip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                ChatAttachedImagePreview(
                                    uriString = pendingImageUri,
                                    modifier = Modifier.size(44.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Imagen adjunta • Brav IA Soporte Multimodal",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = primaryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Lista para auditar producto dañado/vencido, factura NCF o alacena",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    performCrispHaptic(view, HapticStyle.TICK)
                                    onSelectImageUri(null)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar imagen adjunta",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            val canSendNow = inputText.trim().isNotEmpty() || !pendingImageUri.isNullOrBlank()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .bravoAiAura(
                        cornerRadius = 999.dp,
                        enabled = isGenerating || inputText.isNotBlank(),
                        borderWidth = 1.6.dp,
                        haloAlpha = 0.24f
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .translucentBackdropEffect(darkMode = darkMode)
                    // Vidrio: tinte translúcido sobre el desenfoque del fondo (antes era un color opaco).
                    .background(
                        if (darkMode) Color(0xFF1B2333).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.55f)
                    )
                    // Borde con el color animado, leído al dibujar (no reconstruye la barra en cada cuadro)
                    .drawWithContent {
                        drawContent()
                        // "Reducir movimiento" del sistema: el borde queda quieto
                        val phase = if (reduceMotionBorder) 0f else uiColorPhase
                        val primary = sampleSoftAiRedColor(phase)
                        val secondary = sampleSoftAiRedColor(phase + 0.35f)
                        val stroke = 1.4.dp.toPx()
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    primary.copy(alpha = if (isGenerating) 0.65f else 0.38f),
                                    secondary.copy(alpha = if (isGenerating) 0.50f else 0.28f),
                                    primary.copy(alpha = if (isGenerating) 0.65f else 0.38f)
                                )
                            ),
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                            style = Stroke(width = stroke)
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        HapticFeedbackManager.onAssistantTrigger(view)
                        lastSpokenMessageId = messages.lastOrNull()?.id ?: -1L
                        ttsEngine?.stop()
                        isSpeakingTts = false
                        isVoicePaused = false
                        val hasPerm = hasPermission(context, AppPermission.RECORD_AUDIO)
                        isLiveVoiceModeActive = true
                        if (hasPerm) {
                            startSpeechRecognition()
                        } else {
                            audioPermissionLauncher.launch(AppPermission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .testTag("bravia_mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Hablar por micrófono",
                        tint = if (isListeningMic) Color(0xFFE11D48) else primaryColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = {
                        HapticFeedbackManager.onAssistantTrigger(view)
                        photoPickerLauncher.launch()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .testTag("bravia_attach_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Adjuntar foto de producto, factura NCF o alacena",
                        tint = if (!pendingImageUri.isNullOrBlank()) Tone.success else primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = when {
                                !pendingImageUri.isNullOrBlank() -> "Cuéntame qué quieres saber de la foto…"
                                isArca -> "Pregunta por tu mascota o envía una foto…"
                                else -> "Pide una receta o productos…"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(primaryColor),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val trimmed = inputText.trim()
                                if (canSendNow && !isGenerating) {
                                    HapticFeedbackManager.onAssistantTrigger(view)
                                    inputText = ""
                                    onSendPrompt(trimmed)
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bravia_input_field")
                    )
                }

                if (isGenerating) {
                    Surface(
                        onClick = {
                            performCrispHaptic(view, HapticStyle.CONFIRM)
                            stopAllVoiceOutputAndRecording()
                            onPauseGeneration()
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFE31837),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("bravia_pause_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pausar respuesta de Brav IA",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Pausar",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = {
                            val trimmed = inputText.trim()
                            if (canSendNow) {
                                HapticFeedbackManager.onAssistantTrigger(view)
                                inputText = ""
                                onSendPrompt(trimmed)
                            }
                        },
                        enabled = canSendNow,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSendNow) primaryColor
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("bravia_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar a Brav IA",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
    }
}

/**
 * Receta del recetario verificado. Todo sale de [Recipe] y del catálogo: cantidades, pasos y
 * precios. Si un ingrediente no se vende en BravoVa, se dice en vez de inventar un producto.
 */
@Composable
private fun VerifiedRecipeCard(
    recipe: Recipe,
    multiplier: Int,
    productLookup: (String) -> Product?,
    primaryColor: Color,
    onOpenProduct: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("verified_recipe_${recipe.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tone.success, modifier = Modifier.size(16.dp))
                Text("Receta verificada", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Tone.successStrong)
            }
            Text(recipe.name, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            DiagonalMetadataStrip(
                items = listOf(recipe.kind, "${recipe.servings * multiplier} porciones", "≈ ${recipe.totalMinutes} min"),
                slashColor = primaryColor,
                fontSizeSp = 12f,
                maxLines = 2
            )

            if (multiplier > 1) {
                // Las medidas de abajo son de la receta base; las unidades del carrito ya están multiplicadas.
                Text(
                    "Receta base para ${recipe.servings}. Para ${recipe.servings * multiplier} porciones, multiplica las cantidades × $multiplier (las unidades del carrito ya están ajustadas).",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = primaryColor
                )
            }
            Text("Ingredientes", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            recipe.ingredients.forEach { ing ->
                val product = ing.skus.firstNotNullOfOrNull(productLookup)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .then(if (product != null) Modifier.clickable { onOpenProduct(product.sku) } else Modifier)
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "• ${ing.amount} · ${ing.name}" + if (ing.optional) " (opcional)" else "",
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    val tag = when {
                        product == null -> "No disponible en BravoVa"
                        ing.pantry -> "Despensa · RD$${formatPesos(product.price)}"
                        else -> BravIaEngine.unitsFor(ing, multiplier).let { u -> if (u > 1) "$u × " else "" } + "RD$${formatPesos(product.price)}"
                    }
                    Text(
                        text = tag,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (product == null) Tone.warning else primaryColor,
                        textAlign = TextAlign.End
                    )
                }
            }

            Text("Preparación", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            recipe.steps.forEachIndexed { index, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(22.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = primaryColor)
                    }
                    Text(step, fontFamily = PlusJakartaSans, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
            }
            recipe.serveWith?.let {
                Text("Se sirve con: $it", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Tiempos y porciones aproximados. Si tienes alergias, revisa la etiqueta de cada producto.",
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecipeStepsVisualCard(
    steps: List<String>,
    primaryColor: Color,
    isSpeaking: Boolean = false,
    onSpeakSteps: () -> Unit,
    onPauseSpeech: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = primaryColor.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Escuchar los ${steps.size} pasos en voz alta",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isSpeaking) {
                    Surface(
                        onClick = onPauseSpeech,
                        shape = RoundedCornerShape(999.dp),
                        color = Tone.danger.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Tone.danger)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pausar voz de pasos",
                                tint = Tone.danger,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Pausar",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Tone.danger,
                                maxLines = 1
                            )
                        }
                    }
                }
                Surface(
                    onClick = onSpeakSteps,
                    shape = RoundedCornerShape(999.dp),
                    color = primaryColor.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Narrar pasos",
                            tint = primaryColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Narrar pasos",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveVoiceConversationBanner(
    primaryColor: Color,
    darkMode: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    isPaused: Boolean,
    statusText: String,
    onTogglePause: () -> Unit,
    onTapSpeakAgain: () -> Unit,
    onStopVoice: () -> Unit,
    onQuickVoicePrompt: (String) -> Unit
) {
    val hazeState = LocalTranslucentHazeState.current
    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val bannerShape = RoundedCornerShape(20.dp)
    Surface(
        shape = bannerShape,
        color = if (hazeState != null) primaryColor.copy(alpha = 0.06f) else primaryColor.copy(alpha = 0.10f),
        border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(bannerShape)
            .translucentBackdropEffect(darkMode = darkMode, accentColor = primaryColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.height(22.dp)
                    ) {
                        val factors = listOf(0.55f, 1f, 0.72f, 0.92f, 0.62f)
                        factors.forEachIndexed { idx, f ->
                            val osc = 0.45f + 0.55f * ((sin(wavePhase + idx * 0.9f) + 1f) * 0.5f)
                            val barHeight = if ((isListening || isSpeaking) && !isPaused) {
                                (20.dp * osc * f).coerceAtLeast(6.dp)
                            } else {
                                7.dp
                            }
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(barHeight)
                                    .clip(CircleShape)
                                    .background(if (isPaused) MaterialTheme.colorScheme.onSurfaceVariant else primaryColor)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPaused) "Brav IA Live • En Pausa" else "Brav IA Live • Voz en Tiempo Real",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryColor
                        )
                        Text(
                            text = statusText,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onTogglePause,
                        shape = RoundedCornerShape(999.dp),
                        color = if (isPaused) BravoAmber else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("bravia_voice_pause_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Reanudar voz" else "Pausar voz",
                                tint = if (isPaused) Color.White else primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isPaused) "Reanudar" else "Pausar",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isPaused) Color.White else primaryColor
                            )
                        }
                    }

                    Surface(
                        onClick = onTapSpeakAgain,
                        shape = RoundedCornerShape(999.dp),
                        color = primaryColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Hablar",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Hablar",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = onStopVoice,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = "Cerrar modo voz",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val voiceQuickPrompts = listOf(
                    "\"Dime los pasos para preparar Mangú\"" to "Dame los pasos exactos y al grano para preparar un Mangú con Los Tres Golpes.",
                    "\"¿Cómo preparo un Sancocho?\"" to "Explícame paso a paso y directo al grano cómo preparar un Sancocho Dominicano y qué ingredientes necesito.",
                    "\"Receta rápida con pollo\"" to "Dame los pasos concisos para preparar una receta rápida con pechuga de pollo y arroz Bravo."
                )
                voiceQuickPrompts.forEach { (chipTitle, fullPrompt) ->
                    Surface(
                        onClick = { onQuickVoicePrompt(fullPrompt) },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = chipTitle,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class AccountTileItem(
    val subScreen: AccountSubScreen,
    val title: String,
    val icon: ImageVector
)

@Composable
fun AccountGridScreen(
    userProfile: UserProfile,
    activeBranch: String,
    bravisimaBalance: Int,
    cartCount: Int = 0,
    firestoreSyncStatus: String = "Sincronizado con tu cuenta de Google",
    isGuestMode: Boolean = false,
    cacheStatusLabel: String = "Catálogo oficial al día",
    darkMode: Boolean,
    onBack: () -> Unit = {},
    onOpenCart: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onOpenBravisima: () -> Unit = {},
    onOpenSubScreen: (AccountSubScreen) -> Unit
) {
    val view = currentPlatformView()
    val primaryColor = MaterialTheme.colorScheme.primary

    val tileRows = remember {
        listOf(
            AccountTileItem(AccountSubScreen.PEDIDOS, "Mis pedidos", Icons.Outlined.LocalShipping),
            AccountTileItem(AccountSubScreen.PERFIL, "Mi perfil", Icons.Outlined.AccountCircle),
            AccountTileItem(AccountSubScreen.PAGOS, "Métodos de pago", Icons.Outlined.CreditCard),
            AccountTileItem(AccountSubScreen.DIRECCIONES, "Direcciones", Icons.Outlined.LocationOn),
            AccountTileItem(AccountSubScreen.LISTAS, "Mis listas", Icons.AutoMirrored.Outlined.FormatListBulleted),
            AccountTileItem(AccountSubScreen.AJUSTES, "Ajustes", Icons.Outlined.Settings),
            AccountTileItem(AccountSubScreen.SOPORTE, "Soporte", Icons.Outlined.ChatBubbleOutline),
            AccountTileItem(AccountSubScreen.AYUDA, "Ayuda", Icons.AutoMirrored.Outlined.HelpOutline)
        )
    }

    val hazeState = LocalTranslucentHazeState.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("account_grid_screen")
    ) {
        val accountListState = rememberLazyListState()
        val showLargeTitle by remember {
            derivedStateOf { accountListState.firstVisibleItemIndex == 0 && accountListState.firstVisibleItemScrollOffset < 40 }
        }
        LazyColumn(
            state = accountListState,
            modifier = Modifier
                .fillMaxSize()
                .translucentBackdropSource(hazeState, zIndex = 1f),
            contentPadding = PaddingValues(top = 74.dp, bottom = com.example.ui.components.LocalBottomChromeInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "account_large_title") {
                LargeTitle(title = "Mi cuenta", accent = primaryColor)
            }
            // 2.0: Bravísima vive aquí (antes era una pestaña)
            item(key = "account_bravisima_card") {
                TranslucentCard(
                    darkMode = darkMode,
                    cornerRadius = 20.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("account_bravisima_card"),
                    onClick = {
                        HapticFeedbackManager.onNavigationClick(view)
                        onOpenBravisima()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(primaryColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = primaryColor)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Tarjeta Bravísima",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tu código para la caja, puntos y premios",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Lista agrupada (estilo ajustes de iOS / Android) en lugar de casillas grandes.
            item(key = "account_menu", contentType = "account_menu") {
                TranslucentCard(
                    darkMode = darkMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column {
                        tileRows.forEachIndexed { index, tile ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HapticFeedbackManager.onNavigationClick(view)
                                        onOpenSubScreen(tile.subScreen)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp)
                                    .testTag("account_tile_${tile.subScreen.key}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(primaryColor.copy(alpha = if (darkMode) 0.20f else 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(tile.icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(19.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = tile.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (index < tileRows.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 64.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

        item(key = "firestore_account_sync_card", contentType = "firestore_sync") {
            TranslucentCard(
                darkMode = darkMode,
                cornerRadius = 22.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("firestore_sync_account_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Cuenta de Google sincronizada",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGuestMode) "Modo invitado • Sesión en este dispositivo" else userProfile.email,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isGuestMode) "$cacheStatusLabel • Sin cuenta Google" else firestoreSyncStatus,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            HapticFeedbackManager.onNavigationClick(view)
                            onSignOut()
                        },
                        border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("account_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = if (isGuestMode) "Conectar con Google" else "Cerrar sesión",
                            tint = primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGuestMode) "Conectar con Google" else "Salir",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryColor
                        )
                    }
                }
            }
        }
    }

        GlassTopBar(
            darkMode = darkMode,
            accent = primaryColor,
            title = if (showLargeTitle) "" else "Mi cuenta",
            modifier = Modifier.align(Alignment.TopCenter),
            // Sin "atrás": Mi cuenta es una pestaña raíz del dock (como Inicio y Catálogo)
            actions = listOf(
                GlassBarAction(
                    symbol = "cart", icon = Icons.Default.ShoppingCart, label = "Carrito",
                    badge = if (cartCount > 0) cartCount.toString() else null,
                    testTag = "account_cart_button",
                    onClick = {
                        HapticFeedbackManager.onNavigationClick(view)
                        onOpenCart()
                    }
                )
            )
        )
    }
}

@Composable
fun AccountSubScreenView(
    subScreen: AccountSubScreen,
    userProfile: UserProfile,
    activeBranch: String,
    selectedPaletteId: String = "material_you",
    firestoreSyncStatus: String = "Sincronizado con tu cuenta de Google",
    isGuestMode: Boolean = false,
    isOfflineMode: Boolean = false,
    cacheStatusLabel: String = "Catálogo oficial al día",
    npuStatus: NpuHardwareStatus = NpuHardwareStatus(),
    onToggleNpuMode: () -> Unit = {},
    orders: List<OrderRecord>,
    paymentMethods: List<PaymentMethod>,
    addresses: List<UserAddress>,
    shoppingLists: List<ShoppingList>,
    aiPopulatingListId: String? = null,
    frequentStats: List<FrequentProductStat> = emptyList(),
    allProducts: List<Product> = emptyList(),
    productLookup: (String) -> Product? = { null },
    fcmToken: String = "",
    fcmTopics: List<FcmTopicSubscription> = emptyList(),
    pushFeed: List<PushNotificationRecord> = emptyList(),
    darkMode: Boolean,
    notificationsEnabled: Boolean,
    onBack: () -> Unit,
    onRepeatOrder: (String) -> Unit,
    orderRatings: Map<String, Int> = emptyMap(),
    onRateOrder: (String, Int) -> Unit = { _, _ -> },
    onOpenLiveMap: () -> Unit,
    onOpenOrderTracking: (String) -> Unit = { onOpenLiveMap() },
    onSaveProfile: (String, String, String, String) -> Unit,
    onSelectPaymentMethod: (String) -> Unit,
    onAddPaymentCard: () -> Unit,
    onSelectAddress: (String) -> Unit,
    onLoadListToCart: (String) -> Unit,
    onCreateShoppingList: (name: String, autoPopulateWithAi: Boolean, focusHint: String) -> Unit = { _, _, _ -> },
    onDeleteShoppingList: (String) -> Unit = {},
    onRenameShoppingList: (String, String) -> Unit = { _, _ -> },
    onPopulateListWithAi: (String) -> Unit = {},
    onAddProductToList: (String, String) -> Unit = { _, _ -> },
    onRemoveProductFromList: (String, String) -> Unit = { _, _ -> },
    onAddSingleToCart: (String) -> Unit = {},
    onOpenProduct: (String) -> Unit = {},
    onToggleDarkMode: () -> Unit,
    followsSystemTheme: Boolean = false,
    onFollowSystemTheme: () -> Unit = {},
    dataSaver: Boolean = false,
    onToggleDataSaver: (Boolean) -> Unit = {},
    onSelectPalette: (String) -> Unit = {},
    onToggleNotifications: () -> Unit,
    onToggleFcmTopic: (String, Boolean) -> Unit = { _, _ -> },
    onTriggerOfferPush: () -> Unit = {},
    onTriggerOrderPush: () -> Unit = {},
    onSelectPushNotification: (PushNotificationRecord) -> Unit = {},
    onMarkAllPushRead: () -> Unit = {},
    onRefreshLocalCache: () -> Unit = {},
    submittedSuggestions: List<SuggestionMailPayload> = emptyList(),
    onSubmitSupportSuggestion: (category: String, priority: String, subject: String, body: String) -> Unit = { _, _, _, _ -> },
    onOpenBravIaSupportPrompt: (String) -> Unit = {},
    onUpdateBranch: (String) -> Unit,
    onSignOut: () -> Unit = {},
    onShowToast: (String, String) -> Unit
) {
    val view = currentPlatformView()
    val primaryColor = MaterialTheme.colorScheme.primary
    var expandedListId by remember { mutableStateOf<String?>("l2") }
    var supportCategory by remember { mutableStateOf("SUGERENCIA_PRODUCTO") }
    var supportPriority by remember { mutableStateOf("ALTA") }
    var supportSubject by remember { mutableStateOf("") }
    var supportBody by remember { mutableStateOf("") }

    val hazeState = LocalTranslucentHazeState.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("account_subscreen_${subScreen.key}")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .translucentBackdropSource(hazeState, zIndex = 1f),
            contentPadding = PaddingValues(top = 74.dp, bottom = com.example.ui.components.LocalBottomChromeInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (subScreen) {
                AccountSubScreen.PEDIDOS -> {
                    item(key = "firestore_orders_sync_pill") {
                        TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            DiagonalMetadataStrip(
                                items = listOf("Cuenta de Google", firestoreSyncStatus),
                                slashColor = primaryColor,
                                fontSizeSp = 12f
                            )
                        }
                    }
                }

                item(key = "live_order_banner") {
                    val topOrder = orders.firstOrNull()
                    val topStage = OrderDeliveryStage.fromStatusString(topOrder?.status)
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        onClick = {
                            if (topOrder != null) {
                                onOpenOrderTracking(topOrder.id)
                            } else {
                                onOpenLiveMap()
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        BravoUnifiedIcon(
                                            iconKey = topStage.emoji,
                                            tint = Tone.successStrong,
                                            size = 16.dp
                                        )
                                        Text(
                                            text = "Pedido #${topOrder?.id ?: "ORD-88412"} • ${topStage.statusText}",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Tone.successStrong
                                        )
                                    }
                                    DiagonalMetadataStrip(
                                        // Entregado: sin "Avance 100%" ni "Seguimiento en vivo" (ya no hay nada que seguir)
                                        items = if (topStage == OrderDeliveryStage.DELIVERED) listOf(
                                            topOrder?.branch ?: "Av. Winston Churchill",
                                            topOrder?.date ?: "Hoy"
                                        ) else listOf(
                                            topOrder?.branch ?: "Av. Winston Churchill",
                                            "Avance ${topStage.progressPct}%",
                                            "Seguimiento en vivo"
                                        ),
                                        slashColor = primaryColor,
                                        fontSizeSp = 12f
                                    )
                                }
                                Text(
                                    text = "Ver progreso",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = primaryColor
                                )
                            }

                            LinearProgressIndicator(
                                progress = { topStage.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = if (topStage == OrderDeliveryStage.DELIVERED) {
                                    Tone.success
                                } else {
                                    primaryColor
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                items(items = orders, key = { it.id }) { order ->
                    val orderStage = OrderDeliveryStage.fromStatusString(order.status)
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pedido ${order.id}",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (orderStage == OrderDeliveryStage.DELIVERED) {
                                        Color(0xFFECFDF5)
                                    } else {
                                        primaryColor.copy(alpha = 0.12f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        val badgeTint = if (orderStage == OrderDeliveryStage.DELIVERED) {
                                            Tone.successStrong
                                        } else {
                                            primaryColor
                                        }
                                        BravoUnifiedIcon(
                                            iconKey = orderStage.emoji,
                                            tint = badgeTint,
                                            size = 13.dp
                                        )
                                        Text(
                                            // Entregado: el 100% y la barra sobran; solo se muestran mientras va en camino
                                            text = if (orderStage == OrderDeliveryStage.DELIVERED) orderStage.statusText else "${orderStage.statusText} • ${orderStage.progressPct}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTint
                                        )
                                    }
                                }
                            }
                            DiagonalMetadataStrip(
                                items = listOf(order.date, order.branch, "${order.badgeNum} artículos"),
                                slashColor = primaryColor,
                                fontSizeSp = 12f
                            )
                            if (orderStage != OrderDeliveryStage.DELIVERED) LinearProgressIndicator(
                                progress = { orderStage.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = if (orderStage == OrderDeliveryStage.DELIVERED) {
                                    Tone.success
                                } else {
                                    primaryColor
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RD$${formatPesos(order.total.roundToInt())}.00",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = primaryColor
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenOrderTracking(order.id) },
                                        border = BorderStroke(1.2.dp, primaryColor),
                                        shape = RoundedCornerShape(999.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("track_order_button_${order.id}")
                                    ) {
                                        Text(
                                            text = "Seguir envío",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = primaryColor
                                        )
                                    }
                                    AnimatedPurchaseButton(
                                        text = "Repetir",
                                        icon = null,
                                        primaryColor = primaryColor,
                                        onClick = { onRepeatOrder(order.id) }
                                    )
                                }
                            }
                            if (orderStage == OrderDeliveryStage.DELIVERED) {
                                val myRating = orderRatings[order.id] ?: 0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (myRating == 0) "¿Qué tal la entrega?" else "Tu calificación",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row {
                                        (1..5).forEach { star ->
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .clickable { onRateOrder(order.id, star) }
                                                    .semantics { contentDescription = "$star de 5 estrellas" }
                                                    .testTag("rate_${order.id}_$star"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (star <= myRating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                                    contentDescription = null,
                                                    tint = if (star <= myRating) Tone.warningBright else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AccountSubScreen.PERFIL -> {
                item(key = "profile_form") {
                    var name by remember(userProfile) { mutableStateOf(userProfile.name) }
                    var phone by remember(userProfile) { mutableStateOf(userProfile.phone) }
                    var rncNit by remember(userProfile) { mutableStateOf(userProfile.rncNit) }
                    var ncfType by remember(userProfile) { mutableStateOf(userProfile.ncfType) }

                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nombre completo") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Teléfono móvil (RD)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = rncNit,
                                onValueChange = { rncNit = it },
                                label = { Text("Cédula / RNC") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = ncfType,
                                onValueChange = { ncfType = it },
                                label = { Text("Tipo de comprobante fiscal (NCF)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            AnimatedPurchaseButton(
                                text = "Guardar cambios",
                                icon = null,
                                primaryColor = primaryColor,
                                onClick = { onSaveProfile(name, phone, rncNit, ncfType) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedButton(
                                onClick = {
                                    HapticFeedbackManager.onNavigationClick(view)
                                    onSignOut()
                                },
                                border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.65f)),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isGuestMode) {
                                        "Iniciar sesión con Google (salir del modo invitado)"
                                    } else {
                                        "Cerrar sesión de Google (${userProfile.email})"
                                    },
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = primaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            AccountSubScreen.PAGOS -> {
                items(items = paymentMethods, key = { it.id }) { method ->
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        onClick = { onSelectPaymentMethod(method.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${method.brand} ${method.bank} •••• ${method.last4}",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                DiagonalMetadataStrip(
                                    items = listOf("Vence ${method.exp}", if (method.isDefault) "Predeterminada" else "Secundaria"),
                                    slashColor = primaryColor,
                                    fontSizeSp = 12f
                                )
                            }
                            if (method.isDefault) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Predeterminada",
                                    tint = Tone.success
                                )
                            }
                        }
                    }
                }

                item(key = "add_card_btn") {
                    AnimatedPurchaseButton(
                        text = "+ Vincular Nueva Tarjeta Bancaria",
                        icon = null,
                        primaryColor = primaryColor,
                        onClick = onAddPaymentCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    )
                }
            }

            AccountSubScreen.DIRECCIONES -> {
                items(items = addresses, key = { it.id }) { addr ->
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        onClick = { onSelectAddress(addr.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = addr.label,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                DiagonalMetadataStrip(
                                    items = listOf(addr.street, addr.branch),
                                    slashColor = primaryColor,
                                    fontSizeSp = 12f
                                )
                            }
                            if (addr.isDefault) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Activa",
                                    tint = Tone.success
                                )
                            }
                        }
                    }
                }
            }

            AccountSubScreen.LISTAS -> {
                item(key = "bravia_history_hero", contentType = "ai_history_hero") {
                    BravIaHistoryAutoListHeroCard(
                        ordersCount = orders.size,
                        frequentStats = frequentStats,
                        isPopulatingAny = aiPopulatingListId != null,
                        darkMode = darkMode,
                        primaryColor = primaryColor,
                        onCreateAiPresetList = { title, focusHint ->
                            onCreateShoppingList(title, true, focusHint)
                        }
                    )
                }

                item(key = "create_shopping_list_form", contentType = "create_list_form") {
                    CreateShoppingListFormCard(
                        darkMode = darkMode,
                        primaryColor = primaryColor,
                        isPopulatingAny = aiPopulatingListId != null,
                        onCreateList = { name, autoPopulate ->
                            onCreateShoppingList(name, autoPopulate, name)
                        }
                    )
                }

                items(
                    items = shoppingLists,
                    key = { it.id },
                    contentType = { "managed_shopping_list" }
                ) { list ->
                    ManagedShoppingListCard(
                        shoppingList = list,
                        isExpanded = expandedListId == list.id || aiPopulatingListId == list.id,
                        isPopulatingThisList = aiPopulatingListId == list.id,
                        allProducts = allProducts,
                        frequentStats = frequentStats,
                        productLookup = productLookup,
                        darkMode = darkMode,
                        primaryColor = primaryColor,
                        onToggleExpand = {
                            expandedListId = if (expandedListId == list.id) null else list.id
                        },
                        onLoadListToCart = { onLoadListToCart(list.id) },
                        onPopulateWithAi = {
                            expandedListId = list.id
                            onPopulateListWithAi(list.id)
                        },
                        onRenameList = { newName -> onRenameShoppingList(list.id, newName) },
                        onDeleteList = { onDeleteShoppingList(list.id) },
                        onAddProductToList = { sku -> onAddProductToList(list.id, sku) },
                        onRemoveProductFromList = { sku -> onRemoveProductFromList(list.id, sku) },
                        onAddSingleToCart = { sku -> onAddSingleToCart(sku) },
                        onOpenProduct = { sku -> onOpenProduct(sku) }
                    )
                }
            }

            AccountSubScreen.AJUSTES -> {
                val branches = listOf(
                    "Bravo Winston Churchill",
                    "Bravo Enriquillo",
                    "Bravo Núñez de Cáceres",
                    "Bravo Los Próceres",
                    "Bravo San Isidro",
                    "Bravo Santiago"
                )

                item(key = "settings_toggles") {
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Usar tema del dispositivo",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Claro u oscuro según los ajustes del teléfono",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                BravoSwitch(
                                    checked = followsSystemTheme,
                                    onCheckedChange = { follow ->
                                        performCrispHaptic(view, HapticStyle.CONFIRM)
                                        if (follow) onFollowSystemTheme() else onToggleDarkMode()
                                    },
                                    accent = primaryColor,
                                    modifier = Modifier.testTag("follow_system_theme_switch")
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            SwitchRow(
                                title = "Ahorro de datos",
                                subtitle = "Imágenes más livianas y sin animaciones automáticas",
                                checked = dataSaver,
                                accent = primaryColor
                            ) { on ->
                                performCrispHaptic(view, HapticStyle.CONFIRM)
                                onToggleDataSaver(on)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = primaryColor
                                    )
                                    Column {
                                        Text(
                                            text = "Modo oscuro",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        DiagonalMetadataStrip(
                                            items = listOf("Guardado local", if (darkMode) "Activo" else "Inactivo"),
                                            slashColor = primaryColor,
                                            fontSizeSp = 12f
                                        )
                                    }
                                }
                                BravoSwitch(
                                    checked = darkMode,
                                    onCheckedChange = {
                                        performCrispHaptic(view, HapticStyle.CONFIRM)
                                        onToggleDarkMode()
                                    },
                                    accent = primaryColor,
                                    modifier = Modifier.testTag("dark_mode_switch")
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = primaryColor
                                    )
                                    Column {
                                        Text(
                                            text = "Alertas de Ofertas y Delivery",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Notificaciones de tu pedido en tiempo real",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                BravoSwitch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = {
                                        performCrispHaptic(view, HapticStyle.TICK)
                                        onToggleNotifications()
                                    },
                                    accent = primaryColor
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = primaryColor
                                    )
                                    Column {
                                        Text(
                                            text = "Catálogo de Productos y Modo Sin Conexión",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        DiagonalMetadataStrip(
                                            items = listOf(
                                                if (isOfflineMode) "Modo sin conexión activo" else "Sincronizado en el dispositivo",
                                                cacheStatusLabel
                                            ),
                                            slashColor = primaryColor,
                                            fontSizeSp = 12f
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        performCrispHaptic(view, HapticStyle.CONFIRM)
                                        onRefreshLocalCache()
                                    },
                                    border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("refresh_local_cache_button")
                                ) {
                                    Text(
                                        text = "Sincronizar",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = primaryColor
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "fcm_push_controls_and_feed", contentType = "fcm_push_section") {
                    FcmPushControlAndFeedCard(
                        fcmToken = fcmToken,
                        fcmTopics = fcmTopics,
                        pushFeed = pushFeed,
                        darkMode = darkMode,
                        primaryColor = primaryColor,
                        onToggleTopic = onToggleFcmTopic,
                        onTriggerOfferPush = onTriggerOfferPush,
                        onTriggerOrderPush = onTriggerOrderPush,
                        onSelectNotification = onSelectPushNotification,
                        onMarkAllRead = onMarkAllPushRead
                    )
                }

                item(key = "settings_device_dynamic_colors") {
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .testTag("device_dynamic_color_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(primaryColor.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Color Automático del Dispositivo",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Sincronizado con el color elegido en los ajustes de tu Android (Material You) con contraste y legibilidad garantizados.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Activo",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                item(key = "settings_branches") {
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Sucursal preferida",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                BravoUnderline(isArca = false)
                            }
                            branches.forEach { branch ->
                                val selected = activeBranch == branch
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selected) primaryColor.copy(alpha = 0.1f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            performCrispHaptic(view, HapticStyle.TICK)
                                            onUpdateBranch(branch)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = branch,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) primaryColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AccountSubScreen.SOPORTE -> {
                item(key = "support_multimodal_ai_card") {
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Brav IA Soporte • Visión Multimodal y Calidad",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                BravoUnderline(isArca = false)
                            }
                            Text(
                                text = "Envía una foto del producto a Brav IA para registrar tu reporte. El equipo de servicio al cliente de $activeBranch lo revisa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    performCrispHaptic(view, HapticStyle.CONFIRM)
                                    onOpenBravIaSupportPrompt("Quiero reportar un producto dañado o vencido de mi orden en $activeBranch.")
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1. Reportar Producto Dañado / Vencido (Foto)",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = primaryColor
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    performCrispHaptic(view, HapticStyle.CONFIRM)
                                    onOpenBravIaSupportPrompt("Quiero auditar mediante OCR una foto de mi factura o ticket NCF (B01/B02) para verificar ITBIS (18%/16%), descuentos y puntos Bravísima.")
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "2. Auditar Factura o Ticket NCF (OCR)",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = primaryColor
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    performCrispHaptic(view, HapticStyle.CONFIRM)
                                    onOpenBravIaSupportPrompt("Voy a subir una foto de un ingrediente de mi alacena o etiqueta de mascota para buscar su equivalente exacto en el catálogo Bravo o El Arca.")
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "3. Identificar Alacena o Etiqueta de Mascota",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }

                item(key = "support_suggestion_mailbox_card") {
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .testTag("support_suggestion_mailbox_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Buzón de sugerencias y mejora continua",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                BravoUnderline(isArca = false)
                            }
                            DiagonalMetadataStrip(
                                items = listOf("Destino: bezaleelpatrocino0@gmail.com", activeBranch),
                                slashColor = primaryColor,
                                fontSizeSp = 12f
                            )

                            val categories = listOf(
                                "SUGERENCIA_PRODUCTO" to "Producto",
                                "MEJORA_UI_UX" to "Mejora de la app",
                                "REPORTE_SOPORTE" to "Soporte",
                                "FELICITACION" to "Felicitación"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.forEach { (code, label) ->
                                    val selected = supportCategory == code
                                    Surface(
                                        onClick = {
                                            performCrispHaptic(view, HapticStyle.TICK)
                                            supportCategory = code
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (selected) primaryColor else primaryColor.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, primaryColor)
                                    ) {
                                        Text(
                                            text = label,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (selected) Color.White else primaryColor,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            val priorities = listOf("ALTA", "MEDIA", "BAJA")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Prioridad:",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                priorities.forEach { prio ->
                                    val selected = supportPriority == prio
                                    Surface(
                                        onClick = {
                                            performCrispHaptic(view, HapticStyle.TICK)
                                            supportPriority = prio
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (selected) Tone.success else Tone.success.copy(alpha = 0.10f)
                                    ) {
                                        Text(
                                            text = prio,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (selected) Color.White else Tone.successStrong,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = supportSubject,
                                onValueChange = { supportSubject = it },
                                label = { Text("Asunto ejecutivo (opcional)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("support_suggestion_subject_input")
                            )

                            OutlinedTextField(
                                value = supportBody,
                                onValueChange = { supportBody = it },
                                label = { Text("Escribe tu sugerencia, producto deseado o reporte...") },
                                minLines = 3,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("support_suggestion_body_input")
                            )

                            AnimatedPurchaseButton(
                                text = "Enviar a bezaleelpatrocino0@gmail.com",
                                icon = Icons.Default.Email,
                                primaryColor = primaryColor,
                                onClick = {
                                    if (supportBody.trim().isNotEmpty()) {
                                        performCrispHaptic(view, HapticStyle.CONFIRM)
                                        onSubmitSupportSuggestion(
                                            supportCategory,
                                            supportPriority,
                                            supportSubject.trim(),
                                            supportBody.trim()
                                        )
                                        supportSubject = ""
                                        supportBody = ""
                                    } else {
                                        onShowToast("info", "Escribe el detalle de tu sugerencia antes de enviar")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "support_send_suggestion_button"
                            )
                        }
                    }
                }

                if (submittedSuggestions.isNotEmpty()) {
                    items(
                        items = submittedSuggestions,
                        key = { it.timestampIso + it.subject }
                    ) { payload ->
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            SuggestionMailDispatchedCard(
                                payload = payload,
                                primaryColor = primaryColor
                            )
                        }
                    }
                }

                item(key = "support_card") {
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Centro de Atención al Cliente Bravo",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                BravoUnderline(isArca = false)
                            }
                            Text(
                                text = "Horario: ${StoreFacts.HOURS} El tiempo de entrega se calcula para tu dirección y lo ves al pagar y en el seguimiento.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { onShowToast("chat", "Conectando con WhatsApp Bravo (809-540-2728)...") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Chatear por WhatsApp", fontWeight = FontWeight.Bold)
                            }
                            AnimatedPurchaseButton(
                                text = "Llamar al (809) 540-2728",
                                icon = null,
                                primaryColor = primaryColor,
                                onClick = { onShowToast("phone", "Marcando a Servicio al Cliente (809-540-2728)...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            AccountSubScreen.AYUDA -> {
                val faqs = listOf(
                    "¿Cómo funciona el 1% de la Tarjeta Bravísima?" to "${StoreFacts.BRAVISIMA} Presenta el código ${userProfile.bravisimaNumber}.",
                    "¿Cuáles son los horarios y tiempos de entrega?" to "${StoreFacts.HOURS} El tiempo de entrega depende de tu dirección: lo ves al pagar y en el seguimiento del pedido.",
                    "¿Cómo funciona la Facturación Fiscal (NCF)?" to StoreFacts.NCF,
                    "¿Qué hace Brav IA?" to "Te recomienda productos del catálogo con sus precios reales, te da recetas de su recetario verificado (${BRAV_IA_RECIPES.size} platos), avisa si una selección pasa tu presupuesto, mira las fotos que le envíes y registra tus sugerencias. No inventa precios ni recetas: si algo no está en el catálogo o en el recetario, te lo dice."
                )
                items(items = faqs, key = { it.first }) { (q, a) ->
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = q,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = a,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

        GlassTopBar(
            darkMode = darkMode,
            accent = primaryColor,
            title = subScreen.title,
            modifier = Modifier.align(Alignment.TopCenter),
            onBack = {
                HapticFeedbackManager.onNavigationClick(view)
                onBack()
            },
            backLabel = "Volver a mi cuenta",
            backTestTag = "account_sub_back_button"
        )
    }
}

/**
 * Acciones bajo cada respuesta de Brav IA (guía de IA generativa de Apple): Copiar, Reintentar y
 * una valoración 👍/👎 voluntaria. La valoración solo cambia el icono y queda en el registro: no
 * interrumpe la conversación ni abre formularios.
 */
@Composable
private fun AiReplyActions(
    messageId: Long,
    text: String,
    accent: Color,
    canRetry: Boolean,
    onRetry: () -> Unit,
    showDisclaimer: Boolean
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var copied by remember(messageId) { mutableStateOf(false) }
    var rating by rememberSaveable(messageId) { mutableStateOf(0) } // 1 = útil, -1 = no útil
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(copied) {
        if (copied) { kotlinx.coroutines.delay(1500); copied = false }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        @Composable
        fun action(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, enabled: Boolean = true, onClick: () -> Unit) {
            IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
                Icon(icon, contentDescription = label, tint = if (enabled) tint else tint.copy(alpha = 0.35f), modifier = Modifier.size(18.dp))
            }
        }
        action(
            if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
            if (copied) "Copiado" else "Copiar respuesta",
            if (copied) accent else muted
        ) {
            clipboard.setText(androidx.compose.ui.text.AnnotatedString(text))
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            copied = true
        }
        action(Icons.Default.Refresh, "Reintentar respuesta", muted, enabled = canRetry) {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            onRetry()
        }
        Spacer(Modifier.weight(1f))
        action(
            if (rating == 1) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            "Respuesta útil",
            if (rating == 1) accent else muted
        ) {
            rating = if (rating == 1) 0 else 1
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            Log.d("BravIA", "valoracion mensaje=$messageId util=$rating")
        }
        action(
            if (rating == -1) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
            "Respuesta no útil",
            if (rating == -1) accent else muted
        ) {
            rating = if (rating == -1) 0 else -1
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            Log.d("BravIA", "valoracion mensaje=$messageId util=$rating")
        }
    }
    if (showDisclaimer) {
        Text(
            text = "Brav IA puede equivocarse. Revisa precios y cantidades antes de pedir.",
            style = MaterialTheme.typography.labelSmall,
            color = muted.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
        )
    }
}
