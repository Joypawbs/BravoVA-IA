package com.example.ui.screens

import org.jetbrains.compose.resources.stringResource

import com.example.ui.theme.Tone

import androidx.compose.material.icons.outlined.Share
import com.example.ui.components.orderWhatsAppText
import com.example.ui.components.shareOnWhatsApp
import com.example.ui.components.RdCheckoutOptionsCard
import com.example.model.CheckoutExtras
import com.example.ui.components.GlassBarAction
import com.example.ui.components.GlassTopBar
import com.example.platform.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.resources.*
import org.jetbrains.compose.resources.painterResource

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.ui.graphics.SolidColor
import com.example.model.BravisimaCard
import com.example.model.BravisimaMovement
import com.example.model.CartTotals
import com.example.model.DriverChatMessage
import com.example.model.DriverLiveTelemetry
import com.example.model.LiveDeliveryBannerNotification
import com.example.model.OrderDeliveryStage
import com.example.model.OrderRecord
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.RealLocationDeliveryMetrics
import com.example.model.StoreMode
import com.example.model.UserAddress
import com.example.model.UserProfile
import com.example.security.BiometricCheckoutAuthenticator
import com.example.security.BiometricCriticalOperation
import com.example.security.BiometricPromptRequest
import com.example.security.BiometricVerificationResult
import com.example.ui.components.AnimatedPurchaseButton
import com.example.ui.components.BravoOfficialLogoBadge
import com.example.ui.components.BravoUnderline
import com.example.ui.components.BravoUnifiedIcon
import com.example.ui.components.DiagonalMetadataStrip
import com.example.ui.components.FloatingHeaderCapsule
import com.example.ui.components.HapticFeedbackManager
import com.example.ui.components.HapticStyle
import com.example.ui.components.LocalTranslucentHazeState
import com.example.ui.components.ProductArtworkView
import com.example.ui.components.TranslucentCard
import com.example.ui.components.performCrispHaptic
import com.example.ui.components.translucentBackdropSource
import com.example.ui.theme.ArcaTeal
import com.example.ui.theme.BravoAmber
import com.example.ui.theme.BravoRed
import com.example.ui.theme.BravoRedDark
import com.example.ui.theme.PlusJakartaSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private fun formatMoney(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount)
}

