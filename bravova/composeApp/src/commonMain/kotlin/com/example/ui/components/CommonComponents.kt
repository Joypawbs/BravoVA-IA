package com.example.ui.components

import org.jetbrains.compose.resources.stringResource

import com.example.ui.theme.Tone

import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.drawBehind
import com.example.ui.theme.BravoCtaGradient
import com.example.ui.theme.formatRd
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.BadgedBox
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBar
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight

import com.example.platform.*
import coil3.compose.LocalPlatformContext
import androidx.compose.runtime.collectAsState
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import com.example.resources.*
import org.jetbrains.compose.resources.painterResource

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlutterDash
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import com.example.model.AppScreen
import com.example.model.Product
import com.example.model.StoreMode
import com.example.model.nutritionProfile
import com.example.ui.theme.ArcaEmerald
import com.example.ui.theme.ArcaTeal
import com.example.ui.theme.BravoAmber
import com.example.ui.theme.BravoGreen
import com.example.ui.theme.BravoLime
import com.example.ui.theme.BravoRed
import com.example.ui.theme.PlusJakartaSans
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

val DeviceSupportsGpuBlur: Boolean get() = PlatformInfo.supportsGpuBlur

val UniformMenuBlurRadius: Dp = 24.dp
val UniformBackdropBlurRadius: Dp = 38.dp

val LocalTranslucentHazeState = staticCompositionLocalOf<HazeState?> { null }

fun Modifier.realGpuBlur(radiusDp: Dp = UniformMenuBlurRadius): Modifier = this.blur(radiusDp)

@Composable
fun rememberUniformTranslucentHazeStyle(
    darkMode: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary
): HazeStyle {
    val tintBase = if (darkMode) Color(0xFF0B1120) else Color(0xFFFFFFFF)
    val fallbackBg = if (darkMode) Color(0xFF0A0F1A) else Color(0xFFFFFAF5)
    val tintAlpha = if (darkMode) 0.34f else 0.54f
    return remember(darkMode) {
        HazeStyle(
            backgroundColor = fallbackBg,
            tints = listOf(
                HazeTint(color = tintBase.copy(alpha = tintAlpha))
            ),
            blurRadius = UniformMenuBlurRadius,
            noiseFactor = 0f
        )
    }
}

fun Modifier.translucentBackdropSource(
    hazeState: HazeState?,
    zIndex: Float = 0f,
    enabled: Boolean = true
): Modifier {
    return if (enabled && hazeState != null && DeviceSupportsGpuBlur) {
        this.hazeSource(state = hazeState, zIndex = zIndex)
    } else {
        this
    }
}

@OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)
@Composable
fun Modifier.translucentBackdropEffect(
    darkMode: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = LocalTranslucentHazeState.current,
    enabled: Boolean = true
): Modifier {
    if (!enabled || hazeState == null || !DeviceSupportsGpuBlur) return this
    val style = rememberUniformTranslucentHazeStyle(darkMode = darkMode, accentColor = accentColor)
    return this.hazeEffect(state = hazeState, style = style) {
        blurEnabled = true
        inputScale = hazeInputScaleForDevice()
    }
}

/**
 * Rendimiento del vidrio esmerilado: el fondo se desenfoca a menor resolución y se agranda después.
 * Con un desenfoque de 24 dp la diferencia no se nota. Según el autor de Haze, 0.8 reduce ~35 % de
 * píxeles de forma casi imperceptible; en gama baja se usa la escala automática (1/3).
 */
@OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)
fun hazeInputScaleForDevice(): dev.chrisbanes.haze.HazeInputScale =
    if (com.example.platform.DevicePerformance.isLowEnd) dev.chrisbanes.haze.HazeInputScale.Auto
    else dev.chrisbanes.haze.HazeInputScale.Fixed(0.8f)

@Composable
fun uniformMenuTranslucentContainerColor(darkMode: Boolean): Color {
    return MaterialTheme.colorScheme.surface
}

@Composable
fun uniformInlineTranslucentCardColor(darkMode: Boolean): Color {
    return if (darkMode) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
}

fun uniformMenuTranslucentGlowBrush(accentColor: Color, darkMode: Boolean): Brush =
    SolidColor(Color.Transparent)

/** Borde fino: casi invisible en claro (como la app oficial), una línea suave en oscuro. */
fun uniformMenuTranslucentBorderBrush(accentColor: Color, darkMode: Boolean): Brush =
    SolidColor(if (darkMode) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.045f))

fun uniformMenuTranslucentSheenBrush(darkMode: Boolean, accentColor: Color): Brush =
    SolidColor(Color.Transparent)

