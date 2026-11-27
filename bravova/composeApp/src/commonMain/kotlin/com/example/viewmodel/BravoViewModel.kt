package com.example.viewmodel

import com.example.ui.components.play
import com.example.ui.components.PlatformExtras
import com.example.platform.sharedPrefs
import com.example.ui.components.RdPrefs
import com.example.model.CheckoutExtras
import androidx.lifecycle.ViewModel
import com.example.data.remote.CloudRepositoryIds
import com.example.data.remote.createCloudRepository
import com.example.platform.AppContext
import com.example.platform.AppPermission
import com.example.platform.AuthPlatform
import com.example.platform.Calendar
import com.example.platform.ConnectivityMonitor
import com.example.platform.Date
import com.example.platform.GeoPoint
import com.example.platform.JSONArray
import com.example.platform.JSONObject
import com.example.platform.Locale
import com.example.platform.LocationProvider
import com.example.platform.Log
import com.example.platform.PlatformContext
import com.example.platform.PlatformInfo
import com.example.platform.AndroidVersions
import com.example.platform.SimpleDateFormat
import com.example.platform.System
import com.example.platform.TimeZone
import com.example.platform.URLEncoder
import com.example.platform.content
import com.example.platform.createGenerativeModel
import com.example.platform.AiModelConfig
import com.example.platform.AiContent
import com.example.ai.BravIaEngine
import com.example.ai.BravIaText
import com.example.ai.ChatTurn
import com.example.ai.GroundedReply
import com.example.ai.PromptContext
import com.example.platform.currentDarkModeHint
import com.example.platform.format
import com.example.platform.hasPermission
import com.example.platform.httpGetText
import com.example.platform.httpPostJson
import com.example.platform.loadScaledJpeg
import kotlinx.coroutines.IO

import androidx.lifecycle.viewModelScope
import com.example.data.ArcaCatalogData
import com.example.data.BravoCatalogData
import com.example.data.local.BravoDatabase
import com.example.data.local.BravoLocalRepository
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.model.AccountSubScreen
import com.example.model.AppScreen
import com.example.model.BranchGeoCoordinate
import com.example.model.BravisimaCard
import com.example.model.BravisimaMovement
import com.example.model.CartItem
import com.example.model.CartTotals
import com.example.model.CatalogSection
import com.example.model.ChatMessage
import com.example.model.DailyRotatingOffer
import com.example.model.DriverChatMessage
import com.example.model.DriverLiveTelemetry
import com.example.model.FcmTopicSubscription
import com.example.model.FrequentProductStat
import com.example.model.LiveDeliveryBannerNotification
import com.example.model.NpuHardwareStatus
import com.example.model.OrderDeliveryStage
import com.example.model.OrderRecord
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.PushNotificationCategory
import com.example.model.PushNotificationRecord
import com.example.model.RealLocationDeliveryMetrics
import com.example.model.ShoppingList
import com.example.model.StoreMode
import com.example.model.SuggestionMailPayload
import com.example.model.ToastNotification
import com.example.model.UserAddress
import com.example.model.UserProfile
import com.example.model.nutritionProfile
import com.example.notifications.BravoPushNotificationCenter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "BravoViewModel"
private const val MAX_AI_RESPONSE_TIMEOUT_MS = 45_000L
private const val SUGGESTION_TARGET_EMAIL = "bezaleelpatrocino0@gmail.com"

class BravoViewModel(private val application: PlatformContext = AppContext.instance) : ViewModel() {

    private val localRepo = BravoLocalRepository(
        BravoDatabase.getInstance(application).bravoDao(),
        application
    )

    private val firestoreRepo = createCloudRepository(application)

    private var activeAuthUserId: String? = null
    private var profileObserverJob: Job? = null
    private var cartObserverJob: Job? = null
    private var ordersObserverJob: Job? = null
    private var trackedOrderObserverJob: Job? = null
    private var listsObserverJob: Job? = null
    private var bravisimaCardObserverJob: Job? = null
    private var bravisimaMovementsObserverJob: Job? = null
    private var activeAiChatJob: Job? = null
    private var liveTelemetryJob: Job? = null
    private var connectivityUnregister: (() -> Unit)? = null

    private val _firestoreSyncStatus = MutableStateFlow("Sincronizado con tu cuenta de Google")
    val firestoreSyncStatus: StateFlow<String> = _firestoreSyncStatus.asStateFlow()

    private val _isGuestMode = MutableStateFlow(localRepo.getGuestModeSync(false))
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _cacheStatusLabel = MutableStateFlow(localRepo.getLastCacheSyncLabelSync())
    val cacheStatusLabel: StateFlow<String> = _cacheStatusLabel.asStateFlow()

    private val _cachedBravoProducts = MutableStateFlow<List<Product>>(BravoCatalogData.BRAVO_PRODUCTS)
    private val _cachedArcaProducts = MutableStateFlow<List<Product>>(ArcaCatalogData.ARCA_PRODUCTS)
    private val _cachedBravoSections = MutableStateFlow<List<CatalogSection>>(BravoCatalogData.BRAVO_SECTIONS)
    private val _cachedArcaSections = MutableStateFlow<List<CatalogSection>>(ArcaCatalogData.ARCA_SECTIONS)

    private val _activeStoreProducts = MutableStateFlow<List<Product>>(BravoCatalogData.BRAVO_PRODUCTS)
    val activeStoreProducts: StateFlow<List<Product>> = _activeStoreProducts.asStateFlow()

    private val _activeStoreSections = MutableStateFlow<List<CatalogSection>>(BravoCatalogData.BRAVO_SECTIONS)
    val activeStoreSections: StateFlow<List<CatalogSection>> = _activeStoreSections.asStateFlow()

    private val _allCachedProducts = MutableStateFlow<List<Product>>(ArcaCatalogData.getAllProducts())
    val allCachedProducts: StateFlow<List<Product>> = _allCachedProducts.asStateFlow()

    private val geminiModel by lazy {
        createGenerativeModel("gemini-3.8-flash")
    }

    private val geminiFallbackModel by lazy {
        createGenerativeModel("gemini-3.5-flash-lite")
    }

    /*
     * Modelos del chat de Brav IA. Van aparte de los de arriba (que usan las listas y el chat del
     * repartidor) porque responden en JSON, con las reglas como instrucción de sistema y con
     * temperatura baja: menos creatividad = menos datos inventados.
     */
    private val bravIaChatConfig = AiModelConfig(
        systemInstruction = BravIaEngine.SYSTEM_INSTRUCTION,
        jsonOutput = true,
        temperature = 0.3
    )
    private val bravIaChatModel by lazy { createGenerativeModel("gemini-3.8-flash", bravIaChatConfig) }
    private val bravIaChatFallbackModel by lazy { createGenerativeModel("gemini-3.5-flash-lite", bravIaChatConfig) }

    private fun isFirebaseUserAuthenticatedForAi(): Boolean =
        runCatching { AuthPlatform.currentUser() != null }.getOrDefault(false)

    private val _pendingChatImageUri = MutableStateFlow<String?>(null)
    val pendingChatImageUri: StateFlow<String?> = _pendingChatImageUri.asStateFlow()

    private val _submittedSuggestions = MutableStateFlow<List<SuggestionMailPayload>>(emptyList())
    val submittedSuggestions: StateFlow<List<SuggestionMailPayload>> = _submittedSuggestions.asStateFlow()

    private val _isAppBooting = MutableStateFlow(true)
    val isAppBooting: StateFlow<Boolean> = _isAppBooting.asStateFlow()

    private val backStack = ArrayDeque<AppScreen>()

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _activeStore = MutableStateFlow(StoreMode.BRAVO)
    val activeStore: StateFlow<StoreMode> = _activeStore.asStateFlow()

    private val _activeCategory = MutableStateFlow("Hogar y limpieza")
    val activeCategory: StateFlow<String> = _activeCategory.asStateFlow()

    private val _activeProductSku = MutableStateFlow("BRV-001")
    val activeProductSku: StateFlow<String> = _activeProductSku.asStateFlow()

    private val _activeAccountSub = MutableStateFlow(AccountSubScreen.AJUSTES)
    val activeAccountSub: StateFlow<AccountSubScreen> = _activeAccountSub.asStateFlow()

    private val _homeSearchQuery = MutableStateFlow("")
    val homeSearchQuery: StateFlow<String> = _homeSearchQuery.asStateFlow()

    private val _catalogFilterQuery = MutableStateFlow("")
    val catalogFilterQuery: StateFlow<String> = _catalogFilterQuery.asStateFlow()

    private val _categorySearchQuery = MutableStateFlow("")
    val categorySearchQuery: StateFlow<String> = _categorySearchQuery.asStateFlow()

    /* ---------- Funciones pensadas para RD ---------- */
    private val rdPrefs = sharedPrefs("bravova_rd")

    /** Lo que el cliente eligió en el checkout (pago, horario, referencia, propina, diáspora, NCF). */
    private val _checkoutExtras = MutableStateFlow(RdPrefs.loadExtras())
    val checkoutExtras: StateFlow<CheckoutExtras> = _checkoutExtras.asStateFlow()
    fun updateCheckoutExtras(extras: CheckoutExtras) {
        _checkoutExtras.value = extras
        RdPrefs.saveExtras(extras)
    }

    /** Extras de cada pedido, para compartirlo por WhatsApp. */
    private val _orderExtras = MutableStateFlow<Map<String, CheckoutExtras>>(emptyMap())
    val orderExtras: StateFlow<Map<String, CheckoutExtras>> = _orderExtras.asStateFlow()

    /** Pedido que se hizo sin señal y espera a que vuelva internet. */
    private val _pendingOfflineOrder = MutableStateFlow(rdPrefs.getBoolean("pending_offline_order", false))
    val pendingOfflineOrder: StateFlow<Boolean> = _pendingOfflineOrder.asStateFlow()

    /** Ahorro de datos: imágenes livianas y sin animaciones automáticas. */
    private val _dataSaver = MutableStateFlow(rdPrefs.getBoolean("data_saver", false))
    val dataSaver: StateFlow<Boolean> = _dataSaver.asStateFlow()
    fun setDataSaver(enabled: Boolean) {
        _dataSaver.value = enabled
        rdPrefs.edit().putBoolean("data_saver", enabled).apply()
        showToast(if (enabled) "check" else "sun", if (enabled) "Ahorro de datos activado" else "Ahorro de datos desactivado")
    }

    /** Productos con alerta de precio. */
    private val _priceAlerts = MutableStateFlow(rdPrefs.getStringSet("price_alerts", emptySet()).orEmpty())
    val priceAlerts: StateFlow<Set<String>> = _priceAlerts.asStateFlow()
    fun togglePriceAlert(sku: String) {
        val next = if (sku in _priceAlerts.value) _priceAlerts.value - sku else _priceAlerts.value + sku
        _priceAlerts.value = next
        rdPrefs.edit().putStringSet("price_alerts", next).apply()
        showToast("offer", if (sku in next) "Te avisaremos cuando baje de precio" else "Alerta de precio quitada")
        if (sku in next) checkPriceAlerts()
    }

    /** Si alguna oferta del día incluye un producto con alerta, avisa una vez por día. */
    private fun checkPriceAlerts() {
        val alerts = _priceAlerts.value
        if (alerts.isEmpty()) return
        val hit = getDailyRotatingOffersForCurrentStore().firstOrNull { it.sku in alerts } ?: return
        val key = "alert_${hit.sku}_${hit.dayIndex}"
        if (rdPrefs.getBoolean(key, false)) return
        rdPrefs.edit().putBoolean(key, true).apply()
        showToast("offer", "¡Bajó de precio! ${hit.title}: RD$${hit.offerPrice} (-${hit.discountPct}%)")
        if (_pushNotifications.value) {
            BravoPushNotificationCenter.dispatchPushNotification(
                context = application,
                category = PushNotificationCategory.EXCLUSIVE_OFFER,
                title = "Bajó de precio: ${hit.title}",
                body = "Ahora RD$${hit.offerPrice} (antes RD$${hit.originalPrice}). Oferta de ${hit.dayLabel}.",
                topic = BravoPushNotificationCenter.TOPIC_OFFERS_BRAVO,
                targetOrderId = null,
                progressPct = 0,
                postSystemNotification = true
            )
        }
    }

    /** Agrega todos los productos de un combo de temporada de una vez. */
    fun addComboToCart(title: String, skus: List<String>) {
        skus.forEach { changeProductQty(it, 1) }
        showToast("check", "$title: ${skus.size} productos agregados")
    }

    /** null = seguir el tema claro/oscuro del dispositivo. */
    private val _darkModeOverride = MutableStateFlow(localRepo.getDarkModeOverrideSync())
    val darkModeOverride: StateFlow<Boolean?> = _darkModeOverride.asStateFlow()

    private val _systemDark = MutableStateFlow(currentDarkModeHint(application))
    private val _darkMode = MutableStateFlow(_darkModeOverride.value ?: _systemDark.value)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    /** La UI avisa cada vez que cambia el tema del sistema. */
    fun onSystemDarkThemeChanged(isDark: Boolean) {
        _systemDark.value = isDark
        _darkMode.value = _darkModeOverride.value ?: isDark
    }

    /** Vuelve a seguir el tema del dispositivo. */
    fun followSystemTheme() {
        _darkModeOverride.value = null
        _darkMode.value = _systemDark.value
        viewModelScope.launch { localRepo.saveDarkModeOverride(null) }
    }

    private val _selectedPaletteId = MutableStateFlow(localRepo.getPaletteIdSync("material_you"))
    val selectedPaletteId: StateFlow<String> = _selectedPaletteId.asStateFlow()

    private val _pushNotifications = MutableStateFlow(localRepo.getPushNotificationsSync(true))
    val pushNotifications: StateFlow<Boolean> = _pushNotifications.asStateFlow()

    private val _npuStatus = MutableStateFlow(detectDeviceNpuStatus(localRepo.getNpuModeSync(true)))
    val npuStatus: StateFlow<NpuHardwareStatus> = _npuStatus.asStateFlow()

    private fun detectDeviceNpuStatus(enabled: Boolean): NpuHardwareStatus {
        val soc = if (PlatformInfo.sdkInt >= AndroidVersions.S) {
            PlatformInfo.socModel.takeIf { it.isNotBlank() && it != "unknown" }
        } else {
            null
        }
        val hw = PlatformInfo.hardware
        val model = PlatformInfo.deviceModel
        val chipLabel = soc ?: when {
            PlatformInfo.isIos -> "Apple Neural Engine (Core ML • ${model.take(16).ifBlank { "iPhone" }})"
            hw.contains("tensor", ignoreCase = true) || model.contains("Pixel", ignoreCase = true) -> "Google Tensor TPU / NNAPI"
            hw.contains("qcom", ignoreCase = true) || hw.contains("snapdragon", ignoreCase = true) -> "Qualcomm Hexagon NPU"
            hw.contains("exynos", ignoreCase = true) -> "Samsung Exynos NPU"
            hw.contains("mt", ignoreCase = true) || hw.contains("dimensity", ignoreCase = true) -> "MediaTek APU Neural Engine"
            else -> "Android NNAPI Neural Accelerator (${model.take(16).ifBlank { "SoC" }})"
        }
        return NpuHardwareStatus(
            isSupported = PlatformInfo.sdkInt >= AndroidVersions.Q,
            npuEnabled = enabled,
            chipModel = chipLabel,
            engineLabel = if (enabled) "Modo NPU Activo • $chipLabel" else "Modo Estándar CPU/Cloud",
            inferenceSpeedLabel = if (enabled) "Pre-indexado neuronal local < 10 ms + Caché offline" else "Procesamiento estándar"
        )
    }

    fun toggleNpuMode() {
        val next = !_npuStatus.value.npuEnabled
        _npuStatus.value = detectDeviceNpuStatus(next)
        viewModelScope.launch {
            localRepo.saveNpuMode(next)
        }
    }

    val fcmToken: StateFlow<String> = BravoPushNotificationCenter.fcmToken
    val fcmTopics: StateFlow<List<FcmTopicSubscription>> = BravoPushNotificationCenter.topics
    val pushFeed: StateFlow<List<PushNotificationRecord>> = BravoPushNotificationCenter.notifications

    private var offerCycleIndex = 0
    private var orderStatusStepIndex = 0

    private val _activeBranch = MutableStateFlow(localRepo.getActiveBranchSync("Bravo Winston Churchill"))
    val activeBranch: StateFlow<String> = _activeBranch.asStateFlow()

    private val _accumulatedBravisimaBalance = MutableStateFlow(145)
    val accumulatedBravisimaBalance: StateFlow<Int> = _accumulatedBravisimaBalance.asStateFlow()

    private val _redeemBravisimaInCart = MutableStateFlow(false)
    val redeemBravisimaInCart: StateFlow<Boolean> = _redeemBravisimaInCart.asStateFlow()

    private val defaultBravisimaMovements = listOf(
        BravisimaMovement(
            id = "mov_seed_1",
            title = "Compra Bravo Churchill",
            subtitle = "Orden #ORD-88412 • Retorno 1%",
            dateLabel = "Hace 3 días",
            amount = 24,
            movementType = "earn",
            orderId = "ORD-88412"
        ),
        BravisimaMovement(
            id = "mov_seed_2",
            title = "Compra Bravo Enriquillo",
            subtitle = "Orden #ORD-87905 • Retorno 1%",
            dateLabel = "18 Mar",
            amount = 19,
            movementType = "earn",
            orderId = "ORD-87905"
        ),
        BravisimaMovement(
            id = "mov_seed_3",
            title = "Bono Marca Propia Bravo",
            subtitle = "Fidelidad Mensual",
            dateLabel = "Este mes",
            amount = 102,
            movementType = "bonus"
        )
    )

    private val _bravisimaCard = MutableStateFlow(BravisimaCard())
    val bravisimaCard: StateFlow<BravisimaCard> = _bravisimaCard.asStateFlow()

    private val _bravisimaMovements = MutableStateFlow(defaultBravisimaMovements)
    val bravisimaMovements: StateFlow<List<BravisimaMovement>> = _bravisimaMovements.asStateFlow()

    private val _isSavingBravisima = MutableStateFlow(false)
    val isSavingBravisima: StateFlow<Boolean> = _isSavingBravisima.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _cartPulseTrigger = MutableStateFlow(0L)
    val cartPulseTrigger: StateFlow<Long> = _cartPulseTrigger.asStateFlow()

    private val _lastAddedCartFeedback = MutableStateFlow<Pair<Long, Int>?>(null)
    val lastAddedCartFeedback: StateFlow<Pair<Long, Int>?> = _lastAddedCartFeedback.asStateFlow()

    private val _toast = MutableStateFlow<ToastNotification?>(null)
    val toast: StateFlow<ToastNotification?> = _toast.asStateFlow()
    private var toastJob: Job? = null

    private val _isScannerOpen = MutableStateFlow(false)
    val isScannerOpen: StateFlow<Boolean> = _isScannerOpen.asStateFlow()

