package com.example

import com.example.ui.components.nativeLiquidGlassAvailable
import com.example.ui.components.NativeGlassToast
import com.example.ui.theme.formatRd
import com.example.ui.components.shareOnWhatsApp
import com.example.ui.components.ProductHeroScopes
import com.example.ui.components.LocalProductHeroScopes
import com.example.ui.components.LocalDataSaver
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.components.dismissKeyboardOnOutsideTouch
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.delay
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.example.model.AccountSubScreen
import com.example.model.AppScreen
import com.example.model.StoreMode
import com.example.notifications.BravoPushNotificationCenter
import com.example.platform.*
import com.example.security.BiometricCriticalOperation
import com.example.security.BiometricPromptRequest
import com.example.ui.components.AppStartupSkeletonScreen
import com.example.ui.components.BarcodeScannerModal
import com.example.ui.components.BravoUnifiedIcon
import com.example.ui.components.DeviceSupportsGpuBlur
import com.example.ui.components.CartSummaryBar
import com.example.ui.components.CartFlightOverlay
import com.example.ui.components.PawEasterEggOverlay
import com.example.ui.components.HapticFeedbackManager
import com.example.ui.components.LocalTranslucentHazeState
import com.example.ui.components.OrderSuccessCelebrationOverlay
import com.example.ui.components.TranslucentBottomDock
import com.example.ui.components.UniformBackdropBlurRadius
import com.example.ui.components.realGpuBlur
import com.example.ui.components.translucentBackdropEffect
import com.example.ui.components.translucentBackdropSource
import com.example.ui.screens.AccountGridScreen
import com.example.ui.screens.AccountSubScreenView
import com.example.ui.screens.BiometricOrderPromptDialog
import com.example.ui.screens.BravIaChatScreen
import com.example.ui.screens.BravisimaScreen
import com.example.ui.screens.BravoGoogleAuthScreen
import com.example.ui.screens.CartCheckoutScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.CategoryGridScreen
import com.example.ui.screens.DeliveryTrackingScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.authUserFlow
import com.example.ui.screens.initializeVerifiedGoogleFallbackState
import com.example.ui.screens.signOutGoogleUser
import com.example.ui.screens.verifiedGoogleSessionFallback
import com.example.ui.theme.ArcaEmerald
import com.example.ui.theme.ArcaTeal
import com.example.ui.theme.BravoAmber
import com.example.ui.theme.BravoVaTheme
import com.example.ui.theme.PlusJakartaSans
import com.example.viewmodel.BravoViewModel
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Shared entry point for Android (MainActivity) and iOS (MainViewController).
 */