fun parseHexColor(hex: String, fallback: Color = BravoRed): Color {
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or value)
        } else {
            Color(value)
        }
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun DiagonalMetadataStrip(
    items: List<String>,
    slashColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSizeSp: Float = 12f,
    modifier: Modifier = Modifier,
    onItemClick: ((Int, String) -> Unit)? = null,
    /** Líneas permitidas antes de cortar con "…". Usa 2 donde el texto no debe perderse. */
    maxLines: Int = 1
) {
    val cleanItems = remember(items) { items.filter { it.isNotBlank() } }
    if (cleanItems.isEmpty()) return

    if (onItemClick == null) {
        val annotated = remember(cleanItems, slashColor, textColor) {
            buildAnnotatedString {
                cleanItems.forEachIndexed { idx, part ->
                    withStyle(
                        SpanStyle(
                            color = if (idx == 0) slashColor else textColor,
                            fontWeight = if (idx == 0) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    ) {
                        append(part)
                    }
                    if (idx < cleanItems.lastIndex) {
                        withStyle(
                            SpanStyle(
                                color = slashColor.copy(alpha = 0.65f),
                                fontWeight = FontWeight.Black
                            )
                        ) {
                            append("  /  ")
                        }
                    }
                }
            }
        }
        Text(
            text = annotated,
            fontSize = fontSizeSp.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            cleanItems.forEachIndexed { idx, part ->
                Text(
                    text = part,
                    color = if (idx == 0) slashColor else textColor,
                    fontWeight = if (idx == 0) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = fontSizeSp.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onItemClick(idx, part) }
                )
                if (idx < cleanItems.lastIndex) {
                    Text(
                        text = "  /  ",
                        color = slashColor.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Black,
                        fontSize = fontSizeSp.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedPurchaseButton(
    text: String,
    icon: ImageVector? = Icons.Filled.ShoppingCart,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    loading: Boolean = false,
    loadingText: String? = null
) {
    val view = currentPlatformView()
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var flashActive by remember { mutableStateOf(false) }

    val filledState = isPressed || flashActive || loading

    val bgColor by animateColorAsState(
        targetValue = if (filledState) primaryColor else primaryColor.copy(alpha = 0.06f),
        animationSpec = tween(durationMillis = 150),
        label = "btnBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (filledState) Color.White else primaryColor,
        animationSpec = tween(durationMillis = 150),
        label = "btnContent"
    )
    val scale by animateFloatAsState(
        targetValue = if (filledState) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "btnScale"
    )

    // Android (Material 3 Expressive): al tocar, la píldora se vuelve un rectángulo redondeado con un
    // resorte. iPhone conserva la píldora y solo se encoge un poco, como los botones del sistema.
    val cornerPercent by animateIntAsState(
        targetValue = if (com.example.platform.PlatformInfo.isAndroid && filledState) 22 else 50,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "btnShapeMorph"
    )

    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    Surface(
        onClick = {
            if (loading) return@Surface
            performCrispHaptic(view, HapticStyle.CONFIRM)
            scope.launch {
                flashActive = true
                onClick()
                delay(200L)
                flashActive = false
            }
        },
        enabled = !loading,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(percent = cornerPercent),
        color = bgColor,
        border = BorderStroke(1.8.dp, primaryColor),
        modifier = taggedModifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (loading) (loadingText ?: text) else text,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ShimmerSkeletonBox(
    darkMode: Boolean,
    cornerRadius: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -320f,
        targetValue = 920f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val baseColor = if (darkMode) Color(0xFF162032) else Color(0xFFE2E8F0)
    val midColor = if (darkMode) Color(0xFF1E293B) else Color(0xFFEEF2F6)
    val highlightColor = if (darkMode) Tone.inkSoft else Color(0xFFFFFFFF)

    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to baseColor,
            0.35f to midColor,
            0.50f to highlightColor,
            0.65f to midColor,
            1.0f to baseColor
        ),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 260f, 160f)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}


@Composable
fun AppStartupSkeletonScreen(
    darkMode: Boolean,
    isArca: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Pantalla de carga: fondo sólido fijo (sin degradados ni halos) para que el vidrio destaque limpio.
    val brandColor = if (isArca) ArcaTeal else Color(0xFFE30613)
    val background = MaterialTheme.colorScheme.background
    val primaryTextColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Progreso continuo y sin saltos: avanza rápido al principio y frena cerca del final
    // (nunca retrocede); la pantalla se cierra sola cuando el catálogo termina de cargar.
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(0.92f, tween(durationMillis = 2800, easing = FastOutSlowInEasing))
        progress.animateTo(0.98f, tween(durationMillis = 6000, easing = LinearEasing))
    }
    // Aparición suave del emblema
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .testTag("startup_skeleton_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = appear.value
                    val s = 0.94f + 0.06f * appear.value
                    scaleX = s
                    scaleY = s
                }
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_launcher_art),
                contentDescription = "Icono oficial de Bravo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(124.dp)
                    .clip(RoundedCornerShape(30.dp))
            )

            Spacer(modifier = Modifier.height(28.dp))

            BravoOfficialLogoBadge(scale = 1.28f)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Los expertos en vender barato",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.Center,
                color = primaryTextColor
            )

            Spacer(modifier = Modifier.height(36.dp))

            val text = "Cargando catálogo, precios e imágenes…"
            if (nativeLiquidGlassAvailable()) {
                // iOS 26+: cápsula de Liquid Glass nativa con la barra de progreso del sistema
                NativeGlassProgressCapsule(
                    progress = progress.value,
                    text = text,
                    accent = brandColor,
                    darkMode = darkMode,
                    modifier = Modifier
                        .width(280.dp)
                        .height(74.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(37.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(37.dp))
                        // El padding interno mantiene la barra lejos del borde redondeado
                        .padding(horizontal = 26.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = brandColor,
                        trackColor = brandColor.copy(alpha = if (darkMode) 0.22f else 0.14f),
                        drawStopIndicator = {}
                    )
                    Text(
                        text = text,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        color = secondaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun BravoUnderline(isArca: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val brush = if (isArca) {
        Brush.horizontalGradient(listOf(ArcaEmerald, ArcaTeal))
    } else {
        Brush.horizontalGradient(listOf(BravoAmber, BravoRed, primary))
    }
    Box(
        modifier = modifier
            .padding(top = 3.dp)
            .width(46.dp)
            .height(3.5.dp)
            .clip(CircleShape)
            .background(brush)
    )
}

@Composable
fun BravoOfficialLogoBadge(scale: Float = 1f) {
    val cutoutBitmap: ImageBitmap? = null
    if (cutoutBitmap != null) {
        Image(
            bitmap = cutoutBitmap,
            contentDescription = "Logo Oficial Supermercados Bravo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height((31f * scale).dp)
                .width((118f * scale).dp)
        )
    } else {
        Image(
            painter = painterResource(Res.drawable.logo_bravo_official),
            contentDescription = "Logo Oficial Supermercados Bravo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height((31f * scale).dp)
                .width((118f * scale).dp)
        )
    }
}

@Composable
fun ArcaOfficialLogoBadge(scale: Float = 1f) {
    val cutoutBitmap: ImageBitmap? = null
    if (cutoutBitmap != null) {
        Image(
            bitmap = cutoutBitmap,
            contentDescription = "Logo Oficial El Arca Petshop",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height((34f * scale).dp)
                .width((80f * scale).dp)
        )
    } else {
        Image(
            painter = painterResource(Res.drawable.logo_arca_official),
            contentDescription = "Logo Oficial El Arca Petshop",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height((34f * scale).dp)
                .width((80f * scale).dp)
        )
    }
}

@Composable
fun BravoArcaHeaderSwitcher(
    activeStore: StoreMode,
    darkMode: Boolean,
    onSelectStore: (StoreMode) -> Unit,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val view = currentPlatformView()
    var internalExpanded by remember { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded
    val setExpanded: (Boolean) -> Unit = { next ->
        if (onExpandedChange != null) {
            onExpandedChange(next)
        } else {
            internalExpanded = next
        }
    }
    val isArca = activeStore == StoreMode.ARCA
    val accent = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary
    val borderCol = if (isArca) {
        Color(0xFFA3B108).copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    HapticFeedbackManager.onNavigationClick(view)
                    if (PawEasterEgg.registerLogoTap()) setExpanded(false) else setExpanded(!isExpanded)
                }
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag("brand_switcher_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = activeStore,
                transitionSpec = {
                    val toArca = targetState == StoreMode.ARCA
                    val slideOffset = if (toArca) 36 else -36
                    (androidx.compose.animation.fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                        androidx.compose.animation.scaleIn(initialScale = 0.88f, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                        androidx.compose.animation.slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { slideOffset })
                        .togetherWith(
                            androidx.compose.animation.fadeOut(animationSpec = tween(190)) +
                                androidx.compose.animation.scaleOut(targetScale = 0.90f, animationSpec = tween(190))
                        )
                },
                label = "header_brand_logo_morph"
            ) { mode ->
                if (mode == StoreMode.BRAVO) {
                    BravoOfficialLogoBadge(scale = 0.98f)
                } else {
                    ArcaOfficialLogoBadge(scale = 1.02f)
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = "Cambiar tienda",
                tint = if (isArca) Color(0xFF6B7800) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        if (onExpandedChange == null) {
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { setExpanded(false) },
                shape = RoundedCornerShape(22.dp),
                containerColor = uniformMenuTranslucentContainerColor(darkMode),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.1.dp, uniformMenuTranslucentBorderBrush(accent, darkMode)),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                BravoArcaSelectorMenuContent(
                    activeStore = activeStore,
                    borderCol = borderCol,
                    onSelectStore = { mode ->
                        setExpanded(false)
                        onSelectStore(mode)
                    }
                )
            }
        }
    }
}

@Composable
private fun BravoArcaSelectorMenuContent(
    activeStore: StoreMode,
    borderCol: Color,
    onSelectStore: (StoreMode) -> Unit
) {
    val view = currentPlatformView()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CAMBIAR DE TIENDA",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Surface(
            onClick = {
                HapticFeedbackManager.onConfirmAction(view)
                onSelectStore(StoreMode.BRAVO)
            },
            shape = RoundedCornerShape(16.dp),
            color = if (activeStore == StoreMode.BRAVO) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                Color.Transparent
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("switch_to_bravo")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BravoOfficialLogoBadge(scale = 0.92f)
                    Column {
                        Text(
                            text = "Supermercados Bravo",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Los expertos en vender barato",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                if (activeStore == StoreMode.BRAVO) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Activo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = borderCol.copy(alpha = 0.4f)
        )

        Surface(
            onClick = {
                HapticFeedbackManager.onConfirmAction(view)
                onSelectStore(StoreMode.ARCA)
            },
            shape = RoundedCornerShape(16.dp),
            color = if (activeStore == StoreMode.ARCA) {
                ArcaTeal.copy(alpha = 0.16f)
            } else {
                Color.Transparent
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("switch_to_arca")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArcaOfficialLogoBadge(scale = 0.98f)
                    Column {
                        Text(
                            text = "El Arca Petshop",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Nutrición & Cuidado Mascotas",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                if (activeStore == StoreMode.ARCA) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Activo",
                        tint = ArcaTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BravoArcaSelectorMenuOverlay(
    expanded: Boolean,
    activeStore: StoreMode,
    darkMode: Boolean,
    menuHazeState: HazeState?,
    onDismiss: () -> Unit,
    onSelectStore: (StoreMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isArca = activeStore == StoreMode.ARCA
    val accent = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary
    val borderCol = if (isArca) {
        Color(0xFFA3B108).copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    }
    val shape = RoundedCornerShape(22.dp)
    val borderBrush = uniformMenuTranslucentBorderBrush(accent, darkMode)
    val sheenBrush = uniformMenuTranslucentSheenBrush(darkMode, accent)
    val useRealHaze = expanded && menuHazeState != null && DeviceSupportsGpuBlur
    val containerCol = if (useRealHaze) Color.Transparent else uniformMenuTranslucentContainerColor(darkMode)

    androidx.compose.animation.AnimatedVisibility(
        visible = expanded,
        enter = androidx.compose.animation.fadeIn(animationSpec = tween(180)) +
            androidx.compose.animation.scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ),
        exit = androidx.compose.animation.fadeOut(animationSpec = tween(150)) +
            androidx.compose.animation.scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(150)
            ),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Zona invisible: tocar fuera cierra el menú
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            // Los toques rápidos sobre el logo caen aquí cuando el menú está abierto:
                            // también cuentan para el huevo de pascua
                            PawEasterEgg.registerLogoTap()
                            onDismiss()
                        }
                    )
            )

            Surface(
                modifier = Modifier
                    .padding(start = 14.dp, top = 46.dp)
                    .width(296.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = shape,
                        spotColor = accent.copy(alpha = 0.28f)
                    )
                    .clip(shape)
                    .translucentBackdropEffect(
                        darkMode = darkMode,
                        accentColor = accent,
                        hazeState = menuHazeState,
                        enabled = useRealHaze
                    )
                    .testTag("brand_switcher_menu_surface"),
                shape = shape,
                color = containerCol,
                border = BorderStroke(1.2.dp, borderBrush)
            ) {
                Box(
                    modifier = Modifier
                        .background(uniformMenuTranslucentGlowBrush(accent, darkMode))
                        .background(sheenBrush)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    BravoArcaSelectorMenuContent(
                        activeStore = activeStore,
                        borderCol = borderCol,
                        onSelectStore = { mode ->
                            onDismiss()
                            onSelectStore(mode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingHeaderCapsule(
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    enableHazeBlur: Boolean = true,
    content: @Composable () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val useRealHaze = enableHazeBlur && LocalTranslucentHazeState.current != null && DeviceSupportsGpuBlur
    val containerColor = if (useRealHaze) Color.Transparent else uniformMenuTranslucentContainerColor(darkMode)
    val borderBrush = uniformMenuTranslucentBorderBrush(primary, darkMode)
    val sheenBrush = uniformMenuTranslucentSheenBrush(darkMode, primary)
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Cabecera con vidrio líquido (Liquid Glass)
        LiquidGlass(
            darkMode = darkMode,
            accent = primary,
            cornerRadius = 24.dp,
            elevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                content()
            }
        }
    }
}

@Composable
fun TranslucentCard(
    darkMode: Boolean,
    isArca: Boolean = false,
    cornerRadius: Dp = 16.dp,
    modifier: Modifier = Modifier,
    enableHazeBlur: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val view = currentPlatformView()
    val primary = MaterialTheme.colorScheme.primary
    val accent = if (isArca) ArcaTeal else primary
    val useRealHaze = enableHazeBlur && LocalTranslucentHazeState.current != null && DeviceSupportsGpuBlur

    val bg = if (useRealHaze) Color.Transparent else uniformInlineTranslucentCardColor(darkMode)
    val borderBrush = uniformMenuTranslucentBorderBrush(accent, darkMode)
    val spot = Color.Black.copy(alpha = if (darkMode) 0.0f else 0.10f)
    val translucentSheen = uniformMenuTranslucentSheenBrush(darkMode, accent)
    val shape = RoundedCornerShape(cornerRadius)

    val cardModifier = modifier
        .shadow(
            elevation = if (darkMode) 0.dp else 3.dp,
            shape = shape,
            spotColor = spot,
            ambientColor = spot
        )
        .clip(shape)
        .translucentBackdropEffect(
            darkMode = darkMode,
            accentColor = accent,
            enabled = useRealHaze
        )

    if (onClick != null) {
        Card(
            onClick = {
                performCrispHaptic(view, HapticStyle.TICK)
                onClick()
            },
            modifier = cardModifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(0.8.dp, borderBrush)
        ) {
            Box(
                modifier = Modifier
                    .background(uniformMenuTranslucentGlowBrush(accent, darkMode))
                    .background(translucentSheen)
            ) {
                content()
            }
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(0.8.dp, borderBrush)
        ) {
            Box(
                modifier = Modifier
                    .background(uniformMenuTranslucentGlowBrush(accent, darkMode))
                    .background(translucentSheen)
            ) {
                content()
            }
        }
    }
}

@Composable
fun ProductArtworkView(
    product: Product,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val context = LocalPlatformContext.current
    val accent = remember(product.colorHex) { parseHexColor(product.colorHex) }
    val isDark = MaterialTheme.colorScheme.background.red < 0.3f

    Box(
        modifier = modifier
            // Sin fondo: la foto ya llega recortada y centrada (ProductCutout.kt)
            .clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (!product.imageUrl.isNullOrBlank()) {
            val dataSaver = LocalDataSaver.current
            val imageRequest = remember(product.imageUrl, context, dataSaver) {
                ImageRequest.Builder(context)
                    .data(product.imageUrl)
                    // Se decodifica al tamaño que se muestra: menos memoria y scroll más suave
                    .size(if (dataSaver) 256 else 512)
                    .softwareBitmapsForTransformations()
                    .transformations(productImageTransformations())
                    .crossfade(if (dataSaver) 0 else 260)
                    .build()
            }
            // Rendimiento: rememberAsyncImagePainter en vez de SubcomposeAsyncImage. Coil advierte que la
            // subcomposición es lenta en LazyRow/LazyColumn, y aquí hay una foto por tarjeta de producto.
            val painter = rememberAsyncImagePainter(model = imageRequest, contentScale = ContentScale.Fit)
            val imageState by painter.state.collectAsState()
            if (imageState is AsyncImagePainter.State.Error) {
                // Si falló el recorte, intenta la foto original antes de usar el dibujo
                ProductOriginalPhotoOrIllustration(product, accent, isDark, cornerRadius)
            } else {
                if (imageState !is AsyncImagePainter.State.Success) {
                    // Mancha suave del color del producto mientras llega la foto (sin animación infinita)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(
                                Brush.radialGradient(
                                    listOf(accent.copy(alpha = if (isDark) 0.22f else 0.14f), Color.Transparent)
                                )
                            )
                    )
                }
                Image(
                    painter = painter,
                    contentDescription = product.name,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
            }
        } else {
            ProductVectorIllustration(
                product = product,
                accentColor = accent,
                darkMode = isDark,
                cornerRadius = cornerRadius
            )
        }
    }
}

/** Respaldo: la foto original sin recortar; si tampoco carga, el dibujo del producto. */
@Composable
private fun ProductOriginalPhotoOrIllustration(product: Product, accent: Color, isDark: Boolean, cornerRadius: Dp) {
    val painter = rememberAsyncImagePainter(model = product.imageUrl, contentScale = ContentScale.Fit)
    val state by painter.state.collectAsState()
    if (state is AsyncImagePainter.State.Error) {
        ProductVectorIllustration(product = product, accentColor = accent, darkMode = isDark, cornerRadius = cornerRadius)
    } else {
        Image(
            painter = painter,
            contentDescription = product.name,
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(cornerRadius))
        )
    }
}

@Composable
fun ProductVectorIllustration(
    product: Product,
    accentColor: Color,
    modifier: Modifier = Modifier,
    darkMode: Boolean = MaterialTheme.colorScheme.background.red < 0.3f,
    cornerRadius: Dp = 12.dp
) {
    // Dibujo de respaldo: ícono del tipo de producto + su distintivo (ver ProductIllustration.kt)
    val fallbackBg = if (darkMode) Color(0xFF111927) else Tone.surfaceSubtle
    val icon = remember(product.sku) { productCategoryIcon(product) }
    val label = remember(product.badge) { shortBadge(product.badge) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = if (darkMode) 0.24f else 0.16f), fallbackBg)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val side = minOf(maxWidth, maxHeight)
        val showLabel = side >= 72.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .size(side * 0.5f)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (darkMode) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = product.name,
                    tint = accentColor,
                    modifier = Modifier.size(side * 0.3f)
                )
            }
            if (showLabel) {
                Spacer(Modifier.height(side * 0.06f))
                Text(
                    text = label,
                    color = if (darkMode) Color(0xFFE2E8F0) else Tone.ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}


@Composable
fun VerticalProductCard(
    product: Product,
    qtyInCart: Int,
    isArca: Boolean,
    darkMode: Boolean,
    onOpenDetail: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    heroTransition: Boolean = true
) {
    // Tarjeta al estilo de la app oficial: foto grande, nombre centrado,
    // precio a la izquierda y botón "+" redondo (o contador) a la derecha.
    val view = currentPlatformView()
    val accent = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary
    val softAccent = accent.copy(alpha = if (darkMode) 0.22f else 0.10f)
    var pressed by remember { mutableStateOf(false) }
    val cartFlight = LocalCartFlight.current
    var imageCenter by remember { mutableStateOf(Offset.Zero) }
    var showQuickView by remember { mutableStateOf(false) }
    val shareContext = currentPlatformContext()
    if (showQuickView) {
        QuickViewBottomSheet(
            product = product,
            isFavorite = isFavorite,
            accent = accent,
            onDismiss = { showQuickView = false },
            onAdd = { onIncrement() },
            onFavorite = { onToggleFavorite?.invoke() },
            onShare = { shareOnWhatsApp(shareContext, "Mira esto en BravoVa: *${product.name}* (${product.unit}) a ${formatRd(product.price)} 🛒") }
        )
    }
    val dataSaver = LocalDataSaver.current
    val addWithFlight = {
        if (!dataSaver) cartFlight.launch(product, imageCenter)
        onIncrement()
    }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "product_card_press_scale"
    )

    // Baymard (grocery): resaltar en la lista lo que ya está en el carrito; el contador solo
    // se pasa por alto en el teléfono. Borde suave del color de la tienda.
    val inCartBorder by animateColorAsState(
        targetValue = if (qtyInCart > 0) accent.copy(alpha = 0.55f) else Color.Transparent,
        label = "product_card_in_cart_border"
    )

    TranslucentCard(
        darkMode = darkMode,
        isArca = isArca,
        cornerRadius = 16.dp,
        modifier = modifier
            // 248 dp: con el selector de cantidad visible el contenido suma ~246 dp
            // (con 232 dp el selector quedaba cortado por abajo al agregar un producto)
            .height(248.dp)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .border(1.5.dp, inCartBorder, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        pressed = event.changes.any { it.pressed }
                    }
                }
            }
            .testTag("product_card_${product.sku}")
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInWindow()
                        imageCenter = Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height / 2f)
                    }
                    .combinedClickable(
                        onClick = {
                            performCrispHaptic(view, HapticStyle.TICK)
                            onOpenDetail()
                        },
                        onLongClick = {
                            // Mantener presionado: vista rápida (hoja nativa en iOS, Material en Android)
                            performCrispHaptic(view, HapticStyle.CONFIRM)
                            val native = PlatformExtras.presentQuickView(
                                title = product.name,
                                subtitle = product.unit,
                                price = formatRd(product.price),
                                imageUrl = product.imageUrl.orEmpty(),
                                isFavorite = isFavorite,
                                accentArgb = accent.toArgb(),
                                onAdd = { onIncrement() },
                                onFavorite = { onToggleFavorite?.invoke() },
                                onShare = { shareOnWhatsApp(shareContext, "Mira esto en BravoVa: *${product.name}* (${product.unit}) a ${formatRd(product.price)} 🛒") }
                            )
                            if (!native) showQuickView = true
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Esquinas concéntricas (WWDC25): 16 dp de la tarjeta − 10 dp de margen = 6 dp
                ProductArtworkView(product = product, cornerRadius = 6.dp, modifier = Modifier.fillMaxSize().then(if (heroTransition) Modifier.productHeroImage(product.sku) else Modifier))
                if (onToggleFavorite != null) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        // Área táctil de 40 dp (antes 28) con el mismo ícono de 18 dp; Apple pide ~44 pt
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                performCrispHaptic(view, HapticStyle.CONFIRM)
                                onToggleFavorite()
                            }
                            .padding(11.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDetail() }
            )
            Text(
                text = product.unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))
            // 2.0: precio + botón "+"; al agregar, el botón se vuelve un selector de cantidad
            // a todo el ancho de la tarjeta, con toques de 40 dp (Baymard: cantidad visible tras agregar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatRd(product.price),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (qtyInCart <= 0) {
                    // Android (Expressive): el círculo se vuelve "squircle" con un resorte mientras se presiona
                    val addInteraction = remember { MutableInteractionSource() }
                    val addPressed by addInteraction.collectIsPressedAsState()
                    val addCorner by animateIntAsState(
                        targetValue = if (com.example.platform.PlatformInfo.isAndroid && addPressed) 30 else 50,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                        label = "addShapeMorph"
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(percent = addCorner))
                            .background(softAccent)
                            .clickable(interactionSource = addInteraction, indication = LocalIndication.current) {
                                performCrispHaptic(view, HapticStyle.CONFIRM)
                                addWithFlight()
                            }
                            .testTag("add_${product.sku}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Agregar ${product.name}", tint = accent, modifier = Modifier.size(22.dp))
                    }
                }
            }
            // El selector de cantidad entra con un resorte (crece desde el botón "+")
            AnimatedVisibility(
                visible = qtyInCart > 0,
                enter = expandVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
            ) {
              Column {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                performCrispHaptic(view, HapticStyle.TICK)
                                onDecrement()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (qtyInCart == 1) Icons.Filled.DeleteOutline else Icons.Filled.Remove,
                            contentDescription = if (qtyInCart == 1) "Quitar ${product.name} del carrito" else "Quitar uno",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = qtyInCart.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "$qtyInCart en el carrito" }
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                performCrispHaptic(view, HapticStyle.CONFIRM)
                                addWithFlight()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Agregar uno",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
              }
            }
        }
    }
}

@Composable
fun SectionLineIcon(iconType: String, isArca: Boolean) {
    val vectorIcon: ImageVector = when (iconType) {
        "new_badge" -> Icons.Filled.NewReleases
        "oil_bottle" -> Icons.Filled.WaterDrop
        "water_soda" -> Icons.Filled.LocalDrink
        "plate_cutlery" -> Icons.Filled.Restaurant
        "baby_bottle" -> Icons.Filled.LocalDrink
        "wine_glass" -> Icons.Filled.LocalBar
        "cow_meat" -> Icons.Filled.LocalDining
        "salami_deli" -> Icons.Filled.Kitchen
        "fresh_produce" -> Icons.Filled.Eco
        "cleaning_spray" -> Icons.Filled.CleaningServices
        "milk_cheese" -> Icons.Filled.Egg
        "bakery_bread" -> Icons.Filled.BakeryDining
        "fish_seafood" -> Icons.Filled.SetMeal
        "frozen_ice" -> Icons.Filled.AcUnit
        "personal_care" -> Icons.Filled.Spa
        "snacks_candy" -> Icons.Filled.Cookie
        "kibble_bag" -> Icons.Filled.Pets
        "wet_food" -> Icons.Filled.Inventory2
        "vet_cross" -> Icons.Filled.MedicalServices
        "litter_box" -> Icons.Filled.CleaningServices
        "leash" -> Icons.Filled.Pets
        "pet_toy" -> Icons.Filled.SportsBaseball
        "exotic_pets" -> Icons.Filled.FlutterDash
        "pet_pharmacy" -> Icons.Filled.Medication
        else -> if (isArca) Icons.Filled.Pets else Icons.Filled.ShoppingCart
    }
    val accent = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vectorIcon,
            contentDescription = null, // decorativo: el texto de al lado ya lo describe
            tint = accent,
            modifier = Modifier.size(21.dp)
        )
    }
}