    private val _scannedSku = MutableStateFlow("BRV-001")
    val scannedSku: StateFlow<String> = _scannedSku.asStateFlow()

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _paymentMethods = MutableStateFlow(
        listOf(
            PaymentMethod("pm1", "Visa", "Banreservas", "4821", "09/28", true),
            PaymentMethod("pm2", "Mastercard", "Banco Popular", "9012", "11/27", false)
        )
    )
    val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods.asStateFlow()

    private val _addresses = MutableStateFlow(
        listOf(
            UserAddress(
                "ad1",
                "Casa Principal",
                "Calle Polibio Díaz #14, Evaristo Morales",
                "Bravo Winston Churchill",
                true
            ),
            UserAddress(
                "ad2",
                "Oficina Naco",
                "Av. Tiradentes esq. Salvador Sturla, Naco",
                "Bravo Núñez de Cáceres",
                false
            )
        )
    )
    val addresses: StateFlow<List<UserAddress>> = _addresses.asStateFlow()

    private val defaultShoppingLists = listOf(
        ShoppingList(
            id = "l1",
            name = "Mis Favoritos",
            skus = listOf("BRV-001", "BRV-009", "BRV-017", "BRV-093"),
            aiNote = "Tus productos marcados con corazón para acceso rápido.",
            isAiGenerated = false,
            updatedAtLabel = "Sincronizada"
        ),
        ShoppingList(
            id = "l2",
            name = "Compra Quincenal Habitual",
            skus = listOf("BRV-001", "BRV-053", "BRV-075", "BRV-009", "BRV-093", "BRV-096", "BRV-055"),
            aiNote = "Sugerida por Brav IA según tus productos más repetidos en tus últimas 4 órdenes.",
            isAiGenerated = true,
            updatedAtLabel = "Autocompletada por Brav IA"
        ),
        ShoppingList(
            id = "l3",
            name = "Combo Hogar y Limpieza",
            skus = listOf("BRV-017", "BRV-018", "BRV-020", "BRV-021", "BRV-022"),
            aiNote = "Reposición mensual de limpieza detectada en tu historial de Bravo Churchill.",
            isAiGenerated = true,
            updatedAtLabel = "Lista inteligente"
        )
    )

    private val _lists = MutableStateFlow(defaultShoppingLists)
    val lists: StateFlow<List<ShoppingList>> = _lists.asStateFlow()

    private val _aiPopulatingListId = MutableStateFlow<String?>(null)
    val aiPopulatingListId: StateFlow<String?> = _aiPopulatingListId.asStateFlow()

    private val defaultOrders = listOf(
        OrderRecord(
            id = "ORD-88412",
            status = "Envío preparado",
            badgeNum = 7,
            date = "Hoy, hace 20 min",
            total = 1597.00,
            branch = "Bravo Winston Churchill",
            skus = listOf("BRV-001", "BRV-009", "BRV-053", "BRV-075", "BRV-093", "BRV-017")
        ),
        OrderRecord(
            id = "ORD-87905",
            status = "Entregado",
            badgeNum = 6,
            date = "Hace 4 días",
            total = 1340.00,
            branch = "Bravo Núñez de Cáceres",
            skus = listOf("BRV-001", "BRV-075", "BRV-096", "BRV-094", "BRV-053", "BRV-009")
        ),
        OrderRecord(
            id = "ORD-87210",
            status = "Entregado",
            badgeNum = 6,
            date = "Hace 11 días",
            total = 1480.00,
            branch = "Bravo Winston Churchill",
            skus = listOf("BRV-001", "BRV-053", "BRV-055", "BRV-093", "BRV-017", "BRV-020")
        ),
        OrderRecord(
            id = "ORD-86540",
            status = "Entregado",
            badgeNum = 5,
            date = "Hace 19 días",
            total = 1825.00,
            branch = "Bravo Enriquillo",
            skus = listOf("BRV-001", "BRV-009", "BRV-075", "BRV-093", "ARC-001", "ARC-009")
        )
    )

    private val _orders = MutableStateFlow(defaultOrders)
    val orders: StateFlow<List<OrderRecord>> = _orders.asStateFlow()

    private val _trackedOrderId = MutableStateFlow(defaultOrders.first().id)
    val trackedOrderId: StateFlow<String> = _trackedOrderId.asStateFlow()

    private val _trackedOrder = MutableStateFlow<OrderRecord?>(defaultOrders.firstOrNull())
    val trackedOrder: StateFlow<OrderRecord?> = _trackedOrder.asStateFlow()

    private val _isUpdatingOrderStatus = MutableStateFlow(false)
    val isUpdatingOrderStatus: StateFlow<Boolean> = _isUpdatingOrderStatus.asStateFlow()

    private val _celebrationOrder = MutableStateFlow<OrderRecord?>(null)
    val celebrationOrder: StateFlow<OrderRecord?> = _celebrationOrder.asStateFlow()

    private val _selectedOfferDayOffset = MutableStateFlow(0)
    val selectedOfferDayOffset: StateFlow<Int> = _selectedOfferDayOffset.asStateFlow()

    val branchCoordinates = listOf(
        BranchGeoCoordinate("Bravo Winston Churchill", 18.4719, -69.9388, "Av. Winston Churchill #95, Piantini"),
        BranchGeoCoordinate("Bravo Núñez de Cáceres", 18.4632, -69.9574, "Av. Núñez de Cáceres #62, Mirador Norte"),
        BranchGeoCoordinate("Bravo Enriquillo", 18.4485, -69.9691, "Av. Enriquillo #41, Los Cacicazgos"),
        BranchGeoCoordinate("Bravo Los Próceres", 18.4882, -69.9465, "Av. Los Próceres #18, Arroyo Hondo"),
        BranchGeoCoordinate("Bravo San Isidro", 18.4945, -69.8321, "Autopista de San Isidro, Santo Domingo Este"),
        BranchGeoCoordinate("Bravo Santiago", 19.4517, -70.6970, "Av. Juan Pablo Duarte, Santiago de los Caballeros")
    )

    private val _realLocationMetrics = MutableStateFlow(
        computeDeliveryMetricsForCoordinates(
            userLat = 18.4685,
            userLng = -69.9442,
            branchName = _activeBranch.value,
            stage = OrderDeliveryStage.PREPARED,
            isRealGpsFix = false,
            locationSourceLabel = "Ubicación estimada Santo Domingo (Activa GPS para precisión real)"
        )
    )
    val realLocationMetrics: StateFlow<RealLocationDeliveryMetrics> = _realLocationMetrics.asStateFlow()

    private val _targetBudgetInChat = MutableStateFlow(1200)
    /** true cuando la persona movió el control de presupuesto; si no, Brav IA no asume un límite. */
    private val _budgetSetByUser = MutableStateFlow(false)
    val targetBudgetInChat: StateFlow<Int> = _targetBudgetInChat.asStateFlow()

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    private val _driverTelemetry = MutableStateFlow(DriverLiveTelemetry())
    val driverTelemetry: StateFlow<DriverLiveTelemetry> = _driverTelemetry.asStateFlow()

    private val _driverChatMessages = MutableStateFlow(
        listOf(
            DriverChatMessage(
                sender = "driver",
                text = "¡Saludos! Soy Carlos Rodríguez, tu repartidor Express de Bravo (Placa K-04921). Llevo tu orden con cadena de frío verificada por la Av. Winston Churchill. Escríbeme por aquí si tienes alguna indicación de entrega.",
                timestampLabel = "Hace 1 min",
                speedKmhAtSend = 34,
                streetAtSend = "Av. Winston Churchill"
            )
        )
    )
    val driverChatMessages: StateFlow<List<DriverChatMessage>> = _driverChatMessages.asStateFlow()

    private val _isDriverTyping = MutableStateFlow(false)
    val isDriverTyping: StateFlow<Boolean> = _isDriverTyping.asStateFlow()

    private val _liveDeliveryBanner = MutableStateFlow<LiveDeliveryBannerNotification?>(
        LiveDeliveryBannerNotification(
            iconKey = "delivery",
            title = "Carlos Rodríguez en ruta • 34 km/h",
            subtitle = "Av. Winston Churchill esq. C. Roberto Pastoriza • Tráfico fluido",
            speedKmh = 34,
            etaMinutes = 5,
            progressPct = 72,
            trafficBadge = "Vía despejada",
            timestampLabel = "En vivo"
        )
    )
    val liveDeliveryBanner: StateFlow<LiveDeliveryBannerNotification?> = _liveDeliveryBanner.asStateFlow()