@Composable
fun BravoRoot(
    initialFcmScreen: String? = null,
    initialFcmSku: String? = null
) {
    setSingletonImageLoaderFactory { ctx ->
        ImageLoader.Builder(ctx)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
    val instantPrefs = remember { sharedPrefs("bravova_instant_prefs") }
    val initialGuestPref = remember { instantPrefs.getBoolean("guest_mode_enabled", false) }
    val appPrefs = remember { sharedPrefs("bravova_prefs") }
    LaunchedEffect(Unit) { initializeVerifiedGoogleFallbackState(AppContext.instance) }

    val systemDark = isSystemInDarkTheme()
    var authScreenDarkMode by remember(systemDark) {
        mutableStateOf(systemDark)
    }
        val currentUser by authUserFlow().collectAsStateWithLifecycle(initialValue = AuthPlatform.currentUser())
    val fallbackGoogleActive by verifiedGoogleSessionFallback.collectAsStateWithLifecycle()
    val user = currentUser
    var isGuestSession by rememberSaveable { mutableStateOf(initialGuestPref) }

    // Bienvenida de la primera vez (tienda y sucursal) antes de la pantalla de entrar
    var welcomeDone by rememberSaveable { mutableStateOf(instantPrefs.getBoolean("welcome_done", false)) }

    if (user == null && !isGuestSession && !fallbackGoogleActive && !welcomeDone) {
        BravoVaTheme(darkTheme = authScreenDarkMode, isArcaStore = false, paletteId = "material_you") {
            com.example.ui.screens.WelcomeFlow { store, branch ->
                instantPrefs.edit()
                    .putBoolean("welcome_done", true)
                    .putString("welcome_store", store)
                    .putString("welcome_branch", branch)
                    .apply()
                welcomeDone = true
            }
        }
    } else if (user == null && !isGuestSession && !fallbackGoogleActive) {
        BravoVaTheme(
            darkTheme = authScreenDarkMode,
            isArcaStore = false,
            paletteId = "material_you"
        ) {
            BravoGoogleAuthScreen(
                darkMode = authScreenDarkMode,
                isMaterialYouPalette = true,
                onToggleDarkMode = {
                    val nextDark = !authScreenDarkMode
                    authScreenDarkMode = nextDark
                    appPrefs.edit().putBoolean("dark_mode", nextDark).apply()
                },
                onContinueAsGuest = {
                    isGuestSession = true
                }
            )
        }
    } else {
        val sessionKey = user?.uid ?: if (fallbackGoogleActive) "google_verified_user" else "guest_local_session"
        key(sessionKey) {
            val viewModel: BravoViewModel = viewModel(key = sessionKey) { BravoViewModel() }
            val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
            val activeStore by viewModel.activeStore.collectAsStateWithLifecycle()
            val selectedPaletteId by viewModel.selectedPaletteId.collectAsStateWithLifecycle()

            LaunchedEffect(systemDark) {
                viewModel.onSystemDarkThemeChanged(systemDark)
            }

            // Aplica una sola vez lo elegido en la bienvenida. Espera a que termine el arranque:
            // si no, la sucursal guardada que se carga al iniciar podía pisar la que eligió la persona.
            val welcomeBooting by viewModel.isAppBooting.collectAsStateWithLifecycle()
            LaunchedEffect(welcomeBooting) {
                if (welcomeBooting) return@LaunchedEffect
                val store = instantPrefs.getString("welcome_store", null)
                val branch = instantPrefs.getString("welcome_branch", null)
                if (store != null || branch != null) {
                    if (store == "ARCA") viewModel.selectStore(com.example.model.StoreMode.ARCA)
                    if (branch != null) viewModel.setActiveBranch(branch)
                    instantPrefs.edit().remove("welcome_store").remove("welcome_branch").apply()
                }
            }

            LaunchedEffect(darkMode) {
                authScreenDarkMode = darkMode
            }

            LaunchedEffect(user?.uid, user?.displayName, user?.email, isGuestSession, fallbackGoogleActive) {
                when {
                    user != null -> {
                        isGuestSession = false
                        viewModel.bindAuthenticatedUser(user.uid, user.displayName, user.email)
                    }
                    fallbackGoogleActive -> {
                        isGuestSession = false
                        val savedName = instantPrefs.getString("google_fallback_name", "Cliente Google BravoVa")
                        val savedEmail = instantPrefs.getString("google_fallback_email", "cliente.google@bravova.do")
                        viewModel.bindAuthenticatedUser("google_verified_user", savedName, savedEmail)
                    }
                    isGuestSession -> {
                        viewModel.bindGuestUser()
                    }
                }
            }

            LaunchedEffect(initialFcmScreen, initialFcmSku) {
                viewModel.handleFcmDeepLinkIntent(initialFcmScreen, initialFcmSku)
            }
            // Atajos del ícono, widgets y enlaces bravova:// (ver platform/ExternalLinks.kt)
            val externalTarget = com.example.platform.ExternalLinks.pending
            LaunchedEffect(externalTarget) {
                val link = com.example.platform.ExternalLinks.consume() ?: return@LaunchedEffect
                when (com.example.platform.ExternalLinks.targetOf(link)) {
                    "buscar" -> {
                        viewModel.navigateTo(AppScreen.HOME)
                        com.example.ui.components.SearchFocusRequest.request()
                    }
                    "repetir" -> {
                        val last = viewModel.orders.value.firstOrNull()
                        if (last != null) viewModel.reorderPastOrder(last.id) else viewModel.navigateTo(AppScreen.HOME)
                    }
                    "bravisima" -> viewModel.navigateTo(AppScreen.BRAVISIMA)
                    "ofertas" -> viewModel.navigateTo(AppScreen.CATALOG)
                    "pedido" -> viewModel.navigateTo(AppScreen.DELIVERY)
                    "bravia" -> {
                        // bravova://bravia?q=… abre Brav IA y envía la pregunta
                        viewModel.navigateTo(AppScreen.BRAVIA)
                        com.example.platform.ExternalLinks.param(link, "q")?.let { viewModel.sendCustomChatMsg(it) }
                    }
                    else -> Unit
                }
            }

            BravoVaTheme(
                darkTheme = darkMode,
                isArcaStore = activeStore == StoreMode.ARCA,
                paletteId = selectedPaletteId
            ) {
                BravoVaApp(
                    viewModel = viewModel,
                    onExitSession = {
                        viewModel.exitGuestMode()
                        isGuestSession = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BravoVaApp(
    viewModel: BravoViewModel,
    onExitSession: () -> Unit = {}
) {
    val isAppBooting by viewModel.isAppBooting.collectAsStateWithLifecycle()
    val activeStore by viewModel.activeStore.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeCategory by viewModel.activeCategory.collectAsStateWithLifecycle()
    val activeProductSku by viewModel.activeProductSku.collectAsStateWithLifecycle()
    val activeAccountSub by viewModel.activeAccountSub.collectAsStateWithLifecycle()
    val homeSearchQuery by viewModel.homeSearchQuery.collectAsStateWithLifecycle()
    val catalogFilterQuery by viewModel.catalogFilterQuery.collectAsStateWithLifecycle()
    val categorySearchQuery by viewModel.categorySearchQuery.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val bravisimaBalance by viewModel.accumulatedBravisimaBalance.collectAsStateWithLifecycle()
    val bravisimaCard by viewModel.bravisimaCard.collectAsStateWithLifecycle()
    val bravisimaMovements by viewModel.bravisimaMovements.collectAsStateWithLifecycle()
    val isSavingBravisima by viewModel.isSavingBravisima.collectAsStateWithLifecycle()
    val redeemBravisima by viewModel.redeemBravisimaInCart.collectAsStateWithLifecycle()
    val isScannerOpen by viewModel.isScannerOpen.collectAsStateWithLifecycle()
    val scannedSku by viewModel.scannedSku.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val cartPulseTrigger by viewModel.cartPulseTrigger.collectAsStateWithLifecycle()
    val lastAddedCartFeedback by viewModel.lastAddedCartFeedback.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val darkModeOverride by viewModel.darkModeOverride.collectAsStateWithLifecycle()
    val checkoutExtras by viewModel.checkoutExtras.collectAsStateWithLifecycle()
    val orderExtras by viewModel.orderExtras.collectAsStateWithLifecycle()
    val dataSaver by viewModel.dataSaver.collectAsStateWithLifecycle()
    val priceAlerts by viewModel.priceAlerts.collectAsStateWithLifecycle()
    val platformContext = currentPlatformContext()
    val selectedPaletteId by viewModel.selectedPaletteId.collectAsStateWithLifecycle()
    val pushNotifications by viewModel.pushNotifications.collectAsStateWithLifecycle()
    val fcmToken by viewModel.fcmToken.collectAsStateWithLifecycle()
    val fcmTopics by viewModel.fcmTopics.collectAsStateWithLifecycle()
    val pushFeed by viewModel.pushFeed.collectAsStateWithLifecycle()
    val activeBranch by viewModel.activeBranch.collectAsStateWithLifecycle()
    val userProfile by viewModel.profile.collectAsStateWithLifecycle()
    val paymentMethods by viewModel.paymentMethods.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val shoppingLists by viewModel.lists.collectAsStateWithLifecycle()
    val aiPopulatingListId by viewModel.aiPopulatingListId.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val orderRatings by viewModel.orderRatings.collectAsStateWithLifecycle()
    val trackedOrder by viewModel.trackedOrder.collectAsStateWithLifecycle()
    val isUpdatingOrderStatus by viewModel.isUpdatingOrderStatus.collectAsStateWithLifecycle()
    val celebrationOrder by viewModel.celebrationOrder.collectAsStateWithLifecycle()
    val selectedOfferDayOffset by viewModel.selectedOfferDayOffset.collectAsStateWithLifecycle()
    val realLocationMetrics by viewModel.realLocationMetrics.collectAsStateWithLifecycle()
    val driverTelemetry by viewModel.driverTelemetry.collectAsStateWithLifecycle()
    val driverChatMessages by viewModel.driverChatMessages.collectAsStateWithLifecycle()
    val isDriverTyping by viewModel.isDriverTyping.collectAsStateWithLifecycle()
    val liveDeliveryBanner by viewModel.liveDeliveryBanner.collectAsStateWithLifecycle()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsStateWithLifecycle()
    val isGuestMode by viewModel.isGuestMode.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val cacheStatusLabel by viewModel.cacheStatusLabel.collectAsStateWithLifecycle()
    val sections by viewModel.activeStoreSections.collectAsStateWithLifecycle()
    val activeProducts by viewModel.activeStoreProducts.collectAsStateWithLifecycle()
    val allCatalogProducts by viewModel.allCachedProducts.collectAsStateWithLifecycle()
    // "Lo de siempre": productos de los pedidos más recientes, sin repetir
    val recentProducts = remember(orders, activeProducts) {
        val bySku = activeProducts.associateBy { it.sku }
        orders.flatMap { it.skus }.distinct().mapNotNull { bySku[it] }.take(12)
    }
    val chatMessages by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isAiGenerating by viewModel.isAiGenerating.collectAsStateWithLifecycle()
    val budgetAmount by viewModel.targetBudgetInChat.collectAsStateWithLifecycle()
    val npuStatus by viewModel.npuStatus.collectAsStateWithLifecycle()
    val pendingChatImageUri by viewModel.pendingChatImageUri.collectAsStateWithLifecycle()
    val submittedSuggestions by viewModel.submittedSuggestions.collectAsStateWithLifecycle()

    val context = currentPlatformContext()
    val scope = rememberCoroutineScope()

    val isArca = activeStore == StoreMode.ARCA
    val dailyRotatingOffers = remember(activeStore, activeProducts, selectedOfferDayOffset) {
        viewModel.getDailyRotatingOffersForCurrentStore(selectedOfferDayOffset)
    }
    val frequentStats = remember(orders, shoppingLists, allCatalogProducts) {
        viewModel.getPurchaseHistoryStats()
    }
    val cartTotals = remember(cart, redeemBravisima, bravisimaBalance, allCatalogProducts) {
        viewModel.computeCartTotals(cart, redeemBravisima, bravisimaBalance)
    }
    val cartItemPairs = remember(cart, allCatalogProducts) {
        cart.mapNotNull { item ->
            val prod = viewModel.findProductBySku(item.sku)
            if (prod != null) prod to item.qty else null
        }
    }

    val view = currentPlatformView()

    val canInterceptBack = isScannerOpen ||
        currentScreen != AppScreen.HOME ||
        homeSearchQuery.isNotEmpty() ||
        catalogFilterQuery.isNotEmpty() ||
        categorySearchQuery.isNotEmpty()

    PlatformBackHandler(enabled = canInterceptBack) {
        HapticFeedbackManager.onNavigationClick(view)
        viewModel.navigateBack()
    }

    val scannerBlurRadius by animateFloatAsState(
        targetValue = if (isScannerOpen) 22f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "scanner_backdrop_blur"
    )
    val backdropBlurModifier = if (scannerBlurRadius > 0.5f && DeviceSupportsGpuBlur) {
        Modifier.realGpuBlur(scannerBlurRadius.dp)
    } else {
        Modifier
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val focusManager = LocalFocusManager.current
    var areMenusVisible by remember { mutableStateOf(true) }
    LaunchedEffect(currentScreen, activeStore) {
        focusManager.clearFocus(force = true)
        areMenusVisible = true
    }
    val menuScrollConnection = remember {
        object : NestedScrollConnection {
            private var accumulatedDelta = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                val dx = available.x
                if (kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                    if ((dy > 0f && accumulatedDelta < 0f) || (dy < 0f && accumulatedDelta > 0f)) {
                        accumulatedDelta = 0f
                    }
                    accumulatedDelta += dy
                    if (accumulatedDelta < -18f) {
                        areMenusVisible = false
                        accumulatedDelta = 0f
                    } else if (accumulatedDelta > 14f) {
                        areMenusVisible = true
                        accumulatedDelta = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    Crossfade(
        targetState = isAppBooting,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "startup_skeleton_crossfade"
    ) { booting ->
        if (booting) {
            AppStartupSkeletonScreen(darkMode = darkMode, isArca = isArca)
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .then(backdropBlurModifier),
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                val hazeState = rememberHazeState(blurEnabled = true)
                CompositionLocalProvider(LocalTranslucentHazeState provides hazeState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                        .nestedScroll(menuScrollConnection)
                        .dismissKeyboardOnOutsideTouch()
                ) {
                    // Brav IA usa el mismo fondo que el resto de la app; el tono rosado solo aparece
                    // como un brillo breve mientras la IA está pensando (antes teñía toda la pantalla).
                    val isAiAmbientActive = isAiGenerating
                    val aiAmbientAlpha by animateFloatAsState(
                        targetValue = if (isAiGenerating) (if (darkMode) 0.10f else 0.16f) else 0f,
                        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                        label = "global_ai_ambient_alpha"
                    )
                    // Rendimiento: el brillo del fondo da una vuelta cada 12 s y se mueve apenas un 6 %.
                    // Actualizarlo ~15 veces por segundo (8 en gama baja) se ve igual que a 60–120 y evita
                    // que el vidrio esmerilado (que desenfoca este fondo) se recalcule en cada cuadro.
                    val orbDriftPhaseState = remember { mutableFloatStateOf(0f) }
                    LaunchedEffect(Unit) {
                        // Con Liquid Glass nativo (iOS 26+) el vidrio vuelve a muestrear lo que tiene
                        // detrás en cada cambio: un fondo que se mueve 15 veces por segundo obligaba a
                        // redibujar todas las barras de vidrio y causaba tirones al desplazarse.
                        if (com.example.ui.components.nativeLiquidGlassAvailable() ||
                            com.example.platform.systemPrefersReducedMotion()) return@LaunchedEffect
                        val stepMs = if (com.example.platform.DevicePerformance.isLowEnd) 125L else 66L
                        val start = kotlin.time.TimeSource.Monotonic.markNow()
                        while (true) {
                            delay(stepMs)
                            val elapsed = start.elapsedNow().inWholeMilliseconds % 12_000L
                            orbDriftPhaseState.floatValue = (elapsed / 12_000f) * (2f * PI).toFloat()
                        }
                    }
                    val orbDriftPhase by orbDriftPhaseState
                    val targetTopGlowColor = when {
                        isAiAmbientActive -> Color(0xFFF38B9B)
                        isArca -> ArcaTeal
                        else -> primaryColor
                    }
                    val targetMidGlowColor = when {
                        isAiAmbientActive -> Color(0xFFF69E98)
                        isArca -> ArcaEmerald
                        else -> BravoAmber
                    }
                    val targetBottomGlowColor = when {
                        isAiAmbientActive -> Color(0xFFE86A7C)
                        isArca -> ArcaTeal
                        else -> secondaryColor
                    }
                    val topGlowColor by animateColorAsState(
                        targetValue = targetTopGlowColor,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        label = "ambientTopColor"
                    )
                    val midGlowColor by animateColorAsState(
                        targetValue = targetMidGlowColor,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        label = "ambientMidColor"
                    )
                    val bottomGlowColor by animateColorAsState(
                        targetValue = targetBottomGlowColor,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        label = "ambientBottomColor"
                    )
                    val rootBgColor = MaterialTheme.colorScheme.background
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .translucentBackdropSource(hazeState, zIndex = 0f)
                    ) {
                        drawRect(color = rootBgColor)
                        val topGlowAlpha = if (darkMode) 0.07f else 0.05f
                        val midGlowAlpha = if (darkMode) 0.05f else 0.03f
                        val bottomGlowAlpha = if (darkMode) 0.06f else 0.04f
                        val driftX1 = cos(orbDriftPhase) * size.width * 0.06f
                        val driftY1 = sin(orbDriftPhase) * size.height * 0.04f
                        val driftX2 = sin(orbDriftPhase * 0.85f) * size.width * 0.07f
                        val driftY2 = cos(orbDriftPhase * 0.85f) * size.height * 0.05f
                        val topCenter = Offset(size.width * 0.82f + driftX1, size.height * 0.10f + driftY1)
                        val midCenter = Offset(size.width * 0.16f + driftX2, size.height * 0.44f + driftY2)
                        val bottomCenter = Offset(size.width * 0.78f - driftX1, size.height * 0.82f - driftY2)
                        val scalePulse = 1f + sin(orbDriftPhase) * 0.06f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(topGlowColor.copy(alpha = topGlowAlpha + aiAmbientAlpha * 0.45f), Color.Transparent),
                                center = topCenter,
                                radius = size.width * 0.74f * scalePulse
                            ),
                            radius = size.width * 0.74f * scalePulse,
                            center = topCenter
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(midGlowColor.copy(alpha = midGlowAlpha + aiAmbientAlpha * 0.40f), Color.Transparent),
                                center = midCenter,
                                radius = size.width * 0.64f
                            ),
                            radius = size.width * 0.64f,
                            center = midCenter
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(bottomGlowColor.copy(alpha = bottomGlowAlpha + aiAmbientAlpha * 0.45f), Color.Transparent),
                                center = bottomCenter,
                                radius = size.width * 0.68f * scalePulse
                            ),
                            radius = size.width * 0.68f * scalePulse,
                            center = bottomCenter
                        )
                    }

                    SharedTransitionLayout {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            val forward = targetState.ordinal >= initialState.ordinal
                            val slideOffset = { fullWidth: Int ->
                                if (forward) fullWidth / 14 else -fullWidth / 14
                            }
                            (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.98f, animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                    initialOffsetX = slideOffset
                                ))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(160)) +
                                        scaleOut(targetScale = 0.98f, animationSpec = tween(160))
                                )
                        },
                        label = "main_screen_transition"
                    ) { screenState ->
                        CompositionLocalProvider(
                            LocalProductHeroScopes provides ProductHeroScopes(this@SharedTransitionLayout, this@AnimatedContent),
                            LocalDataSaver provides dataSaver
                        ) {
                        when (screenState) {
                        AppScreen.HOME -> {
                            AnimatedContent(
                                targetState = activeStore,
                                transitionSpec = {
                                    val enteringArca = targetState == StoreMode.ARCA
                                    val direction = if (enteringArca) 1 else -1
                                    (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.96f, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                                        slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { fullWidth ->
                                            direction * (fullWidth / 8)
                                        })
                                        .togetherWith(
                                            fadeOut(animationSpec = tween(210)) +
                                                scaleOut(targetScale = 0.96f, animationSpec = tween(210)) +
                                                slideOutHorizontally(animationSpec = tween(210)) { fullWidth ->
                                                    -direction * (fullWidth / 10)
                                                }
                                        )
                                },
                                label = "store_home_transition"
                            ) { storeModeState ->
                                HomeScreen(
                                    storeMode = storeModeState,
                                    darkMode = darkMode,
                                    sections = sections,
                                    allProducts = activeProducts,
                                    searchQuery = homeSearchQuery,
                                    cartQtyProvider = { sku -> cart.find { it.sku == sku }?.qty ?: 0 },
                                    isFavoriteProvider = { sku ->
                                        val favList = shoppingLists.firstOrNull()
                                        favList?.skus?.contains(sku) == true
                                    },
                                    onToggleFavorite = { sku -> viewModel.toggleProductInFavList(sku) },
                                    isOfflineMode = isOfflineMode,
                                    isGuestMode = isGuestMode,
                                    cacheStatusLabel = cacheStatusLabel,
                                    areMenusVisible = areMenusVisible,
                                    dailyRotatingOffers = dailyRotatingOffers,
                                    onRotateOfferDay = { delta -> viewModel.rotateDailyOffersDay(delta) },
                                    onTriggerDailyOfferPush = { viewModel.triggerExclusiveOfferPush() },
                                    onSearchQueryChange = { viewModel.setHomeSearchQuery(it) },
                                    onSelectStoreMode = { viewModel.selectStore(it) },
                                    onOpenScanner = { viewModel.openBarcodeScanner() },
                                    onNavigateToBravIa = { prompt ->
                                        viewModel.navigateTo(AppScreen.BRAVIA)
                                        if (!prompt.isNullOrBlank()) {
                                            viewModel.sendCustomChatMsg(prompt)
                                        }
                                    },
                                    onNavigateToDelivery = { viewModel.openOrderTracking() },
                                    onOpenMyLists = { viewModel.openAccountSubScreen(AccountSubScreen.LISTAS) },
                                    onOpenCatalog = { viewModel.navigateTo(AppScreen.CATALOG) },
                                    onRefresh = { viewModel.refreshLocalCatalogCache() },
                                    onAddCombo = { title, skus -> viewModel.addComboToCart(title, skus) },
                                    recentProducts = recentProducts,
                                    lastOrder = orders.firstOrNull(),
                                    onRepeatOrder = { id -> viewModel.reorderPastOrder(id) },
                                    onOpenCategory = { catName -> viewModel.openCategoryScreen(catName) },
                                    onOpenProduct = { sku -> viewModel.openProductDetail(sku) },
                                    onChangeQty = { sku, delta -> viewModel.changeProductQty(sku, delta) }
                                )
                            }
                        }

                        AppScreen.CATALOG -> {
                            AnimatedContent(
                                targetState = activeStore,
                                transitionSpec = {
                                    val enteringArca = targetState == StoreMode.ARCA
                                    val direction = if (enteringArca) 1 else -1
                                    (fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.96f, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                        slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { fullWidth ->
                                            direction * (fullWidth / 9)
                                        })
                                        .togetherWith(
                                            fadeOut(animationSpec = tween(200)) +
                                                scaleOut(targetScale = 0.96f, animationSpec = tween(200))
                                        )
                                },
                                label = "store_catalog_transition"
                            ) { storeModeState ->
                                CatalogScreen(
                                    storeMode = storeModeState,
                                    darkMode = darkMode,
                                    sections = sections,
                                    allProducts = activeProducts,
                                    filterQuery = catalogFilterQuery,
                                    isOfflineMode = isOfflineMode,
                                    cacheStatusLabel = cacheStatusLabel,
                                    areMenusVisible = areMenusVisible,
                                    dailyRotatingOffers = dailyRotatingOffers,
                                    onRotateOfferDay = { delta -> viewModel.rotateDailyOffersDay(delta) },
                                    onTriggerDailyOfferPush = { viewModel.triggerExclusiveOfferPush() },
                                    onFilterQueryChange = { viewModel.setCatalogFilterQuery(it) },
                                    onToggleStoreMode = { viewModel.toggleStoreMode() },
                                    onSelectStoreMode = { viewModel.selectStore(it) },
                                    onOpenCategory = { catName -> viewModel.openCategoryScreen(catName) },
                                    onOpenProduct = { sku -> viewModel.openProductDetail(sku) },
                                    onChangeQty = { sku, delta -> viewModel.changeProductQty(sku, delta) }
                                )
                            }
                        }

                        AppScreen.CATEGORY -> {
                            CategoryGridScreen(
                                categoryName = activeCategory,
                                products = activeProducts,
                                searchQuery = categorySearchQuery,
                                onSearchQueryChange = { viewModel.setCategorySearchQuery(it) },
                                storeMode = activeStore,
                                darkMode = darkMode,
                                areMenusVisible = areMenusVisible,
                                cartQtyProvider = { sku -> cart.find { it.sku == sku }?.qty ?: 0 },
                                isFavoriteProvider = { sku ->
                                    val favList = shoppingLists.firstOrNull()
                                    favList?.skus?.contains(sku) == true
                                },
                                onToggleFavorite = { sku -> viewModel.toggleProductInFavList(sku) },
                                onBack = { viewModel.navigateBack() },
                                onOpenProduct = { sku -> viewModel.openProductDetail(sku) },
                                onChangeQty = { sku, delta -> viewModel.changeProductQty(sku, delta) }
                            )
                        }

                        AppScreen.PRODUCT -> {
                            // Sin catálogo cargado (primera vez sin internet) no hay producto que mostrar: se vuelve atrás en vez de cerrar la app
                            val prodOrNull = viewModel.findProductBySku(activeProductSku) ?: activeProducts.firstOrNull()
                            if (prodOrNull == null) {
                                LaunchedEffect(Unit) { viewModel.navigateBack() }
                            } else {
                            val prod = prodOrNull
                            val isFav = remember(shoppingLists, prod.sku) {
                                viewModel.isProductFavorite(prod.sku)
                            }
                            ProductDetailScreen(
                                product = prod,
                                qtyInCart = cart.find { it.sku == prod.sku }?.qty ?: 0,
                                totalCartCount = cartTotals.count,
                                isFavorite = isFav,
                                storeMode = activeStore,
                                darkMode = darkMode,
                                onBack = { viewModel.navigateBack() },
                                onOpenCart = { viewModel.navigateTo(AppScreen.CART) },
                                onToggleFavorite = { viewModel.toggleProductInFavList(prod.sku) },
                                onShare = {
                                    shareOnWhatsApp(
                                        platformContext,
                                        "Mira esto en BravoVa: *${prod.name}* (${prod.unit}) a ${formatRd(prod.price)} 🛒"
                                    )
                                },
                                onChangeQty = { delta -> viewModel.changeProductQty(prod.sku, delta) },
                                onRemoveCompletely = { viewModel.removeProductCompletely(prod.sku) },
                                onOpenCategoryFilter = { catName -> viewModel.openCategoryScreen(catName) },
                                onAskBravIa = { viewModel.askAiAboutProduct(prod.sku) },
                                hasPriceAlert = prod.sku in priceAlerts,
                                onTogglePriceAlert = { viewModel.togglePriceAlert(prod.sku) },
                                allProducts = allCatalogProducts,
                                onOpenProduct = { sku -> viewModel.openProductDetail(sku) }
                            )
                            }
                        }

                        AppScreen.BRAVISIMA -> {
                            BravisimaScreen(
                                balance = bravisimaBalance,
                                cartTotals = cartTotals,
                                userProfile = userProfile,
                                bravisimaCard = bravisimaCard,
                                movements = bravisimaMovements,
                                isGuestMode = isGuestMode,
                                isSavingBravisima = isSavingBravisima,
                                darkMode = darkMode,
                                onRedeemInCart = {
                                    viewModel.setRedeemBravisimaInCart(true)
                                    viewModel.navigateTo(AppScreen.CART)
                                },
                                onOpenScanner = { viewModel.openBarcodeScanner() },
                                onToggleCardLock = { viewModel.toggleBravisimaCardLock() },
                                onToggleAutoRedeem = { viewModel.toggleBravisimaAutoRedeem() },
                                onSaveCardSettings = { holderName, monthlyGoal, preferredCategory ->
                                    viewModel.updateBravisimaCardSettings(holderName, monthlyGoal, preferredCategory)
                                },
                                onRedeemPromoCode = { code -> viewModel.redeemBravisimaPromoCode(code) },
                                onCopyCardNumber = { number ->
                                    viewModel.showToast("check", "Número $number copiado al portapapeles")
                                },
                                onSignOutGoogle = {
                                    scope.launch {
                                        signOutGoogleUser(context)
                                        onExitSession()
                                    }
                                }
                            )
                        }

                        AppScreen.CART -> {
                            CartCheckoutScreen(
                                cartItems = cartItemPairs,
                                cartTotals = cartTotals,
                                bravisimaBalance = bravisimaBalance,
                                redeemBravisima = redeemBravisima,
                                isBravisimaLocked = bravisimaCard.isLocked,
                                storeMode = activeStore,
                                darkMode = darkMode,
                                activeBranch = activeBranch,
                                paymentMethods = paymentMethods,
                                addresses = addresses,
                                onBack = { viewModel.navigateBack() },
                                onEmptyCart = { viewModel.clearCart() },
                                onOpenProduct = { sku -> viewModel.openProductDetail(sku) },
                                onUpdateQty = { sku, delta -> viewModel.changeProductQty(sku, delta) },
                                onToggleRedeem = { enabled -> viewModel.setRedeemBravisimaInCart(enabled) },
                                onConfirmCheckout = { viewModel.completeCheckoutOrder() },
                                checkoutExtras = checkoutExtras,
                                onCheckoutExtrasChange = { viewModel.updateCheckoutExtras(it) },
                                onExploreCatalog = { viewModel.navigateTo(AppScreen.HOME) },
                                onAskBravIa = { viewModel.navigateTo(AppScreen.BRAVIA) },
                                onSelectPaymentMethod = { id -> viewModel.setDefaultPayment(id) },
                                onAddPaymentCard = { viewModel.addNewPaymentCard() },
                                onToggleCardLock = { viewModel.toggleBravisimaCardLock() },
                                onBiometricVerified = { res ->
                                    viewModel.showToast("check", "BiometricPrompt verificado (${res.transactionToken})")
                                }
                            )
                        }

                        AppScreen.DELIVERY -> {
                            DeliveryTrackingScreen(
                                branchName = activeBranch,
                                cartItems = cartItemPairs,
                                darkMode = darkMode,
                                trackedOrder = trackedOrder,
                                allOrders = orders,
                                firestoreSyncStatus = firestoreSyncStatus,
                                isGuestMode = isGuestMode,
                                isUpdatingOrderStatus = isUpdatingOrderStatus,
                                realLocationMetrics = realLocationMetrics,
                                driverTelemetry = driverTelemetry,
                                driverChatMessages = driverChatMessages,
                                isDriverTyping = isDriverTyping,
                                liveDeliveryBanner = liveDeliveryBanner,
                                onRefreshRealLocation = { triggerPush ->
                                    viewModel.refreshRealUserLocation(context, triggerPush)
                                },
                                onTriggerDynamicPush = {
                                    viewModel.dispatchDynamicLocationOrderPush()
                                },
                                onSendDriverMessage = { msg ->
                                    viewModel.sendDriverChatMessage(msg)
                                },
                                onToggleAutoSimulation = {
                                    viewModel.toggleLiveTrafficSimulation()
                                },
                                onRecalculateTraffic = {
                                    viewModel.triggerLiveTrafficRecalculation()
                                },
                                onDismissLiveBanner = {
                                    viewModel.dismissLiveDeliveryBanner()
                                },
                                productLookup = { sku -> viewModel.findProductBySku(sku) },
                                onBack = { viewModel.navigateBack() },
                                onCallDriver = {
                                    viewModel.showToast("delivery", "Llamando a Carlos Rodríguez (Repartidor)...")
                                },
                                onMessageDriver = {
                                    viewModel.sendDriverChatMessage("Hola Carlos, ¿por dónde vienes y en cuánto tiempo llegas?")
                                },
                                onSelectOrder = { orderId -> viewModel.selectTrackedOrder(orderId) },
                                onSelectDeliveryStage = { stage ->
                                    viewModel.updateTrackedOrderStatusInFirestore(stage)
                                },
                                onAdvanceDeliveryStage = {
                                    viewModel.advanceTrackedOrderStatusInFirestore()
                                },
                                onOpenProduct = { sku -> viewModel.openProductDetail(sku) },
                                orderExtras = orderExtras
                            )
                        }

                        AppScreen.BRAVIA -> {
                                var pendingChatBiometricMsgId by remember { mutableStateOf<Long?>(null) }
                                BravIaChatScreen(
                                    storeMode = activeStore,
                                    darkMode = darkMode,
                                    areMenusVisible = areMenusVisible,
                                    npuStatus = npuStatus,
                                    onToggleNpuMode = { viewModel.toggleNpuMode() },
                                    pendingImageUri = pendingChatImageUri,
                                    onSelectImageUri = { uri -> viewModel.setPendingChatImageUri(uri) },
                                    messages = chatMessages,
                                    isGenerating = isAiGenerating,
                                    budgetAmount = budgetAmount,
                                    cartTotals = cartTotals,
                                    activeBranch = activeBranch,
                                    estimatedEtaMinutes = realLocationMetrics.stageAdjustedEtaMinutes,
                                    productLookup = { sku -> viewModel.findProductBySku(sku) },
                                    onBack = { viewModel.navigateBack() },
                                    onOpenCart = { viewModel.navigateTo(AppScreen.CART) },
                                    onOpenMyLists = { viewModel.openAccountSubScreen(AccountSubScreen.LISTAS) },
                                    onBudgetChange = { viewModel.setTargetBudgetInChat(it) },
                                    onBuildCartForBudget = { viewModel.applyBudgetInChat(false) },
                                    onAdjustCartToBudget = { viewModel.applyBudgetInChat(true) },
                                    onSendPrompt = { prompt -> viewModel.sendCustomChatMsg(prompt) },
                                    onPauseGeneration = { viewModel.pauseOrStopAiGeneration() },
                                    onAddBundleForMessage = { msgId -> viewModel.addAiBundleToCart(msgId) },
                                    onSaveBundleAsList = { msgId -> viewModel.saveChatMessageBundleAsList(msgId) },
                                    onRequestOrderConfirmation = { msgId -> viewModel.requestOrderConfirmationForMessage(msgId) },
                                    onConfirmOrderFromChat = { msgId -> pendingChatBiometricMsgId = msgId },
                                    onCancelOrderConfirmation = { msgId -> viewModel.cancelOrderConfirmationForMessage(msgId) },
                                    onAddSingleProduct = { sku -> viewModel.changeProductQty(sku, 1) },
                                    onOpenProduct = { sku -> viewModel.openProductDetail(sku) }
                                )

                                pendingChatBiometricMsgId?.let { msgId ->
                                    val targetMsg = chatMessages.find { it.id == msgId }
                                    val msgProducts = targetMsg?.skus.orEmpty().distinct().mapNotNull { viewModel.findProductBySku(it) }
                                    val estTotal = if (msgProducts.isNotEmpty()) msgProducts.sumOf { it.price } else cartTotals.finalPay.toInt()
                                    val estCount = if (msgProducts.isNotEmpty()) msgProducts.size else cartTotals.count.coerceAtLeast(1)
                                    val estCashback = (estTotal * 0.01).toInt().coerceAtLeast(1)
                                    val defaultCard = paymentMethods.firstOrNull { it.isDefault } ?: paymentMethods.firstOrNull()
                                    val cardLabel = defaultCard?.let { "${it.brand} •••• ${it.last4}" } ?: "Visa •••• 4821"
                                    BiometricOrderPromptDialog(
                                        request = BiometricPromptRequest(
                                            operation = BiometricCriticalOperation.CONFIRM_AI_ORDER,
                                            title = "Confirmar Pedido Brav IA",
                                            subtitle = "Total RD$$estTotal.00 • $estCount artículo(s)",
                                            description = "Autoriza con tu huella digital el pago con $cardLabel y el despacho inmediato de tu compra preparada por Brav IA.",
                                            badgeTitle = "AUTORIZACIÓN BIOMÉTRICA BRAV IA",
                                            amountRd = estTotal,
                                            itemCount = estCount,
                                            cashbackRd = estCashback,
                                            paymentMethodLabel = cardLabel,
                                            deliveryOrBranchLabel = "Delivery Express • $activeBranch"
                                        ),
                                        isArca = isArca,
                                        darkMode = darkMode,
                                        onDismiss = { pendingChatBiometricMsgId = null },
                                        onAuthenticatedWithResult = { res ->
                                            val confirmedId = pendingChatBiometricMsgId
                                            pendingChatBiometricMsgId = null
                                            viewModel.showToast("check", "Pedido IA autorizado (${res.transactionToken})")
                                            if (confirmedId != null) {
                                                viewModel.confirmOrderFromChatMessage(confirmedId)
                                            }
                                        }
                                    )
                                }
                            }

                        AppScreen.ACCOUNT -> {
                            AccountGridScreen(
                                userProfile = userProfile,
                                activeBranch = activeBranch,
                                bravisimaBalance = bravisimaBalance,
                                cartCount = cartTotals.count,
                                firestoreSyncStatus = firestoreSyncStatus,
                                isGuestMode = isGuestMode,
                                cacheStatusLabel = cacheStatusLabel,
                                darkMode = darkMode,
                                onBack = { viewModel.navigateBack() },
                                onOpenCart = { viewModel.navigateTo(AppScreen.CART) },
                                onSignOut = {
                                    scope.launch {
                                        signOutGoogleUser(context)
                                        onExitSession()
                                    }
                                },
                                onOpenBravisima = { viewModel.navigateTo(AppScreen.BRAVISIMA) },
                                onOpenSubScreen = { sub -> viewModel.openAccountSubScreen(sub) }
                            )
                        }

                        AppScreen.ACCOUNT_SUB -> {
                            AccountSubScreenView(
                                subScreen = activeAccountSub,
                                userProfile = userProfile,
                                activeBranch = activeBranch,
                                selectedPaletteId = selectedPaletteId,
                                firestoreSyncStatus = firestoreSyncStatus,
                                isGuestMode = isGuestMode,
                                isOfflineMode = isOfflineMode,
                                cacheStatusLabel = cacheStatusLabel,
                                orders = orders,
                                paymentMethods = paymentMethods,
                                addresses = addresses,
                                shoppingLists = shoppingLists,
                                aiPopulatingListId = aiPopulatingListId,
                                frequentStats = frequentStats,
                                allProducts = allCatalogProducts,
                                productLookup = { sku -> viewModel.findProductBySku(sku) },
                                fcmToken = fcmToken,
                                fcmTopics = fcmTopics,
                                pushFeed = pushFeed,
                                darkMode = darkMode,
                                notificationsEnabled = pushNotifications,
                                onBack = { viewModel.navigateBack() },
                                onRepeatOrder = { orderId -> viewModel.reorderPastOrder(orderId) },
                                orderRatings = orderRatings,
                                onRateOrder = { id, stars -> viewModel.rateOrder(id, stars) },
                                onOpenLiveMap = { viewModel.openOrderTracking() },
                                onOpenOrderTracking = { orderId -> viewModel.openOrderTracking(orderId) },
                                onSaveProfile = { n, p, r, ncf -> viewModel.saveUserProfile(n, p, r, ncf) },
                                onSelectPaymentMethod = { id -> viewModel.setDefaultPayment(id) },
                                onAddPaymentCard = { viewModel.addNewPaymentCard() },
                                onSelectAddress = { id -> viewModel.setDefaultAddress(id) },
                                onLoadListToCart = { listId -> viewModel.addListToCart(listId) },
                                onCreateShoppingList = { name, autoPopulate, focusHint ->
                                    viewModel.createShoppingList(
                                        name = name,
                                        autoPopulateWithAi = autoPopulate,
                                        focusHint = focusHint
                                    )
                                },
                                onDeleteShoppingList = { listId -> viewModel.deleteShoppingList(listId) },
                                onRenameShoppingList = { listId, newName -> viewModel.renameShoppingList(listId, newName) },
                                onPopulateListWithAi = { listId -> viewModel.populateListWithBravIa(listId) },
                                onAddProductToList = { listId, sku -> viewModel.addProductToShoppingList(listId, sku) },
                                onRemoveProductFromList = { listId, sku -> viewModel.removeProductFromShoppingList(listId, sku) },
                                onAddSingleToCart = { sku -> viewModel.changeProductQty(sku, 1) },
                                onOpenProduct = { sku -> viewModel.openProductDetail(sku) },
                                onToggleDarkMode = { viewModel.toggleDarkMode() },
                                followsSystemTheme = darkModeOverride == null,
                                onFollowSystemTheme = { viewModel.followSystemTheme() },
                                dataSaver = dataSaver,
                                onToggleDataSaver = { viewModel.setDataSaver(it) },
                                npuStatus = npuStatus,
                                onToggleNpuMode = { viewModel.toggleNpuMode() },
                                onSelectPalette = { paletteId -> viewModel.selectColorPalette(paletteId) },
                                onToggleNotifications = { viewModel.togglePushNotifications() },
                                onToggleFcmTopic = { topicKey, enabled -> viewModel.toggleFcmTopic(topicKey, enabled) },
                                onTriggerOfferPush = { viewModel.triggerExclusiveOfferPush() },
                                onTriggerOrderPush = { viewModel.triggerRealtimeOrderStatusPush() },
                                onSelectPushNotification = { record -> viewModel.handlePushNotificationClick(record) },
                                onMarkAllPushRead = { viewModel.markAllPushNotificationsRead() },
                                onRefreshLocalCache = { viewModel.refreshLocalCatalogCache() },
                                submittedSuggestions = submittedSuggestions,
                                onSubmitSupportSuggestion = { category, priority, subject, body ->
                                    viewModel.submitDirectSuggestionFromSupport(
                                        category = category,
                                        priority = priority,
                                        subject = subject,
                                        summaryBody = body
                                    )
                                },
                                onOpenBravIaSupportPrompt = { prompt ->
                                    viewModel.navigateTo(AppScreen.BRAVIA)
                                    viewModel.sendCustomChatMsg(prompt)
                                },
                                onUpdateBranch = { branch -> viewModel.setActiveBranch(branch) },
                                onSignOut = {
                                    scope.launch {
                                        signOutGoogleUser(context)
                                        onExitSession()
                                    }
                                },
                                onShowToast = { icon, msg -> viewModel.showToast(icon, msg) }
                            )
                        }
                    }
                                            }
                    }
                    }

                    // Miniaturas que vuelan al carrito al agregar productos
                    CartFlightOverlay()

                    if (PlatformInfo.isIos && canInterceptBack) {
                        // iOS no tiene botón "atrás": deslizar desde el borde izquierdo vuelve, como en las apps nativas.
                        val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(20.dp)
                                .pointerInput(Unit) {
                                    var dragged = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragged = 0f },
                                        onDragEnd = {
                                            if (dragged > swipeThresholdPx) {
                                                HapticFeedbackManager.onNavigationClick(view)
                                                viewModel.navigateBack()
                                            }
                                        }
                                    ) { _, dx -> dragged += dx }
                                }
                        )
                    }

                    val showCartBar = cartTotals.count > 0 && currentScreen in listOf(
                        AppScreen.HOME,
                        AppScreen.CATALOG,
                        AppScreen.CATEGORY
                    )
                    AnimatedVisibility(
                        visible = showCartBar && areMenusVisible,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(200)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = if (PlatformInfo.isAndroid) 88.dp else 84.dp)
                    ) {
                        CartSummaryBar(
                            itemCount = cartTotals.count,
                            total = cartTotals.finalPay,
                            isArca = isArca,
                            pulseTrigger = cartPulseTrigger,
                            onOpenCart = { viewModel.navigateTo(AppScreen.CART) }
                        )
                    }

                    // En iOS 26+ la barra nativa no se esconde al hacer scroll: se encoge (como en las apps de Apple)
                    // También en Brav IA: el campo de escribir flota encima de la barra (misma barra en toda la app)
                    val nativeDock = nativeLiquidGlassAvailable()
                    AnimatedVisibility(
                        visible = areMenusVisible || nativeDock,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(250, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(210)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            // En Android la NavigationBar pinta su propio fondo bajo la barra de gestos.
                            .then(if (PlatformInfo.isAndroid) Modifier else Modifier.navigationBarsPadding())
                    ) {
                        TranslucentBottomDock(
                            currentScreen = currentScreen,
                            isArca = isArca,
                            darkMode = darkMode,
                            onNavigate = { target -> viewModel.navigateTo(target) },
                            minimized = nativeDock && !areMenusVisible,
                            onSearch = {
                                viewModel.navigateTo(AppScreen.HOME)
                                com.example.ui.components.SearchFocusRequest.request()
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = toast != null,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp, start = 20.dp, end = 20.dp)
                    ) {
                        toast?.let { activeToast ->
                            if (nativeLiquidGlassAvailable() && activeToast.actionLabel == null) {
                                NativeGlassToast(
                                    text = activeToast.message,
                                    symbol = toastSymbol(activeToast.icon),
                                    accent = primaryColor,
                                    darkMode = darkMode,
                                    modifier = Modifier.fillMaxWidth().height(60.dp).testTag("toast_banner")
                                )
                            } else {
                                // Android: aviso Material (superficie inversa), como un Snackbar
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.inverseSurface,
                                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier.testTag("toast_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        BravoUnifiedIcon(
                                            iconKey = activeToast.icon,
                                            tint = MaterialTheme.colorScheme.inversePrimary,
                                            size = 18.dp
                                        )
                                        Text(
                                            text = activeToast.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        activeToast.actionLabel?.let { label ->
                                            androidx.compose.material3.TextButton(
                                                onClick = { viewModel.onToastAction() },
                                                modifier = Modifier
                                                    .heightIn(min = 44.dp)
                                                    .testTag("toast_action")
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = MaterialTheme.colorScheme.inversePrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    com.example.ui.components.OfflineBanner(
                        visible = isOfflineMode,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 72.dp)
                    )

                    PawEasterEggOverlay()
                    LaunchedEffect(Unit) {
                        com.example.ui.components.EasterEffects.reduceMotion = com.example.platform.systemPrefersReducedMotion()
                    }
                    com.example.ui.components.EasterEffectsOverlay()

                    AnimatedVisibility(
                        visible = celebrationOrder != null,
                        enter = fadeIn(animationSpec = tween(220)),
                        exit = fadeOut(animationSpec = tween(260)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        celebrationOrder?.let { confirmedOrder ->
                            OrderSuccessCelebrationOverlay(
                                orderId = confirmedOrder.id,
                                totalAmountRd = confirmedOrder.total.toInt(),
                                branchName = confirmedOrder.branch,
                                etaMinutes = realLocationMetrics.stageAdjustedEtaMinutes,
                                distanceKmLabel = "${realLocationMetrics.roadDistanceKm} km",
                                onContinueToTracking = {
                                    viewModel.dismissOrderCelebration()
                                }
                            )
                        }
                    }
                }
                }
            }
        }
    }

    if (isScannerOpen) {
        val scannedProduct = viewModel.findProductBySku(scannedSku)
        BarcodeScannerModal(
            selectedProduct = scannedProduct,
            onSelectProductSku = { sku -> viewModel.simulateScanProduct(sku) },
            onAddScannedProduct = { prod ->
                viewModel.changeProductQty(prod.sku, 1)
                viewModel.closeBarcodeScanner()
            },
            onViewDetails = { prod ->
                viewModel.closeBarcodeScanner()
                viewModel.openProductDetail(prod.sku)
            },
            onDismiss = { viewModel.closeBarcodeScanner() }
        )
    }
}

/** Ícono SF Symbol equivalente para los avisos en iOS. */
private fun toastSymbol(key: String): String = when (key) {
    "check", "✓", "✅" -> "checkmark.circle.fill"
    "🗑️", "trash" -> "trash.fill"
    "📋", "list" -> "list.bullet.rectangle.fill"
    "✏️" -> "pencil"
    "✨", "ai" -> "sparkles"
    "🛒", "cart" -> "cart.fill"
    "🐾" -> "pawprint.fill"
    "★" -> "star.fill"
    "chat" -> "message.fill"
    "phone" -> "phone.fill"
    "offer", "🏷️" -> "tag.fill"
    "cloud" -> "icloud.slash.fill"
    "pin" -> "mappin.circle.fill"
    "moon" -> "moon.fill"
    "sun" -> "sun.max.fill"
    "heart" -> "heart.fill"
    else -> "info.circle.fill"
}