/**
 * Devuelve el ícono vectorial que corresponde a cada clave, para no depender de emojis
 * (todos con el mismo grosor de trazo).
 */
@Composable
fun BravoUnifiedIcon(
    iconKey: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val normalized = iconKey.trim().lowercase()
    val vector: ImageVector = when {
        normalized == "🛵" || normalized.contains("delivery") || normalized.contains("moto") || normalized.contains("ruta") -> Icons.AutoMirrored.Filled.DirectionsBike
        normalized == "📋" || normalized.contains("list") || normalized.contains("receipt") || normalized == "🧾" -> Icons.AutoMirrored.Filled.ReceiptLong
        normalized == "🏪" || normalized.contains("store") || normalized.contains("branch") -> Icons.Filled.Storefront
        normalized == "🏠" || normalized.contains("home") -> Icons.Outlined.Home
        normalized == "🔥" || normalized.contains("fire") || normalized.contains("hot") -> Icons.Filled.LocalFireDepartment
        normalized == "🎉" || normalized.contains("offer") || normalized.contains("deal") || normalized.contains("gift") -> Icons.Filled.LocalOffer
        normalized == "✓" || normalized == "✅" || normalized.contains("check") || normalized.contains("success") -> Icons.Filled.CheckCircle
        normalized == "🗑️" || normalized.contains("delete") || normalized.contains("trash") || normalized == "−" -> Icons.Filled.DeleteOutline
        normalized == "🌙" || normalized.contains("dark") || normalized.contains("moon") -> Icons.Filled.DarkMode
        normalized == "☀️" || normalized.contains("light") || normalized.contains("sun") -> Icons.Filled.LightMode
        normalized == "🔔" || normalized.contains("bell") || normalized.contains("notif") -> Icons.Filled.NotificationsActive
        normalized == "💳" || normalized.contains("card") || normalized.contains("pay") -> Icons.Filled.CreditCard
        normalized == "📦" || normalized.contains("box") || normalized.contains("pack") -> Icons.Filled.Inventory2
        normalized == "📍" || normalized.contains("pin") || normalized.contains("location") || normalized.contains("gps") -> Icons.Filled.LocationOn
        normalized == "✨" || normalized.contains("sparkle") || normalized.contains("ai") || normalized.contains("bravia") -> Icons.Filled.AutoAwesome
        normalized == "🐾" || normalized.contains("pet") || normalized.contains("arca") -> Icons.Filled.Pets
        normalized == "🛒" || normalized.contains("cart") -> Icons.Filled.ShoppingCart
        normalized == "🏆" || normalized.contains("trophy") || normalized.contains("points") -> Icons.Filled.EmojiEvents
        normalized == "💬" || normalized.contains("chat") -> Icons.AutoMirrored.Filled.Chat
        normalized == "📞" || normalized.contains("phone") -> Icons.Filled.Phone
        normalized.contains("shipping") || normalized.contains("truck") -> Icons.Filled.LocalShipping
        else -> Icons.Filled.Info
    }
    Icon(
        imageVector = vector,
        contentDescription = null, // decorativo: el texto de al lado ya lo describe
        tint = tint,
        modifier = modifier.size(size)
    )
}