@Composable
fun BravisimaScreen(
    balance: Int,
    cartTotals: CartTotals,
    userProfile: UserProfile,
    bravisimaCard: BravisimaCard,
    movements: List<BravisimaMovement>,
    isGuestMode: Boolean,
    isSavingBravisima: Boolean,
    darkMode: Boolean,
    /** Bravísima se abre desde Mi cuenta: lleva "atrás" como toda pantalla hija. */
    onBack: (() -> Unit)? = null,
    onRedeemInCart: () -> Unit,
    onOpenScanner: () -> Unit,
    onToggleCardLock: () -> Unit,
    onToggleAutoRedeem: () -> Unit,
    onSaveCardSettings: (holderName: String, monthlyGoal: Int, preferredCategory: String) -> Unit,
    onRedeemPromoCode: (String) -> Unit,
    onCopyCardNumber: (String) -> Unit,
    onSignOutGoogle: () -> Unit
) {
    val context = currentPlatformContext()
    val view = currentPlatformView()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val neededForNextPeso = cartTotals.nextHundredRemainder.roundToInt()

    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var authErrorBanner by remember { mutableStateOf<String?>(null) }
    var maskCardNumber by remember { mutableStateOf(false) }
    var isEditingSettings by remember { mutableStateOf(false) }
    var editableHolderName by remember(bravisimaCard.holderName) { mutableStateOf(bravisimaCard.holderName) }
    var editableGoal by remember(bravisimaCard.monthlyGoal) { mutableIntStateOf(bravisimaCard.monthlyGoal) }
    var editableCategory by remember(bravisimaCard.preferredCategory) { mutableStateOf(bravisimaCard.preferredCategory) }
    var promoCodeInput by remember { mutableStateOf("") }
    var showCardLockBiometricPrompt by remember { mutableStateOf(false) }

    val cardNumber = bravisimaCard.cardNumber.ifBlank { userProfile.bravisimaNumber }
    val displayCardNumber = if (maskCardNumber && cardNumber.length >= 4) {
        "BRV-••••-••••-${cardNumber.takeLast(4)}"
    } else {
        cardNumber
    }
    // Código de barras real (Code 128) para que el escáner de la caja lo lea
    val barcodeModules = remember(cardNumber) { com.example.ui.components.Code128.modules(cardNumber) }
    var showFullscreenCode by remember { mutableStateOf(false) }
    if (showFullscreenCode) {
        com.example.ui.components.BravisimaFullscreenCode(
            cardNumber = cardNumber,
            memberName = userProfile.name,
            onDismiss = { showFullscreenCode = false }
        )
    }

    val hazeState = LocalTranslucentHazeState.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bravisima_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .translucentBackdropSource(hazeState, zIndex = 1f),
            contentPadding = PaddingValues(top = 74.dp, bottom = 184.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "bravisima_card", contentType = "loyalty_card") {
                val cardGradient = if (bravisimaCard.isLocked) {
                    listOf(Tone.inkSoft, Color(0xFF1E293B), Tone.ink)
                } else {
                    listOf(primaryColor, BravoRedDark, Color(0xFF4C0519))
                }
                var cardSize by remember { mutableStateOf(IntSize.Zero) }
                var isCardPressed by remember { mutableStateOf(false) }
                var targetTiltX by remember { mutableFloatStateOf(0f) }
                var targetTiltY by remember { mutableFloatStateOf(0f) }
                var glareXFrac by remember { mutableFloatStateOf(0.5f) }
                var glareYFrac by remember { mutableFloatStateOf(0.35f) }

                val animatedTiltX by animateFloatAsState(
                    targetValue = targetTiltX,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "cardTiltX"
                )
                val animatedTiltY by animateFloatAsState(
                    targetValue = targetTiltY,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "cardTiltY"
                )
                val animatedCardScale by animateFloatAsState(
                    targetValue = if (isCardPressed) 1.015f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "cardScale"
                )
                val sweepTransition = rememberInfiniteTransition(label = "card_holo_sweep")
                val sweepProgress by sweepTransition.animateFloat(
                    initialValue = -0.35f,
                    targetValue = 1.45f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 4500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "holoSweepProgress"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .onSizeChanged { cardSize = it }
                        .graphicsLayer {
                            cameraDistance = 12f * density
                            rotationX = animatedTiltX
                            rotationY = animatedTiltY
                            scaleX = animatedCardScale
                            scaleY = animatedCardScale
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val activePointer = event.changes.firstOrNull { it.pressed }
                                    if (activePointer != null && cardSize.width > 0 && cardSize.height > 0) {
                                        isCardPressed = true
                                        val nx = ((activePointer.position.x / cardSize.width) - 0.5f).coerceIn(-0.5f, 0.5f)
                                        val ny = ((activePointer.position.y / cardSize.height) - 0.5f).coerceIn(-0.5f, 0.5f)
                                        targetTiltY = nx * 16f
                                        targetTiltX = -ny * 14f
                                        glareXFrac = (nx + 0.5f).coerceIn(0f, 1f)
                                        glareYFrac = (ny + 0.5f).coerceIn(0f, 1f)
                                    } else {
                                        isCardPressed = false
                                        targetTiltX = 0f
                                        targetTiltY = 0f
                                    }
                                }
                            }
                        }
                        .shadow(18.dp, RoundedCornerShape(26.dp), spotColor = primaryColor.copy(alpha = 0.35f))
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(colors = cardGradient))
                        .testTag("bravisima_digital_card")
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        if (isCardPressed) {
                            val center = Offset(w * glareXFrac, h * glareYFrac)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.24f), Color.Transparent),
                                    center = center,
                                    radius = w * 0.55f
                                ),
                                radius = w * 0.55f,
                                center = center
                            )
                        }
                        val sweepX = w * sweepProgress
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.14f),
                                    BravoAmber.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                start = Offset(sweepX - w * 0.28f, 0f),
                                end = Offset(sweepX + w * 0.18f, h)
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.24f),
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Black.copy(alpha = 0.36f)
                                    )
                                )
                            )
                    )
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                DiagonalMetadataStrip(
                                    items = listOf(
                                        // Sin "NIVEL": así las tres etiquetas caben en una línea
                                        // (con él, "1% CASHBACK" se cortaba)
                                        bravisimaCard.tier.label.uppercase(),
                                        if (bravisimaCard.isLocked) "BLOQUEADA" else "ACTIVA",
                                        "1% CASHBACK"
                                    ),
                                    slashColor = Color.White,
                                    textColor = Color.White,
                                    fontSizeSp = 11f,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Bravísima digital",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = Color.White
                                )

                                Text(
                                    text = bravisimaCard.holderName.uppercase(),
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.1.sp,
                                    color = Color.White.copy(alpha = 0.96f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
                                    BravoOfficialLogoBadge(scale = 0.84f)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .clickable(enabled = !bravisimaCard.isLocked, onClickLabel = "Mostrar el código en grande para la caja") {
                                    performCrispHaptic(view, HapticStyle.CONFIRM)
                                    showFullscreenCode = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("bravisima_barcode_plate"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                val unitWidth = size.width / barcodeModules.size
                                barcodeModules.forEachIndexed { i, isBar ->
                                    if (isBar) drawRect(
                                        color = if (bravisimaCard.isLocked) Tone.muted else Color(0xFF111827),
                                        topLeft = Offset(i * unitWidth, 0f),
                                        size = Size(unitWidth + 0.5f, size.height)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayCardNumber,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp,
                                    color = Tone.ink,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (maskCardNumber) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Mostrar u ocultar número de tarjeta",
                                        tint = Color(0xFF1E293B),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable {
                                                performCrispHaptic(view, HapticStyle.TICK)
                                                maskCardNumber = !maskCardNumber
                                            }
                                            .testTag("bravisima_toggle_mask_button")
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar número Bravísima",
                                        tint = Color(0xFF1E293B),
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clickable {
                                                performCrispHaptic(view, HapticStyle.TICK)
                                                clipboard.setText(AnnotatedString(cardNumber))
                                                onCopyCardNumber(cardNumber)
                                            }
                                            .testTag("bravisima_copy_number_button")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (bravisimaCard.isLocked) "BALANCE PROTEGIDO" else "BALANCE DISPONIBLE",
                                    fontSize = 12.sp,
                                    letterSpacing = 0.7.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White.copy(alpha = 0.94f)
                                )
                                Text(
                                    text = "RD$${formatMoney(balance)}.00",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp,
                                    color = Color.White
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        performCrispHaptic(view, HapticStyle.CONFIRM)
                                        onOpenScanner()
                                    },
                                    enabled = !bravisimaCard.isLocked,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.18f),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.White.copy(alpha = 0.10f),
                                        disabledContentColor = Color.White.copy(alpha = 0.55f)
                                    ),
                                    border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.48f)),
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bravisima_scan_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Escanear",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = {
                                        performCrispHaptic(view, HapticStyle.CONFIRM)
                                        onRedeemInCart()
                                    },
                                    enabled = !bravisimaCard.isLocked,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = BravoRedDark,
                                        disabledContainerColor = Color.White.copy(alpha = 0.55f),
                                        disabledContentColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1.25f)
                                        .testTag("bravisima_redeem_button")
                                ) {
                                    Text(
                                        text = if (bravisimaCard.isLocked) "Bloqueada" else "Canjear en carrito",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bravisima_auth_status", contentType = "auth_status_card") {
                TranslucentCard(
                    darkMode = darkMode,
                    cornerRadius = 22.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("bravisima_auth_card")
                ) {
                    if (isGuestMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Cuenta Google y Bravísima",
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Vincula tu cuenta de Google",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Inicia sesión con tu cuenta de Google para sincronizar y gestionar tu Tarjeta Bravísima en todos tus dispositivos.",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        lineHeight = 15.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = authErrorBanner != null,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                authErrorBanner?.let { err ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = err,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (isGoogleSigningIn) return@Button
                                    HapticFeedbackManager.onConfirmAction(view)
                                    authErrorBanner = null
                                    isGoogleSigningIn = true
                                    scope.launch {
                                        val result = launchGoogleSignInSheet(context)
                                        isGoogleSigningIn = false
                                        result.onFailure { e ->
                                            authErrorBanner = e.message
                                                ?: "No se pudo completar el inicio de sesión con Google."
                                        }
                                    }
                                },
                                enabled = !isGoogleSigningIn,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("bravisima_google_signin_button")
                            ) {
                                if (isGoogleSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Conectando con Google...",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp,
                                        color = Color.White
                                    )
                                } else {
                                    // Sin el icono de destellos: igual que el botón de Google del inicio de sesión
                                    Text(
                                        text = "Iniciar sesión con Google",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
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
                                        .background(Tone.success.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Sincronizado con Google",
                                        tint = Tone.success,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = bravisimaCard.holderName,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isSavingBravisima) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.8.dp,
                                                color = primaryColor
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${bravisimaCard.holderEmail ?: userProfile.email} • Cuenta Google Activa",
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
                                    onSignOutGoogle()
                                },
                                border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("bravisima_sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Cerrar sesión de Google",
                                    tint = primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Salir",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }
            }

            item(key = "bravisima_management", contentType = "card_management") {
                val tier = bravisimaCard.tier
                val nextTarget = tier.nextTierTargetRd
                val tierProgress = if (nextTarget != null && nextTarget > tier.minLifetimeRd) {
                    ((bravisimaCard.lifetimeEarned - tier.minLifetimeRd).toFloat() /
                        (nextTarget - tier.minLifetimeRd).toFloat()).coerceIn(0f, 1f)
                } else {
                    1f
                }
                val goalProgress = (bravisimaCard.balance.toFloat() / bravisimaCard.monthlyGoal.toFloat()).coerceIn(0f, 1f)

                TranslucentCard(
                    darkMode = darkMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("bravisima_management_card")
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
                                text = "Gestionar mi Bravísima",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            BravoUnderline(isArca = false)
                        }

                        // Nivel y meta del mes
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (darkMode) Color.White.copy(alpha = 0.08f)
                                    else Tone.surfaceSubtle
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nivel ${tier.label} • RD$${formatMoney(bravisimaCard.lifetimeEarned)}",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = primaryColor.copy(alpha = if (darkMode) 0.22f else 0.12f)
                                ) {
                                    Text(
                                        text = if (nextTarget != null) "Meta: RD$$nextTarget" else "Nivel máximo",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = if (darkMode) Color.White else BravoRedDark,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            val tipPulse by rememberInfiniteTransition(label = "tier_tip_pulse").animateFloat(
                                initialValue = 0.72f,
                                targetValue = 1.25f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "tierTipPulse"
                            )
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val trackWidth = maxWidth
                                LinearProgressIndicator(
                                    progress = { tierProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp)
                                        .clip(RoundedCornerShape(99.dp)),
                                    color = primaryColor,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                )
                                Box(
                                    modifier = Modifier
                                        .offset(x = ((trackWidth - 13.dp) * tierProgress.coerceIn(0f, 1f)))
                                        .size(13.dp)
                                        .graphicsLayer {
                                            scaleX = tipPulse
                                            scaleY = tipPulse
                                        }
                                        .clip(CircleShape)
                                        .background(BravoAmber)
                                        .border(1.5.dp, Color.White, CircleShape)
                                )
                            }
                            Text(
                                text = "${tier.benefitSummary} • Meta mensual: RD$${formatMoney(bravisimaCard.balance)} / RD$${formatMoney(bravisimaCard.monthlyGoal)} (${(goalProgress * 100).roundToInt()}%)",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f)
                            )
                        }

                        // Bloqueo de seguridad
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (bravisimaCard.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (bravisimaCard.isLocked) BravoRed else Tone.successBright,
                                    modifier = Modifier.size(21.dp)
                                )
                                Column {
                                    Text(
                                        text = if (bravisimaCard.isLocked) "Tarjeta bloqueada" else "Seguridad de tarjeta activa",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (bravisimaCard.isLocked) {
                                            "Canjes y códigos pausados temporalmente"
                                        } else {
                                            "Toca para bloquear temporalmente tu Bravísima"
                                        },
                                        fontFamily = PlusJakartaSans,
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                                    )
                                }
                            }
                            Switch(
                                checked = bravisimaCard.isLocked,
                                onCheckedChange = {
                                    performCrispHaptic(view, HapticStyle.TICK)
                                    showCardLockBiometricPrompt = true
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BravoRed
                                ),
                                modifier = Modifier.testTag("bravisima_lock_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        // Canje automático
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(21.dp)
                                )
                                Column {
                                    Text(
                                        text = "Canje automático en carrito",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Aplica tu balance Bravísima al pagar automáticamente",
                                        fontFamily = PlusJakartaSans,
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                                    )
                                }
                            }
                            Switch(
                                checked = bravisimaCard.autoRedeemInCart && !bravisimaCard.isLocked,
                                onCheckedChange = {
                                    performCrispHaptic(view, HapticStyle.TICK)
                                    onToggleAutoRedeem()
                                },
                                enabled = !bravisimaCard.isLocked,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = primaryColor
                                ),
                                modifier = Modifier.testTag("bravisima_autoredeem_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        // Editar alias y meta de ahorro
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    performCrispHaptic(view, HapticStyle.TICK)
                                    isEditingSettings = !isEditingSettings
                                }
                                .padding(vertical = 4.dp)
                                .testTag("bravisima_toggle_settings_editor"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Personalizar titular, meta y pasillo favorito",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = primaryColor
                                )
                            }
                            Icon(
                                imageVector = if (isEditingSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        }

                        AnimatedVisibility(visible = isEditingSettings) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                OutlinedTextField(
                                    value = editableHolderName,
                                    onValueChange = { editableHolderName = it },
                                    label = { Text("Nombre en la tarjeta Bravísima") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bravisima_holder_name_input")
                                )

                                Text(
                                    text = "Meta mensual de ahorro: RD$${formatMoney(editableGoal)}",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(300, 500, 1000, 2000).forEach { goalOption ->
                                        val selected = editableGoal == goalOption
                                        Surface(
                                            onClick = {
                                                performCrispHaptic(view, HapticStyle.TICK)
                                                editableGoal = goalOption
                                            },
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (selected) primaryColor else primaryColor.copy(alpha = 0.10f),
                                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "RD$$goalOption",
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = if (selected) Color.White else primaryColor,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Categoría preferida de bonos",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Todo Bravo", "Despensa", "Carnes y Frescos", "El Arca Mascotas").forEach { catOption ->
                                        val selected = editableCategory == catOption
                                        Surface(
                                            onClick = {
                                                performCrispHaptic(view, HapticStyle.TICK)
                                                editableCategory = catOption
                                            },
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (selected) primaryColor else primaryColor.copy(alpha = 0.10f),
                                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = catOption,
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = if (selected) Color.White else primaryColor,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        HapticFeedbackManager.onConfirmAction(view)
                                        onSaveCardSettings(editableHolderName, editableGoal, editableCategory)
                                        isEditingSettings = false
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bravisima_save_settings_button")
                                ) {
                                    Text(
                                        text = if (isGuestMode) "Guardar en mi dispositivo" else "Guardar Cambios en mi Cuenta",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bravisima_promo_redeem", contentType = "promo_code_card") {
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
                                text = "Activar código promocional",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            BravoUnderline(isArca = false)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = promoCodeInput,
                                onValueChange = { promoCodeInput = it },
                                placeholder = { Text("Ej. BRAVO25") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bravisima_promo_input")
                            )
                            Button(
                                onClick = {
                                    HapticFeedbackManager.onConfirmAction(view)
                                    onRedeemPromoCode(promoCodeInput)
                                    promoCodeInput = ""
                                },
                                enabled = !bravisimaCard.isLocked && promoCodeInput.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("bravisima_apply_promo_button")
                            ) {
                                Text(
                                    text = "Aplicar",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("BRAVO25" to "+RD$25", "BRAVISIMA50" to "+RD$50", "ARCA20" to "+RD$20").forEach { (code, reward) ->
                                val alreadyClaimed = bravisimaCard.claimedPromoCodes.any { it.equals(code, ignoreCase = true) }
                                Surface(
                                    onClick = {
                                        if (!alreadyClaimed) {
                                            performCrispHaptic(view, HapticStyle.TICK)
                                            promoCodeInput = code
                                        }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (alreadyClaimed) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    } else {
                                        primaryColor.copy(alpha = 0.10f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (alreadyClaimed) Color.Transparent else primaryColor.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Text(
                                        text = if (alreadyClaimed) "$code ✓ Canjeado" else "$code ($reward)",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = if (alreadyClaimed) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                                        } else {
                                            if (darkMode) Color.White else BravoRedDark
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bravisima_progress", contentType = "progress_card") {
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
                                text = "Progreso en tu carrito actual",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            BravoUnderline(isArca = false)
                            Spacer(modifier = Modifier.height(6.dp))
                            DiagonalMetadataStrip(
                                items = listOf("+RD$${cartTotals.bravisimaGenerated}", "Hoy"),
                                slashColor = if (darkMode) Color(0xFF34D399) else Tone.success,
                                fontSizeSp = 12.5f
                            )
                        }

                        Text(
                            text = "Recibes RD$1.00 de retorno automático por cada RD$100.00 en compras.",
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        LinearProgressIndicator(
                            progress = { cartTotals.progressPct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(99.dp)),
                            color = primaryColor,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                        )

                        DiagonalMetadataStrip(
                            items = listOf("Próximo peso", "Faltan RD$$neededForNextPeso"),
                            slashColor = primaryColor,
                            fontSizeSp = 12f
                        )
                    }
                }
            }

            item(key = "bravisima_movements", contentType = "movements_card") {
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
                                text = "Movimientos recientes (${movements.size})",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            BravoUnderline(isArca = false)
                        }

                        if (movements.isEmpty()) {
                            Text(
                                text = "Aún no hay movimientos registrados en tu Tarjeta Bravísima.",
                                fontFamily = PlusJakartaSans,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            movements.forEachIndexed { idx, mov ->
                                val isPositive = mov.amount >= 0
                                val formattedAmount = if (isPositive) {
                                    "+RD$${formatMoney(mov.amount)}.00"
                                } else {
                                    "-RD$${formatMoney(abs(mov.amount))}.00"
                                }
                                MovementRow(
                                    title = mov.title,
                                    meta = listOf(mov.dateLabel, mov.subtitle),
                                    amountText = formattedAmount,
                                    isPositive = isPositive,
                                    darkMode = darkMode
                                )
                                if (idx < movements.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }
        }

        GlassTopBar(
            darkMode = darkMode,
            accent = primaryColor,
            title = "Bravísima",
            modifier = Modifier.align(Alignment.TopCenter),
            onBack = onBack,
            actions = listOf(
                GlassBarAction(symbol = "creditcard", icon = null, label = "Nivel ${bravisimaCard.tier.label}", text = bravisimaCard.tier.label, highlighted = true)
            )
        )

        if (showCardLockBiometricPrompt) {
            val actionTitle = if (bravisimaCard.isLocked) {
                "Desbloquear tarjeta Bravísima"
            } else {
                "Bloquear tarjeta Bravísima"
            }
            val actionDesc = if (bravisimaCard.isLocked) {
                "Confirma tu huella digital para reactivar canjes y uso de tu Tarjeta Bravísima."
            } else {
                "Confirma tu huella digital para proteger y pausar temporalmente tu Tarjeta Bravísima."
            }
            BiometricOrderPromptDialog(
                request = BiometricPromptRequest(
                    operation = BiometricCriticalOperation.UNLOCK_BRAVISIMA_CARD,
                    title = actionTitle,
                    subtitle = "Balance protegido: RD$${formatMoney(balance)}.00",
                    description = actionDesc,
                    badgeTitle = "SEGURIDAD TARJETA BRAVÍSIMA",
                    amountRd = balance,
                    itemCount = 1,
                    cashbackRd = 0,
                    paymentMethodLabel = displayCardNumber,
                    deliveryOrBranchLabel = "Nivel ${bravisimaCard.tier.label}"
                ),
                isArca = false,
                darkMode = darkMode,
                onDismiss = { showCardLockBiometricPrompt = false },
                onAuthenticated = {
                    showCardLockBiometricPrompt = false
                    onToggleCardLock()
                }
            )
        }
    }
}

@Composable
private fun MovementRow(
    title: String,
    meta: List<String>,
    amountText: String,
    isPositive: Boolean,
    darkMode: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            DiagonalMetadataStrip(
                items = meta,
                slashColor = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                fontSizeSp = 12f
            )
        }
        Text(
            text = amountText,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.5.sp,
            color = if (isPositive) {
                if (darkMode) Color(0xFF34D399) else Tone.successStrong
            } else {
                if (darkMode) Color(0xFFFCA5A5) else BravoRedDark
            }
        )
    }
}

@Composable
fun CartCheckoutScreen(
    cartItems: List<Pair<Product, Int>>,
    cartTotals: CartTotals,
    bravisimaBalance: Int,
    redeemBravisima: Boolean,
    isBravisimaLocked: Boolean = false,
    storeMode: StoreMode,
    darkMode: Boolean,
    activeBranch: String = "Bravo Churchill",
    paymentMethods: List<PaymentMethod> = emptyList(),
    addresses: List<UserAddress> = emptyList(),
    onBack: () -> Unit,
    onEmptyCart: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onToggleRedeem: (Boolean) -> Unit,
    onConfirmCheckout: () -> Unit,
    onExploreCatalog: () -> Unit,
    onAskBravIa: () -> Unit,
    onSelectPaymentMethod: (String) -> Unit = {},
    onAddPaymentCard: () -> Unit = {},
    onToggleCardLock: () -> Unit = {},
    onBiometricVerified: (BiometricVerificationResult) -> Unit = {},
    checkoutExtras: CheckoutExtras = CheckoutExtras(),
    onCheckoutExtrasChange: (CheckoutExtras) -> Unit = {}
) {
    val view = currentPlatformView()
    val isArca = storeMode == StoreMode.ARCA
    val primaryColor = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary

    val deliveryMode = "delivery" // la recogida en tienda se quitó: siempre es entrega a domicilio
    val ncfType = checkoutExtras.ncfType // "B02" Consumidor Final, "B01" Crédito Fiscal
    var lastVerificationResult by remember { mutableStateOf<BiometricVerificationResult?>(null) }
    var pendingBiometricRequest by remember { mutableStateOf<BiometricPromptRequest?>(null) }
    var pendingBiometricAction by remember { mutableStateOf<((BiometricVerificationResult) -> Unit)?>(null) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    val payScope = rememberCoroutineScope()

    val activeCard = remember(paymentMethods) {
        paymentMethods.firstOrNull { it.isDefault } ?: paymentMethods.firstOrNull()
    }
    val activeAddress = remember(addresses) {
        addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
    }
    val activeCardSummary = activeCard?.let { "${it.brand} •••• ${it.last4}" } ?: "Visa •••• 4821"
    val activeDeliverySummary = if (deliveryMode == "pickup") {
        "Pickup Express • $activeBranch"
    } else {
        activeAddress?.let { "${it.label}: ${it.street}" } ?: "Delivery Express • $activeBranch"
    }

    fun requestBiometricAuthorization(
        request: BiometricPromptRequest,
        onAuthorized: (BiometricVerificationResult) -> Unit
    ) {
        pendingBiometricRequest = request
        pendingBiometricAction = onAuthorized
    }

    val hazeState = LocalTranslucentHazeState.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cart_checkout_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .translucentBackdropSource(hazeState, zIndex = 1f),
            contentPadding = PaddingValues(top = 74.dp, bottom = 172.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (cartItems.isEmpty()) {
                item(key = "cart_empty_state", contentType = "empty_state") {
                    TranslucentCard(
                    darkMode = darkMode,
                    isArca = isArca,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        com.example.ui.components.EmptyStateBadge(
                            icon = Icons.Default.ShoppingCart,
                            accent = primaryColor
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(Res.string.cart_empty_title),
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(Res.string.cart_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        AnimatedPurchaseButton(
                            text = stringResource(Res.string.cart_empty_cta),
                            primaryColor = primaryColor,
                            onClick = onExploreCatalog,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            items(
                items = cartItems,
                key = { it.first.sku },
                contentType = { "cart_item_row" }
            ) { (product, qty) ->
                TranslucentCard(
                    darkMode = darkMode,
                    isArca = isArca,
                    cornerRadius = 18.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    onClick = { onOpenProduct(product.sku) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ProductArtworkView(product = product)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            DiagonalMetadataStrip(
                                items = listOf(product.category, "RD$${product.price} / ${product.unit}"),
                                slashColor = primaryColor,
                                fontSizeSp = 12f
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "RD$${formatMoney(product.price * qty)}.00",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = primaryColor
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (qty == 1) Tone.dangerSoft else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable {
                                        HapticFeedbackManager.onDecrementOrRemove(view, isFullRemove = qty == 1)
                                        onUpdateQty(product.sku, -1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (qty == 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
                                    contentDescription = "Restar",
                                    tint = if (qty == 1) Tone.dangerStrong else primaryColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Text(
                                text = qty.toString(),
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor)
                                    .clickable {
                                        HapticFeedbackManager.onAddToCart(view)
                                        onUpdateQty(product.sku, 1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Sumar",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "cart_checkout_payment_and_delivery", contentType = "payment_delivery") {
                TranslucentCard(
                    darkMode = darkMode,
                    isArca = isArca,
                    cornerRadius = 22.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("checkout_payment_delivery_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Pago y entrega",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                DiagonalMetadataStrip(
                                    items = listOf("Pago protegido con huella", activeBranch),
                                    slashColor = primaryColor,
                                    fontSizeSp = 12f
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Tone.successBright.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Tone.successBright.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = Tone.successBright,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "3DS + Huella",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = Tone.successBright,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // Solo entrega a domicilio: Bravo en línea no ofrece recogida en esta app.
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = primaryColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().testTag("checkout_mode_delivery")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "Entrega a domicilio",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = activeAddress?.label?.let { "$it • desde $activeBranch" } ?: "Desde $activeBranch",
                                        fontFamily = PlusJakartaSans,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Método de pago (cambiarlo pide huella)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Método de pago",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Surface(
                                    onClick = {
                                        performCrispHaptic(view, HapticStyle.TICK)
                                        requestBiometricAuthorization(
                                            request = BiometricPromptRequest(
                                                operation = BiometricCriticalOperation.CHANGE_PAYMENT_METHOD,
                                                title = "Vincular nueva tarjeta",
                                                subtitle = "Verificación biométrica requerida",
                                                description = "Autoriza con tu huella digital para registrar un nuevo método de pago en el checkout.",
                                                badgeTitle = "SEGURIDAD DE MÉTODO DE PAGO",
                                                amountRd = cartTotals.finalPay.roundToInt(),
                                                itemCount = cartTotals.count,
                                                cashbackRd = cartTotals.bravisimaGenerated,
                                                paymentMethodLabel = "Nueva Tarjeta Crédito / Débito",
                                                deliveryOrBranchLabel = activeDeliverySummary
                                            ),
                                            onAuthorized = { res ->
                                                lastVerificationResult = res
                                                onBiometricVerified(res)
                                                onAddPaymentCard()
                                            }
                                        )
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = primaryColor.copy(alpha = 0.12f),
                                    modifier = Modifier.testTag("checkout_add_card_biometric_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Nueva tarjeta",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = primaryColor
                                        )
                                    }
                                }
                            }

                            val displayCards = if (paymentMethods.isNotEmpty()) {
                                paymentMethods
                            } else {
                                listOf(
                                    PaymentMethod("pm1", "Visa", "Popular", "4821", "09/28", true),
                                    PaymentMethod("pm2", "Mastercard", "BHD", "9034", "11/27", false)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                displayCards.forEach { method ->
                                    val isSelected = method.id == activeCard?.id || (activeCard == null && method.isDefault)
                                    Surface(
                                        onClick = {
                                            if (!isSelected) {
                                                performCrispHaptic(view, HapticStyle.TICK)
                                                requestBiometricAuthorization(
                                                    request = BiometricPromptRequest(
                                                        operation = BiometricCriticalOperation.CHANGE_PAYMENT_METHOD,
                                                        title = "Cambiar Método de Pago",
                                                        subtitle = "${method.brand} •••• ${method.last4} • Exp ${method.exp}",
                                                        description = "Confirma tu huella digital para autorizar el cambio de tarjeta predeterminada en este pedido.",
                                                        badgeTitle = "CAMBIO DE TARJETA EN CHECKOUT",
                                                        amountRd = cartTotals.finalPay.roundToInt(),
                                                        itemCount = cartTotals.count,
                                                        cashbackRd = cartTotals.bravisimaGenerated,
                                                        paymentMethodLabel = "${method.brand} •••• ${method.last4}",
                                                        deliveryOrBranchLabel = activeDeliverySummary
                                                    ),
                                                    onAuthorized = { res ->
                                                        lastVerificationResult = res
                                                        onBiometricVerified(res)
                                                        onSelectPaymentMethod(method.id)
                                                    }
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) primaryColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.4.dp else 1.dp,
                                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier.testTag("checkout_payment_card_${method.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CreditCard,
                                                contentDescription = null,
                                                tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "${method.brand} •••• ${method.last4}",
                                                    fontFamily = PlusJakartaSans,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (isSelected) "Activa • Exp ${method.exp}" else "Toca para autorizar",
                                                    fontFamily = PlusJakartaSans,
                                                    fontSize = 12.sp,
                                                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Comprobante fiscal (NCF): etiqueta arriba y opciones debajo, para que no se aprieten
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Comprobante fiscal (DGII)",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("B02" to "Consumidor final", "B01" to "Crédito fiscal").forEach { (code, label) ->
                                    val selected = ncfType == code
                                    Surface(
                                        onClick = {
                                            performCrispHaptic(view, HapticStyle.TICK)
                                            onCheckoutExtrasChange(checkoutExtras.copy(ncfType = code))
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (selected) primaryColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier.testTag("checkout_ncf_$code")
                                    ) {
                                        Text(
                                            text = "$code • $label",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = if (selected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "cart_rd_options", contentType = "rd_options") {
                RdCheckoutOptionsCard(
                    extras = checkoutExtras,
                    onChange = onCheckoutExtrasChange,
                    baseTotalRd = cartTotals.finalPay,
                    isDelivery = deliveryMode == "delivery",
                    darkMode = darkMode,
                    accent = primaryColor,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                )
            }

            item(key = "cart_substitutions", contentType = "substitutions") {
                TranslucentCard(
                    darkMode = darkMode,
                    isArca = isArca,
                    cornerRadius = 18.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("cart_substitutions_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Si algo se agota",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Elige qué hacemos mientras preparamos tu pedido.",
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        com.example.model.SubstitutionPref.entries.forEach { pref ->
                            val selected = checkoutExtras.substitution == pref
                            Surface(
                                onClick = {
                                    performCrispHaptic(view, HapticStyle.TICK)
                                    onCheckoutExtrasChange(checkoutExtras.copy(substitution = pref))
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) primaryColor.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    if (selected) 1.4.dp else 1.dp,
                                    if (selected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .semantics { this.selected = selected }
                                    .testTag("substitution_${pref.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (selected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pref.label,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = pref.detail,
                                            fontFamily = PlusJakartaSans,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "cart_summary_box", contentType = "summary") {
                TranslucentCard(
                    darkMode = darkMode,
                    isArca = isArca,
                    cornerRadius = 22.dp,
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
                                text = "Resumen de compra",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            BravoUnderline(isArca = isArca)
                        }

                        val canRedeemCard = bravisimaBalance > 0 && !isBravisimaLocked
                        val toggleBravisimaWithBiometrics: (Boolean) -> Unit = { targetRedeem ->
                            performCrispHaptic(view, HapticStyle.TICK)
                            if (targetRedeem && !redeemBravisima) {
                                val maxRedeemable = minOf(bravisimaBalance, cartTotals.subtotal.roundToInt())
                                requestBiometricAuthorization(
                                    request = BiometricPromptRequest(
                                        operation = BiometricCriticalOperation.AUTHORIZE_BRAVISIMA_REDEMPTION,
                                        title = "Autorizar canje Bravísima",
                                        subtitle = "Aplicar -RD$${formatMoney(maxRedeemable)}.00 al pedido",
                                        description = "Confirma tu identidad con huella digital para redimir tus puntos acumulados de Tarjeta Bravísima en este checkout.",
                                        badgeTitle = "CANJE DE SALDO BRAVÍSIMA",
                                        amountRd = maxRedeemable,
                                        itemCount = cartTotals.count,
                                        cashbackRd = cartTotals.bravisimaGenerated,
                                        paymentMethodLabel = "Balance Bravísima (RD$${formatMoney(bravisimaBalance)}.00)",
                                        deliveryOrBranchLabel = activeDeliverySummary
                                    ),
                                    onAuthorized = { res ->
                                        lastVerificationResult = res
                                        onBiometricVerified(res)
                                        onToggleRedeem(true)
                                    }
                                )
                            } else {
                                onToggleRedeem(targetRedeem)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable(enabled = canRedeemCard || isBravisimaLocked) {
                                    if (isBravisimaLocked) {
                                        performCrispHaptic(view, HapticStyle.TICK)
                                        requestBiometricAuthorization(
                                            request = BiometricPromptRequest(
                                                operation = BiometricCriticalOperation.UNLOCK_BRAVISIMA_CARD,
                                                title = "Desbloquear tarjeta Bravísima",
                                                subtitle = "Balance disponible: RD$${formatMoney(bravisimaBalance)}.00",
                                                description = "Tu Tarjeta Bravísima está bloqueada. Verifica tu huella digital para desbloquearla y habilitar el canje en el checkout.",
                                                badgeTitle = "DESBLOQUEO BIOMÉTRICO BRAVÍSIMA",
                                                amountRd = bravisimaBalance,
                                                itemCount = cartTotals.count,
                                                cashbackRd = cartTotals.bravisimaGenerated,
                                                paymentMethodLabel = "Tarjeta Bravísima digital",
                                                deliveryOrBranchLabel = activeDeliverySummary
                                            ),
                                            onAuthorized = { res ->
                                                lastVerificationResult = res
                                                onBiometricVerified(res)
                                                onToggleCardLock()
                                            }
                                        )
                                    } else if (canRedeemCard) {
                                        toggleBravisimaWithBiometrics(!redeemBravisima)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBravisimaLocked) {
                                        "Tarjeta Bravísima bloqueada (Toca para desbloquear)"
                                    } else {
                                        "Canjear balance Tarjeta Bravísima"
                                    },
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isBravisimaLocked) BravoRed else MaterialTheme.colorScheme.onSurface
                                )
                                DiagonalMetadataStrip(
                                    items = if (isBravisimaLocked) {
                                        listOf("Desbloqueo con huella", "RD$${formatMoney(bravisimaBalance)}.00")
                                    } else {
                                        listOf("Disponible", "RD$${formatMoney(bravisimaBalance)}.00", "Protegido con huella")
                                    },
                                    slashColor = if (isBravisimaLocked) BravoRed else primaryColor,
                                    fontSizeSp = 12f
                                )
                            }
                            if (isBravisimaLocked) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = BravoRed.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, BravoRed.copy(alpha = 0.4f)),
                                    modifier = Modifier.testTag("cart_unlock_bravisima_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = "Desbloquear con huella",
                                            tint = BravoRed,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Desbloquear",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = BravoRed
                                        )
                                    }
                                }
                            } else {
                                Checkbox(
                                    checked = redeemBravisima && canRedeemCard,
                                    onCheckedChange = { toggleBravisimaWithBiometrics(it) },
                                    enabled = canRedeemCard,
                                    colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                                    modifier = Modifier.testTag("cart_redeem_checkbox")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal (${cartTotals.count} art.)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "RD$${formatMoney(cartTotals.subtotal.roundToInt())}.00",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (cartTotals.redeemed > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Descuento Bravísima (Autorizado)", color = Tone.success)
                                Text(
                                    "-RD$${formatMoney(cartTotals.redeemed)}.00",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Tone.success
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cargo con $activeCardSummary ($ncfType)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            DiagonalMetadataStrip(
                                items = listOf("GRATIS", "+RD$${cartTotals.bravisimaGenerated} Bravísima"),
                                slashColor = Tone.success,
                                fontSizeSp = 12f
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (checkoutExtras.tipRd > 0) "Total con propina" else "Total a pagar",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "RD$${formatMoney(cartTotals.finalPay.roundToInt() + checkoutExtras.tipRd)}.00",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 23.sp,
                                color = primaryColor
                            )
                        }

                        // Lo que te ahorras hoy con Bravísima y lo que ganas para la próxima compra
                        if (cartTotals.redeemed > 0 || cartTotals.bravisimaGenerated > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Tone.successSoft,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cart_savings_banner")
                            ) {
                                Text(
                                    text = buildString {
                                        if (cartTotals.redeemed > 0) append("Ahorras RD$${formatMoney(cartTotals.redeemed)} hoy con Bravísima")
                                        if (cartTotals.redeemed > 0 && cartTotals.bravisimaGenerated > 0) append(" · ")
                                        if (cartTotals.bravisimaGenerated > 0) append("Ganas RD$${formatMoney(cartTotals.bravisimaGenerated)} para tu próxima compra")
                                    },
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Tone.successStrong,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Aviso del estado de la verificación biométrica
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (lastVerificationResult != null) {
                                        Tone.successBright.copy(alpha = 0.10f)
                                    } else {
                                        primaryColor.copy(alpha = 0.08f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (lastVerificationResult != null) {
                                        Tone.successBright.copy(alpha = 0.38f)
                                    } else {
                                        primaryColor.copy(alpha = 0.28f)
                                    },
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                                .testTag("checkout_biometric_security_banner"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (lastVerificationResult != null) {
                                            Tone.successBright.copy(alpha = 0.18f)
                                        } else {
                                            primaryColor.copy(alpha = 0.16f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Solicitud de huella digital",
                                    tint = if (lastVerificationResult != null) Tone.successBright else primaryColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lastVerificationResult?.let {
                                        "Sello Biométrico Activo • ${it.transactionToken}"
                                    } ?: "Confirmación con BiometricPrompt",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = lastVerificationResult?.let {
                                        if (it.hardwareBacked) {
                                            "Firma criptográfica Keystore AES-256 verificada para este checkout."
                                        } else {
                                            "Identidad biométrica verificada para operaciones críticas del checkout."
                                        }
                                    } ?: "Pagos con $activeCardSummary, canjes Bravísima y envío requieren huella.",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Tone.successBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        val payTotalRd = cartTotals.finalPay.roundToInt() + checkoutExtras.tipRd
                        val needsBiometric = checkoutExtras.payment.needsBiometric
                        fun submitOrder() {
                            if (isProcessingPayment) return
                            isProcessingPayment = true
                            payScope.launch {
                                // Breve paso de "procesando" para que el cobro no se sienta instantáneo ni se repita
                                delay(1100L)
                                HapticFeedbackManager.onCheckoutConfirm(view)
                                onConfirmCheckout()
                                isProcessingPayment = false
                            }
                        }
                        AnimatedPurchaseButton(
                            loading = isProcessingPayment,
                            loadingText = "Procesando pago…",
                            text = if (needsBiometric) "Pagar con huella • RD$${formatMoney(payTotalRd)}"
                            else "Confirmar pedido • RD$${formatMoney(payTotalRd)}",
                            icon = if (needsBiometric) Icons.Default.Fingerprint else Icons.Default.VerifiedUser,
                            primaryColor = primaryColor,
                            onClick = {
                                performCrispHaptic(view, HapticStyle.CONFIRM)
                                if (!needsBiometric) {
                                    submitOrder()
                                    return@AnimatedPurchaseButton
                                }
                                val finalPayRd = cartTotals.finalPay.roundToInt()
                                requestBiometricAuthorization(
                                    request = BiometricPromptRequest(
                                        operation = BiometricCriticalOperation.CONFIRM_PAYMENT_AND_ORDER,
                                        title = "Confirmar Pago y Pedido",
                                        subtitle = "Total RD$${formatMoney(finalPayRd)}.00 • $activeCardSummary",
                                        description = "Coloca tu huella digital para autorizar el cargo en $activeCardSummary ($ncfType) y despachar tu pedido ($activeDeliverySummary).",
                                        badgeTitle = "AUTORIZACIÓN BIOMÉTRICA DE PAGO",
                                        amountRd = finalPayRd,
                                        itemCount = cartTotals.count,
                                        cashbackRd = cartTotals.bravisimaGenerated,
                                        paymentMethodLabel = "$activeCardSummary • NCF $ncfType",
                                        deliveryOrBranchLabel = activeDeliverySummary
                                    ),
                                    onAuthorized = { res ->
                                        lastVerificationResult = res
                                        onBiometricVerified(res)
                                        submitOrder()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "confirm_checkout_button"
                        )
                    }
                }
            }

            item(key = "cart_bravia_option", contentType = "assistant_option") {
                TranslucentCard(
                    darkMode = darkMode,
                    isArca = isArca,
                    cornerRadius = 18.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    onClick = {
                        HapticFeedbackManager.onAssistantTrigger(view)
                        onAskBravIa()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                DiagonalMetadataStrip(
                                    items = listOf("OPCIÓN DE AHORRO", "Brav IA"),
                                    slashColor = primaryColor,
                                    fontSizeSp = 12f
                                )
                                Text(
                                    text = "¿Deseas ajustar este carrito a un presupuesto menor?",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = primaryColor
                        )
                    }
                }
            }
        }
    }

        GlassTopBar(
            darkMode = darkMode,
            accent = primaryColor,
            title = "Mi carrito (${cartTotals.count})",
            modifier = Modifier.align(Alignment.TopCenter),
            onBack = {
                        performCrispHaptic(view, HapticStyle.TICK)
                        onBack()
                    },
            backTestTag = "cart_back_button",
            actions = if (cartItems.isNotEmpty()) listOf(
                GlassBarAction(
                    symbol = "trash", icon = Icons.Default.DeleteOutline, label = "Vaciar carrito", text = "Vaciar",
                    tint = if (darkMode) Color(0xFFFDA4AF) else Tone.dangerStrong,
                    testTag = "cart_empty_button",
                    onClick = {
                            performCrispHaptic(view, HapticStyle.HEAVY)
                            onEmptyCart()
                        }
                )
            ) else emptyList()
        )

        val activePromptRequest = pendingBiometricRequest
        if (activePromptRequest != null) {
            BiometricOrderPromptDialog(
                request = activePromptRequest,
                isArca = isArca,
                darkMode = darkMode,
                onDismiss = {
                    pendingBiometricRequest = null
                    pendingBiometricAction = null
                },
                onAuthenticatedWithResult = { verificationResult ->
                    val action = pendingBiometricAction
                    pendingBiometricRequest = null
                    pendingBiometricAction = null
                    action?.invoke(verificationResult)
                }
            )
        }
    }
}

@Composable
fun BiometricOrderPromptDialog(
    totalAmountRd: Int,
    itemCount: Int,
    cashbackRd: Int,
    isArca: Boolean,
    darkMode: Boolean,
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit
) {
    BiometricOrderPromptDialog(
        request = BiometricPromptRequest(
            operation = BiometricCriticalOperation.CONFIRM_PAYMENT_AND_ORDER,
            title = "Confirmar Pedido con Huella",
            subtitle = "Total RD$${formatMoney(totalAmountRd)}.00 • $itemCount artículo(s)",
            description = "Coloca tu huella digital para autorizar el pago y confirmar tu pedido.",
            badgeTitle = "AUTORIZACIÓN BIOMÉTRICA BRAVO",
            amountRd = totalAmountRd,
            itemCount = itemCount,
            cashbackRd = cashbackRd,
            paymentMethodLabel = "Pago Protegido 3DS + BiometricPrompt",
            deliveryOrBranchLabel = "Despacho inmediato"
        ),
        isArca = isArca,
        darkMode = darkMode,
        onDismiss = onDismiss,
        onAuthenticated = onAuthenticated
    )
}

@Composable
fun BiometricOrderPromptDialog(
    request: BiometricPromptRequest,
    isArca: Boolean,
    darkMode: Boolean,
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit = {},
    onAuthenticatedWithResult: (BiometricVerificationResult) -> Unit = {}
) {
    val context = currentPlatformContext()
    val view = currentPlatformView()
    val scope = rememberCoroutineScope()
    val primaryColor = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary

    var scanState by remember(request) { mutableStateOf("idle") } // "idle", "scanning", "verified", "error"
    // Face ID / Touch ID / huella según el equipo (LAContext.biometryType en iOS)
    val biometry = remember { com.example.platform.deviceBiometryKind() }
    val usesFace = biometry == com.example.platform.BiometryKind.FACE_ID || biometry == com.example.platform.BiometryKind.OPTIC_ID
    val biometryIcon = if (usesFace) Icons.Default.Face else Icons.Default.Fingerprint
    // Los datos del cargo van en filas aparte; aquí solo la instrucción, corta y sin cortes
    val baseInstruction = "${biometry.instruction} para autorizar el pago y despachar tu pedido."
    var statusMessage by remember(request) {
        mutableStateOf(baseInstruction)
    }
    var verifiedTokenPreview by remember(request) { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "biometric_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )
    val laserOffsetFraction by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_offset"
    )

    fun completeVerification(hardwareResult: BiometricVerificationResult? = null) {
        if (scanState == "scanning" || scanState == "verified") return
        scanState = "scanning"
        statusMessage = "Validando firma biométrica y criptograma de seguridad..."
        performCrispHaptic(view, HapticStyle.TICK)
        scope.launch {
            kotlinx.coroutines.delay(420L)
            val finalResult = hardwareResult ?: BiometricVerificationResult(
                operation = request.operation,
                transactionToken = BiometricCheckoutAuthenticator.generateVerificationToken(
                    operation = request.operation,
                    amountRd = request.amountRd,
                    cipherHash = 0
                ),
                timestampMillis = System.currentTimeMillis(),
                hardwareBacked = false
            )
            verifiedTokenPreview = finalResult.transactionToken
            scanState = "verified"
            statusMessage = "¡Identidad verificada! Token ${finalResult.transactionToken}"
            HapticFeedbackManager.onCheckoutConfirm(view)
            kotlinx.coroutines.delay(320L)
            onAuthenticatedWithResult(finalResult)
            onAuthenticated()
        }
    }

    DisposableEffect(request) {
        val signal = BiometricCheckoutAuthenticator.launchSystemBiometricPrompt(
            context = context,
            request = request,
            onHardwareSucceeded = { result ->
                completeVerification(result)
            },
            onHardwareFailed = {
                scanState = "error"
                statusMessage = "No se reconoció ${biometry.displayName}. Intenta de nuevo."
            },
            onFallbackRequested = {
                // Sensor en pantalla (si el usuario lo eligió o el equipo no tiene huella)
            }
        )
        onDispose {
            try {
                signal?.cancel()
            } catch (_: Throwable) {
            }
        }
    }

    val sensorAccent = when (scanState) {
        "verified" -> Tone.successBright
        "scanning" -> Tone.warningBright
        "error" -> Color(0xFFEF4444)
        else -> primaryColor
    }

    com.example.ui.components.SuppressNativeGlass() // el vidrio nativo se dibujaría encima del diálogo
    Dialog(
        onDismissRequest = {
            if (scanState != "scanning" && scanState != "verified") {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("biometric_order_dialog"),
            contentAlignment = Alignment.Center
        ) {
            TranslucentCard(
                darkMode = darkMode,
                isArca = isArca,
                cornerRadius = 28.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = sensorAccent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, sensorAccent.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = sensorAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = request.badgeTitle,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = sensorAccent
                            )
                        }
                    }

                    Text(
                        text = request.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = statusMessage,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (scanState == "verified") Tone.successBright else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Resumen en filas de dos columnas: etiqueta a la izquierda, dato a la derecha
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RD$${formatMoney(request.amountRd)}.00",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (request.cashbackRd > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Tone.successBright.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "+RD$${request.cashbackRd} Bravísima",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            color = Tone.successBright,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            val summaryRows = listOfNotNull(
                                request.paymentMethodLabel.takeIf { it.isNotBlank() }?.let { "Pago" to it },
                                request.deliveryOrBranchLabel.takeIf { it.isNotBlank() }?.let { "Entrega" to it },
                                if (request.itemCount > 0) "Artículos" to "${request.itemCount}" else null
                            )
                            summaryRows.forEach { (label, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.width(72.dp)
                                    )
                                    Text(
                                        text = value,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.End,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Zona del sensor de huella
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .clickable(enabled = scanState != "scanning" && scanState != "verified") {
                                completeVerification()
                            }
                            .testTag("biometric_sensor_touch_area"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val baseRadius = size.minDimension * 0.38f
                            drawCircle(
                                color = sensorAccent.copy(alpha = 0.12f),
                                radius = baseRadius * ringScale
                            )
                            drawCircle(
                                color = sensorAccent.copy(alpha = 0.28f),
                                radius = baseRadius * ((ringScale + 1f) / 2f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            sensorAccent.copy(alpha = 0.26f),
                                            if (darkMode) Color(0xFF131C2E) else Color(0xFFF8FAFC)
                                        )
                                    )
                                )
                                .border(2.dp, sensorAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (scanState == "verified") Icons.Default.CheckCircle else biometryIcon,
                                contentDescription = biometry.displayName,
                                tint = sensorAccent,
                                modifier = Modifier.size(48.dp)
                            )

                            if (scanState == "idle" || scanState == "scanning") {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val yPos = size.height * laserOffsetFraction
                                    drawLine(
                                        color = sensorAccent.copy(alpha = if (scanState == "scanning") 0.95f else 0.55f),
                                        start = Offset(size.width * 0.16f, yPos),
                                        end = Offset(size.width * 0.84f, yPos),
                                        strokeWidth = 2.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = verifiedTokenPreview?.let { "Criptograma verificado: $it" }
                            ?: if (scanState == "verified") "Identidad confirmada" else biometry.sensorHint,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = sensorAccent
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = scanState != "scanning" && scanState != "verified",
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("biometric_cancel_button")
                        ) {
                            Text(
                                text = "Cancelar",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { completeVerification() },
                            enabled = scanState != "scanning" && scanState != "verified",
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("biometric_verify_now_button")
                        ) {
                            Icon(
                                imageVector = biometryIcon,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Una sola línea siempre: "Usar Face ID" / "Usar Touch ID" / "Usar huella"
                            Text(
                                text = "Usar ${biometry.displayName}",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class RoutePoint(
    val xFraction: Float,
    val yFraction: Float,
    val etaMin: Int,
    val distanceKm: String,
    val speedKmh: Int,
    val streetLabel: String
)

private val SANTO_DOMINGO_ROUTE = listOf(
    RoutePoint(0.18f, 0.23f, 9, "1.8 km", 32, "Saliendo de Bravo Av. Winston Churchill"),
    RoutePoint(0.26f, 0.30f, 8, "1.6 km", 36, "Avanzando por Av. Winston Churchill"),
    RoutePoint(0.35f, 0.37f, 7, "1.4 km", 38, "Cruzando C. Roberto Pastoriza"),
    RoutePoint(0.45f, 0.44f, 6, "1.1 km", 34, "Incorporándose a Av. 27 de Febrero"),
    RoutePoint(0.55f, 0.51f, 5, "0.9 km", 40, "Tomando desvío hacia Piantini"),
    RoutePoint(0.64f, 0.58f, 4, "0.7 km", 35, "Cruzando Av. Abraham Lincoln"),
    RoutePoint(0.73f, 0.65f, 3, "0.4 km", 29, "Entrando a tu cuadra residencial"),
    RoutePoint(0.81f, 0.72f, 2, "0.2 km", 22, "A 200 metros de tu edificio"),
    RoutePoint(0.85f, 0.76f, 1, "0.1 km", 15, "¡Llegando a Tu Dirección en Piantini!")
)

@Composable
fun DeliveryTrackingScreen(
    branchName: String,
    cartItems: List<Pair<Product, Int>>,
    darkMode: Boolean,
    trackedOrder: OrderRecord? = null,
    allOrders: List<OrderRecord> = emptyList(),
    firestoreSyncStatus: String = "Sincronizado con tu cuenta de Google",
    isGuestMode: Boolean = false,
    isUpdatingOrderStatus: Boolean = false,
    realLocationMetrics: RealLocationDeliveryMetrics? = null,
    driverTelemetry: DriverLiveTelemetry = DriverLiveTelemetry(),
    driverChatMessages: List<DriverChatMessage> = emptyList(),
    isDriverTyping: Boolean = false,
    liveDeliveryBanner: LiveDeliveryBannerNotification? = null,
    onRefreshRealLocation: (Boolean) -> Unit = {},
    onTriggerDynamicPush: () -> Unit = {},
    onSendDriverMessage: (String) -> Unit = {},
    onToggleAutoSimulation: () -> Unit = {},
    onRecalculateTraffic: () -> Unit = {},
    onDismissLiveBanner: () -> Unit = {},
    productLookup: (String) -> Product? = { null },
    onBack: () -> Unit,
    onCallDriver: () -> Unit,
    onMessageDriver: () -> Unit,
    onSelectOrder: (String) -> Unit = {},
    onSelectDeliveryStage: (OrderDeliveryStage) -> Unit = {},
    onAdvanceDeliveryStage: () -> Unit = {},
    onOpenProduct: (String) -> Unit = {},
    orderExtras: Map<String, CheckoutExtras> = emptyMap()
) {
    val view = currentPlatformView()
    val context = currentPlatformContext()
    val primaryColor = MaterialTheme.colorScheme.primary

    val locationPermissionLauncher = rememberMultiplePermissionsLauncher { permissions ->
        val granted = permissions[AppPermission.ACCESS_FINE_LOCATION] == true ||
            permissions[AppPermission.ACCESS_COARSE_LOCATION] == true
        onRefreshRealLocation(granted)
    }

    LaunchedEffect(Unit) {
        onRefreshRealLocation(false)
    }

    val activeOrder = trackedOrder ?: allOrders.firstOrNull()
    val deliveryStage = remember(activeOrder?.status) {
        OrderDeliveryStage.fromStatusString(activeOrder?.status)
    }
    val effectiveBranch = realLocationMetrics?.branchName?.takeIf { it.isNotBlank() }
        ?: activeOrder?.branch?.takeIf { it.isNotBlank() }
        ?: branchName
    val orderIdDisplay = activeOrder?.id ?: "ORD-88412"
    val effectiveEtaMinutes = driverTelemetry.dynamicEtaMinutes.takeIf { it > 0 }
        ?: realLocationMetrics?.stageAdjustedEtaMinutes
        ?: deliveryStage.etaMinutes
    val effectiveRoadKm = realLocationMetrics?.roadDistanceKm ?: 2.4

    val animatedProgress by animateFloatAsState(
        targetValue = deliveryStage.progressFraction,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "orderDeliveryProgress"
    )

    val exactRouteProgress = driverTelemetry.routeProgressFraction.coerceIn(0f, 1f)
    val totalSegments = (SANTO_DOMINGO_ROUTE.size - 1).coerceAtLeast(1)
    val scaledPos = exactRouteProgress * totalSegments
    val lowerIdx = scaledPos.toInt().coerceIn(0, SANTO_DOMINGO_ROUTE.lastIndex)
    val upperIdx = (lowerIdx + 1).coerceIn(0, SANTO_DOMINGO_ROUTE.lastIndex)
    val segFraction = (scaledPos - lowerIdx).coerceIn(0f, 1f)
    val lowerPt = SANTO_DOMINGO_ROUTE[lowerIdx]
    val upperPt = SANTO_DOMINGO_ROUTE[upperIdx]
    val targetX = lowerPt.xFraction + (upperPt.xFraction - lowerPt.xFraction) * segFraction
    val targetY = lowerPt.yFraction + (upperPt.yFraction - lowerPt.yFraction) * segFraction

    val animatedX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = tween(durationMillis = 260, easing = LinearEasing),
        label = "driverX"
    )
    val animatedY by animateFloatAsState(
        targetValue = targetY,
        animationSpec = tween(durationMillis = 260, easing = LinearEasing),
        label = "driverY"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    val trackedProducts = remember(activeOrder?.skus, cartItems) {
        val fromOrder = activeOrder?.skus.orEmpty().mapNotNull { sku ->
            productLookup(sku)?.let { it to 1 }
        }
        if (fromOrder.isNotEmpty()) fromOrder else cartItems
    }

    val hazeState = LocalTranslucentHazeState.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("delivery_tracking_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .translucentBackdropSource(hazeState, zIndex = 1f),
            contentPadding = PaddingValues(top = 74.dp, bottom = 172.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Aviso en vivo del estado del pedido
            item(key = "live_delivery_notification_banner", contentType = "live_banner") {
                LiveDeliveryNotificationBannerCard(
                    banner = liveDeliveryBanner,
                    deliveryStage = deliveryStage,
                    telemetry = driverTelemetry,
                    orderIdDisplay = orderIdDisplay,
                    darkMode = darkMode,
                    primaryColor = primaryColor,
                    onTriggerSystemNotification = {
                        performCrispHaptic(view, HapticStyle.CONFIRM)
                        onTriggerDynamicPush()
                    },
                    onDismiss = onDismissLiveBanner
                )
            }

            if (allOrders.size > 1) {
                item(key = "delivery_order_selector", contentType = "order_selector") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        allOrders.forEach { ord ->
                            val isSelected = ord.id == orderIdDisplay
                            val ordStage = OrderDeliveryStage.fromStatusString(ord.status)
                            Surface(
                                onClick = {
                                    performCrispHaptic(view, HapticStyle.TICK)
                                    onSelectOrder(ord.id)
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) {
                                    primaryColor
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.testTag("order_selector_chip_${ord.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    BravoUnifiedIcon(
                                        iconKey = ordStage.emoji,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else primaryColor,
                                        size = 14.dp
                                    )
                                    Text(
                                        text = "#${ord.id} • ${ordStage.progressPct}%",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Mapa interactivo con velocidad estimada, tráfico y ETA
            item(key = "delivery_map", contentType = "map_card") {
                val userLat = realLocationMetrics?.userLat ?: 18.4685
                val userLng = realLocationMetrics?.userLng ?: -69.9442
                val branchLat = realLocationMetrics?.branchLat ?: 18.4719
                val branchLng = realLocationMetrics?.branchLng ?: -69.9388
                val deliveryFeeRd = realLocationMetrics?.deliveryFeeRd ?: 95

                TranslucentCard(
                    darkMode = darkMode,
                    cornerRadius = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("google_maps_real_location_card")
                ) {
                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Map,
                                    contentDescription = "Mapa GPS en Vivo",
                                    tint = Tone.success,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Mapa GPS en Tiempo Real • Tráfico y Velocidad",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                            BravoUnderline(isArca = false)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${driverTelemetry.currentStreet} • ${driverTelemetry.lastUpdatedLabel}",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = {
                                        performCrispHaptic(view, HapticStyle.CONFIRM)
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                AppPermission.ACCESS_FINE_LOCATION,
                                                AppPermission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = primaryColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                                    modifier = Modifier.testTag("refresh_real_gps_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MyLocation,
                                            contentDescription = "Actualizar mi ubicación GPS",
                                            tint = primaryColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Mi GPS",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = primaryColor
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        performCrispHaptic(view, HapticStyle.TICK)
                                        onToggleAutoSimulation()
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (driverTelemetry.isAutoSimulating) {
                                        Tone.success.copy(alpha = 0.14f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (driverTelemetry.isAutoSimulating) Tone.success else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.testTag("toggle_auto_traffic_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (driverTelemetry.isAutoSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Alternar actualización automática",
                                            tint = if (driverTelemetry.isAutoSimulating) Tone.success else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (driverTelemetry.isAutoSimulating) "Auto-Tráfico Activo" else "Reanudar ruta",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = if (driverTelemetry.isAutoSimulating) Tone.success else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        performCrispHaptic(view, HapticStyle.CONFIRM)
                                        onRecalculateTraffic()
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.testTag("recalculate_traffic_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Traffic,
                                            contentDescription = "Recalcular tráfico en vivo",
                                            tint = primaryColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Tráfico ahora",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        performCrispHaptic(view, HapticStyle.CONFIRM)
                                        onTriggerDynamicPush()
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .testTag("dynamic_gps_push_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = "Enviar notificación en vivo",
                                        tint = primaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Mapa: real (Apple Maps / Google Maps) o ilustrado si no hay clave de Google Maps
                        RealInteractiveDeliveryMapView(
                            darkMode = darkMode,
                            primaryColor = primaryColor,
                            branchName = effectiveBranch,
                            branchLat = branchLat,
                            branchLng = branchLng,
                            userLat = userLat,
                            userLng = userLng,
                            telemetry = driverTelemetry,
                            deliveryStage = deliveryStage,
                            // Lambdas: el mapa lee estos valores al dibujar, sin reconstruir toda la pantalla
                            animatedX = { animatedX },
                            animatedY = { animatedY },
                            pulseRadius = { pulseRadius }
                        )

                        // Velocidad, tráfico y hora estimada
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Velocidad
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = "Velocidad estimada",
                                            tint = primaryColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "${driverTelemetry.currentSpeedKmh} km/h",
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Prom. ${driverTelemetry.avgSpeedKmh} km/h",
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Tráfico
                                val trafficColor = when (driverTelemetry.trafficLevel) {
                                    "DENSO" -> Tone.warning
                                    "MODERADO" -> Tone.success
                                    else -> Tone.success
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Traffic,
                                            contentDescription = "Estado del tráfico",
                                            tint = trafficColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = when (driverTelemetry.trafficLevel) {
                                                    "DENSO" -> "Tráfico denso"
                                                    "MODERADO" -> "Tráfico moderado"
                                                    else -> "Tráfico fluido"
                                                },
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (driverTelemetry.trafficDelayDeltaMin > 0) "+${driverTelemetry.trafficDelayDeltaMin} min por tráfico" else "Ruta despejada",
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = trafficColor
                                            )
                                        }
                                    }
                                }

                                // Hora estimada
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = primaryColor.copy(alpha = 0.12f),
                                    modifier = Modifier.weight(0.95f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (deliveryStage == OrderDeliveryStage.DELIVERED) "Entregado" else "~$effectiveEtaMinutes min",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.5.sp,
                                            color = primaryColor
                                        )
                                        Text(
                                            text = "${driverTelemetry.remainingDistanceKm} km rest.",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            DiagonalMetadataStrip(
                                items = listOf(
                                    "Ruta real: $effectiveRoadKm km",
                                    driverTelemetry.trafficCondition,
                                    "Envío base RD$$deliveryFeeRd (Gratis Bravísima)"
                                ),
                                slashColor = primaryColor,
                                fontSizeSp = 12f
                            )

                            OutlinedButton(
                                onClick = {
                                    performCrispHaptic(view, HapticStyle.CONFIRM)
                                    runCatching {
                                        PlatformActions.openUrl(
                                            context,
                                            "https://www.google.com/maps/dir/?api=1&origin=$branchLat,$branchLng&destination=$userLat,$userLng&travelmode=driving"
                                        )
                                    }
                                },
                                border = BorderStroke(1.2.dp, Tone.success),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_google_maps_route_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Map,
                                    contentDescription = null,
                                    tint = Tone.success,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Abrir Navegación Completa en Google Maps ($effectiveRoadKm km • ~$effectiveEtaMinutes min)",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Tone.success
                                )
                            }
                        }
                    }
                }
            }

            // 3. Chat con el repartidor (respuestas simuladas por IA)
            item(key = "driver_ai_live_chat_card", contentType = "driver_chat") {
                DriverAiLiveChatCard(
                    darkMode = darkMode,
                    primaryColor = primaryColor,
                    telemetry = driverTelemetry,
                    messages = driverChatMessages,
                    isDriverTyping = isDriverTyping,
                    onSendMessage = onSendDriverMessage,
                    onCallDriver = onCallDriver,
                    onQuickMessageTap = { quickPrompt ->
                        performCrispHaptic(view, HapticStyle.CONFIRM)
                        onSendDriverMessage(quickPrompt)
                    }
                )
            }

            // 4. Etapas del pedido
            item(key = "firestore_realtime_progress_card", contentType = "progress_card") {
                TranslucentCard(
                    darkMode = darkMode,
                    cornerRadius = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("order_tracking_progress_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isUpdatingOrderStatus) Icons.Default.Sync else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Tone.success,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "En vivo • #${orderIdDisplay}",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Tone.success,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = primaryColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = activeOrder?.date ?: deliveryStage.updatedDateLabel,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = primaryColor,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    BravoUnifiedIcon(
                                        iconKey = deliveryStage.emoji,
                                        tint = primaryColor,
                                        size = 20.dp
                                    )
                                    Text(
                                        text = deliveryStage.statusText,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("order_delivery_status_label")
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = deliveryStage.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(56.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("order_delivery_circular_progress"),
                                    color = if (deliveryStage == OrderDeliveryStage.DELIVERED) {
                                        Tone.success
                                    } else {
                                        primaryColor
                                    },
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 5.dp
                                )
                                Text(
                                    text = "${deliveryStage.progressPct}%",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sin porcentaje aquí: el anillo de arriba ya lo muestra (antes se comprimía en vertical)
                                DiagonalMetadataStrip(
                                    items = listOf(
                                        "Etapa ${deliveryStage.stepNumber} de ${OrderDeliveryStage.entries.size}",
                                        effectiveBranch
                                    ),
                                    slashColor = primaryColor,
                                    fontSizeSp = 12f
                                )
                            }

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .testTag("order_delivery_progress_indicator"),
                                color = if (deliveryStage == OrderDeliveryStage.DELIVERED) {
                                    Tone.success
                                } else {
                                    primaryColor
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OrderDeliveryStage.entries.forEach { stage ->
                                val isReached = stage.stepNumber <= deliveryStage.stepNumber
                                val isCurrent = stage == deliveryStage
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            performCrispHaptic(view, HapticStyle.CONFIRM)
                                            onSelectDeliveryStage(stage)
                                        }
                                        .padding(vertical = 4.dp)
                                        .testTag("order_stage_step_${stage.stepNumber}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isCurrent) 30.dp else 26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCurrent -> primaryColor
                                                    isReached -> Tone.success
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .border(
                                                width = if (isCurrent) 2.dp else 0.dp,
                                                color = if (isCurrent) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isReached && !isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = stage.shortLabel,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        } else {
                                            BravoUnifiedIcon(
                                                iconKey = stage.emoji,
                                                tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                size = if (isCurrent) 15.dp else 13.dp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Cinco columnas iguales: el texto se ajusta solo (12 → 9 sp) para no cortarse
                                    androidx.compose.foundation.text.BasicText(
                                        text = stage.shortLabel,
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            color = when {
                                                isCurrent -> primaryColor
                                                isReached -> MaterialTheme.colorScheme.onSurface
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(
                                            minFontSize = 9.sp,
                                            maxFontSize = 12.sp,
                                            stepSize = 0.5.sp
                                        ),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = firestoreSyncStatus,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = {
                                    performCrispHaptic(view, HapticStyle.CONFIRM)
                                    onAdvanceDeliveryStage()
                                },
                                border = BorderStroke(1.2.dp, primaryColor),
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("advance_order_status_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Actualizar etapa de entrega",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }
            }

            if (trackedProducts.isNotEmpty()) {
                item(key = "delivery_order_items_card", contentType = "order_items") {
                    TranslucentCard(
                        darkMode = darkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Artículos en este envío (${trackedProducts.size})",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                BravoUnderline(isArca = false)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Cadena de frío activa • Orden #$orderIdDisplay",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Tone.success,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                trackedProducts.forEach { (prod, qty) ->
                                    Surface(
                                        onClick = { onOpenProduct(prod.sku) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.width(128.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier.size(54.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ProductArtworkView(product = prod)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = prod.name,
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "x$qty • RD$${prod.price}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = primaryColor
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

        GlassTopBar(
            darkMode = darkMode,
            accent = primaryColor,
            title = "Tu pedido",
            modifier = Modifier.align(Alignment.TopCenter),
            actions = listOf(
                GlassBarAction(
                    symbol = "square.and.arrow.up",
                    icon = Icons.Outlined.Share,
                    label = "Compartir por WhatsApp",
                    testTag = "share_order_whatsapp",
                    onClick = {
                        val order = trackedOrder ?: allOrders.firstOrNull()
                        if (order != null) {
                            shareOnWhatsApp(
                                context,
                                orderWhatsAppText(order.id, deliveryStage.statusText, order.total, order.branch, orderExtras[order.id])
                            )
                        }
                    }
                )
            ),
            subtitle = if (deliveryStage == OrderDeliveryStage.DELIVERED) "Entregado" else "${driverTelemetry.currentSpeedKmh} km/h • ~$effectiveEtaMinutes min",
            subtitleColor = if (deliveryStage == OrderDeliveryStage.DELIVERED) Tone.successBright else primaryColor,
            onBack = {
                HapticFeedbackManager.onNavigationClick(view)
                onBack()
            },
            backTestTag = "delivery_back_button"
        )
    }
}

@Composable
private fun LiveDeliveryNotificationBannerCard(
    banner: LiveDeliveryBannerNotification?,
    deliveryStage: OrderDeliveryStage,
    telemetry: DriverLiveTelemetry,
    orderIdDisplay: String,
    darkMode: Boolean,
    primaryColor: Color,
    onTriggerSystemNotification: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeTitle = banner?.title ?: "${deliveryStage.statusText} • Carlos Rodríguez en ruta"
    val activeSubtitle = banner?.subtitle ?: "${telemetry.currentStreet} • ${telemetry.trafficCondition}"
    val activeIconKey = banner?.iconKey ?: deliveryStage.emoji
    val speedKmh = banner?.speedKmh ?: telemetry.currentSpeedKmh
    val etaMin = banner?.etaMinutes ?: telemetry.dynamicEtaMinutes
    val progressFraction = banner?.let { (it.progressPct / 100f).coerceIn(0f, 1f) } ?: deliveryStage.progressFraction

    TranslucentCard(
        darkMode = darkMode,
        cornerRadius = 22.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("live_delivery_notification_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        BravoUnifiedIcon(
                            iconKey = activeIconKey,
                            tint = primaryColor,
                            size = 20.dp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Notificación en vivo",
                                tint = Tone.success,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "NOTIFICACIÓN LIVE • #$orderIdDisplay",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Tone.success
                            )
                        }
                        Text(
                            text = activeTitle,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeSubtitle,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = onTriggerSystemNotification,
                        shape = RoundedCornerShape(999.dp),
                        color = primaryColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "$speedKmh km/h",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = primaryColor
                            )
                        }
                    }
                    if (banner != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Ocultar aviso",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = null,
                        tint = Tone.success,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (deliveryStage == OrderDeliveryStage.DELIVERED) "Pedido entregado en tu puerta" else "Llegada estimada en ~$etaMin min • ${telemetry.remainingDistanceKm} km",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${(progressFraction * 100).roundToInt()}%",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = primaryColor
                )
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = Tone.success,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun RealInteractiveDeliveryMapView(
    darkMode: Boolean,
    primaryColor: Color,
    branchName: String,
    branchLat: Double,
    branchLng: Double,
    userLat: Double,
    userLng: Double,
    telemetry: DriverLiveTelemetry,
    deliveryStage: OrderDeliveryStage,
    animatedX: () -> Float,
    animatedY: () -> Float,
    pulseRadius: () -> Float
) {
    // Mapa real: Apple Maps en iPhone y Google Maps en Android (si la app tiene clave).
    if (com.example.ui.components.nativeDeliveryMapAvailable()) {
        val f = telemetry.routeProgressFraction.coerceIn(0f, 1f).toDouble()
        // Ruta por calles calculada por el ViewModel; el repartidor ya viene ubicado sobre ella
        val route by com.example.delivery.DeliveryRouteStore.route.collectAsState()
        val activeRoute = route?.takeIf { r ->
            r.points.isNotEmpty() &&
                com.example.delivery.RoadRoute.haversine(r.points.first().first, r.points.first().second, branchLat, branchLng) < 0.6 &&
                com.example.delivery.RoadRoute.haversine(r.points.last().first, r.points.last().second, userLat, userLng) < 0.6
        }
        val km = (activeRoute?.totalKm ?: 0.0) * f
        val driverLat = if (activeRoute != null) telemetry.driverLat else branchLat + (userLat - branchLat) * f
        val driverLng = if (activeRoute != null) telemetry.driverLng else branchLng + (userLng - branchLng) * f
        // Las rutas solo se recalculan cuando el repartidor avanza ~20 m (menos trabajo del mapa)
        val kmBucket = (km * 50).roundToInt()
        val travelled = remember(activeRoute, kmBucket) {
            activeRoute?.travelledPath(km)?.map { com.example.ui.components.DeliveryMapPin(it.first, it.second, "") } ?: emptyList()
        }
        val remaining = remember(activeRoute, kmBucket) {
            activeRoute?.remainingPath(km)?.map { com.example.ui.components.DeliveryMapPin(it.first, it.second, "") } ?: emptyList()
        }
        val routeColor = when (telemetry.trafficLevel) {
            "DENSO" -> Tone.warningBright
            "MODERADO" -> Tone.successBright
            else -> Tone.success
        }
        com.example.ui.components.NativeDeliveryMap(
            branch = com.example.ui.components.DeliveryMapPin(branchLat, branchLng, branchName),
            home = com.example.ui.components.DeliveryMapPin(userLat, userLng, "Tu casa"),
            driver = com.example.ui.components.DeliveryMapPin(driverLat, driverLng, "Repartidor • ${telemetry.dynamicEtaMinutes} min"),
            routeColor = routeColor,
            darkMode = darkMode,
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .testTag("interactive_real_map_webview"),
            travelledPath = travelled,
            remainingPath = remaining
        )
        return
    }
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var followDriver by remember { mutableStateOf(true) }

    val animatedZoom by animateFloatAsState(
        targetValue = zoomLevel,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "mapZoom"
    )

    val infiniteFlow = rememberInfiniteTransition(label = "routeFlow")
    val dashPhase by infiniteFlow.animateFloat(
        initialValue = 36f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashPhase"
    )

    val mapBgColor = if (darkMode) Color(0xFF0C1320) else Color(0xFFE9EEF5)
    val blockColor = if (darkMode) Color(0xFF131C2E) else Color(0xFFF4F7FB)
    val parkColor = if (darkMode) Color(0xFF102A22) else Color(0xFFD6EFE0)
    val waterColor = if (darkMode) Color(0xFF0E2238) else Color(0xFFCFE6FA)
    val secondaryStreetColor = if (darkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val primaryAvenueColor = if (darkMode) Color(0xFF283548) else Color(0xFFFFFBEB)
    val avenueBorderColor = if (darkMode) Tone.inkSoft else Color(0xFFCBD5E1)
    val routeActiveColor = when (telemetry.trafficLevel) {
        "DENSO" -> Tone.warningBright
        "MODERADO" -> Tone.successBright
        else -> Tone.success
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
            .background(mapBgColor)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    followDriver = false
                    panOffsetX = (panOffsetX + dragAmount.x).coerceIn(-180f, 180f)
                    panOffsetY = (panOffsetY + dragAmount.y).coerceIn(-140f, 140f)
                }
            }
            .testTag("interactive_real_map_webview")
    ) {
        val canvasTextMeasurer = rememberTextMeasurer()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val focusX = if (followDriver && zoomLevel > 1.05f) (0.5f - animatedX()) * w * 0.45f else panOffsetX
            val focusY = if (followDriver && zoomLevel > 1.05f) (0.5f - animatedY()) * h * 0.45f else panOffsetY

            fun project(xFrac: Float, yFrac: Float): Offset {
                val cx = w * 0.5f
                val cy = h * 0.5f
                val rawX = w * xFrac
                val rawY = h * yFrac
                return Offset(
                    x = cx + (rawX - cx) * animatedZoom + focusX,
                    y = cy + (rawY - cy) * animatedZoom + focusY
                )
            }

            // 1. Manzanas y zonas verdes (Polígono Central, Santo Domingo)
            val blocks = listOf(
                Triple(Offset(0.04f, 0.06f), Offset(0.18f, 0.19f), blockColor),
                Triple(Offset(0.26f, 0.05f), Offset(0.45f, 0.19f), blockColor),
                Triple(Offset(0.51f, 0.05f), Offset(0.64f, 0.19f), blockColor),
                Triple(Offset(0.04f, 0.27f), Offset(0.18f, 0.43f), blockColor),
                Triple(Offset(0.26f, 0.27f), Offset(0.45f, 0.43f), blockColor),
                Triple(Offset(0.51f, 0.27f), Offset(0.64f, 0.43f), blockColor),
                Triple(Offset(0.72f, 0.36f), Offset(0.94f, 0.44f), blockColor),
                Triple(Offset(0.35f, 0.54f), Offset(0.64f, 0.70f), blockColor),
                Triple(Offset(0.72f, 0.54f), Offset(0.95f, 0.70f), blockColor),
                Triple(Offset(0.35f, 0.78f), Offset(0.64f, 0.94f), blockColor)
            )
            blocks.forEach { (tl, br, col) ->
                val p1 = project(tl.x, tl.y)
                val p2 = project(br.x, br.y)
                drawRoundRect(
                    color = col,
                    topLeft = p1,
                    size = Size((p2.x - p1.x).coerceAtLeast(4f), (p2.y - p1.y).coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(12f * animatedZoom, 12f * animatedZoom)
                )
            }

            // Parques y plazas
            val park1TL = project(0.05f, 0.55f)
            val park1BR = project(0.29f, 0.89f)
            drawRoundRect(
                color = parkColor,
                topLeft = park1TL,
                size = Size((park1BR.x - park1TL.x).coerceAtLeast(8f), (park1BR.y - park1TL.y).coerceAtLeast(8f)),
                cornerRadius = CornerRadius(18f * animatedZoom, 18f * animatedZoom)
            )

            val park2TL = project(0.71f, 0.07f)
            val park2BR = project(0.94f, 0.30f)
            drawRoundRect(
                color = parkColor,
                topLeft = park2TL,
                size = Size((park2BR.x - park2TL.x).coerceAtLeast(8f), (park2BR.y - park2TL.y).coerceAtLeast(8f)),
                cornerRadius = CornerRadius(18f * animatedZoom, 18f * animatedZoom)
            )

            val lakeTL = project(0.09f, 0.64f)
            val lakeBR = project(0.21f, 0.78f)
            drawRoundRect(
                color = waterColor,
                topLeft = lakeTL,
                size = Size((lakeBR.x - lakeTL.x).coerceAtLeast(4f), (lakeBR.y - lakeTL.y).coerceAtLeast(4f)),
                cornerRadius = CornerRadius(24f * animatedZoom, 24f * animatedZoom)
            )

            // 2. Calles secundarias
            val secondaryStreets = listOf(
                Offset(0f, 0.22f) to Offset(1f, 0.22f),
                Offset(0f, 0.74f) to Offset(1f, 0.74f),
                Offset(0.48f, 0f) to Offset(0.48f, 1f),
                Offset(0.32f, 0f) to Offset(0.32f, 1f),
                Offset(0.84f, 0f) to Offset(0.84f, 1f),
                Offset(0.18f, 0.22f) to Offset(0.84f, 0.76f)
            )
            secondaryStreets.forEach { (s, e) ->
                val pStart = project(s.x, s.y)
                val pEnd = project(e.x, e.y)
                drawLine(
                    color = avenueBorderColor,
                    start = pStart,
                    end = pEnd,
                    strokeWidth = 13f * animatedZoom,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = secondaryStreetColor,
                    start = pStart,
                    end = pEnd,
                    strokeWidth = 10f * animatedZoom,
                    cap = StrokeCap.Round
                )
            }

            // 3. Avenidas principales (Winston Churchill, Abraham Lincoln…)
            val mainAvenues = listOf(
                Offset(0.21f, 0f) to Offset(0.21f, 1f),
                Offset(0.68f, 0f) to Offset(0.68f, 1f),
                Offset(0f, 0.48f) to Offset(1f, 0.48f)
            )
            mainAvenues.forEach { (s, e) ->
                val pStart = project(s.x, s.y)
                val pEnd = project(e.x, e.y)
                drawLine(
                    color = avenueBorderColor,
                    start = pStart,
                    end = pEnd,
                    strokeWidth = 22f * animatedZoom,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primaryAvenueColor,
                    start = pStart,
                    end = pEnd,
                    strokeWidth = 18f * animatedZoom,
                    cap = StrokeCap.Round
                )
            }

            // 4. Nombres de las calles
            run {
                val labelColor = if (darkMode) Color(203, 213, 225, 215) else Color(51, 65, 85, 220)
                val parkColor = if (darkMode) Color(110, 231, 183, 210) else Color(4, 120, 87, 220)
                val labelSize = (12f * animatedZoom).coerceIn(12f, 16f)
                val parkSize = (12f * animatedZoom).coerceIn(12f, 14f)

                val av27Pos = project(0.35f, 0.465f)
                canvasLabel(canvasTextMeasurer, "Av. 27 de Febrero", av27Pos.x, av27Pos.y, labelColor, labelSize)

                val pastorizaPos = project(0.24f, 0.205f)
                canvasLabel(canvasTextMeasurer, "C. Roberto Pastoriza", pastorizaPos.x, pastorizaPos.y, labelColor, labelSize)

                val mejiaPos = project(0.36f, 0.725f)
                canvasLabel(canvasTextMeasurer, "Av. Gustavo Mejía Ricart", mejiaPos.x, mejiaPos.y, labelColor, labelSize)

                val churchillPos = project(0.03f, 0.38f)
                canvasLabel(canvasTextMeasurer, "Av. Churchill", churchillPos.x, churchillPos.y, labelColor, labelSize)

                val lincolnPos = project(0.70f, 0.62f)
                canvasLabel(canvasTextMeasurer, "Av. Lincoln", lincolnPos.x, lincolnPos.y, labelColor, labelSize)

                val park1Label = project(0.07f, 0.61f)
                canvasLabel(canvasTextMeasurer, "Parque La Lira", park1Label.x, park1Label.y, parkColor, parkSize)

                val park2Label = project(0.73f, 0.16f)
                canvasLabel(canvasTextMeasurer, "Plaza Piantini", park2Label.x, park2Label.y, parkColor, parkSize)
            }

            // 5. Ruta completa (tramo hecho y tramo pendiente)
            val routePoints = SANTO_DOMINGO_ROUTE.map { pt -> project(pt.xFraction, pt.yFraction) }
            if (routePoints.size >= 2) {
                val fullRoutePath = Path().apply {
                    moveTo(routePoints.first().x, routePoints.first().y)
                    for (i in 1 until routePoints.size) {
                        lineTo(routePoints[i].x, routePoints[i].y)
                    }
                }
                // Halo de la ruta
                drawPath(
                    path = fullRoutePath,
                    color = primaryColor.copy(alpha = 0.24f),
                    style = Stroke(width = 18f * animatedZoom, cap = StrokeCap.Round)
                )
                // Trazo base de la ruta
                drawPath(
                    path = fullRoutePath,
                    color = routeActiveColor.copy(alpha = 0.35f),
                    style = Stroke(width = 9f * animatedZoom, cap = StrokeCap.Round)
                )
                // Rayas animadas del tráfico
                drawPath(
                    path = fullRoutePath,
                    color = routeActiveColor,
                    style = Stroke(
                        width = 6.5f * animatedZoom,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 14f), dashPhase)
                    )
                )
            }

            // 6. Marcador de la sucursal
            val originCenter = project(SANTO_DOMINGO_ROUTE.first().xFraction, SANTO_DOMINGO_ROUTE.first().yFraction)
            drawCircle(
                color = primaryColor.copy(alpha = 0.24f),
                radius = 18f * animatedZoom,
                center = originCenter
            )
            drawCircle(
                color = Color.White,
                radius = 11f * animatedZoom,
                center = originCenter
            )
            drawCircle(
                color = primaryColor,
                radius = 8f * animatedZoom,
                center = originCenter
            )

            // 7. Marcador del cliente
            val destCenter = project(SANTO_DOMINGO_ROUTE.last().xFraction, SANTO_DOMINGO_ROUTE.last().yFraction)
            drawCircle(
                color = Tone.success.copy(alpha = 0.26f),
                radius = 20f * animatedZoom,
                center = destCenter
            )
            drawCircle(
                color = Color.White,
                radius = 12f * animatedZoom,
                center = destCenter
            )
            drawCircle(
                color = Tone.success,
                radius = 8.5f * animatedZoom,
                center = destCenter
            )

            // 8. Posición del repartidor con pulso
            val driverCenter = project(animatedX(), animatedY())
            // Tramo recorrido hasta el repartidor
            drawLine(
                color = primaryColor,
                start = originCenter,
                end = driverCenter,
                strokeWidth = 6f * animatedZoom,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.28f),
                radius = pulseRadius() * animatedZoom,
                center = driverCenter
            )
            drawCircle(
                color = Color.White,
                radius = 14f * animatedZoom,
                center = driverCenter
            )
            drawCircle(
                color = primaryColor,
                radius = 10.5f * animatedZoom,
                center = driverCenter
            )
            drawCircle(
                color = Color.White,
                radius = 4f * animatedZoom,
                center = driverCenter
            )

            // Etiquetas de sucursal, cliente y repartidor
            run {
                val pillBg = if (darkMode) Color(15, 23, 42, 235) else Color(255, 255, 255, 242)
                val pillText = if (darkMode) Color.White else Color(15, 23, 42)

                // Etiqueta de la sucursal
                val originLabel = "$branchName"
                val oWidth = measureCanvasLabel(canvasTextMeasurer, originLabel, 10.5f) + 18f * density
                val oHeight = 20f * density
                val oLeft = (originCenter.x - oWidth / 2f).coerceIn(8f, (w - oWidth - 8f).coerceAtLeast(8f))
                val oTop = (originCenter.y - 30f * density).coerceIn(8f, (h - oHeight - 8f).coerceAtLeast(8f))
                drawRoundRect(pillBg, topLeft = Offset(oLeft, oTop), size = Size(oWidth, oHeight), cornerRadius = CornerRadius(999f, 999f))
                canvasLabel(canvasTextMeasurer, originLabel, oLeft + 9f * density, oTop + oHeight / 2f + 3.8f * density, pillText, 10.5f)

                // Etiqueta del cliente
                val destLabel = "Tu entrega"
                val dWidth = measureCanvasLabel(canvasTextMeasurer, destLabel, 10.5f) + 18f * density
                val dLeft = (destCenter.x - dWidth / 2f).coerceIn(8f, (w - dWidth - 8f).coerceAtLeast(8f))
                val dTop = (destCenter.y + 12f * density).coerceIn(8f, (h - oHeight - 46f * density).coerceAtLeast(8f))
                drawRoundRect(pillBg, topLeft = Offset(dLeft, dTop), size = Size(dWidth, oHeight), cornerRadius = CornerRadius(999f, 999f))
                canvasLabel(canvasTextMeasurer, destLabel, dLeft + 9f * density, dTop + oHeight / 2f + 3.8f * density, pillText, 10.5f)

                // Etiqueta del repartidor
                val speedText = "Carlos • ${telemetry.currentSpeedKmh} km/h"
                val drWidth = measureCanvasLabel(canvasTextMeasurer, speedText, 11f) + 20f * density
                val drHeight = 22f * density
                val drLeft = (driverCenter.x - drWidth / 2f).coerceIn(10f, (w - drWidth - 10f).coerceAtLeast(10f))
                val drTop = (driverCenter.y - 36f * density).coerceIn(44f * density, (h - drHeight - 48f * density).coerceAtLeast(44f * density))
                drawRoundRect(Color(227, 24, 55), topLeft = Offset(drLeft, drTop), size = Size(drWidth, drHeight), cornerRadius = CornerRadius(999f, 999f))
                canvasLabel(canvasTextMeasurer, speedText, drLeft + 10f * density, drTop + drHeight / 2f + 4f * density, Color.White, 11f)
            }
        }

        // Calle actual y tráfico (arriba a la izquierda)
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${telemetry.currentStreet} • Tiempo Real 1:1",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Zoom y centrar (arriba a la derecha)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            zoomLevel = (zoomLevel + 0.25f).coerceAtMost(2.25f)
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("map_zoom_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Acercar mapa",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.width(24.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                    IconButton(
                        onClick = {
                            zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.85f)
                            if (zoomLevel <= 1f) {
                                panOffsetX = 0f
                                panOffsetY = 0f
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("map_zoom_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Alejar mapa",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Surface(
                onClick = {
                    followDriver = true
                    zoomLevel = 1.15f
                    panOffsetX = 0f
                    panOffsetY = 0f
                },
                shape = RoundedCornerShape(999.dp),
                color = if (followDriver) primaryColor else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 5.dp,
                border = BorderStroke(
                    1.dp,
                    if (followDriver) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                ),
                modifier = Modifier.testTag("map_center_driver_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Seguir repartidor",
                        tint = if (followDriver) Color.White else primaryColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (followDriver) "Siguiendo" else "Centrar",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (followDriver) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Datos en vivo (abajo)
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFF0B0F17).copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(14.dp)
                )
                DiagonalMetadataStrip(
                    items = listOf(
                        "${telemetry.currentSpeedKmh} km/h",
                        "${telemetry.remainingDistanceKm} km restantes",
                        if (deliveryStage == OrderDeliveryStage.DELIVERED) "Entregado" else "ETA ~${telemetry.dynamicEtaMinutes} min"
                    ),
                    slashColor = Color(0xFF34D399),
                    textColor = Color.White,
                    fontSizeSp = 12f
                )
            }
        }
    }
}

@Composable
private fun DriverAiLiveChatCard(
    darkMode: Boolean,
    primaryColor: Color,
    telemetry: DriverLiveTelemetry,
    messages: List<DriverChatMessage>,
    isDriverTyping: Boolean,
    onSendMessage: (String) -> Unit,
    onCallDriver: () -> Unit,
    onQuickMessageTap: (String) -> Unit
) {
    val view = currentPlatformView()
    var draftMessage by remember { mutableStateOf("") }

    val quickDriverPrompts = remember {
        listOf(
            "¿Por dónde vienes?",
            "¿Cómo está el tráfico?",
            "Déjalo en recepción",
            "Avísame al llegar",
            "¿Viene fría la compra?"
        )
    }

    TranslucentCard(
        darkMode = darkMode,
        cornerRadius = 24.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("driver_ai_chat_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabecera del repartidor
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = "Repartidor: Carlos Rodríguez",
                            tint = primaryColor,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Carlos Rodríguez",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Tone.success.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = if (telemetry.currentSpeedKmh > 0) "${telemetry.currentSpeedKmh} km/h" else "Detenido",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Tone.success,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        DiagonalMetadataStrip(
                            items = listOf("Repartidor BravoVa", "4.95 Estrellas", "Placa K-04921"),
                            slashColor = primaryColor,
                            fontSizeSp = 12f
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onCallDriver,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Tone.success.copy(alpha = 0.14f))
                            .testTag("driver_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Llamar a Carlos",
                            tint = Tone.success,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            onQuickMessageTap("Hola Carlos, ¿por dónde vienes y en cuánto tiempo llegas?")
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.12f))
                            .testTag("driver_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Enviar mensaje rápido a Carlos",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            // Respuestas rápidas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickDriverPrompts.forEach { prompt ->
                    Surface(
                        onClick = { onQuickMessageTap(prompt) },
                        shape = RoundedCornerShape(999.dp),
                        color = primaryColor.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.28f))
                    ) {
                        Text(
                            text = prompt,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Mensajes
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                messages.takeLast(5).forEach { msg ->
                    val isUser = msg.sender == "user"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            color = if (isUser) {
                                primaryColor
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                            },
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isUser) "Tú" else "Carlos Rodríguez (repartidor)",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = if (isUser) Color.White.copy(alpha = 0.9f) else primaryColor
                                    )
                                    Text(
                                        text = "${msg.timestampLabel} • ${msg.speedKmhAtSend} km/h",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isUser) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = msg.text,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.5.sp,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (isDriverTyping) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = primaryColor
                        )
                        Text(
                            text = "Carlos Rodríguez está respondiendo desde la ruta (${telemetry.currentSpeedKmh} km/h)...",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryColor
                        )
                    }
                }
            }

            // Campo para escribirle al repartidor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(start = 14.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (draftMessage.isEmpty()) {
                        Text(
                            text = "Escribe un mensaje al repartidor Carlos...",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = draftMessage,
                        onValueChange = { draftMessage = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(primaryColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("driver_chat_input")
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        val clean = draftMessage.trim()
                        if (clean.isNotEmpty()) {
                            performCrispHaptic(view, HapticStyle.CONFIRM)
                            draftMessage = ""
                            onSendMessage(clean)
                        }
                    },
                    enabled = draftMessage.trim().isNotEmpty() && !isDriverTyping,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (draftMessage.trim().isNotEmpty() && !isDriverTyping) primaryColor
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("driver_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar mensaje al repartidor",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}



private fun DrawScope.canvasLabel(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    baselineY: Float,
    color: Color,
    sizeDp: Float
) {
    val layout = measurer.measure(
        text,
        TextStyle(color = color, fontSize = (sizeDp / fontScale).sp, fontWeight = FontWeight.Bold)
    )
    drawText(layout, topLeft = Offset(x, baselineY - layout.firstBaseline))
}

private fun DrawScope.measureCanvasLabel(measurer: TextMeasurer, text: String, sizeDp: Float): Float =
    measurer.measure(
        text,
        TextStyle(fontSize = (sizeDp / fontScale).sp, fontWeight = FontWeight.Bold)
    ).size.width.toFloat()