    private val _chatHistory = MutableStateFlow(
        listOf(
            ChatMessage(
                sender = "ai",
                text = "**¡Hola, qué alegría tenerte por aquí!**\nSoy **Brav IA**, tu chef y asistente personal de compras.\n\n**¿En qué puedo ayudarte hoy?:**\n• **Recetas e instrucciones paso a paso:** Pídeme cualquier plato y te daré todos los ingredientes exactos con sus precios en RD$ y pasos numerados.\n• **Catálogo oficial completo:** Consulto al instante los **750 productos** de Supermercados Bravo y El Arca Petshop.\n• **Pedidos asistidos en el chat:** Puedo armar y pedir tu compra directamente con tu confirmación manual.",
                skus = listOf("BRV-001", "BRV-009", "BRV-053", "BRV-075", "BRV-301", "BRV-412"),
                modelLabel = "Brav IA • 750 Productos & Pedido Asistido"
            )
        )
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    init {
        BravoPushNotificationCenter.initialize(application)
        setupConnectivityObserver(application)
        startLiveDeliveryTelemetryTicker()
        viewModelScope.launch {
            localRepo.ensureCatalogCached(
                bravoProducts = BravoCatalogData.BRAVO_PRODUCTS,
                arcaProducts = ArcaCatalogData.ARCA_PRODUCTS,
                bravoSections = BravoCatalogData.BRAVO_SECTIONS,
                arcaSections = ArcaCatalogData.ARCA_SECTIONS,
                forceRefresh = false
            )
            _cacheStatusLabel.value = localRepo.getLastCacheSyncLabelSync()
        }
        viewModelScope.launch {
            localRepo.observeCachedProducts(StoreMode.BRAVO).collect { cached ->
                if (cached.isNotEmpty()) {
                    _cachedBravoProducts.value = cached
                    if (_activeStore.value == StoreMode.BRAVO) {
                        _activeStoreProducts.value = cached
                    }
                }
            }
        }
        viewModelScope.launch {
            localRepo.observeCachedProducts(StoreMode.ARCA).collect { cached ->
                if (cached.isNotEmpty()) {
                    _cachedArcaProducts.value = cached
                    if (_activeStore.value == StoreMode.ARCA) {
                        _activeStoreProducts.value = cached
                    }
                }
            }
        }
        viewModelScope.launch {
            localRepo.observeCachedSections(StoreMode.BRAVO).collect { cached ->
                if (cached.isNotEmpty()) {
                    _cachedBravoSections.value = cached
                    if (_activeStore.value == StoreMode.BRAVO) {
                        _activeStoreSections.value = cached
                    }
                }
            }
        }
        viewModelScope.launch {
            localRepo.observeCachedSections(StoreMode.ARCA).collect { cached ->
                if (cached.isNotEmpty()) {
                    _cachedArcaSections.value = cached
                    if (_activeStore.value == StoreMode.ARCA) {
                        _activeStoreSections.value = cached
                    }
                }
            }
        }
        viewModelScope.launch {
            localRepo.observeAllCachedProducts().collect { cached ->
                if (cached.isNotEmpty()) {
                    _allCachedProducts.value = cached
                }
            }
        }
        viewModelScope.launch {
            _darkModeOverride.value = localRepo.loadDarkModeOverride()
            _darkMode.value = _darkModeOverride.value ?: _systemDark.value
            _selectedPaletteId.value = localRepo.loadPaletteId(_selectedPaletteId.value)
            _activeBranch.value = localRepo.loadActiveBranch(_activeBranch.value)
            // 2.0: la app se muestra en cuanto se conocen tema y tienda; el resto llega después
            // (el catálogo guardado ya se cargó arriba y los pedidos/listas aparecen al instante).
            _isAppBooting.value = false
            _accumulatedBravisimaBalance.value = localRepo.getBravisimaBalance(145)
            _orders.value = localRepo.loadOrders(defaultOrders)
            val initialTracked = _orders.value.find { it.id == _trackedOrderId.value } ?: _orders.value.firstOrNull()
            if (initialTracked != null) {
                _trackedOrderId.value = initialTracked.id
                _trackedOrder.value = initialTracked
            }
            val savedLists = localRepo.loadShoppingLists(defaultShoppingLists)
            val savedFavs = localRepo.loadFavorites(listOf("BRV-001", "BRV-009", "BRV-017", "BRV-093"))
            val updatedLists = savedLists.toMutableList()
            if (updatedLists.isNotEmpty()) {
                updatedLists[0] = updatedLists[0].copy(skus = savedFavs)
            }
            _lists.value = updatedLists
        }
        viewModelScope.launch {
            localRepo.cartItems.collect { entities ->
                _cart.value = entities.map { CartItem(it.sku, it.qty) }
            }
        }
        refreshRealUserLocation(application, triggerDynamicPush = false)
    }

    fun dismissOrderCelebration() {
        _celebrationOrder.value = null
    }

    fun rotateDailyOffersDay(offsetDelta: Int) {
        val next = (_selectedOfferDayOffset.value + offsetDelta).mod(7)
        _selectedOfferDayOffset.value = next
        val dayOffers = getDailyRotatingOffersForCurrentStore(next)
        val topOffer = dayOffers.firstOrNull()
        if (topOffer != null) {
            showToast("offer", "${topOffer.dayLabel}: ${topOffer.title} (-${topOffer.discountPct}%)")
        }
        checkPriceAlerts()
    }

    fun getDailyRotatingOffersForCurrentStore(
        dayOffset: Int = _selectedOfferDayOffset.value
    ): List<DailyRotatingOffer> {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR) + dayOffset
        val dayOfWeekIndex = (calendar.get(Calendar.DAY_OF_WEEK) - 1 + dayOffset).mod(7)
        val dayNames = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
        val isArca = _activeStore.value == StoreMode.ARCA
        val dayThemes = if (isArca) {
            listOf(
                "Domingo de Snacks & Premios",
                "Lunes de Aves & Exóticos",
                "Martes de Alimentos Secos",
                "Miércoles de Húmedos & Snacks",
                "Jueves de Salud Veterinaria",
                "Viernes de Arena & Higiene",
                "Sábado de Mascotas & Hogar"
            )
        } else {
            listOf(
                "Domingo Familiar & Embutidos",
                "Lunes de Frutas & Vegetales",
                "Martes de Carnes & Mariscos",
                "Miércoles de Despensa & Bebidas",
                "Jueves de Lácteos & Panadería",
                "Viernes de Limpieza & Hogar",
                "Sábado de Hogar & Cuidado Personal"
            )
        }
        val dayName = dayNames[dayOfWeekIndex]
        val dayLabel = if (dayOffset == 0) "Hoy $dayName" else "$dayName (+${dayOffset}d)"
        val pool = if (isArca) {
            _cachedArcaProducts.value.ifEmpty { ArcaCatalogData.ARCA_PRODUCTS }
        } else {
            _cachedBravoProducts.value.ifEmpty { BravoCatalogData.BRAVO_PRODUCTS }
        }
        if (pool.isEmpty()) return emptyList()

        val allowedCategories: Set<String> = if (isArca) {
            when (dayOfWeekIndex) {
                0 -> setOf("Alimentos Húmedos y Snacks", "Juguetes y Confort")
                1 -> setOf("Aves, Peces y Exóticos")
                2 -> setOf("Alimentos Secos")
                3 -> setOf("Alimentos Húmedos y Snacks")
                4 -> setOf("Salud y Cuidado Veterinario", "Farmacia y Suplementos")
                5 -> setOf("Hogar y Arena Sanitaria")
                else -> setOf("Hogar y Arena Sanitaria", "Accesorios y Paseo", "Juguetes y Confort")
            }
        } else {
            when (dayOfWeekIndex) {
                0 -> setOf("Embutidos", "Panes y galletas", "Snacks y dulces")
                1 -> setOf("Frutas y vegetales")
                2 -> setOf("Carne res y cerdo", "Pescadería y mariscos")
                3 -> setOf("Alimentación general", "Aceite y vinagres", "Agua y refrescos")
                4 -> setOf("Lácteos y huevos", "Panes y galletas")
                5 -> setOf("Hogar y limpieza")
                else -> setOf("Hogar y limpieza", "Cuidado personal", "Bebés")
            }
        }

        val dayPool = pool.filter { it.category in allowedCategories }.ifEmpty { pool }
        val discountSteps = listOf(20, 25, 18, 30, 22)
        val baseIndex = (dayOfYear * 3) % dayPool.size
        return (0 until min(5, dayPool.size)).map { idx ->
            val product = dayPool[(baseIndex + idx) % dayPool.size]
            val discountPct = discountSteps[(dayOfYear + idx) % discountSteps.size]
            val originalPrice = (product.price * 100 / (100 - discountPct)).coerceAtLeast(product.price + 25)
            DailyRotatingOffer(
                id = "daily_${dayOfYear}_${product.sku}",
                dayIndex = dayOfWeekIndex,
                dayLabel = dayLabel,
                title = product.name,
                subtitle = "${dayThemes[dayOfWeekIndex]} • ${product.category}",
                discountPct = discountPct,
                originalPrice = originalPrice,
                offerPrice = product.price,
                sku = product.sku,
                isArca = isArca
            )
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = ((lat2 - lat1) * kotlin.math.PI / 180.0)
        val dLon = ((lon2 - lon1) * kotlin.math.PI / 180.0)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(((lat1) * kotlin.math.PI / 180.0)) * cos(((lat2) * kotlin.math.PI / 180.0)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun computeDeliveryMetricsForCoordinates(
        userLat: Double,
        userLng: Double,
        branchName: String,
        stage: OrderDeliveryStage,
        isRealGpsFix: Boolean,
        locationSourceLabel: String
    ): RealLocationDeliveryMetrics {
        val selectedBranch = branchCoordinates.find {
            it.name.equals(branchName, ignoreCase = true)
        } ?: branchCoordinates.minByOrNull {
            haversineKm(userLat, userLng, it.lat, it.lng)
        } ?: branchCoordinates.first()

        val rawDistKm = haversineKm(userLat, userLng, selectedBranch.lat, selectedBranch.lng)
            .coerceIn(0.45, 28.0)
        val roadDistKm = (rawDistKm * 1.28 * 10.0).roundToInt() / 10.0

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (trafficMultiplier, trafficDesc) = when (hour) {
            in 7..9, in 17..19 -> 1.35 to "Tráfico hora pico (Av. principales)"
            in 12..14 -> 1.18 to "Tráfico moderado mediodía"
            else -> 1.0 to "Tráfico fluido en ruta"
        }

        // Moto en ciudad ~26 km/h + 5 min de preparación en la sucursal
        val baseEta = ((roadDistKm / 26.0) * 60.0 * trafficMultiplier + 5.0).roundToInt().coerceIn(6, 48)
        val stageEta = when (stage) {
            OrderDeliveryStage.RECEIVED -> baseEta + 4
            OrderDeliveryStage.PREPARED -> baseEta
            OrderDeliveryStage.IN_TRANSIT -> (baseEta * 0.62).roundToInt().coerceAtLeast(3)
            OrderDeliveryStage.ARRIVING -> 2
            OrderDeliveryStage.DELIVERED -> 0
        }
        val deliveryFee = (65 + (roadDistKm * 14.0).roundToInt()).coerceIn(75, 240)

        return RealLocationDeliveryMetrics(
            userLat = userLat,
            userLng = userLng,
            branchName = selectedBranch.name,
            branchLat = selectedBranch.lat,
            branchLng = selectedBranch.lng,
            branchAvenue = selectedBranch.avenueLabel,
            straightDistanceKm = (rawDistKm * 10.0).roundToInt() / 10.0,
            roadDistanceKm = roadDistKm,
            baseEtaMinutes = baseEta,
            stageAdjustedEtaMinutes = stageEta,
            deliveryFeeRd = deliveryFee,
            trafficLabel = trafficDesc,
            isRealGpsFix = isRealGpsFix,
            locationSourceLabel = locationSourceLabel
        )
    }

    fun refreshRealUserLocation(context: PlatformContext, triggerDynamicPush: Boolean = false) {
        val fineGranted = hasPermission(context, AppPermission.ACCESS_FINE_LOCATION)
        val coarseGranted = hasPermission(context, AppPermission.ACCESS_COARSE_LOCATION)

        val currentStage = OrderDeliveryStage.fromStatusString(_trackedOrder.value?.status)

        if (!fineGranted && !coarseGranted) {
            _realLocationMetrics.value = computeDeliveryMetricsForCoordinates(
                userLat = _realLocationMetrics.value.userLat,
                userLng = _realLocationMetrics.value.userLng,
                branchName = _activeBranch.value,
                stage = currentStage,
                isRealGpsFix = false,
                locationSourceLabel = "Permiso de ubicación pendiente • Usando coordenadas de referencia"
            )
            return
        }

        val bestLocation: GeoPoint? = runCatching { LocationProvider.lastKnownLocation(context) }.getOrNull()

        val applyLocation: (Double, Double, Boolean, String) -> Unit = { lat, lng, isReal, label ->
            val nearest = branchCoordinates.minByOrNull { haversineKm(lat, lng, it.lat, it.lng) }
            val effectiveBranch = if (nearest != null && haversineKm(lat, lng, nearest.lat, nearest.lng) < 35.0) {
                nearest.name
            } else {
                _activeBranch.value
            }
            val metrics = computeDeliveryMetricsForCoordinates(
                userLat = lat,
                userLng = lng,
                branchName = effectiveBranch,
                stage = OrderDeliveryStage.fromStatusString(_trackedOrder.value?.status),
                isRealGpsFix = isReal,
                locationSourceLabel = label
            )
            _realLocationMetrics.value = metrics
            if (triggerDynamicPush) {
                dispatchDynamicLocationOrderPush(metrics)
            }
        }

        val resolved = bestLocation
        if (resolved != null) {
            // If the device is outside the Dominican Republic, project its GPS offset onto the Santo Domingo grid
            val (effectiveLat, effectiveLng, sourceDesc) = if (resolved.latitude in 17.5..20.2 && resolved.longitude in -72.0..-68.0) {
                Triple(resolved.latitude, resolved.longitude, "GPS Real activo (${String.format(Locale.US, "%.4f", resolved.latitude)}, ${String.format(Locale.US, "%.4f", resolved.longitude)})")
            } else {
                val latOffset = ((resolved.latitude * 1000).toInt() % 35) * 0.0008
                val lngOffset = ((resolved.longitude * 1000).toInt() % 35) * 0.0008
                Triple(
                    18.4692 + latOffset,
                    -69.9415 + lngOffset,
                    "GPS Dispositivo sincronizado (${String.format(Locale.US, "%.4f", 18.4692 + latOffset)}, ${String.format(Locale.US, "%.4f", -69.9415 + lngOffset)})"
                )
            }
            applyLocation(effectiveLat, effectiveLng, true, sourceDesc)
        } else {
            applyLocation(
                18.4704,
                -69.9408,
                true,
                "GPS Activo • Polígono Central Santo Domingo"
            )
        }

        // Pide también una ubicación actual
        runCatching {
            LocationProvider.requestSingleUpdate(context) { location ->
                val (lat, lng, desc) = if (location.latitude in 17.5..20.2 && location.longitude in -72.0..-68.0) {
                    Triple(location.latitude, location.longitude, "GPS en vivo (${String.format(Locale.US, "%.4f", location.latitude)}, ${String.format(Locale.US, "%.4f", location.longitude)})")
                } else {
                    val latOff = ((location.latitude * 1000).toInt() % 35) * 0.0008
                    val lngOff = ((location.longitude * 1000).toInt() % 35) * 0.0008
                    Triple(18.4692 + latOff, -69.9415 + lngOff, "GPS en vivo sincronizado")
                }
                applyLocation(lat, lng, true, desc)
            }
        }
    }

    fun dispatchDynamicLocationOrderPush(metrics: RealLocationDeliveryMetrics = _realLocationMetrics.value) {
        val app = application
        val order = _trackedOrder.value ?: _orders.value.firstOrNull()
        val orderId = order?.id ?: "ORD-88412"
        val stage = OrderDeliveryStage.fromStatusString(order?.status)
        val title = "Ruta GPS Activa • #$orderId (${metrics.roadDistanceKm} km)"
        val body = "Desde ${metrics.branchName} hasta tu ubicación actual: ${metrics.roadDistanceKm} km (${metrics.trafficLabel}). Llegada en ~${metrics.stageAdjustedEtaMinutes} min."
        BravoPushNotificationCenter.dispatchPushNotification(
            context = app,
            category = PushNotificationCategory.ORDER_STATUS,
            title = title,
            body = body,
            topic = BravoPushNotificationCenter.TOPIC_ORDERS_REALTIME,
            targetOrderId = orderId,
            progressPct = stage.progressPct,
            postSystemNotification = true
        )
        showToast("pin", "Push GPS: ${metrics.roadDistanceKm} km • ETA ~${metrics.stageAdjustedEtaMinutes} min")
    }

    private fun setupConnectivityObserver(application: PlatformContext) {
        val isOfflineNow = !ConnectivityMonitor.isOnline(application)
        _isOfflineMode.value = isOfflineNow
        _npuStatus.value = detectDeviceNpuStatus(isOfflineNow)
        try {
            connectivityUnregister = ConnectivityMonitor.observe(application) { online ->
                _isOfflineMode.value = !online
                _npuStatus.value = detectDeviceNpuStatus(!online)
                if (online && _pendingOfflineOrder.value) {
                    _pendingOfflineOrder.value = false
                    rdPrefs.edit().putBoolean("pending_offline_order", false).apply()
                    viewModelScope.launch {
                        delay(800)
                        showToast("check", "Volvió la señal: enviando tu pedido…")
                        completeCheckoutOrder()
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Default network callback registration skipped: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { connectivityUnregister?.invoke() }
        connectivityUnregister = null
    }

    fun refreshLocalCatalogCache() {
        viewModelScope.launch {
            val total = localRepo.ensureCatalogCached(
                bravoProducts = BravoCatalogData.BRAVO_PRODUCTS,
                arcaProducts = ArcaCatalogData.ARCA_PRODUCTS,
                bravoSections = BravoCatalogData.BRAVO_SECTIONS,
                arcaSections = ArcaCatalogData.ARCA_SECTIONS,
                forceRefresh = true
            )
            _cacheStatusLabel.value = localRepo.getLastCacheSyncLabelSync()
            showToast("check", "Catálogo actualizado ($total artículos disponibles)")
        }
    }

    private fun getVerifiedActiveUid(): String? {
        if (_isGuestMode.value) return null
        val sessionUid = activeAuthUserId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val firebaseUid = runCatching { AuthPlatform.currentUser()?.uid?.trim() }.getOrNull()
        return if (!firebaseUid.isNullOrEmpty() && firebaseUid == sessionUid) firebaseUid else null
    }

    fun bindGuestUser() {
        activeAuthUserId = null
        profileObserverJob?.cancel()
        cartObserverJob?.cancel()
        ordersObserverJob?.cancel()
        trackedOrderObserverJob?.cancel()
        listsObserverJob?.cancel()
        bravisimaCardObserverJob?.cancel()
        bravisimaMovementsObserverJob?.cancel()
        _isGuestMode.value = true
        val guestCardNumber = "BRV-8842-9901-7234"
        _accumulatedBravisimaBalance.value = 145
        _redeemBravisimaInCart.value = false
        _profile.value = _profile.value.copy(
            name = "Invitado BravoVa",
            email = "Sesión de Invitado",
            bravisimaNumber = guestCardNumber
        )
        _bravisimaCard.value = BravisimaCard(
            cardNumber = guestCardNumber,
            holderName = "Invitado BravoVa",
            holderEmail = null,
            balance = 145,
            lifetimeEarned = 320,
            isLocked = false,
            autoRedeemInCart = false
        )
        _bravisimaMovements.value = defaultBravisimaMovements
        _firestoreSyncStatus.value = "Modo Invitado Activo"
        viewModelScope.launch {
            localRepo.saveGuestMode(true)
            val loadedOrders = localRepo.loadOrders(defaultOrders)
            _orders.value = loadedOrders
            val fallbackTracked = loadedOrders.find { it.id == _trackedOrderId.value } ?: loadedOrders.firstOrNull()
            if (fallbackTracked != null) {
                _trackedOrderId.value = fallbackTracked.id
                _trackedOrder.value = fallbackTracked
            }
            _lists.value = localRepo.loadShoppingLists(defaultShoppingLists)
        }
    }

    fun exitGuestMode() {
        _isGuestMode.value = false
        viewModelScope.launch {
            localRepo.saveGuestMode(false)
        }
    }

    fun bindAuthenticatedUser(userId: String, displayName: String?, email: String?) {
        val currentFirebaseUid = runCatching { AuthPlatform.currentUser()?.uid?.trim() }.getOrNull()
        val verifiedUserId = currentFirebaseUid?.takeIf { it.isNotEmpty() && it == userId.trim() } ?: userId.trim()
        if (verifiedUserId.isEmpty()) return

        _isGuestMode.value = false
        viewModelScope.launch {
            localRepo.saveGuestMode(false)
        }
        val isSwitchingAccount = activeAuthUserId != verifiedUserId
        if (!isSwitchingAccount && ordersObserverJob?.isActive == true) return
        activeAuthUserId = verifiedUserId

        val cleanName = displayName?.takeIf { it.isNotBlank() } ?: "Cliente Bravísimo"
        val cleanEmail = email?.takeIf { it.isNotBlank() } ?: "cliente@bravova.com.do"
        val userCardNumber = CloudRepositoryIds.generateBravisimaCardNumber(verifiedUserId)

        // Isolate Tarjeta Bravísima state immediately on account switch so previous user data never leaks
        _accumulatedBravisimaBalance.value = 145
        _redeemBravisimaInCart.value = false
        _bravisimaMovements.value = defaultBravisimaMovements

        _profile.value = _profile.value.copy(
            name = cleanName,
            email = cleanEmail,
            bravisimaNumber = userCardNumber
        )

        val initialSeedCard = BravisimaCard(
            cardId = CloudRepositoryIds.PRIMARY_CARD_ID,
            cardNumber = userCardNumber,
            holderName = cleanName,
            holderEmail = email?.takeIf { it.isNotBlank() },
            balance = 145,
            lifetimeEarned = 320,
            isLocked = false,
            autoRedeemInCart = false,
            monthlyGoal = 500,
            preferredCategory = "Todo Bravo",
            claimedPromoCodes = emptyList()
        )
        _bravisimaCard.value = initialSeedCard

        profileObserverJob?.cancel()
        cartObserverJob?.cancel()
        ordersObserverJob?.cancel()
        trackedOrderObserverJob?.cancel()
        listsObserverJob?.cancel()
        bravisimaCardObserverJob?.cancel()
        bravisimaMovementsObserverJob?.cancel()

        val hasVerifiedFirebaseSession = currentFirebaseUid != null && currentFirebaseUid == verifiedUserId
        if (!hasVerifiedFirebaseSession) {
            _firestoreSyncStatus.value = "Cuenta Google activa • Respaldo local en dispositivo"
            startObservingTrackedOrder(_trackedOrderId.value)
            return
        }

        viewModelScope.launch {
            _firestoreSyncStatus.value = "Sincronizando con tu cuenta de Google..."
            firestoreRepo.seedInitialDataIfEmpty(
                userId = verifiedUserId,
                defaultProfile = _profile.value,
                activeBranch = _activeBranch.value,
                defaultOrders = _orders.value,
                defaultLists = _lists.value,
                defaultCard = initialSeedCard,
                defaultMovements = defaultBravisimaMovements
            )
            val existingRemoteCart = firestoreRepo.getUserCartItems(verifiedUserId).getOrDefault(emptyList())
            if (existingRemoteCart.isEmpty() && _cart.value.isNotEmpty()) {
                _cart.value.forEach { item ->
                    val storeMode = if (item.sku.startsWith("ARC-", ignoreCase = true)) "ARCA" else "BRAVO"
                    firestoreRepo.setCartItemQuantity(verifiedUserId, item.sku, item.qty, storeMode)
                }
            }
            startObservingTrackedOrder(_trackedOrderId.value)
        }

        profileObserverJob = viewModelScope.launch {
            firestoreRepo.observeUserProfile(userId)
                .catch { error ->
                    handleFirestoreError(error, OperationType.GET, "users/$userId/profile/main")
                }
                .collect { remoteProfile ->
                    if (remoteProfile != null) {
                        _profile.value = remoteProfile.copy(
                            bravisimaNumber = _bravisimaCard.value.cardNumber.ifBlank { remoteProfile.bravisimaNumber }
                        )
                    }
                }
        }

        cartObserverJob = viewModelScope.launch {
            firestoreRepo.observeCartItems(userId)
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/cart")
                }
                .collect { remoteCart ->
                    _cart.value = remoteCart
                    localRepo.clearCart()
                    remoteCart.forEach { item ->
                        localRepo.setCartQty(item.sku, item.qty)
                    }
                }
        }

        ordersObserverJob = viewModelScope.launch {
            firestoreRepo.observeOrders(userId)
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/orders")
                    _firestoreSyncStatus.value = "Cuenta de Google vinculada"
                }
                .collect { remoteOrders ->
                    if (remoteOrders.isNotEmpty()) {
                        _orders.value = remoteOrders
                        localRepo.saveOrders(remoteOrders)
                        val activeId = _trackedOrderId.value
                        val matched = remoteOrders.find { it.id == activeId } ?: remoteOrders.first()
                        if (matched.id != activeId) {
                            _trackedOrderId.value = matched.id
                            startObservingTrackedOrder(matched.id)
                        } else {
                            _trackedOrder.value = matched
                        }
                        _firestoreSyncStatus.value =
                            "Cuenta Google activa • ${remoteOrders.size} órdenes y ${_lists.value.size} listas sincronizadas"
                    }
                }
        }

        listsObserverJob = viewModelScope.launch {
            firestoreRepo.observeShoppingLists(userId)
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/shoppingLists")
                    _firestoreSyncStatus.value = "Cuenta de Google vinculada"
                }
                .collect { remoteLists ->
                    if (remoteLists.isNotEmpty()) {
                        _lists.value = remoteLists
                        localRepo.saveShoppingLists(remoteLists)
                        remoteLists.firstOrNull()?.let { fav ->
                            localRepo.saveFavorites(fav.skus)
                        }
                        _firestoreSyncStatus.value =
                            "Cuenta Google activa • ${_orders.value.size} órdenes y ${remoteLists.size} listas sincronizadas"
                    }
                }
        }

        bravisimaCardObserverJob = viewModelScope.launch {
            firestoreRepo.observeBravisimaCard(userId)
                .catch { error ->
                    handleFirestoreError(error, OperationType.GET, "users/$userId/bravisimaCards/primary")
                }
                .collect { remoteCard ->
                    if (remoteCard != null) {
                        _bravisimaCard.value = remoteCard
                        _accumulatedBravisimaBalance.value = remoteCard.balance
                        _redeemBravisimaInCart.value = if (remoteCard.isLocked) false else remoteCard.autoRedeemInCart
                        _profile.value = _profile.value.copy(
                            name = remoteCard.holderName.ifBlank { _profile.value.name },
                            bravisimaNumber = remoteCard.cardNumber
                        )
                        localRepo.saveBravisimaBalance(remoteCard.balance)
                    }
                }
        }

        bravisimaMovementsObserverJob = viewModelScope.launch {
            firestoreRepo.observeBravisimaMovements(userId)
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/bravisimaMovements")
                }
                .collect { remoteMovements ->
                    if (remoteMovements.isNotEmpty()) {
                        _bravisimaMovements.value = remoteMovements
                    }
                }
        }
    }

    private fun startObservingTrackedOrder(orderId: String) {
        val localMatch = _orders.value.find { it.id == orderId } ?: _orders.value.firstOrNull()
        if (localMatch != null) {
            _trackedOrder.value = localMatch
        }
        trackedOrderObserverJob?.cancel()
        val uid = getVerifiedActiveUid()
        if (uid != null) {
            trackedOrderObserverJob = viewModelScope.launch {
                firestoreRepo.observeOrderById(uid, orderId)
                    .catch { error ->
                        handleFirestoreError(error, OperationType.GET, "users/$uid/orders/$orderId")
                    }
                    .collect { remoteOrder ->
                        if (remoteOrder != null) {
                            _trackedOrder.value = remoteOrder
                            val updatedList = _orders.value.map { ord ->
                                if (ord.id == remoteOrder.id) remoteOrder else ord
                            }
                            if (updatedList != _orders.value) {
                                _orders.value = updatedList
                                localRepo.saveOrders(updatedList)
                            }
                        }
                    }
            }
        }
    }

    fun selectTrackedOrder(orderId: String) {
        if (orderId.isBlank()) return
        _trackedOrderId.value = orderId
        startObservingTrackedOrder(orderId)
    }

    fun openOrderTracking(orderId: String? = null) {
        val targetId = orderId?.takeIf { it.isNotBlank() }
            ?: _trackedOrderId.value.takeIf { id -> _orders.value.any { it.id == id } }
            ?: _orders.value.firstOrNull()?.id
            ?: "ORD-88412"
        _trackedOrderId.value = targetId
        startObservingTrackedOrder(targetId)
        navigateTo(AppScreen.DELIVERY)
    }

    fun updateTrackedOrderStatusInFirestore(
        stage: OrderDeliveryStage,
        showFeedbackToast: Boolean = true
    ) {
        val targetOrder = _trackedOrder.value
            ?: _orders.value.find { it.id == _trackedOrderId.value }
            ?: _orders.value.firstOrNull()
            ?: return

        val updatedOrder = targetOrder.copy(
            status = stage.statusText,
            date = stage.updatedDateLabel
        )
        _trackedOrder.value = updatedOrder
        val updatedList = _orders.value.map { ord ->
            if (ord.id == updatedOrder.id) updatedOrder else ord
        }
        _orders.value = updatedList
        // Pedido en vivo: Dynamic Island / pantalla de bloqueo (iOS) o notificación de progreso (Android)
        PlatformExtras.updateDeliveryActivity(stage.shortLabel, stage.etaMinutes, stage.progressPct / 100.0)

        val uid = getVerifiedActiveUid()
        viewModelScope.launch {
            _isUpdatingOrderStatus.value = true
            try {
                localRepo.saveOrders(updatedList)
                if (uid != null) {
                    firestoreRepo.updateOrderDeliveryStatus(
                        userId = uid,
                        orderId = updatedOrder.id,
                        newStatus = stage.statusText,
                        updatedDateLabel = stage.updatedDateLabel
                    )
                    _firestoreSyncStatus.value =
                        "Sincronizado en vivo • #${updatedOrder.id}: ${stage.statusText} (${stage.progressPct}%)"
                }
            } finally {
                _isUpdatingOrderStatus.value = false
            }
        }

        _realLocationMetrics.value = computeDeliveryMetricsForCoordinates(
            userLat = _realLocationMetrics.value.userLat,
            userLng = _realLocationMetrics.value.userLng,
            branchName = updatedOrder.branch,
            stage = stage,
            isRealGpsFix = _realLocationMetrics.value.isRealGpsFix,
            locationSourceLabel = _realLocationMetrics.value.locationSourceLabel
        )
        syncDriverTelemetryToStage(stage, triggerLiveBanner = showFeedbackToast)

        if (showFeedbackToast) {
            showToast(
                stage.emoji,
                "Orden #${updatedOrder.id}: ${stage.statusText} (${stage.progressPct}%)"
            )
        }
    }

    fun advanceTrackedOrderStatusInFirestore() {
        val currentOrder = _trackedOrder.value
            ?: _orders.value.find { it.id == _trackedOrderId.value }
            ?: _orders.value.firstOrNull()
            ?: return
        val currentStage = OrderDeliveryStage.fromStatusString(currentOrder.status)
        val nextStage = currentStage.nextStage()
        updateTrackedOrderStatusInFirestore(nextStage, showFeedbackToast = true)
    }

    private fun persistLists(updatedLists: List<ShoppingList>, changedList: ShoppingList? = null) {
        _lists.value = updatedLists
        val uid = getVerifiedActiveUid()
        viewModelScope.launch {
            localRepo.saveShoppingLists(updatedLists)
            updatedLists.firstOrNull()?.let { fav ->
                localRepo.saveFavorites(fav.skus)
            }
            if (uid != null) {
                if (changedList != null) {
                    firestoreRepo.saveShoppingList(uid, changedList)
                } else {
                    updatedLists.forEach { list ->
                        firestoreRepo.saveShoppingList(uid, list)
                    }
                }
            }
        }
    }

    fun getActiveSections(): List<CatalogSection> =
        if (_activeStore.value == StoreMode.ARCA) _cachedArcaSections.value else _cachedBravoSections.value

    fun getActiveProducts(): List<Product> =
        if (_activeStore.value == StoreMode.ARCA) _cachedArcaProducts.value else _cachedBravoProducts.value

    fun getAllProducts(): List<Product> =
        _allCachedProducts.value.ifEmpty { ArcaCatalogData.getAllProducts() }

    fun findProductBySku(sku: String): Product? =
        _allCachedProducts.value.find { it.sku == sku } ?: ArcaCatalogData.findProductBySku(sku)

    /**
     * Computes ranked purchase frequency statistics from the user's order history (`_orders`)
     * and favorite items so both the UI and `Brav IA` can inspect usual buying habits.
     */
    fun getPurchaseHistoryStats(): List<FrequentProductStat> {
        val counts = LinkedHashMap<String, Int>()
        val lastBought = LinkedHashMap<String, String>()
        _orders.value.forEach { order ->
            order.skus.forEach { sku ->
                counts[sku] = (counts[sku] ?: 0) + 1
                if (!lastBought.containsKey(sku)) {
                    lastBought[sku] = order.date
                }
            }
        }
        _lists.value.firstOrNull()?.skus?.forEach { favSku ->
            if (!counts.containsKey(favSku)) {
                counts[favSku] = 1
                lastBought[favSku] = "En Mis Favoritos"
            }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .mapNotNull { (sku, count) ->
                val prod = findProductBySku(sku)
                if (prod != null) {
                    FrequentProductStat(
                        product = prod,
                        purchaseCount = count,
                        lastBoughtLabel = lastBought[sku] ?: "Compra reciente"
                    )
                } else {
                    null
                }
            }
    }

    private fun buildPurchaseHistorySummaryForAi(): String {
        val stats = getPurchaseHistoryStats()
        val ordersSummary = _orders.value.joinToString("\n") { ord ->
            val names = ord.skus.mapNotNull { findProductBySku(it)?.let { p -> "${p.name} [${p.sku}]" } }
            "- Orden ${ord.id} (${ord.date}, ${ord.branch}, Total RD$${ord.total.toInt()}): ${names.joinToString(", ")}"
        }
        val freqSummary = stats.joinToString("\n") { st ->
            "- [${st.product.sku}] ${st.product.name} (${st.product.category}, RD$${st.product.price}): comprado ${st.purchaseCount} veces (última vez: ${st.lastBoughtLabel})"
        }
        return "ÓRDENES RECIENTES DEL CLIENTE:\n$ordersSummary\n\nFRECUENCIA DE COMPRA HABITUAL:\n$freqSummary"
    }

    fun createShoppingList(
        name: String,
        initialSkus: List<String> = emptyList(),
        autoPopulateWithAi: Boolean = false,
        focusHint: String = ""
    ) {
        val cleanName = name.trim().ifEmpty { "Mi Lista Personalizada" }
        val newId = "list_${System.currentTimeMillis()}"
        val newList = ShoppingList(
            id = newId,
            name = cleanName,
            skus = initialSkus.distinct(),
            aiNote = if (autoPopulateWithAi) "Analizando tu historial de compras con Brav IA..." else null,
            isAiGenerated = autoPopulateWithAi,
            updatedAtLabel = if (autoPopulateWithAi) "Autocompletando con Brav IA..." else "Creada ahora"
        )
        // take(1) en vez de first(): no se cierra si todavía no existe la lista de favoritos
        val updated = _lists.value.take(1) + listOf(newList) + _lists.value.drop(1)
        persistLists(updated, changedList = newList)
        if (autoPopulateWithAi) {
            populateListWithBravIa(newId, focusHint.ifBlank { cleanName })
        } else {
            showToast("📋", "Lista \"$cleanName\" creada y sincronizada")
        }
    }

    fun deleteShoppingList(listId: String) {
        if (listId == "l1") {
            showToast("ℹ️", "La lista Mis Favoritos es principal y no se elimina")
            return
        }
        val target = _lists.value.find { it.id == listId }
        val updated = _lists.value.filterNot { it.id == listId }
        _lists.value = updated
        val uid = getVerifiedActiveUid()
        viewModelScope.launch {
            localRepo.saveShoppingLists(updated)
            if (uid != null) {
                firestoreRepo.deleteShoppingList(uid, listId)
            }
        }
        showToast("🗑️", "Lista \"${target?.name ?: ""}\" eliminada de tus dispositivos")
    }

    fun renameShoppingList(listId: String, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        var modified: ShoppingList? = null
        val updated = _lists.value.map { lst ->
            if (lst.id == listId) {
                lst.copy(name = clean, updatedAtLabel = "Sincronizada con tu cuenta").also { modified = it }
            } else {
                lst
            }
        }
        persistLists(updated, changedList = modified)
        showToast("✏️", "Lista renombrada a \"$clean\"")
    }

    fun addProductToShoppingList(listId: String, sku: String) {
        val prod = findProductBySku(sku) ?: return
        var modified: ShoppingList? = null
        val updated = _lists.value.map { lst ->
            if (lst.id == listId) {
                if (lst.skus.contains(sku)) {
                    showToast("ℹ️", "${prod.name} ya está en ${lst.name}")
                    return
                }
                lst.copy(
                    skus = lst.skus + sku,
                    updatedAtLabel = "Sincronizada con tu cuenta"
                ).also { modified = it }
            } else {
                lst
            }
        }
        persistLists(updated, changedList = modified)
        showToast("✓", "${prod.name} agregado y sincronizado")
    }

    fun removeProductFromShoppingList(listId: String, sku: String) {
        val prod = findProductBySku(sku)
        var modified: ShoppingList? = null
        val updated = _lists.value.map { lst ->
            if (lst.id == listId) {
                lst.copy(
                    skus = lst.skus.filterNot { it == sku },
                    updatedAtLabel = "Sincronizada con tu cuenta"
                ).also { modified = it }
            } else {
                lst
            }
        }
        persistLists(updated, changedList = modified)
        if (prod != null) {
            showToast("🗑️", "${prod.name} quitado de la lista")
        }
    }

    fun saveChatMessageBundleAsList(messageId: Long, customName: String? = null) {
        val msg = _chatHistory.value.find { it.id == messageId } ?: return
        if (msg.skus.isEmpty()) return
        val derivedTitle = customName?.takeIf { it.isNotBlank() } ?: run {
            val firstLine = msg.text
                .lineSequence()
                .map { it.replace("**", "").trim() }
                .firstOrNull { it.isNotEmpty() }
                ?.take(34)
            if (!firstLine.isNullOrBlank() && firstLine.length > 6) {
                "Lista IA: $firstLine"
            } else {
                "Selección Brav IA (${msg.skus.size} art.)"
            }
        }
        val newList = ShoppingList(
            id = "list_${System.currentTimeMillis()}",
            name = derivedTitle,
            skus = msg.skus.distinct(),
            aiNote = "Guardada directamente desde tu conversación con Brav IA.",
            isAiGenerated = true,
            updatedAtLabel = "Creada por Brav IA • Sincronizada"
        )
        // take(1) en vez de first(): no se cierra si todavía no existe la lista de favoritos
        val updated = _lists.value.take(1) + listOf(newList) + _lists.value.drop(1)
        persistLists(updated, changedList = newList)
        showToast("📋", "Lista guardada: \"$derivedTitle\"")
    }

    /**
     * Deterministic smart fallback that selects products from the user's purchase history
     * tailored to the requested list focus/name, plus complementary items.
     */
    private fun buildHistorySmartFallbackForList(
        listName: String,
        existingSkus: List<String>
    ): Pair<List<String>, String> {
        val lower = listName.lowercase()
        val stats = getPurchaseHistoryStats()
        val topHistorySkus = stats.map { it.product.sku }

        val selected = when {
            lower.contains("desayuno") || lower.contains("cena") || lower.contains("fresco") -> {
                val relevantHistory = topHistorySkus.filter { sku ->
                    val cat = findProductBySku(sku)?.category?.lowercase().orEmpty()
                    cat.contains("fruta") || cat.contains("huevo") || cat.contains("lácteo") ||
                        cat.contains("embutido") || cat.contains("pan") || cat.contains("carne")
                }
                (existingSkus + relevantHistory + listOf("BRV-001", "BRV-009", "BRV-093", "BRV-096", "BRV-094", "BRV-037"))
                    .distinct()
                    .take(8)
            }
            lower.contains("limpieza") || lower.contains("hogar") -> {
                val relevantHistory = topHistorySkus.filter { sku ->
                    val cat = findProductBySku(sku)?.category?.lowercase().orEmpty()
                    cat.contains("hogar") || cat.contains("limpieza")
                }
                (existingSkus + relevantHistory + listOf("BRV-017", "BRV-018", "BRV-020", "BRV-021", "BRV-022"))
                    .distinct()
                    .take(7)
            }
            lower.contains("mascota") || lower.contains("perro") || lower.contains("gato") || lower.contains("arca") -> {
                val relevantHistory = topHistorySkus.filter { it.startsWith("ARC-") }
                (existingSkus + relevantHistory + listOf("ARC-001", "ARC-009", "ARC-012", "ARC-015", "ARC-021"))
                    .distinct()
                    .take(6)
            }
            else -> {
                (existingSkus + topHistorySkus.take(7) + listOf("BRV-055", "BRV-098", "BRV-083"))
                    .distinct()
                    .take(9)
            }
        }

        val topNames = selected.take(4).mapNotNull { findProductBySku(it)?.name }.joinToString(", ")
        val note = "Autocompletada por Brav IA analizando tus ${_orders.value.size} órdenes recientes: prioriza tus compras más repetidas ($topNames) más básicos complementarios."
        return selected to note
    }

    private fun normalizeSearchText(input: String): String {
        return input.lowercase()
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
            .replace('ü', 'u')
            .replace('ñ', 'n')
    }

    private fun isOrderIntentQuery(query: String): Boolean {
        val q = normalizeSearchText(query)
        val orderPhrases = listOf(
            "pedir por mi", "pide por mi", "pideme", "hacer pedido", "haz el pedido",
            "hazme el pedido", "hazme un pedido", "realizar pedido", "ordenar", "ordename",
            "ordena por mi", "comprame", "comprar por mi", "encargame", "encargar",
            "quiero pedir", "quiero ordenar", "confirmar pedido", "enviar pedido", "pide esto"
        )
        return orderPhrases.any { q.contains(it) } ||
            (q.contains("pide") && (q.contains("carrito") || q.contains("compra") || q.contains("orden") || q.contains("super") || q.contains("lista")))
    }

    private fun isInstructionOrRecipeQuery(query: String): Boolean {
        val q = normalizeSearchText(query)
        val instructionPatterns = listOf(
            "como hacer", "como se hace", "como preparar", "como cocino", "como cocinar",
            "receta", "paso a paso", "pasos para", "pasos de", "instrucciones",
            "preparacion", "ensename a", "dime como", "que necesito para hacer",
            "ingredientes para", "hacer una ", "hacer un ", "preparar una ", "preparar un ",
            "cocinar una ", "cocinar un ", "hornear", "guisar"
        )
        val dishKeywords = listOf(
            "pizza", "hamburguesa", "burger", "lasana", "taco", "burrito", "quesadilla",
            "mofongo", "chofan", "sancocho", "mangu", "tres golpes", "pastelon",
            "bandera", "locrio", "moro", "pasta", "alfredo", "espagueti", "carbonara",
            "arroz con leche", "habichuelas con dulce", "panqueque", "hotcake", "waffle",
            "ensalada cesar", "club sandwich", "asopao", "churrasco", "parrillada",
            "camarones al ajillo", "salmon al horno", "pescado frito", "tostones"
        )
        return instructionPatterns.any { q.contains(it) } || dishKeywords.any { q.contains(it) }
    }

    /** Productos relevantes de la tienda activa para un pedido a la IA (sin precios ni nutrición: eso lo pone la app). */
    private fun buildFocusedCatalogContext(query: String): String {
        val habitual = runCatching { getPurchaseHistoryStats().map { it.product.sku } }.getOrDefault(emptyList())
        return "PRODUCTOS DISPONIBLES (SKU | nombre | presentación):\n" +
            BravIaEngine.selectCandidates(query, getActiveProducts(), BravIaEngine.matchRecipe(query), habitual, limit = 80)
                .joinToString("\n") { "${it.sku} | ${it.name} | ${it.unit}" }
    }

    fun populateListWithBravIa(listId: String, focusHint: String = "") {
        val targetList = _lists.value.find { it.id == listId } ?: return
        if (_aiPopulatingListId.value != null) return

        _aiPopulatingListId.value = listId
        val effectiveFocus = focusHint.ifBlank { targetList.name }
        val historyContext = buildPurchaseHistorySummaryForAi()
        val focusedCatalog = buildFocusedCatalogContext(effectiveFocus)

        val prompt = """
            Eres Brav IA, el asistente inteligente de compras de Supermercados Bravo y El Arca Petshop.
            AUTOCOMPLETA la lista de compras "${targetList.name}" (Enfoque: $effectiveFocus) en base al historial del cliente.
            PRODUCTOS ACTUALES EN LA LISTA: ${targetList.skus.joinToString(", ").ifBlank { "Ninguno" }}
            
            $historyContext
            
            INSTRUCCIONES:
            1. Selecciona entre 6 y 9 productos del catálogo combinando sus compras más repetidas con 2 o 3 complementos útiles para "${targetList.name}".
            2. Primera línea: "NOTA: <explicación cálida y directa de máximo 200 caracteres>".
            3. Segunda línea: SKUS: ["SKU1", "SKU2", "SKU3", ...]
            
            CATÁLOGO OFICIAL PRIORIZADO:
            $focusedCatalog
        """.trimIndent()

        viewModelScope.launch {
            try {
                val (finalSkus, finalNote) = if (!isFirebaseUserAuthenticatedForAi()) {
                    delay(320L)
                    buildHistorySmartFallbackForList(effectiveFocus, targetList.skus)
                } else {
                    val response = withTimeout(MAX_AI_RESPONSE_TIMEOUT_MS) {
                        geminiModel.generateContent(prompt)
                    }
                    val rawText = response.text.orEmpty()
                    val (cleanText, allParsedSkus) = extractSkusFromResponse(rawText)
                    val storePrefix = if (_activeStore.value == StoreMode.ARCA) "ARC-" else "BRV-"
                    val parsedSkus = allParsedSkus.filter { it.startsWith(storePrefix) }
                    val noteLine = cleanText
                        .lineSequence()
                        .map { BravIaEngine.sanitize(it.replace(Regex("""^[\*\s]*NOTA:\s*""", RegexOption.IGNORE_CASE), "").replace("**", "")).trim() }
                        .firstOrNull { it.length > 12 }

                    if (parsedSkus.size >= 3) {
                        val merged = (targetList.skus + parsedSkus).distinct().take(10)
                        val note = noteLine
                            ?: "Autocompletada por Brav IA a partir de tus ${_orders.value.size} compras recientes y hábitos de consumo."
                        merged to note
                    } else {
                        buildHistorySmartFallbackForList(effectiveFocus, targetList.skus)
                    }
                }

                var modifiedList: ShoppingList? = null
                val updatedLists = _lists.value.map { lst ->
                    if (lst.id == listId) {
                        lst.copy(
                            skus = finalSkus,
                            aiNote = finalNote,
                            isAiGenerated = true,
                            updatedAtLabel = "Autocompletada por Brav IA • Sincronizada"
                        ).also { modifiedList = it }
                    } else {
                        lst
                    }
                }
                persistLists(updatedLists, changedList = modifiedList)
                showToast("✨", "Brav IA completó \"${targetList.name}\" con ${finalSkus.size} productos")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "AI list population local fallback triggered: ${e.message}")
                val (fallbackSkus, fallbackNote) = buildHistorySmartFallbackForList(effectiveFocus, targetList.skus)
                var modifiedList: ShoppingList? = null
                val updatedLists = _lists.value.map { lst ->
                    if (lst.id == listId) {
                        lst.copy(
                            skus = fallbackSkus,
                            aiNote = fallbackNote,
                            isAiGenerated = true,
                            updatedAtLabel = "Autocompletada por Brav IA • Sincronizada"
                        ).also { modifiedList = it }
                    } else {
                        lst
                    }
                }
                persistLists(updatedLists, changedList = modifiedList)
                showToast("✨", "Brav IA completó \"${targetList.name}\" según tu historial")
            } finally {
                _aiPopulatingListId.value = null
            }
        }
    }

    fun selectStore(store: StoreMode) {
        if (_activeStore.value == store) return
        _activeStore.value = store
        _activeStoreProducts.value = if (store == StoreMode.ARCA) _cachedArcaProducts.value else _cachedBravoProducts.value
        _activeStoreSections.value = if (store == StoreMode.ARCA) _cachedArcaSections.value else _cachedBravoSections.value
        _activeCategory.value = if (store == StoreMode.ARCA) "Alimentos Secos" else "Hogar y limpieza"
        _homeSearchQuery.value = ""
        _catalogFilterQuery.value = ""
        _categorySearchQuery.value = ""
        if (_currentScreen.value == AppScreen.PRODUCT || _currentScreen.value == AppScreen.CATEGORY) {
            _currentScreen.value = AppScreen.HOME
            backStack.clear()
        }
        showToast(
            if (store == StoreMode.ARCA) "🐾" else "🛒",
            if (store == StoreMode.ARCA) "El Arca Petshop • Catálogo en caché activo" else "Supermercados Bravo • Catálogo en caché activo"
        )
    }

    fun toggleStoreMode() {
        val next = if (_activeStore.value == StoreMode.BRAVO) StoreMode.ARCA else StoreMode.BRAVO
        selectStore(next)
    }

    fun navigateTo(screen: AppScreen) {
        val current = _currentScreen.value
        if (current == screen) return
        if (screen == AppScreen.HOME) {
            backStack.clear()
        } else {
            backStack.addLast(current)
        }
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (_isScannerOpen.value) {
            closeBarcodeScanner()
            return true
        }
        val curr = _currentScreen.value
        if (curr == AppScreen.HOME) {
            if (_homeSearchQuery.value.isNotEmpty()) {
                _homeSearchQuery.value = ""
                return true
            }
            return false
        }
        if (curr == AppScreen.CATEGORY && _categorySearchQuery.value.isNotEmpty()) {
            _categorySearchQuery.value = ""
            return true
        }
        if (curr == AppScreen.CATALOG && _catalogFilterQuery.value.isNotEmpty()) {
            _catalogFilterQuery.value = ""
            return true
        }

        val target = when {
            curr == AppScreen.ACCOUNT_SUB -> {
                while (backStack.isNotEmpty() && backStack.last() == AppScreen.ACCOUNT_SUB) {
                    backStack.removeLast()
                }
                if (backStack.isNotEmpty() && backStack.last() == AppScreen.ACCOUNT) {
                    backStack.removeLast()
                }
                AppScreen.ACCOUNT
            }
            backStack.isNotEmpty() -> backStack.removeLast()
            else -> AppScreen.HOME
        }
        _currentScreen.value = target
        return true
    }

    fun openCategoryScreen(categoryName: String) {
        backStack.addLast(_currentScreen.value)
        _activeCategory.value = categoryName
        _categorySearchQuery.value = ""
        _currentScreen.value = AppScreen.CATEGORY
    }

    fun openProductDetail(sku: String) {
        backStack.addLast(_currentScreen.value)
        _activeProductSku.value = sku
        _currentScreen.value = AppScreen.PRODUCT
    }

    fun openAccountSubScreen(sub: AccountSubScreen) {
        backStack.addLast(AppScreen.ACCOUNT)
        _activeAccountSub.value = sub
        _currentScreen.value = AppScreen.ACCOUNT_SUB
    }

    fun setHomeSearchQuery(q: String) {
        _homeSearchQuery.value = q
        checkEasterEgg(q)
    }

    fun setCatalogFilterQuery(q: String) {
        _catalogFilterQuery.value = q
        checkEasterEgg(q)
    }

    fun setCategorySearchQuery(q: String) {
        _categorySearchQuery.value = q
        checkEasterEgg(q)
    }

    /* Huevos de pascua para fans de Toby Fox (Undertale / Deltarune): ver ui/components/EasterEggs.kt */
    private var lastEasterEgg = ""
    private fun checkEasterEgg(raw: String): Boolean {
        val egg = com.example.ui.components.findEasterEgg(raw) ?: run {
            lastEasterEgg = ""
            return false
        }
        // No repetir efecto, sonido ni aviso mientras la búsqueda sigue siendo la misma palabra
        if (egg.line == lastEasterEgg) return true
        lastEasterEgg = egg.line
        egg.play()
        showToast("heart", egg.line)
        return true
    }

    fun computeCartTotals(
        cartItems: List<CartItem> = _cart.value,
        redeemBravisima: Boolean = _redeemBravisimaInCart.value,
        balance: Int = _accumulatedBravisimaBalance.value
    ): CartTotals {
        var count = 0
        var subtotal = 0.0
        for (item in cartItems) {
            val p = findProductBySku(item.sku)
            if (p != null) {
                count += item.qty
                subtotal += p.price * item.qty
            }
        }
        val bravisimaGenerated = floor(subtotal / 100.0).toInt()
        val remainderMod = subtotal % 100.0
        val nextHundredRemainder = 100.0 - remainderMod
        val progressPct = (remainderMod / 100.0).toFloat().coerceIn(0f, 1f)
        val redeemed = if (redeemBravisima) min(balance.toDouble(), subtotal).toInt() else 0
        val finalPay = max(0.0, subtotal - redeemed)
        return CartTotals(
            count = count,
            subtotal = subtotal,
            bravisimaGenerated = bravisimaGenerated,
            nextHundredRemainder = nextHundredRemainder,
            progressPct = progressPct,
            redeemed = redeemed,
            finalPay = finalPay
        )
    }

    fun showToast(icon: String, message: String, actionLabel: String? = null) {
        toastJob?.cancel()
        _toast.value = ToastNotification(icon = icon, message = message, actionLabel = actionLabel)
        toastJob = viewModelScope.launch {
            // Con botón ("Deshacer") se deja más tiempo para alcanzar a tocarlo
            delay(if (actionLabel != null) 4500L else 1850L)
            _toast.value = null
            if (actionLabel != null) lastRemovedCartItem = null
        }
    }

    /** Último producto quitado del carrito, para poder deshacerlo desde el aviso. */
    private var lastRemovedCartItem: CartItem? = null

    /** Toque en el botón del aviso. Hoy solo existe "Deshacer" al quitar del carrito. */
    fun onToastAction() {
        val removed = lastRemovedCartItem ?: return
        lastRemovedCartItem = null
        toastJob?.cancel()
        _toast.value = null
        changeProductQty(removed.sku, removed.qty, silent = true)
        showToast("cart", "Lo devolvimos a tu carrito")
    }

    fun changeProductQty(sku: String, delta: Int, silent: Boolean = false) {
        val p = findProductBySku(sku)
        val currentQty = _cart.value.find { it.sku == sku }?.qty ?: 0
        val newQty = (currentQty + delta).coerceAtLeast(0)

        if (delta > 0) {
            val trigger = System.nanoTime()
            _cartPulseTrigger.value = trigger
            if (p != null) {
                _lastAddedCartFeedback.value = trigger to p.price
            }
        }

        val updated = _cart.value.toMutableList()
        val idx = updated.indexOfFirst { it.sku == sku }
        if (idx >= 0) {
            if (newQty <= 0) updated.removeAt(idx) else updated[idx] = CartItem(sku, newQty)
        } else if (newQty > 0) {
            updated.add(CartItem(sku, newQty))
        }
        _cart.value = updated

        val uid = getVerifiedActiveUid()
        val storeMode = if (sku.startsWith("ARC-", ignoreCase = true)) "ARCA" else "BRAVO"
        viewModelScope.launch {
            localRepo.setCartQty(sku, newQty)
            if (uid != null) {
                firestoreRepo.setCartItemQuantity(uid, sku, newQty, storeMode)
            }
        }

        if (p != null && !silent) {
            if (delta > 0) {
                showToast("✓", "${p.name} • RD$${p.price}")
            } else if (delta < 0 && newQty == 0) {
                lastRemovedCartItem = CartItem(sku, currentQty)
                showToast("🗑️", "${p.name} quitado", actionLabel = "Deshacer")
            }
        }
    }

    fun removeProductCompletely(sku: String) {
        val removedQty = _cart.value.find { it.sku == sku }?.qty ?: 0
        _cart.value = _cart.value.filterNot { it.sku == sku }
        val uid = getVerifiedActiveUid()
        viewModelScope.launch {
            localRepo.setCartQty(sku, 0)
            if (uid != null) {
                firestoreRepo.setCartItemQuantity(uid, sku, 0)
            }
        }
        if (removedQty > 0) {
            lastRemovedCartItem = CartItem(sku, removedQty)
            showToast("🗑️", "Producto quitado del carrito", actionLabel = "Deshacer")
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        val uid = getVerifiedActiveUid()
        viewModelScope.launch {
            localRepo.clearCart()
            if (uid != null) {
                firestoreRepo.clearUserCart(uid)
            }
        }
        showToast("🗑️", "Carrito vaciado")
    }

    fun setRedeemBravisimaInCart(redeem: Boolean) {
        if (redeem && _bravisimaCard.value.isLocked) {
            showToast("lock", "Tu Tarjeta Bravísima está bloqueada. Desbloquéala primero.")
            _redeemBravisimaInCart.value = false
            return
        }
        _redeemBravisimaInCart.value = redeem
    }

    fun toggleBravisimaCardLock() {
        val current = _bravisimaCard.value
        val nextLocked = !current.isLocked
        val updatedCard = current.copy(
            isLocked = nextLocked,
            autoRedeemInCart = if (nextLocked) false else current.autoRedeemInCart
        )
        _bravisimaCard.value = updatedCard
        if (nextLocked) {
            _redeemBravisimaInCart.value = false
        }
        val uid = getVerifiedActiveUid()
        if (uid != null) {
            viewModelScope.launch {
                _isSavingBravisima.value = true
                try {
                    firestoreRepo.saveOrUpdateBravisimaCard(uid, updatedCard)
                } finally {
                    _isSavingBravisima.value = false
                }
            }
        }
        showToast(
            if (nextLocked) "lock" else "check",
            if (nextLocked) "Tarjeta Bravísima bloqueada temporalmente" else "Tarjeta Bravísima activada y lista"
        )
    }

    fun toggleBravisimaAutoRedeem() {
        val current = _bravisimaCard.value
        if (current.isLocked && !current.autoRedeemInCart) {
            showToast("lock", "Desbloquea tu Tarjeta Bravísima para activar el canje automático.")
            return
        }
        val nextAuto = !current.autoRedeemInCart
        val updatedCard = current.copy(autoRedeemInCart = nextAuto)
        _bravisimaCard.value = updatedCard
        _redeemBravisimaInCart.value = nextAuto
        val uid = getVerifiedActiveUid()
        if (uid != null) {
            viewModelScope.launch {
                _isSavingBravisima.value = true
                try {
                    firestoreRepo.saveOrUpdateBravisimaCard(uid, updatedCard)
                } finally {
                    _isSavingBravisima.value = false
                }
            }
        }
        showToast(
            "sparkles",
            if (nextAuto) "Canje automático en carrito activado" else "Canje automático desactivado"
        )
    }

    fun updateBravisimaCardSettings(
        holderName: String,
        monthlyGoal: Int,
        preferredCategory: String
    ) {
        val current = _bravisimaCard.value
        val cleanName = holderName.trim().ifBlank { current.holderName }
        val cleanGoal = monthlyGoal.coerceIn(100, 50000)
        val cleanCat = preferredCategory.trim().ifBlank { current.preferredCategory }
        val updatedCard = current.copy(
            holderName = cleanName,
            monthlyGoal = cleanGoal,
            preferredCategory = cleanCat
        )
        _bravisimaCard.value = updatedCard
        _profile.value = _profile.value.copy(name = cleanName)
        val uid = getVerifiedActiveUid()
        if (uid != null) {
            viewModelScope.launch {
                _isSavingBravisima.value = true
                try {
                    firestoreRepo.saveOrUpdateBravisimaCard(uid, updatedCard)
                    firestoreRepo.saveOrUpdateUserProfile(uid, _profile.value, _activeBranch.value)
                } finally {
                    _isSavingBravisima.value = false
                }
            }
        }
        showToast("check", "Configuración de Tarjeta Bravísima guardada")
    }

    fun redeemBravisimaPromoCode(rawCode: String) {
        val code = rawCode.trim().uppercase()
        if (code.isBlank()) {
            showToast("offer", "Ingresa un código promocional Bravísima.")
            return
        }
        val current = _bravisimaCard.value
        if (current.isLocked) {
            showToast("lock", "Desbloquea tu tarjeta antes de aplicar bonos promocionales.")
            return
        }
        if (current.claimedPromoCodes.any { it.equals(code, ignoreCase = true) }) {
            showToast("offer", "El código $code ya fue canjeado en tu Tarjeta Bravísima.")
            return
        }
        val bonusAmount = when (code) {
            "BRAVO25" -> 25
            "BRAVISIMA50" -> 50
            "ARCA20" -> 20
            "VIP30" -> 30
            else -> {
                showToast("offer", "Código no reconocido. Prueba: BRAVO25, BRAVISIMA50 o ARCA20.")
                return
            }
        }

        val updatedCard = current.copy(
            balance = current.balance + bonusAmount,
            lifetimeEarned = current.lifetimeEarned + bonusAmount,
            claimedPromoCodes = (current.claimedPromoCodes + code).distinct().take(20)
        )
        val bonusMovement = BravisimaMovement(
            id = "mov_promo_${System.currentTimeMillis()}",
            title = "Bono Promocional $code",
            subtitle = "Acreditado a Tarjeta Bravísima",
            dateLabel = "Hace un instante",
            amount = bonusAmount,
            movementType = "bonus"
        )
        _bravisimaCard.value = updatedCard
        _accumulatedBravisimaBalance.value = updatedCard.balance
        _bravisimaMovements.value = listOf(bonusMovement) + _bravisimaMovements.value

        val uid = getVerifiedActiveUid()
        viewModelScope.launch {
            localRepo.saveBravisimaBalance(updatedCard.balance)
            if (uid != null) {
                _isSavingBravisima.value = true
                try {
                    firestoreRepo.saveOrUpdateBravisimaCard(uid, updatedCard)
                    firestoreRepo.recordBravisimaMovement(uid, bonusMovement)
                } finally {
                    _isSavingBravisima.value = false
                }
            }
        }
        showToast("sparkles", "¡Código $code aplicado! +RD$$bonusAmount.00 en Bravísima")
    }

    fun completeCheckoutOrder() {
        if (_isOfflineMode.value) {
            // Sin señal: el pedido queda en cola y se envía solo al volver internet
            _pendingOfflineOrder.value = true
            rdPrefs.edit().putBoolean("pending_offline_order", true).apply()
            showToast("cloud", "Sin conexión: enviaremos tu pedido apenas vuelva la señal")
            return
        }
        val extras = _checkoutExtras.value
        val currentCard = _bravisimaCard.value
        val effectiveRedeem = _redeemBravisimaInCart.value && !currentCard.isLocked
        val t = computeCartTotals(redeemBravisima = effectiveRedeem)
        if (t.count == 0) return

        val newBalance = (_accumulatedBravisimaBalance.value - t.redeemed + t.bravisimaGenerated).coerceAtLeast(0)
        val updatedCard = currentCard.copy(
            balance = newBalance,
            lifetimeEarned = currentCard.lifetimeEarned + t.bravisimaGenerated
        )
        _accumulatedBravisimaBalance.value = newBalance
        _bravisimaCard.value = updatedCard

        val orderId = "ORD-${(10000..99999).random()}"
        val branch = _activeBranch.value
        val newOrder = OrderRecord(
            id = orderId,
            status = OrderDeliveryStage.PREPARED.statusText,
            badgeNum = t.count,
            date = "Hace un instante",
            total = t.finalPay + extras.tipRd,
            branch = branch,
            skus = _cart.value.map { it.sku }
        )
        _orderExtras.value = _orderExtras.value + (orderId to extras)
        // La propina y el comprobante se usan una sola vez
        updateCheckoutExtras(extras.copy(tipRd = 0, transferProofAttached = false))

        val generatedMovements = buildList {
            if (t.bravisimaGenerated > 0) {
                add(
                    BravisimaMovement(
                        id = "mov_earn_${orderId}_${System.currentTimeMillis()}",
                        title = "Compra $branch",
                        subtitle = "Orden #$orderId • Retorno 1%",
                        dateLabel = "Hace un instante",
                        amount = t.bravisimaGenerated,
                        movementType = "earn",
                        orderId = orderId
                    )
                )
            }
            if (t.redeemed > 0) {
                add(
                    BravisimaMovement(
                        id = "mov_redeem_${orderId}_${System.currentTimeMillis() + 1}",
                        title = "Canje en Carrito",
                        subtitle = "Orden #$orderId • Descuento aplicado",
                        dateLabel = "Hace un instante",
                        amount = -t.redeemed,
                        movementType = "redeem",
                        orderId = orderId
                    )
                )
            }
        }
        if (generatedMovements.isNotEmpty()) {
            _bravisimaMovements.value = generatedMovements + _bravisimaMovements.value
        }

        val updatedOrders = listOf(newOrder) + _orders.value
        _orders.value = updatedOrders
        _trackedOrderId.value = orderId
        _trackedOrder.value = newOrder
        val uid = getVerifiedActiveUid()
        viewModelScope.launch {
            localRepo.saveBravisimaBalance(newBalance)
            localRepo.clearCart()
            localRepo.saveOrders(updatedOrders)
            if (uid != null) {
                firestoreRepo.clearUserCart(uid)
                firestoreRepo.createOrder(uid, newOrder)
                firestoreRepo.saveOrUpdateBravisimaCard(uid, updatedCard)
                generatedMovements.forEach { mov ->
                    firestoreRepo.recordBravisimaMovement(uid, mov)
                }
                startObservingTrackedOrder(orderId)
            }
        }
        _cart.value = emptyList()
        _redeemBravisimaInCart.value = updatedCard.autoRedeemInCart && !updatedCard.isLocked

        val updatedMetrics = computeDeliveryMetricsForCoordinates(
            userLat = _realLocationMetrics.value.userLat,
            userLng = _realLocationMetrics.value.userLng,
            branchName = branch,
            stage = OrderDeliveryStage.PREPARED,
            isRealGpsFix = _realLocationMetrics.value.isRealGpsFix,
            locationSourceLabel = _realLocationMetrics.value.locationSourceLabel
        )
        _realLocationMetrics.value = updatedMetrics
        _celebrationOrder.value = newOrder

        showToast("check", "¡Pedido confirmado! +RD$${t.bravisimaGenerated} en tu Bravísima")
        navigateTo(AppScreen.DELIVERY)
        PlatformExtras.startDeliveryActivity(
            orderId = orderId,
            branch = branch,
            stage = OrderDeliveryStage.PREPARED.shortLabel,
            etaMinutes = updatedMetrics.stageAdjustedEtaMinutes,
            progress = OrderDeliveryStage.PREPARED.progressPct / 100.0
        )

        if (_pushNotifications.value) {
            val app = application
            BravoPushNotificationCenter.dispatchPushNotification(
                context = app,
                category = PushNotificationCategory.ORDER_STATUS,
                title = "Pedido #$orderId Confirmado en $branch",
                body = "Ruta calculada por GPS (${updatedMetrics.roadDistanceKm} km • ${updatedMetrics.trafficLabel}). Total RD$${t.finalPay.toInt()}.00 (+RD$${t.bravisimaGenerated} en Bravísima). ETA: ~${updatedMetrics.stageAdjustedEtaMinutes} min.",
                topic = BravoPushNotificationCenter.TOPIC_ORDERS_REALTIME,
                targetOrderId = orderId,
                progressPct = OrderDeliveryStage.PREPARED.progressPct,
                postSystemNotification = true
            )
            viewModelScope.launch {
                delay(4500L)
                updateTrackedOrderStatusInFirestore(
                    stage = OrderDeliveryStage.IN_TRANSIT,
                    showFeedbackToast = false
                )
                val transitMetrics = _realLocationMetrics.value
                if (_pushNotifications.value) {
                    BravoPushNotificationCenter.dispatchPushNotification(
                        context = app,
                        category = PushNotificationCategory.ORDER_STATUS,
                        title = "Repartidor en camino • #$orderId (${transitMetrics.roadDistanceKm} km)",
                        body = "Carlos Rodríguez salió de $branch hacia tus coordenadas GPS. Llegada estimada: ~${transitMetrics.stageAdjustedEtaMinutes} min.",
                        topic = BravoPushNotificationCenter.TOPIC_ORDERS_REALTIME,
                        targetOrderId = orderId,
                        progressPct = OrderDeliveryStage.IN_TRANSIT.progressPct,
                        postSystemNotification = true
                    )
                }
            }
        }
    }

    fun openBarcodeScanner(initialSku: String = "BRV-001") {
        _scannedSku.value = initialSku
        _isScannerOpen.value = true
    }

    fun closeBarcodeScanner() {
        _isScannerOpen.value = false
    }

    fun simulateScanProduct(sku: String) {
        _scannedSku.value = sku
    }

    fun toggleDarkMode() {
        val next = !_darkMode.value
        _darkModeOverride.value = next
        _darkMode.value = next
        viewModelScope.launch {
            localRepo.saveDarkModeOverride(next)
        }
        showToast(
            if (next) "moon" else "sun",
            if (next) "Modo oscuro activado" else "Modo oscuro desactivado"
        )
    }

    fun selectColorPalette(paletteId: String) {
        _selectedPaletteId.value = paletteId
        viewModelScope.launch {
            localRepo.savePaletteId(paletteId)
        }
        showToast("sparkles", "Color sincronizado con el dispositivo")
    }

    fun setActiveBranch(branch: String) {
        _activeBranch.value = branch
        _realLocationMetrics.value = computeDeliveryMetricsForCoordinates(
            userLat = _realLocationMetrics.value.userLat,
            userLng = _realLocationMetrics.value.userLng,
            branchName = branch,
            stage = OrderDeliveryStage.fromStatusString(_trackedOrder.value?.status),
            isRealGpsFix = _realLocationMetrics.value.isRealGpsFix,
            locationSourceLabel = _realLocationMetrics.value.locationSourceLabel
        )
        viewModelScope.launch {
            localRepo.saveActiveBranch(branch)
        }
        showToast("store", "Sucursal guardada: $branch (${_realLocationMetrics.value.roadDistanceKm} km)")
    }

    fun togglePushNotifications() {
        val next = !_pushNotifications.value
        _pushNotifications.value = next
        viewModelScope.launch {
            localRepo.savePushNotifications(next)
        }
        fcmTopics.value.forEach { sub ->
            BravoPushNotificationCenter.setTopicEnabled(sub.topicKey, next)
        }
        showToast("bell", if (next) "Notificaciones en vivo activadas" else "Notificaciones en vivo pausadas")
    }

    fun toggleFcmTopic(topicKey: String, enabled: Boolean) {
        BravoPushNotificationCenter.setTopicEnabled(topicKey, enabled)
        if (enabled && !_pushNotifications.value) {
            _pushNotifications.value = true
            viewModelScope.launch { localRepo.savePushNotifications(true) }
        }
        showToast(
            "bell",
            if (enabled) "Canal de alertas activado" else "Canal de alertas silenciado"
        )
    }

    fun triggerExclusiveOfferPush() {
        val app = application
        val rotatingOffers = getDailyRotatingOffersForCurrentStore()
        val chosenOffer = if (rotatingOffers.isNotEmpty()) {
            rotatingOffers[offerCycleIndex % rotatingOffers.size]
        } else {
            null
        }
        offerCycleIndex++

        val title = if (chosenOffer != null) {
            "Oferta del Día (${chosenOffer.dayLabel}): -${chosenOffer.discountPct}% en ${chosenOffer.title}"
        } else {
            "Oferta del Día Bravo: Pollo Fresco Entero"
        }
        val body = if (chosenOffer != null) {
            "Hoy baja de RD$${chosenOffer.originalPrice} a solo RD$${chosenOffer.offerPrice} en ${_activeBranch.value} (${_realLocationMetrics.value.roadDistanceKm} km de ti)."
        } else {
            "Hoy a solo RD$185.00/lb en ${_activeBranch.value} + doble retorno en tu Tarjeta Bravísima."
        }
        val sku = chosenOffer?.sku ?: "BRV-075"
        val isArcaSku = sku.startsWith("ARC-")
        val topic = if (isArcaSku) {
            BravoPushNotificationCenter.TOPIC_OFFERS_ARCA
        } else {
            BravoPushNotificationCenter.TOPIC_OFFERS_BRAVO
        }
        BravoPushNotificationCenter.dispatchPushNotification(
            context = app,
            category = PushNotificationCategory.EXCLUSIVE_OFFER,
            title = title,
            body = body,
            topic = topic,
            targetSku = sku,
            postSystemNotification = true
        )
        showToast("offer", title)
    }

    fun triggerRealtimeOrderStatusPush() {
        val app = application
        val latestOrder = _trackedOrder.value ?: _orders.value.firstOrNull()
        val orderId = latestOrder?.id ?: "ORD-88412"
        val branch = latestOrder?.branch ?: _activeBranch.value
        val stages = listOf(
            OrderDeliveryStage.PREPARED,
            OrderDeliveryStage.IN_TRANSIT,
            OrderDeliveryStage.ARRIVING,
            OrderDeliveryStage.DELIVERED
        )
        val targetStage = stages[orderStatusStepIndex % stages.size]
        orderStatusStepIndex++

        updateTrackedOrderStatusInFirestore(targetStage, showFeedbackToast = false)
        val metrics = _realLocationMetrics.value

        val title = "Pedido #$orderId: ${targetStage.statusText} (${metrics.roadDistanceKm} km)"
        val body = "${targetStage.subtitle} ($branch • ETA GPS: ~${metrics.stageAdjustedEtaMinutes} min • ${targetStage.progressPct}%)."
        BravoPushNotificationCenter.dispatchPushNotification(
            context = app,
            category = PushNotificationCategory.ORDER_STATUS,
            title = title,
            body = body,
            topic = BravoPushNotificationCenter.TOPIC_ORDERS_REALTIME,
            targetOrderId = orderId,
            progressPct = targetStage.progressPct,
            postSystemNotification = true
        )
        showToast(targetStage.emoji, title)
    }

    fun handlePushNotificationClick(record: PushNotificationRecord) {
        BravoPushNotificationCenter.markNotificationRead(record.id)
        if (record.category == PushNotificationCategory.ORDER_STATUS) {
            openOrderTracking(record.targetOrderId)
        } else if (!record.targetSku.isNullOrBlank() && findProductBySku(record.targetSku) != null) {
            openProductDetail(record.targetSku)
        } else {
            navigateTo(AppScreen.HOME)
        }
    }

    fun markAllPushNotificationsRead() {
        BravoPushNotificationCenter.markAllAsRead()
        showToast("✓", "Todas las notificaciones marcadas como leídas")
    }

    fun handleFcmDeepLinkIntent(screenTarget: String?, productSku: String?) {
        if (screenTarget.isNullOrBlank()) return
        when (screenTarget.uppercase()) {
            "DELIVERY" -> navigateTo(AppScreen.DELIVERY)
            "PRODUCT" -> {
                if (!productSku.isNullOrBlank() && findProductBySku(productSku) != null) {
                    openProductDetail(productSku)
                }
            }
            "AJUSTES" -> openAccountSubScreen(AccountSubScreen.AJUSTES)
        }
    }

    fun toggleProductInFavList(sku: String) {
        val currentLists = _lists.value.toMutableList()
        val favList = currentLists.firstOrNull() ?: return
        val skus = favList.skus.toMutableList()
        if (skus.contains(sku)) {
            skus.remove(sku)
            showToast("★", "Removido de Mis Favoritos")
        } else {
            skus.add(sku)
            showToast("★", "Guardado en Mis Favoritos")
        }
        val updatedFav = favList.copy(skus = skus, updatedAtLabel = "Sincronizada con tu cuenta")
        currentLists[0] = updatedFav
        persistLists(currentLists, changedList = updatedFav)
    }

    fun isProductFavorite(sku: String): Boolean {
        return _lists.value.firstOrNull()?.skus?.contains(sku) == true
    }

    fun addListToCart(listId: String) {
        val lst = _lists.value.find { it.id == listId } ?: return
        if (lst.skus.isEmpty()) {
            showToast("ℹ️", "Esta lista aún no tiene productos")
            return
        }
        lst.skus.forEach { sku -> changeProductQty(sku, 1) }
        showToast("🛒", "${lst.skus.size} productos de \"${lst.name}\" cargados al carrito")
        navigateTo(AppScreen.CART)
    }

    fun reorderPastOrder(orderId: String) {
        val ord = _orders.value.find { it.id == orderId } ?: return
        // Silencioso: un solo aviso al final en vez de uno por producto
        ord.skus.forEach { sku -> changeProductQty(sku, 1, silent = true) }
        showToast("cart", "Agregamos ${ord.skus.size} productos de tu pedido")
        navigateTo(AppScreen.CART)
    }

    /** Calificación de 1 a 5 de cada pedido entregado (se guarda en el teléfono). */
    private val _orderRatings = MutableStateFlow(
        rdPrefs.getStringSet("order_ratings", emptySet()).orEmpty()
            .mapNotNull { e -> e.split(':').takeIf { it.size == 2 }?.let { (id, n) -> n.toIntOrNull()?.let { id to it } } }
            .toMap()
    )
    val orderRatings: StateFlow<Map<String, Int>> = _orderRatings.asStateFlow()

    fun rateOrder(orderId: String, stars: Int) {
        val updated = _orderRatings.value + (orderId to stars.coerceIn(1, 5))
        _orderRatings.value = updated
        rdPrefs.edit().putStringSet("order_ratings", updated.map { (id, n) -> "$id:$n" }.toSet()).apply()
        showToast("star", if (stars >= 4) "¡Gracias! Se lo diremos a tu repartidor" else "Gracias, vamos a mejorar")
    }

    fun saveUserProfile(name: String, phone: String, rncNit: String, ncfType: String) {
        val updatedProfile = _profile.value.copy(
            name = name.ifBlank { _profile.value.name },
            phone = phone.ifBlank { _profile.value.phone },
            rncNit = rncNit.ifBlank { _profile.value.rncNit },
            ncfType = ncfType
        )
        _profile.value = updatedProfile
        val uid = getVerifiedActiveUid()
        if (uid != null) {
            viewModelScope.launch {
                firestoreRepo.saveOrUpdateUserProfile(uid, updatedProfile, _activeBranch.value)
            }
        }
        showToast("✓", "Perfil actualizado correctamente")
    }

    fun setDefaultPayment(pmId: String) {
        _paymentMethods.value = _paymentMethods.value.map {
            it.copy(isDefault = it.id == pmId)
        }
        showToast("💳", "Método de pago actualizado")
    }

    fun addNewPaymentCard() {
        val newId = "pm_${System.currentTimeMillis()}"
        val last4 = (1000..9999).random().toString()
        val updated = _paymentMethods.value.map { it.copy(isDefault = false) } +
            PaymentMethod(newId, "Visa", "BHD", last4, "08/29", true)
        _paymentMethods.value = updated
        showToast("💳", "Nueva tarjeta •••• $last4 agregada")
    }

    fun setDefaultAddress(adId: String) {
        var newBranch = _activeBranch.value
        _addresses.value = _addresses.value.map { ad ->
            val isDef = ad.id == adId
            if (isDef) newBranch = ad.branch
            ad.copy(isDefault = isDef)
        }
        setActiveBranch(newBranch)
    }

    fun setTargetBudgetInChat(budget: Int) {
        _targetBudgetInChat.value = budget.coerceIn(400, 3500)
        _budgetSetByUser.value = true
    }

    /**
     * Botones de presupuesto del chat.
     * - optimizeExisting = true: recorta TU carrito (quita unidades de lo más caro) hasta caber en el límite.
     *   Antes reemplazaba el carrito entero por una lista fija; ahora nunca agrega cosas que no elegiste.
     * - optimizeExisting = false: propone una compra básica dentro del límite, sin tocar el carrito.
     *   La persona la agrega con el botón del mensaje.
     */
    fun applyBudgetInChat(optimizeExisting: Boolean) {
        val limit = _targetBudgetInChat.value
        _budgetSetByUser.value = true
        val isArca = _activeStore.value == StoreMode.ARCA
        val storeName = if (isArca) "El Arca Petshop" else "Supermercados Bravo"
        val userMsg = ChatMessage(
            sender = "user",
            text = if (optimizeExisting) "Ajustar mi carrito actual para no pasarme de RD$$limit" else "Armar una compra básica dentro de RD$$limit"
        )

        if (optimizeExisting) {
            val working = _cart.value.mapNotNull { item -> findProductBySku(item.sku)?.let { it to item.qty } }.toMutableList()
            val before = working.sumOf { it.first.price * it.second }
            if (working.isEmpty()) {
                _chatHistory.value = _chatHistory.value + userMsg + ChatMessage(sender = "ai", text = "Tu carrito está vacío. Agrega productos o pídeme una compra básica y la ajusto a tu presupuesto.", modelLabel = "Planificador de presupuesto")
                return
            }
            var total = before
            // Quita una unidad a la vez de lo más caro hasta caber; así se conserva la variedad.
            while (total > limit && working.isNotEmpty()) {
                val idx = working.indices.maxBy { working[it].first.price }
                val (product, qty) = working[idx]
                if (qty > 1) working[idx] = product to (qty - 1) else working.removeAt(idx)
                total -= product.price
            }
            val newCart = working.map { CartItem(it.first.sku, it.second) }
            val removedUnits = _cart.value.sumOf { it.qty } - newCart.sumOf { it.qty }
            _cart.value = newCart
            viewModelScope.launch { localRepo.replaceCart(newCart.associate { it.sku to it.qty }) }
            val text = if (removedUnits == 0) {
                "Tu carrito ya cabe en el presupuesto: **RD$$before** de **RD$$limit**. No cambié nada."
            } else {
                "Ajusté tu carrito de **RD$$before** a **RD$$total** quitando $removedUnits ${if (removedUnits == 1) "unidad" else "unidades"} de los productos más caros. Te quedan **RD$${limit - total}** de margen."
            }
            _chatHistory.value = _chatHistory.value + userMsg + ChatMessage(sender = "ai", text = text, skus = newCart.map { it.sku }, quantities = newCart.associate { it.sku to it.qty }, modelLabel = "Planificador de presupuesto")
            showToast("💰", "Carrito: RD$$total")
            return
        }

        val essentials = if (isArca) {
            listOf("ARC-001", "ARC-009", "ARC-012", "ARC-015", "ARC-021", "ARC-025", "ARC-028", "ARC-010")
        } else {
            listOf("BRV-053", "BRV-055", "BRV-075", "BRV-096", "BRV-009", "BRV-098", "BRV-020", "BRV-019", "BRV-002", "BRV-037")
        }
        val chosen = mutableListOf<String>()
        var sum = 0
        for (sku in essentials) {
            val p = findProductBySku(sku)
            if (p != null && sum + p.price <= limit) {
                chosen.add(sku)
                sum += p.price
            }
        }
        val aiMsg = ChatMessage(
            sender = "ai",
            text = "Te propongo **${chosen.size} productos básicos de $storeName** por **RD$$sum** (te quedan **RD$${limit - sum}** de tu meta). Si te sirven, agrégalos con el botón de abajo; tu carrito actual no cambió.",
            skus = chosen,
            modelLabel = "Planificador de presupuesto"
        )
        _chatHistory.value = _chatHistory.value + userMsg + aiMsg
    }

    fun askAiAboutProduct(sku: String) {
        val p = findProductBySku(sku) ?: return
        navigateTo(AppScreen.BRAVIA)
        // Si el producto es ingrediente principal de una receta verificada, abrimos esa receta.
        val recipe = BravIaEngine.recipesUsing(sku).firstOrNull { r -> r.ingredients.any { sku in it.skus && !it.pantry } }
        sendCustomChatMsg(
            if (recipe != null) "Receta de ${recipe.name} con ${p.name}"
            else "¿Qué puedo preparar o combinar con ${p.name}? Recomiéndame productos del catálogo."
        )
    }

    fun addAiBundleToCart(messageId: Long) {
        val msg = _chatHistory.value.find { it.id == messageId } ?: return
        if (msg.skus.isEmpty()) return
        var units = 0
        msg.skus.forEach { sku ->
            val qty = msg.quantities[sku] ?: 1
            changeProductQty(sku, qty, silent = true)
            units += qty
        }
        showToast("✓", "$units ${if (units == 1) "artículo agregado" else "artículos agregados"} al carrito")
    }

    private val inlineSkuExtractRegex = Regex("""\b(BRV-\d{3}|ARC-\d{3})\b""", RegexOption.IGNORE_CASE)
    private val skuDisplayStripRegex = Regex(
        pattern = """\s*(?:\[\s*(?:SKU\s*:?\s*)?(?:BRV|ARC)-\d{3}\s*\]|\(\s*(?:SKU\s*:?\s*)?(?:BRV|ARC)-\d{3}\s*\)|\b(?:SKU\s*:?\s*)?(?:BRV|ARC)-\d{3}\b)""",
        option = RegexOption.IGNORE_CASE
    )
    fun setPendingChatImageUri(uri: String?) {
        _pendingChatImageUri.value = uri
        if (!uri.isNullOrBlank()) {
            showToast("sparkles", "Imagen adjuntada para análisis visual con Brav IA Soporte")
        }
    }

    private fun currentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun dispatchSuggestionMailPayloadAsync(payload: SuggestionMailPayload) {
        _submittedSuggestions.value = listOf(payload) + _submittedSuggestions.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("to_email", payload.toEmail)
                    put("from_user", payload.fromUser)
                    put("user_contact", payload.userContact)
                    put("active_branch", payload.activeBranch)
                    put("active_store", payload.activeStore)
                    put("category", payload.category)
                    put("priority", payload.priority)
                    put("subject", payload.subject)
                    put("summary_body", payload.summaryBody)
                    put("timestamp_iso", payload.timestampIso)
                }.toString()

                // Dispatch to FormSubmit / webhook relay for bezaleelpatrocino0@gmail.com
                val code = httpPostJson(
                    "https://formsubmit.co/ajax/${payload.toEmail}",
                    jsonBody,
                    mapOf("Accept" to "application/json")
                ).code
                Log.d(TAG, "Suggestion payload dispatched to ${payload.toEmail} (HTTP $code)")
            } catch (e: Exception) {
                Log.d(TAG, "Suggestion payload queued locally for ${payload.toEmail}: ${e.message}")
            }
        }
    }

    fun submitDirectSuggestionFromSupport(
        category: String,
        priority: String,
        subject: String = "",
        summaryBody: String
    ) {
        val clean = summaryBody.trim()
        if (clean.isEmpty()) {
            showToast("info", "Escribe tu sugerencia o reporte antes de enviar")
            return
        }
        val activeStoreCode = if (_activeStore.value == StoreMode.ARCA) "arca" else "bravo"
        val userProfile = _profile.value
        val finalSubject = subject.trim().ifEmpty { "[BravoVa Feedback] ${clean.take(50)}" }
        val payload = SuggestionMailPayload(
            toEmail = SUGGESTION_TARGET_EMAIL,
            fromUser = userProfile.name,
            userContact = "${userProfile.email} | ${userProfile.phone}",
            activeBranch = _activeBranch.value,
            activeStore = activeStoreCode,
            category = category,
            priority = priority,
            subject = finalSubject,
            summaryBody = clean,
            timestampIso = currentIsoTimestamp()
        )
        dispatchSuggestionMailPayloadAsync(payload)
        showToast("check", "Sugerencia enviada a $SUGGESTION_TARGET_EMAIL")
    }

    private fun extractSkusFromResponse(rawText: String): Pair<String, List<String>> {
        val regex = Regex("""[\*\s_]*SKUS?[\*\s_:]*\[(.*?)\]""", RegexOption.IGNORE_CASE)
        val match = regex.find(rawText)
        val explicitSkus = if (match != null) {
            match.groupValues[1]
                .split(",")
                .map { it.trim().replace("\"", "").replace("'", "").replace("`", "") }
                .filter { findProductBySku(it) != null }
                .distinct()
        } else {
            emptyList()
        }
        val withoutFooter = rawText.replace(regex, "").trim()

        val inlineSkus = inlineSkuExtractRegex.findAll(withoutFooter)
            .map { it.groupValues[1].uppercase() }
            .filter { findProductBySku(it) != null }
            .distinct()
            .toList()

        val cleanText = withoutFooter.replace(skuDisplayStripRegex, "").trim()
        val finalSkus = if (explicitSkus.isNotEmpty()) explicitSkus else inlineSkus
        return cleanText to finalSkus
    }

    fun requestOrderConfirmationForMessage(messageId: Long) {
        val target = _chatHistory.value.find { it.id == messageId } ?: return
        if (target.skus.isEmpty()) {
            showToast("ℹ️", "Este mensaje no contiene artículos para pedir")
            return
        }
        _chatHistory.value = _chatHistory.value.map { msg ->
            if (msg.id == messageId) {
                msg.copy(requiresOrderConfirmation = true)
            } else {
                msg
            }
        }
        showToast("🛒", "Revisa el resumen en el chat y toca Confirmar Pedido")
    }

    fun cancelOrderConfirmationForMessage(messageId: Long) {
        _chatHistory.value = _chatHistory.value.map { msg ->
            if (msg.id == messageId) {
                msg.copy(requiresOrderConfirmation = false)
            } else {
                msg
            }
        }
        showToast("ℹ️", "Confirmación de pedido cancelada en el chat")
    }

    fun confirmOrderFromChatMessage(messageId: Long) {
        val msg = _chatHistory.value.find { it.id == messageId } ?: return
        if (msg.orderConfirmed) {
            openOrderTracking(msg.confirmedOrderId)
            return
        }
        val orderSkus = msg.skus.distinct().filter { findProductBySku(it) != null }
        if (orderSkus.isEmpty()) {
            showToast("ℹ️", "No hay artículos válidos para procesar el pedido")
            return
        }

        // Se pide EXACTAMENTE lo que muestra la tarjeta (con sus cantidades). Antes se mezclaba con
        // todo el carrito y se cobraba más de lo que decía "Total a autorizar".
        val previousCart = _cart.value
        val orderItems = orderSkus.map { sku -> CartItem(sku, (msg.quantities[sku] ?: 1).coerceIn(1, 20)) }
        _cart.value = orderItems

        completeCheckoutOrder()

        // Lo que la persona ya tenía en el carrito se conserva para su próxima compra.
        if (previousCart.isNotEmpty()) {
            _cart.value = previousCart
            val uid = getVerifiedActiveUid()
            viewModelScope.launch {
                // El pedido vacía el carrito guardado en segundo plano; esperamos a que termine y lo reponemos.
                delay(900L)
                localRepo.replaceCart(previousCart.associate { it.sku to it.qty })
                if (uid != null) previousCart.forEach { firestoreRepo.setCartItemQuantity(uid, it.sku, it.qty, if (it.sku.startsWith("ARC-")) "ARCA" else "BRAVO") }
            }
        }
        val createdOrderId = _trackedOrderId.value
        val createdOrder = _trackedOrder.value

        val confirmationMsg = ChatMessage(
            sender = "ai",
            text = "¡Pedido **#$createdOrderId** confirmado manualmente con éxito! Se enviaron **${createdOrder?.badgeNum ?: orderSkus.size} artículos** desde **${_activeBranch.value}** por **RD$${createdOrder?.total?.toInt() ?: 0}.00** (Ruta GPS: ${_realLocationMetrics.value.roadDistanceKm} km • ETA ~${_realLocationMetrics.value.stageAdjustedEtaMinutes} min).",
            skus = orderSkus,
            modelLabel = "Brav IA • Pedido Confirmado #$createdOrderId",
            requiresOrderConfirmation = false,
            orderConfirmed = true,
            confirmedOrderId = createdOrderId
        )

        _chatHistory.value = _chatHistory.value.map { item ->
            if (item.id == messageId) {
                item.copy(
                    requiresOrderConfirmation = false,
                    orderConfirmed = true,
                    confirmedOrderId = createdOrderId
                )
            } else {
                item
            }
        } + confirmationMsg
    }

    fun buildPersonalizedThinkingText(query: String, hasAttachedImage: Boolean = false): String {
        val q = normalizeSearchText(query)
        val storeName = if (_activeStore.value == StoreMode.ARCA) "El Arca Petshop" else "Supermercados Bravo"
        val cleanSnippet = query.trim().replace("\n", " ").take(26)
        return when {
            hasAttachedImage -> "Mirando tu imagen…"
            q.contains("sugerencia") || q.contains("sugiero") || q.contains("queja") || q.contains("reclamo") || q.contains("reporte") || q.contains("felicit") ->
                "Registrando tu comentario…"
            isOrderIntentQuery(query) -> "Armando tu pedido con productos de $storeName…"
            q.contains("alergia") || q.contains("sin lactosa") || q.contains("sin gluten") || q.contains("vegano") || q.contains("keto") || q.contains("diabet") ->
                "Buscando opciones para «$cleanSnippet»…"
            BravIaEngine.matchRecipe(query) != null -> "Abriendo la receta verificada de ${BravIaEngine.matchRecipe(query)?.name}…"
            isInstructionOrRecipeQuery(query) -> "Buscando en el recetario y el catálogo…"
            q.contains("oferta") || q.contains("descuento") || q.contains("barato") ->
                "Buscando ofertas en ${_activeBranch.value}…"
            q.contains("historial") || q.contains("habitual") || q.contains("lista") ->
                "Revisando tus compras frecuentes…"
            else -> "Buscando «$cleanSnippet» en $storeName…"
        }
    }

    private fun isSuggestionOrSupportFeedbackQuery(query: String): Boolean {
        val q = normalizeSearchText(query)
        return q.contains("sugerencia") ||
            q.contains("sugiero") ||
            q.contains("deberian") ||
            q.contains("seria bueno") ||
            q.contains("queja") ||
            q.contains("reclamo") ||
            q.contains("reportar") ||
            q.contains("reporte") ||
            q.contains("mejorar la app") ||
            q.contains("mejora") ||
            q.contains("felicitacion") ||
            q.contains("felicito") ||
            q.contains("excelente servicio")
    }

    fun pauseOrStopAiGeneration() {
        activeAiChatJob?.cancel()
        activeAiChatJob = null
        _isAiGenerating.value = false
        _chatHistory.value = _chatHistory.value.map { msg ->
            if (msg.isLoading) {
                msg.copy(
                    text = "**Generación en pausa:**\n• Detuve la respuesta inmediatamente a tu solicitud.\n• Puedes escribir otra consulta, adjuntar una foto o tocar una sugerencia cuando desees continuar.",
                    isLoading = false,
                    modelLabel = "Brav IA • Respuesta Pausada"
                )
            } else {
                msg
            }
        }
        showToast("info", "Respuesta de Brav IA pausada")
    }

    fun sendCustomChatMsg(rawQuery: String, imageUriOverride: String? = null) {
        val attachedUri = imageUriOverride ?: _pendingChatImageUri.value
        val hasImage = !attachedUri.isNullOrBlank()
        val query = rawQuery.trim().ifEmpty {
            if (hasImage) "¿Qué ves en esta imagen y cómo me puedes ayudar con ella?" else ""
        }
        if (query.isEmpty()) return
        checkEasterEgg(query)
        if (_isAiGenerating.value) pauseOrStopAiGeneration()
        _pendingChatImageUri.value = null

        // Historial previo (antes de agregar este turno), para que Brav IA recuerde la conversación.
        val previousTurns = _chatHistory.value
            .filter { !it.isLoading && it.text.isNotBlank() }
            .takeLast(6)
            .map { ChatTurn(fromUser = it.sender == "user", text = it.text) }

        val userMsg = ChatMessage(sender = "user", text = query, attachedImageUri = attachedUri)
        val orderIntent = isOrderIntentQuery(query)
        val suggestionIntent = isSuggestionOrSupportFeedbackQuery(query)
        val isArca = _activeStore.value == StoreMode.ARCA
        val storePrefix = if (isArca) "ARC" else "BRV"
        val storeName = if (isArca) "El Arca Petshop" else "Supermercados Bravo"
        val storeProducts = getActiveProducts().ifEmpty { getAllProducts().filter { it.sku.startsWith(storePrefix) } }
        val lookup: (String) -> Product? = { sku -> findProductBySku(sku) }
        // Presupuesto: el que la persona escribe en el mensaje, o el que fijó a mano con el control.
        val budget = BravIaEngine.budgetFromQuery(query) ?: _targetBudgetInChat.value.takeIf { _budgetSetByUser.value }
        val matchedRecipe = if (isArca) null else BravIaEngine.matchRecipe(query)

        _isAiGenerating.value = true
        val loadingMsgId = System.nanoTime() + 1L
        val thinking = buildPersonalizedThinkingText(query, hasAttachedImage = hasImage)
        _chatHistory.value = _chatHistory.value + userMsg + ChatMessage(id = loadingMsgId, sender = "ai", text = thinking, isLoading = true)

        fun publish(reply: GroundedReply, label: String, durationMs: Long) {
            val suggestion = reply.suggestion?.let { draft -> buildSuggestionPayload(draft.subject, draft.summary.ifBlank { query }, query) }
                ?: if (suggestionIntent) buildSuggestionPayload("Sugerencia de cliente", query, query) else null
            suggestion?.let { dispatchSuggestionMailPayloadAsync(it) }
            val skus = reply.items.map { it.sku }
            _chatHistory.value = _chatHistory.value.map { msg ->
                if (msg.id != loadingMsgId) msg else msg.copy(
                    text = reply.text.ifBlank { "Listo." },
                    skus = skus,
                    quantities = reply.items.associate { it.sku to it.qty },
                    recipeId = reply.recipe?.id,
                    budgetExceededBy = reply.budgetExceededBy,
                    isLoading = false,
                    modelLabel = if (suggestion != null) "Brav IA • Sugerencia registrada" else label,
                    durationMs = durationMs,
                    requiresOrderConfirmation = orderIntent && skus.isNotEmpty(),
                    suggestionPayload = suggestion
                )
            }
        }

        activeAiChatJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            fun elapsed() = (System.currentTimeMillis() - startTime).coerceAtMost(MAX_AI_RESPONSE_TIMEOUT_MS)
            fun localAnswer() = BravIaEngine.localReply(query, storeProducts, lookup, storePrefix, budget, hasImage)
            val accumulated = StringBuilder()
            try {
                // Sin sesión de Firebase no hay modelo: respondemos con datos del catálogo y del recetario.
                if (!isFirebaseUserAuthenticatedForAi()) {
                    delay(250L)
                    publish(localAnswer(), "Brav IA • Modo sin conexión", elapsed())
                    return@launch
                }

                val imageBytes: ByteArray? = attachedUri?.let { loadScaledJpeg(application, it) }
                val frequent = runCatching { getPurchaseHistoryStats().take(5).map { it.product.name } }.getOrDefault(emptyList())
                val cartLines = _cart.value.mapNotNull { item -> findProductBySku(item.sku)?.let { "${item.qty} × ${it.name}" } }
                val prompt = BravIaEngine.buildPrompt(
                    PromptContext(
                        query = query,
                        storeName = storeName,
                        branch = _activeBranch.value,
                        etaMinutes = _realLocationMetrics.value.stageAdjustedEtaMinutes.takeIf { it > 0 },
                        candidates = BravIaEngine.selectCandidates(
                            query = query + " " + previousTurns.lastOrNull { it.fromUser }?.text.orEmpty(),
                            storeProducts = storeProducts,
                            recipe = matchedRecipe,
                            extraSkus = _cart.value.map { it.sku }
                        ),
                        matchedRecipe = matchedRecipe,
                        history = previousTurns,
                        cartSummary = cartLines.takeIf { it.isNotEmpty() }?.joinToString(", ")?.take(500),
                        budget = budget,
                        frequentProducts = frequent,
                        hasImage = imageBytes != null
                    )
                )
                val input = AiContent(prompt, imageBytes)

                withTimeout(MAX_AI_RESPONSE_TIMEOUT_MS) {
                    val stream = try {
                        bravIaChatModel.generateContentStream(input)
                    } catch (_: Exception) {
                        bravIaChatFallbackModel.generateContentStream(input)
                    }
                    try {
                        stream.collect { chunk ->
                            val part = chunk.text.orEmpty()
                            if (part.isEmpty()) return@collect
                            accumulated.append(part)
                            // Vista previa en vivo: solo el campo "respuesta", ya sin precios ni SKUs.
                            val preview = BravIaEngine.streamingAnswer(accumulated.toString())?.let { BravIaEngine.sanitize(it) }
                            if (!preview.isNullOrBlank()) {
                                _chatHistory.value = _chatHistory.value.map { msg ->
                                    if (msg.id == loadingMsgId) msg.copy(text = preview, isLoading = false, modelLabel = "Brav IA") else msg
                                }
                            }
                        }
                    } catch (streamErr: Exception) {
                        if (BravIaEngine.parseReply(accumulated.toString()) == null) {
                            val backup = bravIaChatFallbackModel.generateContent(input).text.orEmpty()
                            if (backup.isBlank()) throw streamErr
                            accumulated.clear()
                            accumulated.append(backup)
                        }
                    }
                }

                val raw = accumulated.toString()
                val parsed = BravIaEngine.parseReply(raw) ?: raw.takeIf { it.length >= 20 }?.let { BravIaEngine.fromPlainText(it) }
                if (parsed == null) {
                    publish(localAnswer(), "Brav IA • Modo sin conexión", elapsed())
                } else {
                    val reply = BravIaEngine.ground(parsed, lookup, storePrefix, matchedRecipe, budget)
                    publish(reply, if (reply.recipe != null) "Brav IA • Receta verificada" else "Brav IA", elapsed())
                }
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    val partial = BravIaEngine.parseReply(accumulated.toString())
                    val reply = partial?.let { BravIaEngine.ground(it, lookup, storePrefix, matchedRecipe, budget) } ?: localAnswer()
                    publish(reply, "Brav IA • Respuesta parcial", elapsed())
                } else {
                    throw e
                }
            } catch (e: Exception) {
                Log.d(TAG, "Brav IA usa la respuesta local: ${e.message}")
                publish(localAnswer(), "Brav IA • Modo sin conexión", elapsed())
            } finally {
                _isAiGenerating.value = false
                activeAiChatJob = null
            }
        }
    }

    /** Sugerencia para el buzón. Los datos de contacto quedan en el dispositivo; nunca se le pasan al modelo. */
    private fun buildSuggestionPayload(subject: String, summary: String, query: String): SuggestionMailPayload {
        val n = BravIaText.normalize(query)
        return SuggestionMailPayload(
            toEmail = "bezaleelpatrocino0@gmail.com",
            fromUser = _profile.value.name,
            userContact = "${_profile.value.email} / ${_profile.value.phone}",
            activeBranch = _activeBranch.value,
            activeStore = if (_activeStore.value == StoreMode.ARCA) "arca" else "bravo",
            category = when {
                listOf("danad", "vencid", "error", "fallo", "bug", "reclamo", "queja").any { n.contains(it) } -> "REPORTE_SOPORTE"
                n.contains("felicit") || n.contains("gracias") -> "FELICITACION"
                n.contains("producto") || n.contains("agreguen") || n.contains("vendan") -> "SUGERENCIA_PRODUCTO"
                else -> "MEJORA_UI_UX"
            },
            priority = "MEDIA",
            subject = "[Brav IA] ${subject.take(80)}",
            summaryBody = summary,
            timestampIso = currentIsoTimestamp()
        )
    }

    // =========================================================================
    // REAL-TIME TRAFFIC, SPEED & ETA SIMULATION + AI DRIVER CHAT (CARLOS RODRÍGUEZ)
    // =========================================================================

    private data class StreetRouteSegment(
        val index: Int,
        val fraction: Float,
        val streetLabel: String,
        val baseSpeedKmh: Int,
        val trafficCondition: String,
        val trafficLevel: String,
        val delayDeltaMin: Int
    )

    private val santoDomingoRouteSegments = listOf(
        StreetRouteSegment(0, 0.05f, "Zona de Despacho Cadena de Frío • Sucursal Bravo", 0, "Verificando sellado térmico", "FLUIDO", 0),
        StreetRouteSegment(1, 0.18f, "Incorporación a Av. Winston Churchill", 26, "Salida despejada de sucursal", "FLUIDO", 0),
        StreetRouteSegment(2, 0.34f, "Av. Winston Churchill esq. C. Roberto Pastoriza", 38, "Tráfico fluido en corredor central", "FLUIDO", 0),
        StreetRouteSegment(3, 0.50f, "Crucero Semafórico Av. 27 de Febrero", 19, "Tráfico moderado en intersección (+1 min)", "MODERADO", 1),
        StreetRouteSegment(4, 0.66f, "Av. Abraham Lincoln • Polígono Central Piantini", 42, "Vía rápida despejada (-1 min)", "FLUIDO", -1),
        StreetRouteSegment(5, 0.78f, "Calle Paseo de los Locutores • Ensanche Naco", 33, "Flujo vehicular continuo", "FLUIDO", 0),
        StreetRouteSegment(6, 0.88f, "Calle Gustavo Mejía Ricart • Vía Residencial", 25, "Reducción preventiva en zona residencial", "FLUIDO", 0),
        StreetRouteSegment(7, 0.95f, "Entrando a tu cuadra residencial (A 140 metros)", 16, "Aproximación final a tu edificio", "FLUIDO", 0),
        StreetRouteSegment(8, 1.00f, "En la puerta de tu dirección / Lobby principal", 0, "¡El repartidor ha llegado a tu puerta!", "FLUIDO", 0)
    )

    private fun startLiveDeliveryTelemetryTicker() {
        liveTelemetryJob?.cancel()
        liveTelemetryJob = viewModelScope.launch {
            val tickMs = 250L
            var tickCounter = 0L
            while (true) {
                delay(tickMs)
                tickCounter++
                val currentTelemetry = _driverTelemetry.value
                if (!currentTelemetry.isAutoSimulating) continue

                val activeOrd = _trackedOrder.value ?: _orders.value.firstOrNull()
                val stage = OrderDeliveryStage.fromStatusString(activeOrd?.status)
                val metrics = _realLocationMetrics.value
                val totalRoadKm = metrics.roadDistanceKm.coerceAtLeast(0.85)

                if (stage == OrderDeliveryStage.DELIVERED || currentTelemetry.routeProgressFraction >= 0.999f) {
                    if (currentTelemetry.currentSpeedKmh != 0 || currentTelemetry.routeProgressFraction < 1.0f) {
                        val destSeg = santoDomingoRouteSegments.last()
                        _driverTelemetry.value = currentTelemetry.copy(
                            driverLat = metrics.userLat,
                            driverLng = metrics.userLng,
                            currentSpeedKmh = 0,
                            currentStreet = destSeg.streetLabel,
                            trafficCondition = destSeg.trafficCondition,
                            trafficLevel = destSeg.trafficLevel,
                            dynamicEtaMinutes = 0,
                            remainingDistanceKm = 0.0,
                            routeProgressFraction = 1.0f,
                            waypointIndex = 8
                        )
                    }
                    continue
                }

                // Find active road segment from current continuous routeProgressFraction
                val currentFrac = currentTelemetry.routeProgressFraction.coerceIn(0.05f, 0.999f)
                val seg = santoDomingoRouteSegments.firstOrNull { it.fraction >= currentFrac }
                    ?: santoDomingoRouteSegments.last()
                val segIndex = seg.index

                // Natural speed micro-variation around the street segment's real speed (km/h)
                val baseSpeed = if (seg.baseSpeedKmh > 0) seg.baseSpeedKmh else 26
                val wave = sin(tickCounter * 0.08).toInt() * 2
                val liveSpeed = (baseSpeed + wave).coerceIn(14, 48)

                // True 1:1 physical real-time progression:
                // distance traveled in 250ms (0.25s) at liveSpeed (km/h) = (liveSpeed / 3600.0) * 0.25 km
                val distanceTraveledKm = (liveSpeed.toDouble() / 3600.0) * (tickMs / 1000.0)
                val fractionDelta = (distanceTraveledKm / totalRoadKm).toFloat()
                val nextFrac = (currentFrac + fractionDelta).coerceIn(0.05f, 1.0f)

                val liveLat = metrics.branchLat + (metrics.userLat - metrics.branchLat) * nextFrac
                val liveLng = metrics.branchLng + (metrics.userLng - metrics.branchLng) * nextFrac
                val remKm = ((totalRoadKm * (1f - nextFrac) * 100.0).roundToInt() / 100.0).coerceAtLeast(0.0)

                val speedForEta = max(liveSpeed, 22).toDouble()
                val rawEta = if (nextFrac >= 0.995f) {
                    0
                } else {
                    val travelMin = ((remKm / speedForEta) * 60.0).roundToInt()
                    (travelMin + seg.delayDeltaMin).coerceAtLeast(1)
                }

                val timeFmt = SimpleDateFormat("h:mm:ss a", Locale.US).format(Date())
                val prevWaypointIdx = currentTelemetry.waypointIndex

                _driverTelemetry.value = currentTelemetry.copy(
                    driverLat = liveLat,
                    driverLng = liveLng,
                    currentSpeedKmh = if (nextFrac >= 0.999f) 0 else liveSpeed,
                    avgSpeedKmh = if (tickCounter % 16L == 0L) (currentTelemetry.avgSpeedKmh * 3 + liveSpeed) / 4 else currentTelemetry.avgSpeedKmh,
                    peakSpeedKmh = max(currentTelemetry.peakSpeedKmh, liveSpeed),
                    currentStreet = seg.streetLabel,
                    trafficCondition = seg.trafficCondition,
                    trafficLevel = seg.trafficLevel,
                    trafficDelayDeltaMin = seg.delayDeltaMin,
                    dynamicEtaMinutes = rawEta,
                    remainingDistanceKm = remKm,
                    routeProgressFraction = nextFrac,
                    waypointIndex = segIndex,
                    lastUpdatedLabel = "GPS 1:1 en tiempo real • $timeFmt"
                )

                // Trigger live notifications only when crossing into a new physical waypoint along the route
                if (segIndex != prevWaypointIdx) {
                    val impliedStage = when (segIndex) {
                        0 -> OrderDeliveryStage.RECEIVED
                        1, 2 -> OrderDeliveryStage.PREPARED
                        3, 4, 5, 6 -> OrderDeliveryStage.IN_TRANSIT
                        7 -> OrderDeliveryStage.ARRIVING
                        else -> OrderDeliveryStage.DELIVERED
                    }
                    if (impliedStage != stage) {
                        updateTrackedOrderStatusInFirestore(impliedStage, showFeedbackToast = false)
                    }
                    val iconKey = when (impliedStage) {
                        OrderDeliveryStage.RECEIVED -> "receipt"
                        OrderDeliveryStage.PREPARED -> "box"
                        OrderDeliveryStage.IN_TRANSIT -> "delivery"
                        OrderDeliveryStage.ARRIVING -> "pin"
                        OrderDeliveryStage.DELIVERED -> "check"
                    }
                    _liveDeliveryBanner.value = LiveDeliveryBannerNotification(
                        iconKey = iconKey,
                        title = "${impliedStage.statusText} • ${if (liveSpeed > 0) "$liveSpeed km/h" else "En punto"}",
                        subtitle = "${seg.streetLabel} • ${seg.trafficCondition}",
                        speedKmh = liveSpeed,
                        etaMinutes = rawEta,
                        progressPct = (nextFrac * 100).roundToInt(),
                        trafficBadge = seg.trafficLevel,
                        timestampLabel = "Ahora"
                    )
                    if (segIndex == 4 || segIndex == 7 || segIndex == 8) {
                        val autoNote = when (segIndex) {
                            4 -> "Voy cruzando por ${seg.streetLabel} a $liveSpeed km/h. El tráfico está ${seg.trafficLevel.lowercase()}, llego en unos $rawEta min."
                            7 -> "¡Ya estoy entrando a tu cuadra a menos de 150 metros! Ve preparándote para recibir el pedido."
                            else -> "¡Acabo de llegar a la entrada principal con tu pedido Bravo! Cadena de frío 100% intacta."
                        }
                        _driverChatMessages.value = _driverChatMessages.value + DriverChatMessage(
                            sender = "driver",
                            text = autoNote,
                            timestampLabel = SimpleDateFormat("h:mm a", Locale.US).format(Date()),
                            speedKmhAtSend = liveSpeed,
                            streetAtSend = seg.streetLabel
                        )
                    }
                }
            }
        }
    }

    private fun syncDriverTelemetryToStage(stage: OrderDeliveryStage, triggerLiveBanner: Boolean) {
        val wpIdx = stage.routePointIndex.coerceIn(0, santoDomingoRouteSegments.lastIndex)
        val seg = santoDomingoRouteSegments[wpIdx]
        val metrics = _realLocationMetrics.value
        val frac = seg.fraction
        val liveLat = metrics.branchLat + (metrics.userLat - metrics.branchLat) * frac
        val liveLng = metrics.branchLng + (metrics.userLng - metrics.branchLng) * frac
        val remKm = ((metrics.roadDistanceKm * (1f - frac) * 100.0).roundToInt() / 100.0).coerceAtLeast(0.0)
        val eta = if (stage == OrderDeliveryStage.DELIVERED) 0 else max(1, stage.etaMinutes + seg.delayDeltaMin)
        val timeFmt = SimpleDateFormat("h:mm:ss a", Locale.US).format(Date())

        _driverTelemetry.value = _driverTelemetry.value.copy(
            driverLat = liveLat,
            driverLng = liveLng,
            currentSpeedKmh = seg.baseSpeedKmh,
            currentStreet = seg.streetLabel,
            trafficCondition = seg.trafficCondition,
            trafficLevel = seg.trafficLevel,
            trafficDelayDeltaMin = seg.delayDeltaMin,
            dynamicEtaMinutes = eta,
            remainingDistanceKm = remKm,
            routeProgressFraction = frac,
            waypointIndex = wpIdx,
            lastUpdatedLabel = "Actualizado • $timeFmt"
        )

        if (triggerLiveBanner) {
            _liveDeliveryBanner.value = LiveDeliveryBannerNotification(
                iconKey = stage.emoji,
                title = "${stage.statusText} • ${if (seg.baseSpeedKmh > 0) "${seg.baseSpeedKmh} km/h" else "0 km/h"}",
                subtitle = "${seg.streetLabel} • ${seg.trafficCondition}",
                speedKmh = seg.baseSpeedKmh,
                etaMinutes = eta,
                progressPct = stage.progressPct,
                trafficBadge = seg.trafficLevel,
                timestampLabel = "En vivo"
            )
            if (_pushNotifications.value) {
                BravoPushNotificationCenter.dispatchPushNotification(
                    context = application,
                    category = PushNotificationCategory.ORDER_STATUS,
                    title = "${stage.statusText} (${seg.baseSpeedKmh} km/h) • ETA ~$eta min",
                    body = "Carlos Rodríguez reporta: ${seg.streetLabel} (${seg.trafficCondition}). Restan $remKm km.",
                    topic = BravoPushNotificationCenter.TOPIC_ORDERS_REALTIME,
                    targetOrderId = _trackedOrder.value?.id ?: "ORD-88412",
                    progressPct = stage.progressPct,
                    postSystemNotification = true
                )
            }
        }
    }

    fun toggleLiveTrafficSimulation() {
        val next = !_driverTelemetry.value.isAutoSimulating
        _driverTelemetry.value = _driverTelemetry.value.copy(isAutoSimulating = next)
        showToast(
            if (next) "delivery" else "info",
            if (next) "Actualización automática de tráfico y GPS activada" else "Simulación en tiempo real en pausa"
        )
    }

    fun triggerLiveTrafficRecalculation() {
        val currentIdx = _driverTelemetry.value.waypointIndex
        val nextIdx = if (currentIdx >= 8) 1 else (currentIdx + 1).coerceAtMost(8)
        val seg = santoDomingoRouteSegments[nextIdx]
        val impliedStage = when (nextIdx) {
            0 -> OrderDeliveryStage.RECEIVED
            1, 2 -> OrderDeliveryStage.PREPARED
            3, 4, 5, 6 -> OrderDeliveryStage.IN_TRANSIT
            7 -> OrderDeliveryStage.ARRIVING
            else -> OrderDeliveryStage.DELIVERED
        }
        updateTrackedOrderStatusInFirestore(impliedStage, showFeedbackToast = false)
        syncDriverTelemetryToStage(impliedStage, triggerLiveBanner = true)
        showToast("bell", "Tráfico en vivo actualizado: ${seg.baseSpeedKmh} km/h • ${seg.trafficCondition}")
    }

    fun dismissLiveDeliveryBanner() {
        _liveDeliveryBanner.value = null
    }

    private fun buildSmartDriverAiReply(
        userMessage: String,
        telemetry: DriverLiveTelemetry,
        orderId: String
    ): String {
        val lower = userMessage.lowercase()
        val firstName = _profile.value.name.substringBefore(" ").ifBlank { "Cliente" }
        return when {
            lower.contains("donde") || lower.contains("dónde") || lower.contains("ubicacion") || lower.contains("vienes") ->
                "¡Hola $firstName! Ahora mismo voy por **${telemetry.currentStreet}** a **${telemetry.currentSpeedKmh} km/h**. Me faltan **${telemetry.remainingDistanceKm} km** (~${telemetry.dynamicEtaMinutes} min) para llegar."
            lower.contains("recepcion") || lower.contains("recepción") || lower.contains("lobby") || lower.contains("porteria") || lower.contains("conserje") ->
                "¡Copiado $firstName! Apenas llegue al edificio en unos **${telemetry.dynamicEtaMinutes} minutos**, lo dejo en recepción/lobby con el encargado y te aviso por aquí."
            lower.contains("frio") || lower.contains("frío") || lower.contains("helado") || lower.contains("congelado") || lower.contains("carne") ->
                "¡Tranquilo $firstName! Llevo tus refrigerados en la cava térmica sellada a **2°C**. Van 100% frescos y sin perder cadena de frío."
            lower.contains("trafico") || lower.contains("tráfico") || lower.contains("tarda") || lower.contains("demora") || lower.contains("tiempo") ->
                "El reporte en vivo marca **${telemetry.trafficCondition.lowercase()}** por ${telemetry.currentStreet}. Mantengo **${telemetry.currentSpeedKmh} km/h** y llego en **~${telemetry.dynamicEtaMinutes} min**."
            lower.contains("avisa") || lower.contains("llamar") || lower.contains("timbre") || lower.contains("llegar") ->
                "¡Claro que sí $firstName! En cuanto esté parqueando frente a tu entrada (en ~${telemetry.dynamicEtaMinutes} min) te timbro y te mando alerta en vivo."
            else ->
                "¡Recibido $firstName! Tomo en cuenta tu mensaje («$userMessage») para la orden **#$orderId**. Avanzo por **${telemetry.currentStreet}** a **${telemetry.currentSpeedKmh} km/h** (ETA ~${telemetry.dynamicEtaMinutes} min)."
        }
    }

    fun sendDriverChatMessage(rawMessage: String) {
        val cleanMsg = rawMessage.trim()
        if (cleanMsg.isEmpty() || _isDriverTyping.value) return

        val telemetrySnapshot = _driverTelemetry.value
        val nowLabel = SimpleDateFormat("h:mm a", Locale.US).format(Date())
        val userEntry = DriverChatMessage(
            sender = "user",
            text = cleanMsg,
            timestampLabel = nowLabel,
            speedKmhAtSend = telemetrySnapshot.currentSpeedKmh,
            streetAtSend = telemetrySnapshot.currentStreet
        )
        _driverChatMessages.value = _driverChatMessages.value + userEntry
        _isDriverTyping.value = true

        val activeOrd = _trackedOrder.value ?: _orders.value.firstOrNull()
        val orderId = activeOrd?.id ?: "ORD-88412"
        val firstName = _profile.value.name.substringBefore(" ").ifBlank { "Cliente" }
        val orderItemNames = activeOrd?.skus
            ?.mapNotNull { findProductBySku(it)?.name }
            ?.take(5)
            ?.joinToString(", ")
            ?: "productos frescos de Supermercados Bravo"

        val driverPrompt = """
            Actúa como Carlos Rodríguez, repartidor motorista Express de Supermercados Bravo en Santo Domingo (Placa K-04921, calificación 4.95 estrellas).
            Estás entregando en tiempo real el pedido #$orderId al cliente $firstName.
            Datos reales del GPS en este instante:
            - Calle actual: ${telemetrySnapshot.currentStreet}
            - Velocidad actual: ${telemetrySnapshot.currentSpeedKmh} km/h
            - Estado del tráfico: ${telemetrySnapshot.trafficCondition} (${telemetrySnapshot.trafficLevel})
            - Distancia restante: ${telemetrySnapshot.remainingDistanceKm} km
            - Tiempo estimado de llegada (ETA): ${telemetrySnapshot.dynamicEtaMinutes} minutos
            - Artículos en la mochila térmica: $orderItemNames
            
            El cliente te acaba de escribir este mensaje: "$cleanMsg"
            Responde en primera persona como Carlos Rodríguez, en 1 o 2 oraciones breves, amables, profesionales y realistas, mencionando tu calle o velocidad/ETA actual si aplica. No uses códigos SKU.
        """.trimIndent()

        viewModelScope.launch {
            val replyText = if (!isFirebaseUserAuthenticatedForAi()) {
                delay(380L)
                buildSmartDriverAiReply(cleanMsg, _driverTelemetry.value, orderId)
            } else {
                try {
                    withTimeout(8500L) {
                        val response = geminiModel.generateContent(driverPrompt)
                        response.text?.trim()?.takeIf { it.length >= 12 }
                            ?: buildSmartDriverAiReply(cleanMsg, _driverTelemetry.value, orderId)
                    }
                } catch (_: Exception) {
                    delay(650L)
                    buildSmartDriverAiReply(cleanMsg, _driverTelemetry.value, orderId)
                }
            }

            val latestTelemetry = _driverTelemetry.value
            val driverReply = DriverChatMessage(
                sender = "driver",
                text = replyText.replace(skuDisplayStripRegex, ""),
                timestampLabel = SimpleDateFormat("h:mm a", Locale.US).format(Date()),
                speedKmhAtSend = latestTelemetry.currentSpeedKmh,
                streetAtSend = latestTelemetry.currentStreet
            )
            _driverChatMessages.value = _driverChatMessages.value + driverReply
            _isDriverTyping.value = false

            _liveDeliveryBanner.value = LiveDeliveryBannerNotification(
                iconKey = "delivery",
                title = "Mensaje de Carlos Rodríguez (${latestTelemetry.currentSpeedKmh} km/h)",
                subtitle = driverReply.text.replace("**", ""),
                speedKmh = latestTelemetry.currentSpeedKmh,
                etaMinutes = latestTelemetry.dynamicEtaMinutes,
                progressPct = (latestTelemetry.routeProgressFraction * 100).roundToInt(),
                trafficBadge = latestTelemetry.trafficLevel,
                timestampLabel = "Ahora"
            )
        }
    }
}