private val SkuNoiseCleanupRegex = Regex(
    pattern = """\s*(?:\[\s*(?:SKU\s*:?\s*)?(?:BRV|ARC)-\d{3}\s*\]|\(\s*(?:SKU\s*:?\s*)?(?:BRV|ARC)-\d{3}\s*\)|\b(?:SKU\s*:?\s*)?(?:BRV|ARC)-\d{3}\b)""",
    option = RegexOption.IGNORE_CASE
)
private val SkusFooterCleanupRegex = Regex("""(?im)^\s*SKUS\s*:\s*\[.*?\]\s*$""")
private val NumberedItemMatchRegex = Regex("""^(?:\*\*)?(?:Paso\s*)?(\d+)[\.\)\:\-]\s*(?:\*\*)?\s*(.+)$""", RegexOption.IGNORE_CASE)
private val BulletItemMatchRegex = Regex("""^(?:[•\-\*▪▸])\s+(.+)$""")

private sealed class ParsedChatBlock {
    data class TopicHeader(val title: String) : ParsedChatBlock()
    data class EnumeratedPoint(val badgeNumber: Int, val content: String) : ParsedChatBlock()
    data class Paragraph(val content: String) : ParsedChatBlock()
}

private fun buildBoldAnnotatedLine(rawLine: String): androidx.compose.ui.text.AnnotatedString {
    val cleaned = rawLine.replace(SkuNoiseCleanupRegex, "").trim()
    return buildAnnotatedString {
        val boldParts = cleaned.split("**")
        for (i in boldParts.indices) {
            if (i % 2 == 1) {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                    append(boldParts[i])
                }
            } else {
                val italicParts = boldParts[i].split("*")
                for (j in italicParts.indices) {
                    if (j % 2 == 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(italicParts[j])
                        }
                    } else {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(italicParts[j])
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedChatText(
    rawText: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val sanitizedText = remember(rawText) {
        rawText
            .replace(SkusFooterCleanupRegex, "")
            .replace(SkuNoiseCleanupRegex, "")
            .trim()
    }

    val blocks = remember(sanitizedText) {
        val lines = sanitizedText.lines()
        val parsed = mutableListOf<ParsedChatBlock>()
        var runningBulletNumber = 1

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            val plainNoStars = trimmed.removePrefix("###").removePrefix("##").removePrefix("#").trim()
                .removeSurrounding("**").trim()

            val numberedMatch = NumberedItemMatchRegex.find(trimmed)
            val bulletMatch = BulletItemMatchRegex.find(trimmed)

            val isTopicHeader = (trimmed.startsWith("#") ||
                (trimmed.startsWith("**") && trimmed.endsWith("**") && plainNoStars.length <= 68) ||
                (plainNoStars.endsWith(":") && plainNoStars.length <= 68 && numberedMatch == null && bulletMatch == null))

            when {
                isTopicHeader -> {
                    runningBulletNumber = 1
                    parsed.add(ParsedChatBlock.TopicHeader(plainNoStars.removeSuffix(":")))
                }
                numberedMatch != null -> {
                    val explicitNum = numberedMatch.groupValues[1].toIntOrNull() ?: runningBulletNumber
                    val body = numberedMatch.groupValues[2].trim().removePrefix("**").trim()
                    runningBulletNumber = explicitNum + 1
                    parsed.add(ParsedChatBlock.EnumeratedPoint(explicitNum, body))
                }
                bulletMatch != null -> {
                    val body = bulletMatch.groupValues[1].trim()
                    val num = runningBulletNumber++
                    parsed.add(ParsedChatBlock.EnumeratedPoint(num, body))
                }
                else -> {
                    parsed.add(ParsedChatBlock.Paragraph(trimmed))
                }
            }
        }
        parsed
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is ParsedChatBlock.TopicHeader -> {
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(
                            color = primaryColor.copy(alpha = 0.22f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                        )
                        Text(
                            text = block.title,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            lineHeight = 19.sp,
                            color = textColor
                        )
                    }
                }
                is ParsedChatBlock.EnumeratedPoint -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryColor.copy(alpha = 0.07f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = block.badgeNumber.toString(),
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = buildBoldAnnotatedLine(block.content),
                            fontFamily = PlusJakartaSans,
                            // Cuerpo en peso medio: así las **negritas** del texto sí destacan
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            fontSize = 13.5.sp,
                            lineHeight = 19.5.sp,
                            letterSpacing = 0.1.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is ParsedChatBlock.Paragraph -> {
                    Text(
                        text = buildBoldAnnotatedLine(block.content),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        fontSize = 13.8.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.1.sp
                    )
                }
            }
        }
    }
}

private val BravoIridescentSpectrum = listOf(
    Color(0xFFD9232A),
    Tone.warningBright,
    Tone.successBright,
    Color(0xFF06B6D4),
    Color(0xFF3B82F6),
    Color(0xFFD9232A)
)

fun Modifier.bravoAiAura(
    cornerRadius: Dp = 999.dp,
    enabled: Boolean = true,
    borderWidth: Dp = 1.6.dp,
    haloAlpha: Float = 0.26f
): Modifier = if (!enabled) {
    this
} else {
    composed {
        val infiniteTransition = rememberInfiniteTransition(label = "bravo_ai_aura")
        val shift by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ai_spectrum_shift"
        )
        this.drawWithCache {
            val radiusPx = if (cornerRadius >= 900.dp) size.minDimension / 2f else cornerRadius.toPx()
            val strokePx = borderWidth.toPx()
            val haloStrokePx = strokePx * 3.4f
            val travel = size.width * 1.4f
            val offsetX = (shift * travel) - (travel * 0.5f)
            val borderBrush = Brush.linearGradient(
                colors = BravoIridescentSpectrum,
                start = Offset(offsetX, 0f),
                end = Offset(offsetX + size.width, size.height)
            )
            val haloBrush = Brush.linearGradient(
                colors = BravoIridescentSpectrum.map { it.copy(alpha = haloAlpha) },
                start = Offset(offsetX, 0f),
                end = Offset(offsetX + size.width, size.height)
            )
            onDrawWithContent {
                drawContent()
                drawRoundRect(
                    brush = haloBrush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = haloStrokePx)
                )
                drawRoundRect(
                    brush = borderBrush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokePx)
                )
            }
        }
    }
}

@Composable
fun BravoAiPlasmaOrb(
    size: Dp = 36.dp,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "bravo_ai_plasma_orb")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 2600 else 4400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbPhase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 900 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )
    Canvas(
        modifier = modifier
            .size(size)
            .testTag("bravo_ai_plasma_orb")
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = (w.coerceAtMost(h) * 0.48f) * pulse

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFD9232A).copy(alpha = 0.28f),
                    Color(0xFF06B6D4).copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = r * 1.15f
            ),
            radius = r * 1.15f,
            center = Offset(cx, cy)
        )

        val lobe1 = Offset(cx + cos(phase) * r * 0.24f, cy + sin(phase) * r * 0.24f)
        val lobe2 = Offset(cx - cos(phase * 0.8f) * r * 0.22f, cy - sin(phase * 0.8f) * r * 0.22f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Tone.warningBright.copy(alpha = 0.72f), Color(0xFFD9232A).copy(alpha = 0.35f), Color.Transparent),
                center = lobe1,
                radius = r * 0.78f
            ),
            radius = r * 0.78f,
            center = lobe1
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.62f), Tone.successBright.copy(alpha = 0.28f), Color.Transparent),
                center = lobe2,
                radius = r * 0.72f
            ),
            radius = r * 0.72f,
            center = lobe2
        )

        val starR = r * 0.46f
        val innerR = starR * 0.28f
        val starPath = Path().apply {
            moveTo(cx, cy - starR)
            quadraticTo(cx + innerR * 0.36f, cy - innerR * 0.36f, cx + starR, cy)
            quadraticTo(cx + innerR * 0.36f, cy + innerR * 0.36f, cx, cy + starR)
            quadraticTo(cx - innerR * 0.36f, cy + innerR * 0.36f, cx - starR, cy)
            quadraticTo(cx - innerR * 0.36f, cy - innerR * 0.36f, cx, cy - starR)
            close()
        }
        drawPath(path = starPath, color = Color.White.copy(alpha = 0.94f))
    }
}


@Composable
fun TranslucentBottomDock(
    currentScreen: AppScreen,
    isArca: Boolean,
    darkMode: Boolean,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier,
    minimized: Boolean = false,
    onSearch: () -> Unit = {}
) {
    val view = currentPlatformView()
    val activeTab = when (currentScreen) {
        AppScreen.BRAVISIMA -> AppScreen.ACCOUNT
        AppScreen.CATALOG, AppScreen.CATEGORY, AppScreen.PRODUCT -> AppScreen.CATALOG
        AppScreen.HOME, AppScreen.CART, AppScreen.DELIVERY -> AppScreen.HOME
        AppScreen.BRAVIA -> AppScreen.BRAVIA
        AppScreen.ACCOUNT, AppScreen.ACCOUNT_SUB -> AppScreen.ACCOUNT
    }
    val activeColor = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary
    val inactiveColor = if (darkMode) Tone.muted else Color(0xFF1E293B)
    val useRealHaze = LocalTranslucentHazeState.current != null && DeviceSupportsGpuBlur
    val dockBg = if (useRealHaze) Color.Transparent else uniformMenuTranslucentContainerColor(darkMode)
    val borderBrush = uniformMenuTranslucentBorderBrush(activeColor, darkMode)
    val sheenBrush = uniformMenuTranslucentSheenBrush(darkMode, activeColor)

    // 2.0: cuatro pestañas (Bravísima pasó a Cuenta) + Búsqueda aparte
    val tabCatalog = stringResource(Res.string.tab_catalog)
    val tabHome = stringResource(Res.string.tab_home)
    val tabBravia = stringResource(Res.string.tab_bravia)
    val tabAccount = stringResource(Res.string.tab_account)
    val tabSearch = stringResource(Res.string.tab_search)
    val navItems = remember(tabCatalog, tabHome, tabBravia, tabAccount) {
        listOf(
            Triple(AppScreen.HOME, Icons.Outlined.Home, tabHome),
            Triple(AppScreen.CATALOG, Icons.Outlined.GridView, tabCatalog),
            Triple(AppScreen.BRAVIA, Icons.Outlined.AutoAwesome, tabBravia),
            Triple(AppScreen.ACCOUNT, Icons.Outlined.PersonOutline, tabAccount)
        )
    }

    if (nativeLiquidGlassAvailable()) {
        // iOS 26: barra de pestañas con el vidrio líquido nativo de Apple
        val selectedIndex = navItems.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)
        NativeGlassDock(
            symbols = listOf("house", "square.grid.2x2", "sparkles", "person.crop.circle"),
            labels = navItems.map { it.third },
            selectedIndex = selectedIndex,
            accent = activeColor,
            darkMode = darkMode,
            onSelect = { i ->
                val screen = navItems[i].first
                if (screen == AppScreen.BRAVIA) HapticFeedbackManager.onAssistantTrigger(view)
                else HapticFeedbackManager.onNavigationClick(view)
                onNavigate(screen)
            },
            modifier = modifier.fillMaxWidth().height(74.dp),
            minimized = minimized,
            onSearch = {
                HapticFeedbackManager.onNavigationClick(view)
                onSearch()
            }
        )
        return
    }

    if (PlatformInfo.isAndroid) {
        // Android: barra de navegación Material 3 con colores dinámicos del dispositivo
        NavigationBar(
            modifier = modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            navItems.forEach { (screen, icon, label) ->
                val selected = screen == activeTab
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (screen == AppScreen.BRAVIA) HapticFeedbackManager.onAssistantTrigger(view)
                        else HapticFeedbackManager.onNavigationClick(view)
                        onNavigate(screen)
                    },
                    icon = { Icon(icon, contentDescription = null) },
                    label = { Text(label, maxLines = 1, fontSize = 12.sp) },
                    modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                )
                if (screen == AppScreen.CATALOG) {
                    // Búsqueda: su propio botón en la barra (como la pestaña de búsqueda de iOS)
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            HapticFeedbackManager.onNavigationClick(view)
                            onSearch()
                        },
                        icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        label = { Text(tabSearch, maxLines = 1, fontSize = 12.sp) },
                        modifier = Modifier.testTag("nav_search")
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(58.dp)
    ) {
        LiquidGlass(
            darkMode = darkMode,
            accent = activeColor,
            cornerRadius = 29.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            // Gota de vidrio que se desliza bajo la pestaña activa (Liquid Glass)
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                val itemWidth = maxWidth / navItems.size
                val activeIndex = navItems.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)
                val dropX by animateDpAsState(
                    targetValue = itemWidth * activeIndex,
                    animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
                    label = "liquidDockDrop"
                )
                Box(
                    modifier = Modifier
                        .offset(x = dropX)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(activeColor.copy(alpha = if (darkMode) 0.20f else 0.12f))
                        .border(
                            0.9.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = if (darkMode) 0.42f else 0.85f), activeColor.copy(alpha = 0.22f))
                            ),
                            RoundedCornerShape(999.dp)
                        )
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { (screen, icon, contentDesc) ->
                    val isSelected = activeTab == screen
                    val isSearchAi = screen == AppScreen.BRAVIA

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.16f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "navScale"
                    )
                    val pillAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "navPillAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                if (isSearchAi) {
                                    HapticFeedbackManager.onAssistantTrigger(view)
                                } else {
                                    HapticFeedbackManager.onNavigationClick(view)
                                }
                                onNavigate(screen)
                            }
                            .testTag("nav_${screen.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.TopEnd,
                            modifier = Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                        ) {
                            if (screen == AppScreen.BRAVISIMA) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_bravisima_card),
                                    contentDescription = contentDesc,
                                    tint = if (isSelected) activeColor else inactiveColor,
                                    modifier = Modifier.size(25.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = contentDesc,
                                    tint = if (isSelected) activeColor else inactiveColor,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            if (isSearchAi) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = BravoAmber,
                                    modifier = Modifier
                                        .size(11.dp)
                                        .offset(x = 4.dp, y = (-3).dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pantalla verde de pedido confirmado, con la marca de verificación animada
 * y una pequeña melodía de celebración.
 */
@Composable
fun OrderSuccessCelebrationOverlay(
    orderId: String,
    totalAmountRd: Int,
    branchName: String,
    etaMinutes: Int,
    distanceKmLabel: String,
    onContinueToTracking: () -> Unit
) {
    val view = currentPlatformView()
    val circleScale = remember { Animatable(0.2f) }
    val checkProgress = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0.45f) }
    val confettiProgress = remember { Animatable(0f) }
    val pulseTransition = rememberInfiniteTransition(label = "greenCelebrationPulse")
    val ringScale by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )
    // Fondo oscuro fijo con acento esmeralda (antes: verde plano chillón)
    val emerald = Color(0xFF10B981)
    val emeraldButton = Color(0xFF047857) // texto blanco encima: contraste 5.5:1 (WCAG AA)
    val screenBg = MaterialTheme.colorScheme.background
    val onBg = MaterialTheme.colorScheme.onBackground
    val onBgMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val confettiSeeds = remember(orderId) {
        val colors = listOf(BravoRed, BravoAmber, Tone.successBright, Color.White)
        List(24) { idx ->
            val angle = ((idx * 137.5f) % 360f) * (PI.toFloat() / 180f)
            val speed = 0.35f + ((idx * 29) % 100) / 150f
            Triple(angle, speed, colors[idx % colors.size])
        }
    }

    LaunchedEffect(orderId) {
        HapticFeedbackManager.onOrderCelebration(view)
        flashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 280))
    }

    LaunchedEffect(orderId) {
        confettiProgress.animateTo(1f, animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(orderId) {
        circleScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        checkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
        )
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 360)
        )
        kotlinx.coroutines.delay(2400L)
        onContinueToTracking()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Pinta también detrás de la barra de estado: sin franja negra arriba
            .drawBehind {
                val extra = 400f
                drawRect(
                    color = screenBg,
                    topLeft = Offset(0f, -extra),
                    size = Size(size.width, size.height + extra * 2)
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                HapticFeedbackManager.onConfirmAction(view)
                onContinueToTracking()
            }
            .testTag("order_celebration_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = confettiProgress.value
            val cx = size.width * 0.5f
            val cy = size.height * 0.34f
            confettiSeeds.forEachIndexed { idx, (angle, speed, color) ->
                val dist = t * size.width * 0.72f * speed
                val gravity = t * t * size.height * 0.42f
                val px = cx + cos(angle) * dist
                val py = cy + sin(angle) * dist * 0.68f + gravity
                val alpha = (1f - t).coerceIn(0f, 1f)
                val rot = t * 540f + idx * 24f
                rotate(degrees = rot, pivot = Offset(px, py)) {
                    drawRoundRect(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(px - 5.dp.toPx(), py - 3.dp.toPx()),
                        size = Size(10.dp.toPx(), 6.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
            if (flashAlpha.value > 0.01f) {
                drawRect(color = emerald.copy(alpha = flashAlpha.value * 0.35f))
            }
        }

        // Anillos que se expanden
        Box(
            modifier = Modifier
                .size(250.dp)
                .graphicsLayer {
                    scaleX = ringScale
                    scaleY = ringScale
                    alpha = 0.10f
                }
                .clip(CircleShape)
                .background(emerald)
        )
        Box(
            modifier = Modifier
                .size(188.dp)
                .graphicsLayer {
                    scaleX = (ringScale * 0.92f)
                    scaleY = (ringScale * 0.92f)
                    alpha = 0.14f
                }
                .clip(CircleShape)
                .background(emerald)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .graphicsLayer {
                        scaleX = circleScale.value
                        scaleY = circleScale.value
                    }
                    .clip(CircleShape)
                    .background(emerald.copy(alpha = 0.16f))
                    .border(3.5.dp, emerald, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(68.dp)) {
                    val progress = checkProgress.value
                    val w = size.width
                    val h = size.height
                    val start = Offset(w * 0.18f, h * 0.52f)
                    val mid = Offset(w * 0.43f, h * 0.76f)
                    val end = Offset(w * 0.84f, h * 0.26f)

                    val path = Path().apply {
                        moveTo(start.x, start.y)
                        val firstLegFraction = (progress / 0.42f).coerceIn(0f, 1f)
                        lineTo(
                            start.x + (mid.x - start.x) * firstLegFraction,
                            start.y + (mid.y - start.y) * firstLegFraction
                        )
                        if (progress > 0.42f) {
                            val secondLegFraction = ((progress - 0.42f) / 0.58f).coerceIn(0f, 1f)
                            lineTo(
                                mid.x + (end.x - mid.x) * secondLegFraction,
                                mid.y + (end.y - mid.y) * secondLegFraction
                            )
                        }
                    }

                    drawPath(
                        path = path,
                        color = emerald,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 9.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
            ) {
                Text(
                    text = "¡Pedido Confirmado!",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    color = onBg
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Orden $orderId • RD$$totalAmountRd",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = onBgMuted
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, emerald.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Ubicación real",
                            tint = emerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "$branchName • $distanceKmLabel",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = onBg
                            )
                            Text(
                                text = "Llegada estimada por GPS: ~$etaMinutes min",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = onBgMuted
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                Surface(
                    onClick = {
                        HapticFeedbackManager.onConfirmAction(view)
                        onContinueToTracking()
                    },
                    shape = CircleShape,
                    color = emeraldButton,
                    modifier = Modifier.testTag("celebration_continue_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "Ver Mapa y Rastreo en Vivo",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PredictiveCatalogSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    catalogProducts: List<Product>,
    onSelectPredictedProduct: (Product) -> Unit,
    placeholderText: String,
    darkMode: Boolean,
    isArca: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {},
    trailingActionContent: (@Composable () -> Unit)? = null,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    /** Ícono SF Symbol del botón derecho en el buscador nativo de iOS (vacío = sin botón). */
    nativeTrailingSymbol: String = "",
    onNativeTrailing: () -> Unit = {}
) {
    val primaryColor = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary
    val view = currentPlatformView()

    // Mientras se escribe, busca el producto del catálogo que mejor completa el texto
    val predictedProduct = remember(query, catalogProducts) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            null
        } else {
            val lower = clean.lowercase()
            catalogProducts.firstOrNull { it.name.lowercase().startsWith(lower) }
                ?: catalogProducts.firstOrNull { prod ->
                    prod.name.lowercase().split(" ").any { word -> word.startsWith(lower) }
                }
                ?: catalogProducts.firstOrNull { it.name.lowercase().contains(lower) }
        }
    }

    // Sugerencia en gris con el resto del nombre del producto
    val inlineGhostSuffix = remember(query, predictedProduct) {
        val clean = query
        if (clean.isBlank() || predictedProduct == null) {
            ""
        } else {
            val prodName = predictedProduct.name
            if (prodName.startsWith(clean, ignoreCase = true) && prodName.length > clean.length) {
                prodName.substring(clean.length)
            } else {
                "  —  ${predictedProduct.name}"
            }
        }
    }

    if (nativeLiquidGlassAvailable()) {
        // iOS 26+: el buscador es un campo nativo de vidrio líquido. Quien pide el foco con
        // focusRequester lo recibe aquí y se reenvía al campo nativo para abrir el teclado.
        var focusSignal by remember { mutableIntStateOf(0) }
        Box(modifier = modifier.fillMaxWidth().height(50.dp).testTag(testTag)) {
            if (focusRequester != null) {
                Box(
                    Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { if (it.isFocused) focusSignal++ }
                        .focusable()
                )
            }
            NativeGlassSearchField(
                text = query,
                placeholder = placeholderText,
                accent = primaryColor,
                darkMode = darkMode,
                trailingSymbol = nativeTrailingSymbol,
                focusSignal = focusSignal,
                onChange = onQueryChange,
                onSubmit = { if (predictedProduct != null) onSelectPredictedProduct(predictedProduct) },
                onFocus = onFocusChanged,
                onTrailing = onNativeTrailing,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TranslucentCard(
            darkMode = darkMode,
            isArca = isArca,
            cornerRadius = 999.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Buscar",
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholderText,
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (predictedProduct != null && inlineGhostSuffix.isNotEmpty()) {
                        // Texto sugerido detrás de lo que escribe el usuario
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = Color.Transparent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) {
                                    append(query)
                                }
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = if (darkMode) 0.55f else 0.48f
                                        ),
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    append(inlineGhostSuffix)
                                }
                            },
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(primaryColor),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (predictedProduct != null) {
                                    onQueryChange(predictedProduct.name)
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                            .onFocusChanged { onFocusChanged(it.isFocused) }
                            .testTag(testTag)
                    )
                }

                // 2.0: campo limpio. En lugar de la pastilla con precio y calorías, solo un botón
                // pequeño para completar con la sugerencia (el texto fantasma ya la muestra).
                if (query.isNotBlank() && predictedProduct != null && !predictedProduct.name.equals(query.trim(), ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                performCrispHaptic(view, HapticStyle.TICK)
                                onQueryChange(predictedProduct.name)
                            }
                            .testTag("${testTag}_inline_prediction"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NorthWest,
                            contentDescription = "Completar con ${predictedProduct.name}",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                            .clickable {
                                performCrispHaptic(view, HapticStyle.TICK)
                                onQueryChange("")
                            }
                            .testTag("${testTag}_clear"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Limpiar búsqueda",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (trailingActionContent != null) {
                    trailingActionContent()
                }
            }
        }
    }
}

/**
 * Barra roja del carrito, como en la app oficial: total a la izquierda y "Ver carrito" a la derecha.
 * Rebota cada vez que se agrega un producto.
 */
@Composable
fun CartSummaryBar(
    itemCount: Int,
    total: Double,
    isArca: Boolean,
    pulseTrigger: Long,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = currentPlatformView()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger == 0L) return@LaunchedEffect
        scale.snapTo(1.05f)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    val gradient = if (isArca) listOf(ArcaEmerald, ArcaTeal) else BravoCtaGradient
    if (nativeLiquidGlassAvailable()) {
        // iOS 26+: barra de vidrio nativo teñida de rojo
        NativeGlassCartBar(
            count = itemCount,
            total = formatRd(total),
            accent = gradient.last(),
            darkMode = MaterialTheme.colorScheme.background.luminance() < 0.4f,
            onTap = {
                performCrispHaptic(view, HapticStyle.CONFIRM)
                onOpenCart()
            },
            modifier = modifier.fillMaxWidth().height(60.dp).testTag("cart_summary_bar")
        )
        return
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .shadow(8.dp, shape, spotColor = gradient.last().copy(alpha = 0.35f))
            .clip(shape)
            .background(Brush.horizontalGradient(gradient))
            .clickable {
                performCrispHaptic(view, HapticStyle.CONFIRM)
                onOpenCart()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("cart_summary_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(badge = {
            Badge(containerColor = Color.White, contentColor = gradient.last()) {
                Text(itemCount.toString(), fontWeight = FontWeight.Bold)
            }
        }) {
            val cartFlight = LocalCartFlight.current
            Icon(
                Icons.Outlined.ShoppingCart,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    cartFlight.target = Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height / 2f)
                }
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = formatRd(total),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Ver carrito",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.22f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/** Modo ahorro de datos: imágenes más livianas y menos animaciones. Lo provee App.kt. */
val LocalDataSaver = androidx.compose.runtime.compositionLocalOf { false }
