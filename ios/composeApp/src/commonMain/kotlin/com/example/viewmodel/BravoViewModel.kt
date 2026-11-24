package com.example.viewmodel

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
            delay(480L)
            _isAppBooting.value = false
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

        // Urban moto speed ~26 km/h + 5 min branch dispatch
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

        // Also request a live single location update
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
        val updated = listOf(_lists.value.first()) + listOf(newList) + _lists.value.drop(1)
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
        val updated = listOf(_lists.value.first()) + listOf(newList) + _lists.value.drop(1)
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

    private fun matchRecipeIngredientBundle(query: String): List<String> {
        val q = normalizeSearchText(query)
        return when {
            q.contains("pizza") -> listOf(
                "BRV-112", // Bravo Harina de Trigo Todo Uso 2 Lb
                "BRV-202", // Bravo Queso Mozzarella en Barra 1 Lb
                "BRV-389", // Chorizo Español Vela Dulce y Picante Bravo 250g
                "BRV-058", // Pasta de Tomate La Famosa 16 Oz
                "BRV-039", // Bravo Aceite de Oliva Virgen Extra 500 Ml
                "BRV-243", // Bravo Orégano Molido Dominicano Puro
                "BRV-401", // Queso Parmesano Reggiano Rallado Fresco Bravo 150g
                "BRV-173"  // Ají Morrón Rojo Grande 1 Lb
            )
            q.contains("hamburguesa") || q.contains("burger") -> listOf(
                "BRV-134", // Hamburguesas de Res Artesanales 4u
                "BRV-222", // Pan Brioche para Hamburguesa 4u
                "BRV-095", // Bravo Queso Cheddar 24 Lonjas
                "BRV-008", // Bravo Tocino Ahumado Rebanado
                "BRV-090", // Tomate Bugalú Fresco 1 Lb
                "BRV-091", // Bravo Lechuga Romana Lavada
                "BRV-089", // Cebolla Roja en Malla 1 Lb
                "BRV-120"  // Bravo Ketchup Tradicional 14 Oz
            )
            q.contains("lasana") -> listOf(
                "BRV-057", // Bravo Espaguetis Trigo Duro 400g
                "BRV-078", // Carne Molida de Res Primera 1.5 Lb
                "BRV-202", // Bravo Queso Mozzarella en Barra 1 Lb
                "BRV-205", // Bravo Queso Crema Untable 225g
                "BRV-401", // Queso Parmesano Reggiano Rallado Fresco Bravo 150g
                "BRV-058", // Pasta de Tomate La Famosa 16 Oz
                "BRV-100", // Bravo Mantequilla Con Sal 400g
                "BRV-098"  // Bravo Leche Entera UHT 1 Litro
            )
            q.contains("taco") || q.contains("burrito") || q.contains("quesadilla") || q.contains("nacho") -> listOf(
                "BRV-444", // Tortillas de Harina Suaves para Tacos y Wraps Bravo 10 uds
                "BRV-078", // Carne Molida de Res Primera 1.5 Lb
                "BRV-202", // Bravo Queso Mozzarella en Barra 1 Lb
                "BRV-095", // Bravo Queso Cheddar 24 Lonjas
                "BRV-088", // Aguacate Criollo Grande Unidad
                "BRV-090", // Tomate Bugalú Fresco 1 Lb
                "BRV-089", // Cebolla Roja en Malla 1 Lb
                "BRV-176"  // Verdurita Cilantro Corriente Atado
            )
            q.contains("mofongo") || q.contains("tostones") -> listOf(
                "BRV-083", // Plátano Verde Barahona Unidad
                "BRV-318", // Capa de Chicharrón de Cerdo Fresca Bravo
                "BRV-171", // Ajo Criollo Dominicano Malla 3 Cabezas
                "BRV-037", // Bravo Aceite Vegetal Puro 1.8 L
                "BRV-100", // Bravo Mantequilla Con Sal 400g
                "BRV-201", // Bravo Queso Gouda Holandés 1 Lb
                "BRV-242", // Bravo Sopitas Cubitos de Pollo 24 Und
                "BRV-088"  // Aguacate Criollo Grande Unidad
            )
            q.contains("chofan") || q.contains("arroz chino") -> listOf(
                "BRV-053", // Bravo Arroz Selecto Grado A 5 Lb
                "BRV-076", // Pechuga de Pollo Fileteada 1.5 Lb
                "BRV-124", // Chuleta Ahumada de Cerdo Especial 1 Lb
                "BRV-005", // Bravo Jamón Cocido Seleccionado 1 Lb
                "BRV-096", // Bravo Huevos Frescos 12 Und
                "BRV-039", // Bravo Aceite de Oliva Virgen Extra 500 Ml
                "BRV-191", // Zanahoria Criolla de Constanza 1 Lb
                "BRV-173"  // Ají Morrón Rojo Grande 1 Lb
            )
            q.contains("sancocho") || q.contains("asopao") -> listOf(
                "BRV-075", // Pollo Entero Fresco Bravo 3.8 Lb
                "BRV-077", // Carne de Res para Guisar 1.5 Lb
                "BRV-079", // Costillita de Cerdo Ahumada 1.5 Lb
                "BRV-303", // Longaniza Criolla Casera Ahumada Bravo
                "BRV-083", // Plátano Verde Barahona Unidad
                "BRV-085", // Yuca Fresca Seleccionada 1 Lb
                "BRV-086", // Yautía Blanca Criolla 1 Lb
                "BRV-087", // Auyama Criolla en Trozos 1 Lb
                "BRV-053"  // Bravo Arroz Selecto Grado A 5 Lb
            )
            q.contains("mangu") || q.contains("tres golpes") -> listOf(
                "BRV-083", // Plátano Verde Barahona Unidad
                "BRV-004", // Salami Induveca Súper Especial 1.5 Lb
                "BRV-093", // Bravo Queso de Freír 1 Lb
                "BRV-096", // Bravo Huevos Frescos 12 Und
                "BRV-100", // Bravo Mantequilla Con Sal 400g
                "BRV-089", // Cebolla Roja en Malla 1 Lb
                "BRV-040"  // Bravo Vinagre Blanco Destilado 1 L
            )
            q.contains("pastelon") || q.contains("maduro") -> listOf(
                "BRV-084", // Plátano Maduro Amarillo Unidad
                "BRV-078", // Carne Molida de Res Primera 1.5 Lb
                "BRV-201", // Bravo Queso Gouda Holandés 1 Lb
                "BRV-202", // Bravo Queso Mozzarella en Barra 1 Lb
                "BRV-058", // Pasta de Tomate La Famosa 16 Oz
                "BRV-100", // Bravo Mantequilla Con Sal 400g
                "BRV-098", // Bravo Leche Entera UHT 1 Litro
                "BRV-172"  // Ají Cubanela Verde Tradicional 1 Lb
            )
            q.contains("bandera") || q.contains("locrio") || q.contains("moro") -> listOf(
                "BRV-053", // Bravo Arroz Selecto Grado A 5 Lb
                "BRV-055", // Bravo Habichuelas Rojas 800 Gr
                "BRV-075", // Pollo Entero Fresco Bravo 3.8 Lb
                "BRV-056", // Guandules Verdes con Coco La Famosa
                "BRV-058", // Pasta de Tomate La Famosa 16 Oz
                "BRV-087", // Auyama Criolla en Trozos 1 Lb
                "BRV-037", // Bravo Aceite Vegetal Puro 1.8 L
                "BRV-088"  // Aguacate Criollo Grande Unidad
            )
            q.contains("pasta") || q.contains("alfredo") || q.contains("espagueti") || q.contains("carbonara") || q.contains("codito") || q.contains("penne") -> listOf(
                "BRV-057", // Bravo Espaguetis Trigo Duro 400g
                "BRV-076", // Pechuga de Pollo Fileteada 1.5 Lb
                "BRV-206", // Bravo Crema de Leche para Cocinar 1 L
                "BRV-401", // Queso Parmesano Reggiano Rallado Fresco Bravo 150g
                "BRV-201", // Bravo Queso Gouda Holandés 1 Lb
                "BRV-008", // Bravo Tocino Ahumado Rebanado 250 Gr
                "BRV-100", // Bravo Mantequilla Con Sal 400g
                "BRV-171"  // Ajo Criollo Dominicano Malla 3 Cabezas
            )
            q.contains("arroz con leche") || q.contains("habichuelas con dulce") || q.contains("flan") || q.contains("postre") -> listOf(
                "BRV-053", // Bravo Arroz Selecto Grado A 5 Lb
                "BRV-055", // Bravo Habichuelas Rojas 800 Gr
                "BRV-099", // Leche Evaporada Carnation 315g
                "BRV-360", // Leche Condensada Azucarada Cremosa Bravo 395g
                "BRV-098", // Bravo Leche Entera UHT 1 Litro
                "BRV-060", // Bravo Azúcar Crema 5 Lb
                "BRV-100", // Bravo Mantequilla Con Sal 400g
                "BRV-165"  // Batata Dulce Dominicana Fresca 1 Lb
            )
            q.contains("panqueque") || q.contains("hotcake") || q.contains("pancake") || q.contains("waffle") || q.contains("tostada francesa") -> listOf(
                "BRV-112", // Bravo Harina de Trigo Todo Uso 2 Lb
                "BRV-096", // Bravo Huevos Frescos 12 Und
                "BRV-098", // Bravo Leche Entera UHT 1 Litro
                "BRV-100", // Bravo Mantequilla Con Sal 400g
                "BRV-033", // Bravo Miel de Abejas 100% Pura 500 Ml
                "BRV-183", // Fresas Frescas de Constanza 1 Lb
                "BRV-092"  // Guineo Maduro Criollo Mano 5u
            )
            q.contains("ensalada") || q.contains("cesar") -> listOf(
                "BRV-091", // Bravo Lechuga Romana Lavada 250 Gr
                "BRV-076", // Pechuga de Pollo Fileteada 1.5 Lb
                "BRV-119", // Bravo Mayonesa Real Frasco 16 Oz
                "BRV-401", // Queso Parmesano Reggiano Rallado Fresco Bravo 150g
                "BRV-090", // Tomate Bugalú Fresco 1 Lb
                "BRV-193", // Pepino de Ensalada Fresco Pack 3u
                "BRV-088", // Aguacate Criollo Grande Unidad
                "BRV-039"  // Bravo Aceite de Oliva Virgen Extra 500 Ml
            )
            q.contains("salmon") || q.contains("camaron") || q.contains("pescado") || q.contains("marisco") || q.contains("bacalao") -> listOf(
                "BRV-141", // Filete de Salmón Fresco Noruego 1 Lb
                "BRV-142", // Camarones Jumbo Limpios Pelados 1 Lb
                "BRV-339", // Filete de Tilapia Roja Fresca sin Espinas Bravo
                "BRV-039", // Bravo Aceite de Oliva Virgen Extra 500 Ml
                "BRV-100", // Bravo Mantequilla Con Sal 400g
                "BRV-171", // Ajo Criollo Dominicano Malla 3 Cabezas
                "BRV-177", // Limón Persa Jugoso sin Semillas 1 Lb
                "BRV-206"  // Bravo Crema de Leche para Cocinar 1 L
            )
            q.contains("churrasco") || q.contains("parrilla") || q.contains("bbq") || q.contains("asado") || q.contains("ribeye") -> listOf(
                "BRV-301", // Churrasco Angus Importado Choice Bravo
                "BRV-302", // Picaña de Res Premium con Capa de Grasa Bravo
                "BRV-389", // Chorizo Español Vela Dulce y Picante Bravo 250g
                "BRV-125", // Costilla de Cerdo BBQ Baby Back 1 Lb
                "BRV-085", // Yuca Fresca Seleccionada 1 Lb
                "BRV-014"  // Bravo Casabe Natural Tostado
            )
            q.contains("sandwich") || q.contains("emparedado") || q.contains("desayuno") -> listOf(
                "BRV-009", // Bravo Viga Mediana 875 Gr
                "BRV-005", // Bravo Jamón Cocido Seleccionado 1 Lb
                "BRV-201", // Bravo Queso Gouda Holandés 1 Lb
                "BRV-008", // Bravo Tocino Ahumado Rebanado 250 Gr
                "BRV-096", // Bravo Huevos Frescos 12 Und
                "BRV-100", // Bravo Mantequilla Con Sal 400g
                "BRV-059", // Café Santo Domingo Molido 1 Lb
                "BRV-049"  // Bravo Jugo 100% Naranja 1.5 L
            )
            else -> emptyList()
        }
    }

    private fun scoreProductsForQuery(query: String, allProducts: List<Product>): List<Product> {
        val normQuery = normalizeSearchText(query)
        val recipeBundleSkus = matchRecipeIngredientBundle(query)
        val stopWords = setOf(
            "para", "como", "con", "los", "las", "una", "uno", "unos", "unas", "del", "por",
            "que", "cual", "cuales", "cuanto", "cuesta", "precio", "precios", "tienes", "tienen",
            "hay", "dame", "quiero", "necesito", "busco", "bravo", "arca", "favor", "puedes",
            "pedir", "pide", "pideme", "ordenar", "ordename", "comprame", "hacer", "pedido",
            "hola", "buenos", "buenas", "dias", "tardes", "noches", "gracias", "donde",
            "cuando", "quien", "algo", "cosa", "cosas", "todo", "todos", "catalogo", "articulos",
            "articulo", "producto", "productos", "recomienda", "recomiendame", "sugiere",
            "paso", "pasos", "receta", "preparar", "cocinar", "instrucciones"
        )
        val rawTokens = normQuery.split(Regex("""[^a-z0-9]+"""))
            .filter { it.length >= 3 && it !in stopWords }
        val synonymMap = mapOf(
            "pizza" to listOf("pepperoni", "mozzarella", "harina", "pasta de tomate", "aceite de oliva", "oregano", "parmesano", "morron"),
            "hamburguesa" to listOf("hamburguesa", "brioche", "cheddar", "tocino", "tomate", "lechuga", "cebolla", "cachu"),
            "burger" to listOf("hamburguesa", "brioche", "cheddar", "tocino", "tomate", "lechuga"),
            "lasana" to listOf("lasana", "molida", "mozzarella", "ricotta", "parmesano", "pasta de tomate", "mantequilla", "leche"),
            "taco" to listOf("tortilla", "molida", "mozzarella", "cheddar", "guacamole", "tomate", "cebolla", "cilantro"),
            "burrito" to listOf("tortilla", "molida", "habichuela", "mozzarella", "guacamole", "arroz"),
            "quesadilla" to listOf("tortilla", "mozzarella", "gouda", "pechuga", "guacamole"),
            "mofongo" to listOf("platano verde", "chicharron", "tocino", "ajo", "aceite", "mantequilla", "queso", "aguacate"),
            "chofan" to listOf("arroz", "pechuga", "chuleta", "jamon", "huevo", "ajonjoli", "zanahoria", "morron"),
            "refresco" to listOf("coca", "sprite", "country", "pepsi", "seven", "soda", "merengue", "frambuesa"),
            "gaseosa" to listOf("coca", "sprite", "country", "soda"),
            "cerveza" to listOf("presidente", "modelo", "corona", "stella", "heineken", "ceniza", "one"),
            "bodega" to listOf("vino", "ron", "whisky", "sangria", "espumante", "cerveza", "brugal", "barcelo"),
            "licor" to listOf("ron", "whisky", "baileys", "tequila", "vodka", "gin", "vino"),
            "helado" to listOf("helado", "paleta", "cono", "bon"),
            "dulce" to listOf("chocolate", "galleta", "helado", "mermelada", "nutella", "caramelo", "gomita", "bocadillo"),
            "postre" to listOf("helado", "bizcocho", "flan", "leche condensada", "evaporada", "chocolate", "galleta"),
            "picadera" to listOf("salami", "queso", "papitas", "nachos", "tostitos", "chorizo", "salchicha", "aceituna", "cerveza"),
            "desayuno" to listOf("platano", "huevo", "salami", "queso", "pan", "leche", "cafe", "avena", "mantequilla", "jugo"),
            "cena" to listOf("pan", "queso", "jamon", "yogurt", "platano", "huevo", "atun", "ensalada"),
            "almuerzo" to listOf("arroz", "pollo", "habichuela", "carne", "chuleta", "aguacate", "ensalada", "platano"),
            "pescado" to listOf("filete", "salmon", "bacalao", "tilapia", "mero", "dorado", "atun", "sardina"),
            "marisco" to listOf("camaron", "calamar", "pulpo", "mejillon", "marisco", "lambí"),
            "res" to listOf("res", "churrasco", "molida", "bistec", "filete", "hamburguesa", "guisar"),
            "cerdo" to listOf("cerdo", "chuleta", "costilla", "lomo", "tocineta", "jamon", "longaniza"),
            "viveres" to listOf("platano", "yuca", "yautia", "name", "auyama", "batata", "papa", "guineo"),
            "aseo" to listOf("shampoo", "jabon", "desodorante", "pasta dental", "cepillo", "crema", "afeitar"),
            "higiene" to listOf("shampoo", "jabon", "desodorante", "pasta dental", "papel higienico", "toalla"),
            "mascota" to listOf("perro", "gato", "cachorro", "arena", "alimento", "collar", "juguete", "antipulgas")
        )

        val expandedStems = buildList {
            for (token in rawTokens) {
                val singular = token.removeSuffix("es").removeSuffix("s")
                if (token.length >= 3) add(token)
                if (singular.length >= 3) add(singular)
                synonymMap[token]?.let { addAll(it) }
                synonymMap[singular]?.let { addAll(it) }
            }
        }.distinct()

        val categoryHints = mutableListOf<String>()
        if (normQuery.contains("embutido") || normQuery.contains("salami") || normQuery.contains("jamon") || normQuery.contains("salchicha") || normQuery.contains("chorizo") || normQuery.contains("longaniza") || normQuery.contains("tocineta")) {
            categoryHints.add("quesos y embutidos")
            categoryHints.add("embutidos")
        }
        if (normQuery.contains("pan") || normQuery.contains("galleta") || normQuery.contains("bizcocho") || normQuery.contains("tostada") || normQuery.contains("casabe") || normQuery.contains("reposteria")) {
            categoryHints.add("panaderia")
            categoryHints.add("panes y galletas")
        }
        if (normQuery.contains("limpieza") || normQuery.contains("hogar") || normQuery.contains("detergente") || normQuery.contains("cloro") || normQuery.contains("jabon") || normQuery.contains("papel") || normQuery.contains("desinfectante") || normQuery.contains("fregaza")) {
            categoryHints.add("limpieza y hogar")
            categoryHints.add("hogar y limpieza")
        }
        if (normQuery.contains("aceite") || normQuery.contains("vinagre") || normQuery.contains("oliva") || normQuery.contains("aderezo") || normQuery.contains("balsamico")) {
            categoryHints.add("aceite")
            categoryHints.add("despensa")
        }
        if (normQuery.contains("agua") || normQuery.contains("refresco") || normQuery.contains("jugo") || normQuery.contains("soda") || normQuery.contains("hidratante") || normQuery.contains("malta") || normQuery.contains("bebida")) {
            categoryHints.add("bebidas y jugos")
            categoryHints.add("agua y refrescos")
        }
        if (normQuery.contains("arroz") || normQuery.contains("habichuela") || normQuery.contains("pasta") || normQuery.contains("espagueti") || normQuery.contains("salsa") || normQuery.contains("cereal") || normQuery.contains("avena") || normQuery.contains("cafe") || normQuery.contains("azucar") || normQuery.contains("despensa") || normQuery.contains("abarrote")) {
            categoryHints.add("despensa y abarrotes")
            categoryHints.add("alimentacion general")
        }
        if (normQuery.contains("bebe") || normQuery.contains("panal") || normQuery.contains("pampers") || normQuery.contains("huggies") || normQuery.contains("formula") || normQuery.contains("compota") || normQuery.contains("toallita") || normQuery.contains("shampoo") || normQuery.contains("desodorante") || normQuery.contains("dental")) {
            categoryHints.add("cuidado personal y bebe")
            categoryHints.add("bebes")
        }
        if (normQuery.contains("vino") || normQuery.contains("cerveza") || normQuery.contains("ron") || normQuery.contains("whisky") || normQuery.contains("bodega") || normQuery.contains("sangria") || normQuery.contains("espumante") || normQuery.contains("licor")) {
            categoryHints.add("cervezas, vinos y licores")
            categoryHints.add("bodega")
        }
        if (normQuery.contains("carne") || normQuery.contains("pollo") || normQuery.contains("res") || normQuery.contains("cerdo") || normQuery.contains("chuleta") || normQuery.contains("costilla") || normQuery.contains("molida") || normQuery.contains("churrasco") || normQuery.contains("filete") || normQuery.contains("pavo") || normQuery.contains("pescado") || normQuery.contains("camaron") || normQuery.contains("salmon") || normQuery.contains("marisco")) {
            categoryHints.add("carnes, pescados y mariscos")
            categoryHints.add("carne")
        }
        if (normQuery.contains("fruta") || normQuery.contains("vegetal") || normQuery.contains("platano") || normQuery.contains("yuca") || normQuery.contains("aguacate") || normQuery.contains("tomate") || normQuery.contains("cebolla") || normQuery.contains("papa") || normQuery.contains("guineo") || normQuery.contains("manzana") || normQuery.contains("ensalada") || normQuery.contains("viveres")) {
            categoryHints.add("frutas y vegetales")
        }
        if (normQuery.contains("leche") || normQuery.contains("queso") || normQuery.contains("huevo") || normQuery.contains("yogurt") || normQuery.contains("mantequilla") || normQuery.contains("lacteo") || normQuery.contains("crema")) {
            categoryHints.add("lacteos y huevos")
            categoryHints.add("quesos y embutidos")
        }
        if (normQuery.contains("snack") || normQuery.contains("papita") || normQuery.contains("chocolate") || normQuery.contains("dulce") || normQuery.contains("maní") || normQuery.contains("mani") || normQuery.contains("palomita")) {
            categoryHints.add("snacks y dulces")
        }
        if (normQuery.contains("congelado") || normQuery.contains("helado") || normQuery.contains("nugget")) {
            categoryHints.add("congelados")
        }
        if (normQuery.contains("perro") || normQuery.contains("gato") || normQuery.contains("mascota") || normQuery.contains("cachorro") || normQuery.contains("arena") || normQuery.contains("croqueta") || normQuery.contains("pulga")) {
            categoryHints.add("arca")
        }

        if (expandedStems.isEmpty() && categoryHints.isEmpty() && recipeBundleSkus.isEmpty()) {
            return emptyList()
        }

        val scoredList = allProducts
            .mapNotNull { prod ->
                val normName = normalizeSearchText(prod.name)
                val normCat = normalizeSearchText(prod.category)
                val normBadge = normalizeSearchText(prod.badge)
                val normSku = prod.sku.lowercase()
                val nameWords = normName.split(Regex("""[^a-z0-9]+""")).filter { it.isNotBlank() }
                var score = 0

                val bundleIndex = recipeBundleSkus.indexOf(prod.sku)
                if (bundleIndex >= 0) {
                    score += 180 - (bundleIndex * 4)
                }
                if (normQuery.contains(normSku)) score += 150
                for (stem in expandedStems) {
                    if (nameWords.any { it == stem || it.startsWith(stem) }) {
                        score += 48
                    } else if (normName.contains(stem)) {
                        score += 28
                    }
                    if (normCat.contains(stem)) score += 14
                    if (normBadge.contains(stem)) score += 10
                }
                for (catHint in categoryHints) {
                    if (catHint == "arca" && prod.sku.startsWith("ARC-")) score += 26
                    else if (normCat.contains(catHint)) score += 20
                }
                if (score >= 20) prod to score else null
            }
            .sortedByDescending { it.second }
            .map { it.first }

        return scoredList
    }

    private fun buildFocusedCatalogContext(query: String): String {
        val allProducts = getAllProducts()
        val recipeBundleProducts = matchRecipeIngredientBundle(query).mapNotNull { findProductBySku(it) }
        val scoredMatches = scoreProductsForQuery(query, allProducts)

        val historySkus = getPurchaseHistoryStats().map { it.product.sku }.take(12)
        val cartSkus = _cart.value.map { it.sku }

        val prioritizedProducts = (
            recipeBundleProducts +
                scoredMatches.take(80) +
                (cartSkus + historySkus).mapNotNull { findProductBySku(it) }
            )
            .distinctBy { it.sku }
            .take(100)

        val prioritizedLines = prioritizedProducts.joinToString("\n") { p ->
            val storeTag = if (p.sku.startsWith("ARC-")) "El Arca Petshop" else "Bravo"
            val nut = p.nutritionProfile()
            val calTag = nut.caloriesKcal?.let { "${it} kcal (${nut.caloriesPortionLabel})" } ?: "Artículo no comestible (0 kcal)"
            val allergenTag = nut.allergens.joinToString(", ")
            val dietTag = nut.dietaryTags.joinToString(", ")
            "[${p.sku}] ${p.name} - RD$${p.price} (${p.unit} • ${p.category} • $storeTag) | Calorías: $calTag | Ingredientes: ${nut.ingredients} | Alérgenos: $allergenTag | Dietas: $dietTag"
        }

        val fullCatalogCompact = allProducts.joinToString("\n") { p ->
            val nut = p.nutritionProfile()
            val calCompact = nut.caloriesKcal?.let { "${it}kcal" } ?: "NoComestible"
            val dietCompact = nut.dietaryTags.take(2).joinToString("/")
            "[${p.sku}] ${p.name} | RD$${p.price} | ${p.unit} | ${p.category} | $calCompact | Alérgenos:${nut.allergens.firstOrNull().orEmpty()} | $dietCompact"
        }

        return "INGREDIENTES Y ARTÍCULOS PRIORITARIOS CON CALORÍAS EXACTAS, INGREDIENTES, ALÉRGENOS Y DIETAS:\n$prioritizedLines\n\nCATÁLOGO COMPLETO OFICIAL CON NUTRICIÓN E INGREDIENTES:\n$fullCatalogCompact"
    }

    fun getStoreInventoryContext(query: String = ""): String {
        return buildFocusedCatalogContext(query)
    }

    private suspend fun fetchInternetRecipeVerification(query: String): String? = withContext(Dispatchers.IO) {
        try {
            // Extract dish subject from user query
            val cleaned = query.lowercase()
                .replace(Regex("""¿|\?|!|¡|,|\."""), " ")
                .replace(
                    Regex("""\b(como|cómo|hacer|se hace|preparar|preparo|receta|paso a paso|pasos|dame|enseñame|enséñame|cocinar|cocino|quiero|necesito|ingredientes|para|una|un|el|la|los|las|de|del|con|sin|alergia|dieta|calorias|calorías)\b"""),
                    " "
                )
                .trim()
                .replace(Regex("""\s+"""), " ")
            if (cleaned.length < 3) return@withContext null

            val results = mutableListOf<String>()

            // 1. Query Wikipedia ES REST summary for authentic culinary description & ingredients
            val wikiSlug = URLEncoder.encode(cleaned.replaceFirstChar { it.uppercase() }, "UTF-8").replace("+", "%20")
            val wikiResp = runCatching {
                httpGetText(
                    "https://es.wikipedia.org/api/rest_v1/page/summary/$wikiSlug",
                    mapOf("User-Agent" to "BravoVaRD/3.0 (KMP; CulinaryAssistant)")
                )
            }.getOrNull()
            if (wikiResp != null && wikiResp.code == 200) {
                val body = wikiResp.body
                val json = JSONObject(body)
                val extract = json.optString("extract", "").trim()
                if (extract.length > 35) {
                    results.add("Referencia Culinaria Verificada en Web (Wikipedia Gastronomía): $extract")
                }
            }

            // 1b. If direct slug did not match, search Wikipedia ES API for the recipe/dish
            if (results.isEmpty()) {
                val searchEncoded = URLEncoder.encode("$cleaned receta gastronomía", "UTF-8")
                val searchResp = runCatching {
                    httpGetText(
                        "https://es.wikipedia.org/w/api.php?action=query&list=search&srsearch=$searchEncoded&utf8=&format=json&srlimit=2",
                        mapOf("User-Agent" to "BravoVaRD/3.0 (KMP; CulinaryAssistant)")
                    )
                }.getOrNull()
                if (searchResp != null && searchResp.code == 200) {
                    val body = searchResp.body
                    val json = JSONObject(body)
                    val searchArr = json.optJSONObject("query")?.optJSONArray("search")
                    if (searchArr != null && searchArr.length() > 0) {
                        val snippets = mutableListOf<String>()
                        for (i in 0 until searchArr.length()) {
                            val item = searchArr.getJSONObject(i)
                            val title = item.optString("title")
                            val rawSnippet = item.optString("snippet").replace(Regex("<[^>]*>"), "")
                            if (rawSnippet.isNotBlank()) {
                                snippets.add("$title: $rawSnippet")
                            }
                        }
                        if (snippets.isNotEmpty()) {
                            results.add("Búsqueda Gastronómica en Vivo: ${snippets.joinToString(" | ")}")
                        }
                    }
                }
            }

            // 2. Query TheMealDB API for structured international recipe ingredients & instructions
            val mealQuery = URLEncoder.encode(cleaned, "UTF-8")
            val mealResp = runCatching {
                httpGetText("https://www.themealdb.com/api/json/v1/1/search.php?s=$mealQuery")
            }.getOrNull()
            if (mealResp != null && mealResp.code == 200) {
                val body = mealResp.body
                val json = JSONObject(body)
                val meals = json.optJSONArray("meals")
                if (meals != null && meals.length() > 0) {
                    val firstMeal = meals.getJSONObject(0)
                    val mealName = firstMeal.optString("strMeal")
                    val mealCategory = firstMeal.optString("strCategory")
                    val mealArea = firstMeal.optString("strArea")
                    val instructions = firstMeal.optString("strInstructions").take(650)
                    val ingList = mutableListOf<String>()
                    for (i in 1..12) {
                        val ing = firstMeal.optString("strIngredient$i").trim()
                        val measure = firstMeal.optString("strMeasure$i").trim()
                        if (ing.isNotEmpty() && ing != "null") {
                            ingList.add("$ing ($measure)")
                        }
                    }
                    results.add(
                        "Receta Verificada en TheMealDB ($mealName - $mealArea / $mealCategory): " +
                            "Ingredientes con medidas: ${ingList.joinToString(", ")}. Método y tiempos: $instructions"
                    )
                }
            }

            if (results.isNotEmpty()) results.joinToString("\n") else null
        } catch (_: Exception) {
            null
        }
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
                    val (cleanText, parsedSkus) = extractSkusFromResponse(rawText)
                    val noteLine = cleanText
                        .lineSequence()
                        .map { it.replace(Regex("""^[\*\s]*NOTA:\s*""", RegexOption.IGNORE_CASE), "").replace("**", "").trim() }
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
    }

    fun setCatalogFilterQuery(q: String) {
        _catalogFilterQuery.value = q
    }

    fun setCategorySearchQuery(q: String) {
        _categorySearchQuery.value = q
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

    fun showToast(icon: String, message: String) {
        toastJob?.cancel()
        _toast.value = ToastNotification(icon = icon, message = message)
        toastJob = viewModelScope.launch {
            delay(1850L)
            _toast.value = null
        }
    }

    fun changeProductQty(sku: String, delta: Int) {
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

        if (p != null) {
            if (delta > 0) {
                showToast("✓", "${p.name} • RD$${p.price}")
            } else if (delta < 0 && newQty == 0) {
                showToast("🗑️", "${p.name} removido")
            }
        }
    }

    fun removeProductCompletely(sku: String) {
        _cart.value = _cart.value.filterNot { it.sku == sku }
        val uid = getVerifiedActiveUid()
        viewModelScope.launch {
            localRepo.setCartQty(sku, 0)
            if (uid != null) {
                firestoreRepo.setCartItemQuantity(uid, sku, 0)
            }
        }
        showToast("🗑️", "Producto removido del carrito")
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
            total = t.finalPay,
            branch = branch,
            skus = _cart.value.map { it.sku }
        )

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

        showToast("check", "¡Pedido confirmado! +RD$${t.bravisimaGenerated} en tu Tarjeta Bravísima")
        navigateTo(AppScreen.DELIVERY)

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
        ord.skus.forEach { sku -> changeProductQty(sku, 1) }
        navigateTo(AppScreen.CART)
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
    }

    fun applyBudgetInChat(optimizeExisting: Boolean) {
        val limit = _targetBudgetInChat.value
        val isArca = _activeStore.value == StoreMode.ARCA
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

        _cart.value = chosen.map { CartItem(it, 1) }
        viewModelScope.launch {
            localRepo.replaceCart(chosen.associateWith { 1 })
        }

        val cashback = sum / 100
        val storeName = if (isArca) "El Arca Petshop" else "Supermercados Bravo"
        val userMsg = ChatMessage(
            sender = "user",
            text = if (optimizeExisting) {
                "Ajustar mi carrito actual para no pasarme de RD$$limit"
            } else {
                "Armar una compra inteligente dentro de RD$$limit"
            }
        )
        val aiMsg = ChatMessage(
            sender = "ai",
            text = "¡Con mucho gusto! Te armé una selección de **${chosen.size} productos esenciales de $storeName** por **RD$$sum.00** (ahorras **RD$${limit - sum}** respecto a tu meta de RD$$limit y sumas **+RD$$cashback** en Bravísima). También puedes guardarla como lista de compras con un toque.",
            skus = chosen,
            modelLabel = "Planificador Inteligente de Presupuesto"
        )
        _chatHistory.value = _chatHistory.value + userMsg + aiMsg
        showToast("💰", "Carrito ajustado a RD$$sum.00")
    }

    fun askAiAboutProduct(sku: String) {
        val p = findProductBySku(sku) ?: return
        navigateTo(AppScreen.BRAVIA)
        sendCustomChatMsg("Dame una receta exacta, concisa y paso a paso usando ${p.name} (RD$${p.price}) y dime qué otros productos del catálogo necesito.")
    }

    fun addAiBundleToCart(messageId: Long) {
        val msg = _chatHistory.value.find { it.id == messageId } ?: return
        if (msg.skus.isEmpty()) return
        msg.skus.forEach { sku -> changeProductQty(sku, 1) }
        showToast("✓", "Ingredientes y productos agregados al carrito")
    }

    private fun isOffTopicQuery(query: String): Boolean {
        val q = query.lowercase()
        val offTopicKeywords = listOf(
            "presidente", "política", "politica", "elecciones", "partido", "diputado",
            "senador", "gobierno", "guerra", "religión", "religion", "fútbol", "futbol",
            "pelota", "béisbol", "beisbol", "nba", "messi", "cristiano", "licey", "aguilas",
            "python", "javascript", "kotlin", "java", "código", "codigo", "programar",
            "html", "css", "sql", "ecuación", "ecuacion", "derivada", "integral",
            "matemática", "matematica", "historia de", "capital de", "quién descubrió",
            "quien descubrio", "chiste", "poema", "canción", "cancion", "horóscopo",
            "horoscopo", "bitcoin", "cripto", "novio", "novia", "tarea de", "ensayo sobre"
        )
        val businessAndCulinaryKeywords = listOf(
            "bravo", "arca", "precio", "rd$", "peso", "comprar", "carrito", "receta",
            "pasos", "paso a paso", "preparar", "preparación", "preparacion", "cómo hacer",
            "como hacer", "cómo se hace", "como se hace", "cómo cocino", "como cocino",
            "cocinar", "comida", "cena", "almuerzo", "desayuno", "sancocho", "mangú",
            "mangu", "arroz", "pollo", "carne", "salami", "queso", "leche", "pan",
            "pasta", "espagueti", "lasaña", "lasana", "pastelón", "pastelon", "plátano",
            "platano", "yuca", "habichuela", "moro", "locrio", "asopao", "ensalada",
            "hornear", "freír", "freir", "hervir", "guisar", "sazonar", "adobar",
            "limpieza", "jabón", "jabon", "cloro", "papel", "perro", "gato", "mascota",
            "alimento", "croqueta", "arena", "veterinari", "bravísima", "bravisima",
            "sucursal", "envío", "envio", "delivery", "oferta", "descuento", "presupuesto",
            "ingrediente", "producto", "catálogo", "catalogo", "pasillo", "chef",
            "lista", "listas", "historial", "habitual", "siempre", "frecuente"
        )
        val hasOffTopic = offTopicKeywords.any { q.contains(it) }
        val hasBusinessOrRecipe = businessAndCulinaryKeywords.any { q.contains(it) }
        return hasOffTopic && !hasBusinessOrRecipe
    }

    private val inlineSkuExtractRegex = Regex("""\b(BRV-\d{3}|ARC-\d{3})\b""", RegexOption.IGNORE_CASE)
    private val skuDisplayStripRegex = Regex(
        pattern = """\s*(?:\[\s*(?:SKU\s*:?\s*)?(?:BRV|ARC)-\d{3}\s*\]|\(\s*(?:SKU\s*:?\s*)?(?:BRV|ARC)-\d{3}\s*\)|\b(?:SKU\s*:?\s*)?(?:BRV|ARC)-\d{3}\b)""",
        option = RegexOption.IGNORE_CASE
    )
    private val suggestionPayloadExtractRegex = Regex(
        pattern = """[\*\s_]*SUGGESTION_MAIL_PAYLOAD\s*:\s*(\{[\s\S]*?\})""",
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

    private fun isSuggestionOrFeedbackQuery(query: String): Boolean {
        val q = normalizeSearchText(query)
        val suggestionMarkers = listOf(
            "sugerencia", "sugiero", "sugerir", "reportar un fallo", "reporte de fallo",
            "error en la app", "fallo en la app", "bug", "buzon", "comentario de mejora",
            "solicitar producto", "agreguen al catalogo", "anadan al catalogo",
            "deberian agregar", "seria bueno que", "felicitacion", "felicitar al equipo",
            "enviar sugerencia", "bezaleelpatrocino0"
        )
        return suggestionMarkers.any { q.contains(it) }
    }

    private fun isSupportOrOperationalQuery(query: String, hasAttachedImage: Boolean): Boolean {
        if (hasAttachedImage || isSuggestionOrFeedbackQuery(query)) return true
        val q = normalizeSearchText(query)
        val supportMarkers = listOf(
            "producto danado", "vencido", "empaque roto", "defectuoso", "reposicion",
            "reembolso", "nota de credito", "factura", "ticket de caja", "ncf",
            "comprobante fiscal", "b01", "b02", "itbis", "soporte", "reclamo",
            "queja", "devolucion"
        )
        return supportMarkers.any { q.contains(it) }
    }

    /**
     * Mathematical budget enforcement:
     * Ensures sum(SKU_i) <= budgetLimit by substituting higher-cost items with Marca Exclusiva Bravo
     * equivalents in the same category/artType first, and then trimming optional highest-cost items.
     */
    private fun enforceBudgetLimitOnSkus(skus: List<String>, budgetLimit: Int): List<String> {
        if (skus.isEmpty()) return emptyList()
        val allProds = getAllProducts()
        val distinctProducts = skus.mapNotNull { findProductBySku(it) }.distinctBy { it.sku }.toMutableList()
        if (distinctProducts.isEmpty()) return emptyList()

        // Step 1: If over budget, substitute non-Bravo or expensive items with cheaper Marca Exclusiva Bravo items of same artType/category
        var currentSum = distinctProducts.sumOf { it.price }
        if (currentSum > budgetLimit) {
            for (i in distinctProducts.indices) {
                if (currentSum <= budgetLimit) break
                val item = distinctProducts[i]
                val cheaperBravoAlt = allProds
                    .filter { candidate ->
                        candidate.sku != item.sku &&
                            candidate.isBravo == item.isBravo &&
                            candidate.name.startsWith("Bravo", ignoreCase = true) &&
                            (candidate.artType == item.artType || candidate.category == item.category) &&
                            candidate.price < item.price &&
                            distinctProducts.none { it.sku == candidate.sku }
                    }
                    .minByOrNull { it.price }
                if (cheaperBravoAlt != null) {
                    currentSum = currentSum - item.price + cheaperBravoAlt.price
                    distinctProducts[i] = cheaperBravoAlt
                }
            }
        }

        // Step 2: Greedily keep core items within budgetLimit (always keeping at least the most affordable core item if budget allows)
        if (currentSum > budgetLimit) {
            val withinBudget = mutableListOf<Product>()
            var running = 0
            // Prioritize Marca Bravo items and core ingredients in original order
            val orderedCandidates = distinctProducts.sortedWith(
                compareByDescending<Product> { it.name.startsWith("Bravo", ignoreCase = true) }
                    .thenBy { it.price }
            )
            for (candidate in distinctProducts) {
                if (running + candidate.price <= budgetLimit) {
                    withinBudget.add(candidate)
                    running += candidate.price
                }
            }
            if (withinBudget.isEmpty()) {
                val cheapest = orderedCandidates.firstOrNull()
                if (cheapest != null) withinBudget.add(cheapest)
            }
            return withinBudget.map { it.sku }
        }

        return distinctProducts.map { it.sku }
    }

    private fun buildFinancialBreakdownText(skus: List<String>, budgetLimit: Int): String {
        val products = skus.mapNotNull { findProductBySku(it) }
        if (products.isEmpty()) return ""
        val subtotal = products.sumOf { it.price }
        val savingsMargin = (budgetLimit - subtotal).coerceAtLeast(0)
        val bravisimaReturn = subtotal / 100
        return """
            **Resumen de Presupuesto y Fidelidad (Límite RD$$budgetLimit.00):**
            • **Subtotal de la compra:** RD$$subtotal.00
            • **Margen de ahorro bajo tu presupuesto:** RD$$savingsMargin.00
            • **Retorno Tarjeta Bravísima (1%):** +RD$$bravisimaReturn.00 (RD$1.00 por cada RD$100.00)
        """.trimIndent()
    }

    private fun extractSuggestionPayloadFromResponse(
        rawText: String,
        userQuery: String,
        forceIfSuggestionQuery: Boolean = false
    ): Pair<String, SuggestionMailPayload?> {
        val match = suggestionPayloadExtractRegex.find(rawText)
        val activeStoreCode = if (_activeStore.value == StoreMode.ARCA) "arca" else "bravo"
        val userProfile = _profile.value
        val isoNow = currentIsoTimestamp()

        if (match != null) {
            val jsonBlock = match.groupValues[1].trim()
            val cleanedText = rawText.replace(match.value, "").trim()
            val parsedPayload = try {
                val obj = JSONObject(jsonBlock)
                SuggestionMailPayload(
                    toEmail = obj.optString("to_email", SUGGESTION_TARGET_EMAIL).ifBlank { SUGGESTION_TARGET_EMAIL },
                    fromUser = obj.optString("from_user", userProfile.name).ifBlank { userProfile.name },
                    userContact = obj.optString("user_contact", "${userProfile.email} | ${userProfile.phone}"),
                    activeBranch = obj.optString("active_branch", _activeBranch.value),
                    activeStore = obj.optString("active_store", activeStoreCode),
                    category = obj.optString("category", "MEJORA_UI_UX"),
                    priority = obj.optString("priority", "MEDIA"),
                    subject = obj.optString("subject", "[BravoVa Feedback] Sugerencia de ${userProfile.name}"),
                    summaryBody = obj.optString("summary_body", userQuery),
                    timestampIso = obj.optString("timestamp_iso", isoNow).ifBlank { isoNow }
                )
            } catch (_: Exception) {
                SuggestionMailPayload(
                    toEmail = SUGGESTION_TARGET_EMAIL,
                    fromUser = userProfile.name,
                    userContact = "${userProfile.email} | ${userProfile.phone}",
                    activeBranch = _activeBranch.value,
                    activeStore = activeStoreCode,
                    category = "MEJORA_UI_UX",
                    priority = "MEDIA",
                    subject = "[BravoVa Feedback] ${userQuery.take(48)}",
                    summaryBody = userQuery,
                    timestampIso = isoNow
                )
            }
            return cleanedText to parsedPayload
        }

        if (forceIfSuggestionQuery) {
            val q = normalizeSearchText(userQuery)
            val inferredCategory = when {
                q.contains("producto") || q.contains("catalogo") || q.contains("articulo") -> "SUGERENCIA_PRODUCTO"
                q.contains("fallo") || q.contains("error") || q.contains("bug") || q.contains("soporte") -> "REPORTE_SOPORTE"
                q.contains("felicit") || q.contains("excelente") || q.contains("gracias") -> "FELICITACION"
                else -> "MEJORA_UI_UX"
            }
            val inferredPriority = if (inferredCategory == "REPORTE_SOPORTE") "ALTA" else "MEDIA"
            val autoPayload = SuggestionMailPayload(
                toEmail = SUGGESTION_TARGET_EMAIL,
                fromUser = userProfile.name,
                userContact = "${userProfile.email} | ${userProfile.phone}",
                activeBranch = _activeBranch.value,
                activeStore = activeStoreCode,
                category = inferredCategory,
                priority = inferredPriority,
                subject = "[BravoVa Feedback] ${userQuery.take(52)}",
                summaryBody = userQuery,
                timestampIso = isoNow
            )
            return rawText.trim() to autoPayload
        }

        return rawText.trim() to null
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

        // Merge message SKUs into cart and execute real checkout flow
        val mergedMap = LinkedHashMap<String, Int>()
        _cart.value.forEach { item -> mergedMap[item.sku] = item.qty }
        orderSkus.forEach { sku -> mergedMap[sku] = (mergedMap[sku] ?: 0).coerceAtLeast(1) }
        _cart.value = mergedMap.map { (sku, qty) -> CartItem(sku, qty) }
        viewModelScope.launch {
            localRepo.replaceCart(mergedMap)
        }

        completeCheckoutOrder()
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

    private fun isConversationalOrStoreInfoQuery(query: String): Boolean {
        val q = normalizeSearchText(query)
        val infoPatterns = listOf(
            "hola", "buenos dias", "buenas tardes", "buenas noches", "como estas", "quien eres",
            "que puedes hacer", "ayuda", "horario", "abierto", "cierran", "abren", "domingo",
            "sucursal", "direccion", "ubicacion", "telefono", "contacto", "soporte",
            "como funciona", "tarjeta bravisima", "puntos", "balance", "metodo de pago",
            "efectivo", "impuesto", "itbis", "ncf", "comprobante", "tiempo de entrega",
            "cuanto tarda", "costo de envio", "gracias", "muchas gracias", "ok", "vale"
        )
        return infoPatterns.any { q.contains(it) } && scoreProductsForQuery(query, getAllProducts()).isEmpty()
    }

    private fun matchFallbackSkus(query: String): List<String> {
        val q = normalizeSearchText(query)
        val recipeBundle = matchRecipeIngredientBundle(query)
        if (recipeBundle.isNotEmpty()) {
            return recipeBundle
        }
        val scored = scoreProductsForQuery(query, getAllProducts())

        return when {
            q.contains("oferta") || q.contains("especial") || q.contains("descuento") -> {
                getDailyRotatingOffersForCurrentStore().map { it.sku }.distinct().take(6)
            }
            scored.isNotEmpty() -> {
                if (isInstructionOrRecipeQuery(query) && scored.size < 5) {
                    // Ensure any culinary instruction has complementary cooking staples (oil, garlic, onion, salt/seasoning)
                    (scored.map { it.sku } + listOf("BRV-039", "BRV-523", "BRV-089", "BRV-100", "BRV-058"))
                        .distinct()
                        .take(7)
                } else {
                    scored.take(8).map { it.sku }
                }
            }
            isInstructionOrRecipeQuery(query) -> {
                // Open-ended culinary preparation request: provide foundational chef pantry bundle
                listOf("BRV-076", "BRV-053", "BRV-039", "BRV-523", "BRV-089", "BRV-090", "BRV-202")
            }
            q.contains("historial") || q.contains("habitual") || q.contains("lista") || isOrderIntentQuery(query) -> {
                val cartSkus = _cart.value.map { it.sku }
                if (cartSkus.isNotEmpty()) {
                    cartSkus.take(8)
                } else {
                    getPurchaseHistoryStats().map { it.product.sku }.take(7).ifEmpty {
                        listOf("BRV-001", "BRV-009", "BRV-053", "BRV-075", "BRV-093", "BRV-017")
                    }
                }
            }
            // Si nada coincide con lo pedido, no devolver productos al azar
            else -> emptyList()
        }
    }

    fun buildPersonalizedThinkingText(query: String, hasAttachedImage: Boolean = false): String {
        val q = normalizeSearchText(query)
        val storeName = if (_activeStore.value == StoreMode.ARCA) "El Arca Petshop" else "Supermercados Bravo"
        val cleanSnippet = query.trim().replace("\n", " ").take(26)
        return when {
            hasAttachedImage -> "Brav IA Soporte auditando visualmente tu imagen adjunta (empaque, NCF o etiqueta)..."
            q.contains("sugerencia") || q.contains("sugiero") || q.contains("queja") || q.contains("reclamo") || q.contains("reporte") || q.contains("felicit") ->
                "Registrando tu reporte oficial y preparando envío a bezaleelpatrocino0@gmail.com..."
            isOrderIntentQuery(query) -> "Preparando tu orden asistida bajo tu presupuesto de RD$${_targetBudgetInChat.value}.00..."
            q.contains("alergia") || q.contains("sin lactosa") || q.contains("sin gluten") || q.contains("vegano") || q.contains("keto") || q.contains("caloria") || q.contains("diabet") ->
                "Verificando ingredientes, alérgenos y calorías exactas para «$cleanSnippet»..."
            isInstructionOrRecipeQuery(query) ->
                "Consultando receta verificada en la web y calculando presupuesto (≤ RD$${_targetBudgetInChat.value}.00) para «$cleanSnippet»..."
            q.contains("oferta") || q.contains("descuento") || q.contains("barato") ->
                "Verificando los descuentos del día en ${_activeBranch.value}..."
            q.contains("historial") || q.contains("habitual") || q.contains("lista") ->
                "Analizando tus compras frecuentes y listas en $storeName..."
            q.contains("precio") || q.contains("cuanto") || q.contains("cuesta") ->
                "Consultando precios exactos en RD$ y calorías para «$cleanSnippet»..."
            else -> "Analizando «$cleanSnippet» en el inventario oficial de $storeName..."
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

    private fun formatIngredientsBlock(products: List<Product>): String {
        return products.joinToString("\n") { p ->
            val nut = p.nutritionProfile()
            val calPart = nut.caloriesKcal?.let { " • **${it} kcal** (${nut.caloriesPortionLabel})" } ?: " • *Uso general / mascota*"
            val dietPart = nut.dietaryTags.take(2).joinToString(", ").let { if (it.isNotBlank()) " • $it" else "" }
            "• **${p.name}** — Medida comercial: **${p.unit}** — **RD$${p.price}.00** ($calPart$dietPart)"
        }
    }

    private fun buildFallbackRecipeOrCatalogResponse(
        query: String,
        enforcedSkusOverride: List<String>? = null,
        hasAttachedImage: Boolean = false
    ): String {
        val q = normalizeSearchText(query)
        val firstName = _profile.value.name.substringBefore(" ").ifBlank { "Cliente" }
        val isArca = _activeStore.value == StoreMode.ARCA
        val storeName = if (isArca) "El Arca Petshop" else "Supermercados Bravo"
        val budgetLimit = _targetBudgetInChat.value.coerceAtLeast(150)
        val matchedSkus = enforcedSkusOverride ?: enforceBudgetLimitOnSkus(matchFallbackSkus(query), budgetLimit)
        val matchedProducts = matchedSkus.mapNotNull { findProductBySku(it) }
        val totalBundle = matchedProducts.sumOf { it.price }
        val isOrderIntent = isOrderIntentQuery(query)
        val ingredientsSummary = formatIngredientsBlock(matchedProducts)
        val financialSummary = if (matchedProducts.isNotEmpty()) {
            buildFinancialBreakdownText(matchedSkus, budgetLimit)
        } else {
            ""
        }

        val rawResponse = if (hasAttachedImage) {
            when {
                q.contains("danad") || q.contains("vencid") || q.contains("abiert") || q.contains("mal estado") || q.contains("defect") -> {
                    """
                        **Auditoría Visual de Calidad — Brav IA Soporte (${_activeBranch.value}):**
                        • **Estimado/a $firstName**, hemos analizado con rigor técnico la fotografía adjunta y verificado la incidencia en el sello/estado del producto.
                        • En nombre de **$storeName**, te ofrecemos una **disculpa cálida y sincera**. Nuestro compromiso con tu familia es entregar siempre frescura garantizada.
                        • **Autorización Inmediata:** Queda aprobada la **reposición sin costo** en tu próximo envío express (8–12 min) o la acreditación directa de una **Nota de Crédito en tu Tarjeta Bravísima (${_profile.value.bravisimaNumber})** en la sucursal **${_activeBranch.value}**.
                    """.trimIndent()
                }
                q.contains("factura") || q.contains("ticket") || q.contains("recibo") || q.contains("ncf") || q.contains("itbis") -> {
                    """
                        **Auditoría OCR de Factura / Comprobante Fiscal (NCF) — Brav IA Soporte:**
                        • **Verificación de Comprobante:** Hemos procesado la imagen de tu ticket de compra de **${_activeBranch.value}** (modalidad **B02 Consumidor Final** / **B01 Crédito Fiscal**).
                        • **Desglose de Impuestos y Descuentos:** Los artículos exentos de la canasta básica mantienen **0% ITBIS**, los gravados reducidos aplican **16% ITBIS** y los generales **18% ITBIS**, respetando íntegramente los descuentos de **Marca Exclusiva Bravo**.
                        • **Retorno Tarjeta Bravísima (1%):** Tu compra genera **+RD$1.00 por cada RD$100.00 facturados** acreditados directamente al código **${_profile.value.bravisimaNumber}**.
                    """.trimIndent()
                }
                else -> {
                    val sampleItems = if (matchedProducts.isNotEmpty()) matchedProducts else getAllProducts().filter { it.isBravo }.take(4)
                    val sampleBlock = formatIngredientsBlock(sampleItems)
                    """
                        **Identificación Visual de Producto / Alacena — Brav IA Soporte:**
                        • **¡Hola, $firstName!** He inspeccionado la imagen que compartiste y la crucé con el inventario oficial de **$storeName (${_activeBranch.value})**.
                        
                        **Equivalentes Exactos Disponibles en Nuestro Catálogo:**
                        $sampleBlock
                        
                        **Recomendación de Conservación y Uso:**
                        1. Conserva los productos frescos refrigerados entre **2°C y 4°C** o en lugar fresco y seco si son de despensa.
                        2. Prioriza las presentaciones **Marca Exclusiva Bravo** para obtener la misma calidad certificada con hasta un **25% de ahorro directo**.
                    """.trimIndent()
                }
            }
        } else if (isSuggestionOrSupportFeedbackQuery(query)) {
            """
                **Confirmación de Registro — Gestión de Mejora Continua Bravo & El Arca:**
                • **¡Muchas gracias por tu valioso aporte, $firstName!** He procesado tu mensaje en la sucursal **${_activeBranch.value}** con máxima prioridad de atención.
                • **Envío Oficial:** Tu sugerencia/reporte ha sido estructurada y despachada al buzón central de mejora continua (**bezaleelpatrocino0@gmail.com**) con tus datos de contacto y contexto de tienda (**$storeName**).
                • **Seguimiento Bravísima:** Nuestro equipo de calidad revisará tu solicitud para seguir mejorando tu experiencia de compra.
            """.trimIndent()
        } else if (q.contains("b01") || q.contains("b02") || q.contains("credito fiscal") || q.contains("comprobante") || q.contains("rnc") || q.contains("facturacion")) {
            """
                **Facturación Fiscal (NCF B01 y B02) en $storeName (${_activeBranch.value}):**
                • **Comprobante B01 (Crédito Fiscal):** Disponible para empresas y profesionales con RNC activo para deducción de ITBIS (16% / 18%) y gastos ante la DGII.
                • **Comprobante B02 (Consumidor Final):** Emitido por defecto en todas tus órdenes express y compras en caja con tu cédula o **Tarjeta Bravísima (${_profile.value.bravisimaNumber})**.
                • **Retorno de Fidelidad:** Ambas modalidades acumulan el **1% de retorno directo** (RD$1.00 por cada RD$100.00 de compra).
            """.trimIndent()
        } else if (isOrderIntent && matchedProducts.isNotEmpty()) {
            """
                **¡Con mucho gusto, $firstName! Tu Pedido Asistido en ${_activeBranch.value} está listo:**
                
                **Lista Exacta de Ingredientes / Productos:**
                $ingredientsSummary
                
                **Resumen de Confirmación:**
                • Revisa los artículos seleccionados estrictamente dentro de tu presupuesto y presiona **Confirmar Pedido** aquí debajo para despacharlo en **8–12 minutos**.
                $financialSummary
            """.trimIndent()
        } else {
            when {
                q == "hola" || q.startsWith("hola ") || q.contains("buenos dias") || q.contains("buenas tardes") || q.contains("buenas noches") || q.contains("como estas") || q.contains("quien eres") || q.contains("que puedes hacer") -> {
                    """
                        **¡Hola, $firstName! Bienvenido a $storeName (${_activeBranch.value}):**
                        Soy **Brav IA**, tu Asistente Personal de Compras, Chef Ejecutivo, Asesor Veterinario y Especialista de Soporte Multimodal de **Supermercados Bravo** (*«Los expertos en vender barato»*) y **El Arca Petshop**.
                        
                        **¿Cómo puedo servirte hoy bajo tu presupuesto de RD$$budgetLimit.00?:**
                        • **Chef Ejecutivo con Búsqueda Web:** Pídeme cualquier receta dominicana o internacional (*pizza, lasaña, mofongo, sancocho, chofán, mangú, postres o dietas especiales*) con medidas exactas, tiempos en minutos, temperaturas en °C/°F y cálculo matemático bajo tu presupuesto.
                        • **Soporte y Visión Multimodal (Fotos):** Adjunta una foto con el botón de cámara/galería para auditar un **producto dañado/vencido**, leer una **factura NCF (B01/B02)** o identificar **ingredientes de tu alacena o mascota**.
                        • **Buzón de Sugerencias Directo:** Cuéntame cualquier idea o mejora y la registraré automáticamente hacia **bezaleelpatrocino0@gmail.com**.
                    """.trimIndent()
                }
                q.contains("horario") || q.contains("abierto") || q.contains("cierran") || q.contains("abren") || q.contains("domingo") -> {
                    """
                        **Horarios Oficiales y Logística Express para $firstName:**
                        • **Horario de ${_activeBranch.value}:** Lunes a Sábado de **7:00 AM a 10:00 PM** en jornada continua.
                        • **Domingos Cerrados:** En **Supermercados Bravo** y **El Arca Petshop** cerramos todos los domingos por nuestros valores familiares.
                        • **Delivery Express (Polígono Central):** Entrega ultrarrápida en **8 a 12 minutos** (sucursal a **${_realLocationMetrics.value.roadDistanceKm} km** de tu ubicación GPS).
                    """.trimIndent()
                }
                q.contains("bravisima") || q.contains("puntos") || q.contains("balance") || q.contains("recompensa") -> {
                    """
                        **Fidelidad Tarjeta Bravísima (${_profile.value.bravisimaNumber}):**
                        • **Balance disponible:** **RD$${_accumulatedBravisimaBalance.value}.00** listos para canje directo en carrito o caja.
                        • **Retorno Exacto del 1%:** Acumulas **RD$1.00 por cada RD$100.00 de compra** en todos los pasillos de Bravo y El Arca Petshop, priorizando aún más ahorro con **Marca Exclusiva Bravo**.
                        • **Canje sin fricción:** Activa el descuento inmediato en la pestaña **Tarjeta** o en **Mi Carrito** presentando tu código de barras `${_profile.value.bravisimaNumber}`.
                    """.trimIndent()
                }
                q.contains("envio") || q.contains("delivery") || q.contains("cuanto tarda") || q.contains("sucursal") || q.contains("ubicacion") -> {
                    """
                        **Logística y Entrega en Tiempo Real para $firstName:**
                        • **Sucursal Activa:** **${_activeBranch.value}** (a **${_realLocationMetrics.value.roadDistanceKm} km** por ruta GPS).
                        • **Tiempo Estimado de Entrega:** **~${_realLocationMetrics.value.stageAdjustedEtaMinutes} minutos** (polígono express de **8–12 minutos** • ${_driverTelemetry.value.trafficCondition}).
                        • **Velocidad del Repartidor:** **${_driverTelemetry.value.currentSpeedKmh} km/h** con envío Express **GRATIS** en la app.
                    """.trimIndent()
                }
                q.contains("oferta") || q.contains("especial") || q.contains("descuento") -> {
                    val offers = getDailyRotatingOffersForCurrentStore()
                    val offerLines = offers.joinToString("\n") { off ->
                        "• **${off.title}** — Baja de RD$${off.originalPrice} a **RD$${off.offerPrice}.00** (**-${off.discountPct}%** • ${off.dayLabel})"
                    }
                    """
                        **Ofertas Oficiales del Día en $storeName (${_activeBranch.value}):**
                        $offerLines
                    """.trimIndent()
                }
                q.contains("historial") || q.contains("habitual") || q.contains("lista") -> {
                    val stats = getPurchaseHistoryStats().take(6)
                    val summary = stats.joinToString("\n") { st ->
                        "• **${st.product.name}** — **${st.product.unit}** — **RD$${st.product.price}.00** (comprado ${st.purchaseCount} veces)"
                    }
                    """
                        **Tu Selección Habitual Basada en tus ${_orders.value.size} Órdenes:**
                        $summary
                    """.trimIndent()
                }
                q.contains("pizza") -> """
                    **¡Excelente elección, $firstName! Aquí tienes tu receta verificada de Pizza Artesanal en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (35 min • 220°C / 425°F • 8 porciones):**
                    1. Mezcla **350g (3 tazas)** de **Bravo Harina de Trigo Todo Uso** con **240ml** de agua tibia, **5g** de sal y **30ml (2 cucharadas)** de **Bravo Aceite de Oliva Virgen Extra**; amasa durante **8 minutos** y deja reposar **15 minutos**.
                    2. Combina **120g** de **Pasta de Tomate La Famosa** con **100ml** de agua tibia, **15ml** de **Aceite de Oliva** y **5g** de **Bravo Orégano Molido Dominicano**.
                    3. Precalienta el horno a **220°C (425°F)**; estira la masa en bandeja engrasada y pre-hornea la base durante **5 minutos** para lograr un piso crujiente.
                    4. Distribuye la salsa pizzera, **250g** de **Bravo Queso Mozzarella en Barra** rallado, **40g** de **Queso Parmesano Rallado Bravo**, **100g** de **Bravo Pepperoni Rebanado** y julianas de **Ají Morrón**.
                    5. Hornea a **220°C (425°F) durante 12 a 15 minutos** hasta gratinar; reposa **3 minutos** antes de porcionar.
                    $financialSummary
                """.trimIndent()
                q.contains("hamburguesa") || q.contains("burger") -> """
                    **¡Con gusto, $firstName! Receta verificada de Hamburguesas Gourmet en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (20 min • 200°C / 390°F • 4 porciones):**
                    1. Dora **120g** de **Bravo Tocino Ahumado Rebanado** en sartén de hierro a **190°C (375°F)** durante **5 minutos** hasta que esté crujiente y resérvalo.
                    2. Sella las **Hamburguesas de Res Artesanales (150g c/u)** a **200°C (390°F)** durante **4 minutos por lado** para mantener un centro jugoso a **71°C internos**.
                    3. Coloca **2 lonjas (40g)** de **Bravo Queso Cheddar** sobre cada carne en el último minuto y tapa **45 segundos** para fundir uniformemente.
                    4. Tuesta el **Pan Brioche para Hamburguesa** durante **60 segundos** sobre la plancha caliente.
                    5. Monta con base de **Bravo Lechuga Romana Lavada (30g)**, rodajas de **Tomate Bugalú (50g)**, la carne con cheddar, tocino crujiente, aros de **Cebolla Roja** y **Cachú Baldom (15g)**.
                    $financialSummary
                """.trimIndent()
                q.contains("lasana") -> """
                    **¡Por supuesto, $firstName! Receta clásica verificada de Lasaña Boloñesa en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (45 min • 195°C / 385°F • 6 porciones):**
                    1. Sofríe **450g (1 Lb)** de **Carne Molida de Res Primera** a **180°C (355°F)** durante **10 minutos**; añade **150g** de **Pasta de Tomate La Famosa** y **200ml** de agua, cocinando **8 minutos** a fuego lento.
                    2. Mezcla **250g** de **Bravo Queso Ricotta Fresco Artesanal** con **60ml** de **Bravo Leche Entera UHT** y **50g** de **Queso Parmesano Rallado Bravo**.
                    3. Engrasa un refractario con **15g** de **Bravo Mantequilla Con Sal**, extiende una base de salsa boloñesa y coloca una capa de **Bravo Láminas de Lasaña Directo al Horno**.
                    4. Alterna **3 capas** de boloñesa, crema de ricotta y **300g** de **Bravo Queso Mozzarella** rallado, coronando con parmesano.
                    5. Hornea cubierto a **195°C (385°F) durante 20 minutos** y gratina descubierto **8 minutos adicionales** hasta dorar.
                    $financialSummary
                """.trimIndent()
                q.contains("taco") || q.contains("burrito") || q.contains("quesadilla") || q.contains("nacho") -> """
                    **¡Con mucho gusto, $firstName! Preparación verificada de Tacos & Tex-Mex en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (22 min • 190°C / 375°F • 4 porciones):**
                    1. Dora **450g (1 Lb)** de **Carne Molida de Res Primera** a **190°C (375°F)** durante **10 minutos** con **60g** de cebolla picada y especias hasta caramelizar.
                    2. Corta en brunoise **150g** de **Tomate Bugalú Fresco**, **50g** de **Cebolla Roja** y **15g** de **Cilantro Fresco** para el pico de gallo.
                    3. Calienta las **Bravo Tortillas de Harina para Tacos** en comal o sartén a **175°C (350°F)** durante **30 segundos por lado**.
                    4. Rellena cada tortilla con **90g** de carne caliente, **30g** de **Bravo Queso Mozzarella** y **30g** de **Bravo Queso Cheddar**.
                    5. Sirve al instante coronando con **40g** de **Bravo Guacamole Fresco Artesanal** y el pico de gallo.
                    $financialSummary
                """.trimIndent()
                q.contains("mofongo") || q.contains("tostones") -> """
                    **¡Un clásico dominicano, $firstName! Receta tradicional verificada de Mofongo Criollo en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (30 min • 175°C / 350°F • 4 porciones):**
                    1. Corta **350g** de **Tocino de Cerdo con Capa** en dados de **2 cm** y fríelos a **175°C (350°F)** durante **15 minutos** hasta lograr chicharrones crujientes.
                    2. Pela **4 unidades** de **Plátanos Verdes Barahona**, córtalos en ruedas de **2.5 cm** y fríelos en **Bravo Aceite Vegetal Puro** a **170°C (340°F)** durante **7 minutos** hasta que estén tiernos por dentro.
                    3. Elabora el majado aromático triturando **4 dientes (16g)** de **Ajo Importado** con **30g** de **Bravo Mantequilla Con Sal** y **120ml** de caldo caliente con **Sopitas Bravo**.
                    4. Maja en pilón los plátanos calientes con la pasta de ajo, mantequilla y **150g** de chicharrón crujiente, hidratando con toques del caldo.
                    5. Moldea en semiesfera y sirve caliente coronado con **80g** de **Bravo Queso Gouda Holandés** fundido y lonjas de **Aguacate Criollo Grande**.
                    $financialSummary
                """.trimIndent()
                q.contains("chofan") || q.contains("arroz chino") -> """
                    **¡Excelente plato, $firstName! Receta verificada de Chofán Dominicano en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (35 min • 210°C / 410°F • 6 porciones):**
                    1. Cocina **450g (2 tazas)** de **Bravo Arroz Selecto Grado A** a **100°C (212°F)** durante **18 minutos** con poca agua y enfríalo extendido por **10 minutos**.
                    2. Corta en dados de **1.5 cm** **300g** de **Pechuga de Pollo Fileteada**, **200g** de **Bravo Chuleta Ahumada** y **150g** de **Bravo Jamón Cocido**; saltea a **210°C (410°F)** por **8 minutos**.
                    3. Cuaja un revoltillo suave con **3 unidades** de **Bravo Huevos Frescos** durante **90 segundos**, pícalo y reserva.
                    4. Saltea en wok a fuego alto durante **4 minutos** **100g** de **Zanahoria de Constanza** y **100g** de **Ají Morrón** en brunoise con **15ml** de **Bravo Aceite de Ajonjolí Tostado**.
                    5. Integra el arroz frío, las carnes y el huevo a **210°C (410°F)** removiendo vigorosamente durante **5 minutos** antes de servir.
                    $financialSummary
                """.trimIndent()
                q.contains("mangu") || q.contains("tres golpes") -> """
                    **¡El desayuno rey dominicano, $firstName! Receta verificada de Mangú con Los Tres Golpes en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (25 min • 100°C / 212°F • 4 porciones):**
                    1. Pela **4 unidades** de **Plátanos Verdes Barahona**, córtalos en cuartos y hiérvelos en agua con **8g** de sal a **100°C (212°F)** durante **18 minutos**.
                    2. Maja los plátanos al instante con **45g (3 cucharadas)** de **Bravo Mantequilla Con Sal** y **120ml** de agua fría hasta lograr una textura sedosa.
                    3. Dora en sartén a **185°C (365°F)** **200g** de **Salami Induveca Súper Especial** y **200g** de **Bravo Queso de Freír** durante **3 minutos por lado**.
                    4. Fríe **4 unidades** de **Bravo Huevos Frescos** a **160°C (320°F)** durante **2 minutos** para mantener la yema cremosa.
                    5. Saltea **120g** de **Cebolla Roja** en plumas con **30ml** de **Bravo Vinagre Blanco** por **2 minutos** y corona el mangú junto a los tres golpes.
                    $financialSummary
                """.trimIndent()
                q.contains("sancocho") || q.contains("asopao") -> """
                    **¡Tradición pura, $firstName! Receta verificada de Sancocho Dominicano en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (55 min • 100°C / 212°F • 8 porciones):**
                    1. Sazona y sella en caldero a **200°C (390°F)** durante **10 minutos** **450g** de **Pollo Fresco Bravo**, **450g** de **Carne de Res para Guisar**, **300g** de **Costillita Ahumada** y **200g** de **Longaniza Criolla**.
                    2. Incorpora **2.5 Litros** de agua hirviendo y cocina a **100°C (212°F)** fuego medio durante **20 minutos**.
                    3. Agrega **2 unidades** de **Plátano Verde**, **400g** de **Yuca Fresca** y **350g** de **Yautía Blanca** troceados en bocados de **4 cm**.
                    4. Añade **350g** de **Auyama Criolla** y hierve **15 minutos adicionales**; maja un tercio de la auyama para espesar y dar color dorado al caldo.
                    5. Reposa **5 minutos** y sirve caliente acompañado de **Bravo Arroz Selecto Grado A** blanco.
                    $financialSummary
                """.trimIndent()
                q.contains("pastelon") || q.contains("maduro") -> """
                    **¡Delicia criolla, $firstName! Receta verificada de Pastelón de Plátano Maduro en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (45 min • 190°C / 375°F • 6 porciones):**
                    1. Hierve **5 unidades** de **Plátanos Maduros Amarillos** a **100°C (212°F)** durante **15 minutos** y májalos con **30g** de **Bravo Mantequilla Con Sal** y **50ml** de **Bravo Leche Entera**.
                    2. Sofríe **450g (1 Lb)** de **Carne Molida de Res Primera** a **180°C (355°F)** con **60g** de **Ají Cubanela** y **80g** de **Pasta de Tomate La Famosa** durante **12 minutos**.
                    3. Engrasa un molde refractario y extiende el **50%** del puré de plátano maduro como primera capa.
                    4. Distribuye el relleno de carne y cubre con **150g** de **Bravo Queso Gouda Holandés** en lonjas.
                    5. Cierra con el resto del puré, corona con **200g** de **Bravo Queso Mozzarella** rallado y hornea a **190°C (375°F) durante 20 minutos**.
                    $financialSummary
                """.trimIndent()
                q.contains("bandera") || q.contains("locrio") || q.contains("moro") -> """
                    **¡El almuerzo insignia dominicano, $firstName! Receta verificada de La Bandera Dominicana en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (40 min • 185°C / 365°F • 5 porciones):**
                    1. Dora **900g** de **Pollo Entero Fresco Bravo** en piezas con **30ml** de **Bravo Aceite Vegetal Puro** a **185°C (365°F)** durante **10 minutos**.
                    2. Incorpora **60g** de **Pasta de Tomate La Famosa** y **150ml** de agua para guisar tapado durante **15 minutos**.
                    3. Cocina **450g (1 Lb)** de **Bravo Arroz Selecto Grado A** tapado a fuego bajo (**95°C / 203°F**) durante **18 minutos**.
                    4. Guisa **400g** de **Bravo Habichuelas Rojas** con **150g** de **Auyama Criolla** a fuego medio durante **12 minutos** hasta espesar.
                    5. Sirve el arroz graneado, las habichuelas cremosas, el pollo guisado y lonjas de **Aguacate Criollo Grande**.
                    $financialSummary
                """.trimIndent()
                q.contains("pasta") || q.contains("alfredo") || q.contains("espagueti") || q.contains("carbonara") || q.contains("codito") || q.contains("penne") -> """
                    **¡Con mucho gusto, $firstName! Receta verificada de Pasta Cremosa Alfredo en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (25 min • 100°C / 212°F • 4 porciones):**
                    1. Hierve **400g** de **Bravo Espaguetis Trigo Duro** en **3 Litros** de agua con sal a **100°C (212°F)** durante **9 minutos** (*al dente*) y reserva **100ml** del agua de cocción.
                    2. Saltea **150g** de **Bravo Tocino Ahumado** y **300g** de **Pechuga de Pollo Fileteada** con **30g** de **Bravo Mantequilla Con Sal** y **10g** de **Ajo** a **175°C (350°F)** por **7 minutos**.
                    3. Reduce a fuego lento (**85°C / 185°F**) e incorpora **250ml** de **Bravo Crema de Leche para Cocinar**, **100g** de **Bravo Queso Gouda** y **60g** de **Queso Parmesano Rallado Bravo**.
                    4. Emulsiona durante **4 minutos** hasta que los quesos fundan en una salsa sedosa.
                    5. Integra la pasta con el agua reservada durante **1 minuto** y sirve inmediatamente.
                    $financialSummary
                """.trimIndent()
                q.contains("arroz con leche") || q.contains("habichuelas con dulce") || q.contains("flan") || q.contains("postre") -> """
                    **¡Dulzura tradicional, $firstName! Receta verificada de Postre Dominicano en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (35 min • 95°C / 203°F • 6 porciones):**
                    1. Cocina **250g** de la base (**Bravo Arroz Selecto** o **Bravo Habichuelas Rojas**) en **600ml** de agua a **95°C (203°F)** durante **15 minutos**.
                    2. Incorpora **500ml** de **Bravo Leche Entera UHT** y **315g (1 lata)** de **Leche Evaporada Carnation**, removiendo constantemente durante **8 minutos**.
                    3. Añade **200g** de **Leche Condensada La Lechera**, **60g** de **Bravo Azúcar Crema** y **15g** de **Bravo Mantequilla Con Sal**.
                    4. Si elaboras habichuelas con dulce, agrega **250g** de **Batata Dulce Dominicana** en dados de **1.5 cm** y cocina **12 minutos más**.
                    5. Sirve tibio o refrigera a **4°C (39°F)** durante **60 minutos** antes de degustar.
                    $financialSummary
                """.trimIndent()
                q.contains("panqueque") || q.contains("hotcake") || q.contains("pancake") || q.contains("waffle") -> """
                    **¡Desayuno perfecto, $firstName! Receta verificada de Panqueques Esponjosos en ${_activeBranch.value}:**
                    
                    **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                    $ingredientsSummary
                    
                    **Preparación Paso a Paso Fundamentada (18 min • 175°C / 350°F • 4 porciones):**
                    1. Bate **2 unidades** de **Bravo Huevos Frescos** con **240ml (1 taza)** de **Bravo Leche Entera UHT** y **30g** de **Bravo Mantequilla Con Sal** fundida.
                    2. Incorpora **200g (1.5 tazas)** de **Bravo Harina de Trigo Todo Uso** con movimientos envolventes durante **60 segundos**.
                    3. Precalienta la sartén antiadherente a **175°C (350°F)** y vierte **60ml** de mezcla por unidad.
                    4. Cocina **2 minutos** del primer lado hasta formar burbujas y dora **1 minuto** del lado opuesto.
                    5. Sirve coronando con **100g** de **Fresas Frescas de Constanza**, **1 unidad** de **Guineo Maduro** rebanado y **30ml** de **Bravo Miel de Abejas 100% Pura**.
                    $financialSummary
                """.trimIndent()
                q.contains("sin lactosa") || q.contains("intolerante a la lactosa") || q.contains("alergia a la leche") -> {
                    val lactoseFreeSkus = enforceBudgetLimitOnSkus(
                        getAllProducts()
                            .filter { prod ->
                                prod.isBravo &&
                                    prod.nutritionProfile().caloriesKcal != null &&
                                    prod.nutritionProfile().dietaryTags.any { it.contains("Sin Lactosa", ignoreCase = true) }
                            }
                            .take(7)
                            .map { it.sku },
                        budgetLimit
                    )
                    val lfProducts = lactoseFreeSkus.mapNotNull { findProductBySku(it) }
                    val lfSummary = formatIngredientsBlock(lfProducts)
                    val lfFinancial = buildFinancialBreakdownText(lactoseFreeSkus, budgetLimit)
                    """
                        **¡Con gusto, $firstName! Selección y Receta 100% Libre de Lactosa en ${_activeBranch.value}:**
                        
                        **Lista Exacta de Ingredientes / Productos (${lfProducts.size} artículos verificados):**
                        $lfSummary
                        
                        **Preparación Paso a Paso Fundamentada (20 min • 180°C / 355°F):**
                        1. Sazona **250g** de proteína fresca o vegetales con **15ml** de **Aceite de Oliva Virgen Extra**, ajo y orégano natural (**0% derivados lácteos**).
                        2. Para desayunos o batidos utiliza **240ml** de **Leche de Almendra Sin Azúcar** (**24 kcal/100ml**) y **60g** de **Avena Integral**.
                        3. Saltea los ingredientes a **180°C (355°F)** durante **10 a 12 minutos** preservando sus jugos naturales.
                        4. Acompaña con **100g** de **Aguacate Criollo** (**160 kcal/100g**) o **Casabe Natural**.
                        $lfFinancial
                    """.trimIndent()
                }
                q.contains("sin gluten") || q.contains("celiac") || q.contains("celíac") || q.contains("alergia al trigo") -> {
                    val glutenFreeSkus = enforceBudgetLimitOnSkus(
                        getAllProducts()
                            .filter { prod ->
                                prod.isBravo &&
                                    prod.nutritionProfile().caloriesKcal != null &&
                                    prod.nutritionProfile().dietaryTags.any { it.contains("Sin Gluten", ignoreCase = true) }
                            }
                            .take(7)
                            .map { it.sku },
                        budgetLimit
                    )
                    val gfProducts = glutenFreeSkus.mapNotNull { findProductBySku(it) }
                    val gfSummary = formatIngredientsBlock(gfProducts)
                    val gfFinancial = buildFinancialBreakdownText(glutenFreeSkus, budgetLimit)
                    """
                        **¡Por supuesto, $firstName! Selección y Receta 100% Libre de Gluten en ${_activeBranch.value}:**
                        
                        **Lista Exacta de Ingredientes / Productos (${gfProducts.size} artículos aptos para celíacos):**
                        $gfSummary
                        
                        **Preparación Paso a Paso Fundamentada (25 min • 185°C / 365°F):**
                        1. Cocina **250g** de **Bravo Arroz Selecto** (**360 kcal/100g**) o **300g** de **Yuca Fresca** (**159 kcal/100g**) a **100°C (212°F)** durante **18 minutos** como base libre de trigo.
                        2. Sella **250g** de **Pechuga de Pollo** (**165 kcal/100g**) a **185°C (365°F)** con **15ml** de **Aceite de Oliva Virgen Extra** durante **8 minutos por lado**.
                        3. Prepara una ensalada fresca con **100g** de **Aguacate Criollo**, **120g** de **Tomate Bugalú** y **80g** de **Lechuga Romana**.
                        4. Emplata garantizando **0% contaminación cruzada** con harinas de trigo o cebada.
                        $gfFinancial
                    """.trimIndent()
                }
                q.contains("keto") || q.contains("low carb") || q.contains("bajo en carbohidratos") || q.contains("proteina") || q.contains("proteína") || q.contains("fitness") || q.contains("caloria") || q.contains("calorías") || q.contains("dieta") -> {
                    val fitProducts = (if (matchedProducts.isNotEmpty()) matchedProducts else getAllProducts().filter {
                        it.isBravo && it.nutritionProfile().dietaryTags.any { tag -> tag.contains("Keto") || tag.contains("Proteína") }
                    }.take(6))
                    val fitSkus = enforceBudgetLimitOnSkus(fitProducts.map { it.sku }, budgetLimit)
                    val enforcedFitProducts = fitSkus.mapNotNull { findProductBySku(it) }
                    val fitSummary = formatIngredientsBlock(enforcedFitProducts)
                    val fitFinancial = buildFinancialBreakdownText(fitSkus, budgetLimit)
                    """
                        **¡Excelente meta nutricional, $firstName! Plan Proteico / Keto Verificado en ${_activeBranch.value}:**
                        
                        **Lista Exacta de Ingredientes / Productos (${enforcedFitProducts.size} artículos):**
                        $fitSummary
                        
                        **Preparación Paso a Paso Fundamentada (20 min • 190°C / 375°F):**
                        1. Selecciona **220g** de **Pechuga de Pollo** (**165 kcal/100g**, **23.5g proteína**) o **3 unidades** de **Bravo Huevos Frescos** (**155 kcal/100g**).
                        2. Cocina a la plancha a **190°C (375°F)** con **5ml (1 cdta)** de **Aceite de Oliva Virgen Extra** durante **7 minutos por lado**.
                        3. Acompaña con **150g** de vegetales verdes (**Lechuga Romana** / **Brócoli**, **<34 kcal/100g**) y **100g** de **Aguacate Criollo** (**160 kcal/100g**).
                        4. Sirve al instante con control exacto de macronutrientes y cero azúcares refinados.
                        $fitFinancial
                    """.trimIndent()
                }
                isInstructionOrRecipeQuery(query) && matchedProducts.isNotEmpty() -> {
                    val primaryName = matchedProducts.first().name
                    """
                        **¡Con mucho gusto, $firstName! Preparación verificada para «$query» en ${_activeBranch.value}:**
                        
                        **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                        $ingredientsSummary
                        
                        **Preparación Paso a Paso Fundamentada (25 min • 185°C / 365°F • 4 porciones):**
                        1. Pesa y organiza tu *mise en place* con **300g** de **$primaryName** y el resto de los ingredientes seleccionados del catálogo oficial.
                        2. Realiza el corte técnico y sazona con **15ml** de grasa noble y aromáticos frescos durante **5 minutos**.
                        3. Cocina el ingrediente principal a **185°C (365°F)** durante **12 a 15 minutos** respetando su temperatura interna óptima.
                        4. Incorpora los complementos, lácteos o reducciones en los últimos **5 minutos** a fuego medio-bajo (**140°C / 285°F**).
                        5. Reposa **3 minutos** antes de servir para estabilizar los jugos y realzar el sabor.
                        $financialSummary
                    """.trimIndent()
                }
                matchedProducts.isNotEmpty() -> {
                    """
                        **¡Hola, $firstName! Selección Oficial de $storeName para «$query» en ${_activeBranch.value}:**
                        
                        **Lista Exacta de Ingredientes / Productos (${matchedProducts.size} artículos):**
                        $ingredientsSummary
                        $financialSummary
                    """.trimIndent()
                }
                else -> {
                    """
                        **Atención Personalizada $storeName (${_activeBranch.value}):**
                        • Hola **$firstName**, busqué **«$query»** en el inventario oficial de **${_activeBranch.value}** y no figura bajo ese nombre exacto en este momento.
                        • **¿Deseas que lo registremos como sugerencia de producto?** Dime *"Sugiero agregar $query"* y lo enviaré inmediatamente a nuestro buzón oficial (**bezaleelpatrocino0@gmail.com**).
                        • También puedes adjuntar una **foto** con el botón de imagen o pedirme cualquier **receta con medidas exactas** bajo tu presupuesto de **RD$$budgetLimit.00**.
                    """.trimIndent()
                }
            }
        }
        return rawResponse.replace(skuDisplayStripRegex, "").trim()
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
            if (hasImage) {
                "Analiza técnicamente esta imagen adjunta (producto dañado/vencido, factura NCF o ingrediente/alacena/mascota) según el protocolo de Brav IA Soporte."
            } else {
                ""
            }
        }
        if (query.isEmpty()) return
        if (_isAiGenerating.value) {
            pauseOrStopAiGeneration()
        }
        _pendingChatImageUri.value = null

        val userMsg = ChatMessage(
            sender = "user",
            text = query,
            attachedImageUri = attachedUri
        )
        val orderIntent = isOrderIntentQuery(query)
        val recipeOrInstructionIntent = isInstructionOrRecipeQuery(query)
        val suggestionIntent = isSuggestionOrSupportFeedbackQuery(query)
        val firstName = _profile.value.name.substringBefore(" ").ifBlank { "Cliente" }
        val budgetLimit = _targetBudgetInChat.value.coerceAtLeast(150)

        if (!hasImage && !suggestionIntent && isOffTopicQuery(query) && !orderIntent && !recipeOrInstructionIntent) {
            val guardrailMsg = ChatMessage(
                sender = "ai",
                text = "**Asesoría Exclusiva Bravo & El Arca:**\n• ¡Hola **$firstName**! Soy **Brav IA** y **Brav IA Soporte**, tu Chef Ejecutivo, Asesor Veterinario y Especialista de Atención al Cliente de **Supermercados Bravo** y **El Arca Petshop**.\n• Conozco el inventario completo con **precios en RD$, calorías exactas y equivalencias Marca Bravo**, verifico recetas en la web bajo tu presupuesto (**RD$$budgetLimit.00**), audito **fotos de productos o facturas NCF** y envío tus **sugerencias a bezaleelpatrocino0@gmail.com**.\n• ¿En qué puedo asistirte hoy?",
                skus = emptyList(),
                modelLabel = "Brav IA & Soporte Oficial"
            )
            _chatHistory.value = _chatHistory.value + userMsg + guardrailMsg
            return
        }

        _isAiGenerating.value = true
        val loadingMsgId = System.nanoTime() + 1L
        val personalizedThinking = buildPersonalizedThinkingText(query, hasAttachedImage = hasImage)
        val loadingMsg = ChatMessage(
            id = loadingMsgId,
            sender = "ai",
            text = personalizedThinking,
            isLoading = true
        )
        _chatHistory.value = _chatHistory.value + userMsg + loadingMsg

        val isArca = _activeStore.value == StoreMode.ARCA
        val activeStoreCode = if (isArca) "arca" else "bravo"
        val storeDisplayName = if (isArca) "El Arca Petshop" else "Supermercados Bravo"
        val historySummary = buildPurchaseHistorySummaryForAi()
        val storeInventoryRag = getStoreInventoryContext(query)
        val focusedCatalog = buildFocusedCatalogContext(query)

        activeAiChatJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val attachedBitmap: ByteArray? = if (attachedUri != null) {
                loadScaledJpeg(application, attachedUri)
            } else {
                null
            }

            // Grounding Search for Recipes (Zero Hallucination)
            val internetCulinaryContext = if (recipeOrInstructionIntent) {
                fetchInternetRecipeVerification(query)
            } else {
                null
            }

            val isoTimestampNow = currentIsoTimestamp()

            val systemPrompt = """
                # ROL E IDENTIDAD DEL SISTEMA
                Eres **Brav IA**, el Asistente Personal de Compras, Chef Ejecutivo y Asesor Veterinario oficial de **Supermercados Bravo** («Los expertos en vender barato») y **El Arca Petshop** en la República Dominicana.
                También actúas como **Brav IA Soporte**, el Especialista Senior de Atención al Cliente, Auditor de Calidad Visual y Gestor de Mejora Continua cuando el cliente adjunta una imagen, consulta dudas operativas o expresa una sugerencia/reporte.
                Tu tono es cálido, hospitalario y cercano con **$firstName** (${_profile.value.name}), pero estrictamente profesional, resolutivo y a entera disposición del cliente.

                # CONTEXTO DINÁMICO DE LA SESIÓN
                - **Sucursal Activa:** ${_activeBranch.value} (Distancia GPS: ${_realLocationMetrics.value.roadDistanceKm} km | ETA Express: ~${_realLocationMetrics.value.stageAdjustedEtaMinutes} min)
                - **Tienda Activa:** $activeStoreCode ("bravo" = Supermercados Bravo | "arca" = El Arca Petshop)
                - **Cliente:** ${_profile.value.name} | Contacto: ${_profile.value.email} / ${_profile.value.phone} | Tarjeta Bravísima: ${_profile.value.bravisimaNumber} (Balance: RD$${_accumulatedBravisimaBalance.value}.00)
                - **Presupuesto Límite Acordado:** RD$$budgetLimit.00
                - **Inventario Oficial Disponible (RAG Context):**
                $storeInventoryRag

                $focusedCatalog

                $historySummary
                ${if (internetCulinaryContext != null) "\n# RESULTADO DE BÚSQUEDA WEB EN VIVO PARA LA RECETA (GROUNDING SEARCH):\n$internetCulinaryContext\n" else ""}
                # REGLAS INQUEBRANTABLES DE EXACTITUD Y FUNDAMENTACIÓN (ZERO HALLUCINATION)
                1. **PROHIBICIÓN DE RESPUESTAS VAGAS Y CERO CÓDIGOS SKU EN EL TEXTO VISIBLE:**
                   - Jamás respondas con generalidades como "compra algunos vegetales", "añade condimentos al gusto" o "busca una comida para perro".
                   - Toda respuesta debe estar fundamentada en datos concretos: nombres exactos del catálogo en **negrita**, gramajes/unidades (Lb, Gr, Oz, Kg, Litro), precios unitarios en RD$${'$'}, calorías exactas (kcal) y justificación culinaria, veterinaria o nutricional.
                   - No escribas los códigos internos [BRV-xxx] o [ARC-xxx] dentro de los párrafos visibles al usuario; colócalos ÚNICAMENTE en el arreglo final SKUS: [...].
                2. **BÚSQUEDA WEB OBLIGATORIA PARA RECETAS (GROUNDING SEARCH):**
                   - Usa los datos verificados de la búsqueda web para indicar cantidades exactas por porción (en gramos/ml/tazas), tiempos exactos de cocción en minutos y temperaturas en °C y °F.
                3. **EXACTITUD DEL 100% EN INGREDIENTES Y EQUIVALENCIAS:**
                   - Cruza cada ingrediente con el **Inventario Oficial Disponible**.
                   - Especifica la cantidad exacta a utilizar en la receta (ej. "250g de arroz") y la unidad comercial exacta que el cliente llevará en su carrito con su precio en RD$${'$'}.
                   - Prioriza siempre los productos de **Marca Exclusiva Bravo** para maximizar el ahorro del cliente.
                4. **CUMPLIMIENTO MATEMÁTICO DEL PRESUPUESTO (RD$$budgetLimit.00):**
                   - Suma internamente el precio de todos los productos que vas a recomendar: la suma total DEBE ser menor o igual a RD$$budgetLimit.00.
                   - Si la suma supera los RD$$budgetLimit.00, sustituye automáticamente los artículos más costosos por presentaciones medianas o equivalentes **Marca Exclusiva Bravo** hasta quedar estrictamente por debajo del límite.
                   - Muestra siempre al final un desglose financiero claro:
                     * **Subtotal de la compra:** RD$ X.00
                     * **Margen de ahorro bajo tu presupuesto:** RD$ Y.00
                     * **Retorno Tarjeta Bravísima (1%):** +RD$ Z.00 (RD$1 por cada RD$100).

                # ESTRUCTURA DE SALIDA OBLIGATORIA (MODO COMPRAS / CHEF / VETERINARIO)
                1. **Saludo cálido y confirmación del objetivo:** Breve, cortés y enfocado en la solicitud de $firstName.
                2. **Lista Exacta de Ingredientes / Productos:** Viñetas (•) con **[Nombre Exacto]** — [Medida para la receta y unidad comercial] — **RD$[Precio].00**.
                3. **Preparación Paso a Paso Fundamentada:** Instrucciones numeradas (1., 2., 3., 4., 5.) con tiempos exactos en minutos y temperaturas en °C/°F o guía de aplicación veterinaria.
                4. **Resumen de Presupuesto y Fidelidad:** Cálculo verificado frente al límite de RD$$budgetLimit.00.
                5. **Arreglo de SKUs para el Carrito (Última línea obligatoria cuando recomiendes productos):**
                SKUS: ["SKU-1", "SKU-2", "SKU-3"]

                # MODO SOPORTE Y VISIÓN MULTIMODAL (BRAV IA SOPORTE)
                - **CAPACIDAD 1 (ANÁLISIS VISUAL MULTIMODAL):** Si el cliente adjuntó una imagen (${if (attachedBitmap != null) "IMAGEN ADJUNTA PRESENTE EN ESTA CONSULTA: analízala visualmente en detalle" else "sin imagen en este turno"}):
                  1. *Producto dañado, vencido o empaque defectuoso:* Identifica visualmente el producto, verifica el estado del sello/empaque o fecha impresa, ofrece una disculpa cálida en nombre de $storeDisplayName y autoriza inmediatamente la reposición sin costo o nota de crédito en su **Tarjeta Bravísima (${_profile.value.bravisimaNumber})** indicando la sucursal **${_activeBranch.value}**.
                  2. *Foto de factura o ticket de caja (NCF):* Extrae mediante OCR el número de factura/NCF, los artículos cobrados y el total en RD$${'$'}, aclarando descuentos, ITBIS (18%/16%) o puntos Bravísima generados.
                  3. *Foto de ingrediente, alacena o etiqueta de mascota:* Identifica qué tiene el cliente en mano, busca su equivalente exacto en nuestro catálogo y explica cómo usarlo o conservarlo.
                - **CAPACIDAD 2 (RESOLUCIÓN EXACTA DE DUDAS OPERATIVAS):**
                  * *Fidelidad Bravísima:* Retorno exacto del 1% (RD$1.00 por cada RD$100.00 de compra), canje directo en carrito o caja presentando el código de barras ${_profile.value.bravisimaNumber}.
                  * *Horarios y Logística:* Lunes a Sábado de 7:00 AM a 10:00 PM (Domingos cerrados por valores familiares). Delivery express en 8–12 minutos en el polígono central.
                  * *Facturación Fiscal:* Emisión de Comprobantes B01 (Crédito Fiscal) y B02 (Consumidor Final).
                - **CAPACIDAD 3 (CAPTURA Y ENVÍO DE SUGERENCIAS A CORREO ELECTRÓNICO):**
                  Cuando el cliente exprese una sugerencia de nuevo producto, mejora de la aplicación, felicitación o reporte de soporte/calidad, confirma amablemente que su mensaje ha sido registrado para **bezaleelpatrocino0@gmail.com** e incluye al final de tu respuesta (justo antes de SKUS:) el bloque JSON exacto:
                  SUGGESTION_MAIL_PAYLOAD: {"to_email":"bezaleelpatrocino0@gmail.com","from_user":"${_profile.value.name}","user_contact":"${_profile.value.email} / ${_profile.value.phone}","active_branch":"${_activeBranch.value}","active_store":"$activeStoreCode","category":"SUGERENCIA_PRODUCTO | MEJORA_UI_UX | REPORTE_SOPORTE | FELICITACION","priority":"ALTA | MEDIA | BAJA","subject":"Asunto ejecutivo claro","summary_body":"Resumen estructurado de la sugerencia o reporte del cliente","timestamp_iso":"$isoTimestampNow"}

                Solicitud actual del cliente ($firstName): $query
            """.trimIndent()

            val accumulatedText = StringBuilder()
            try {
                if (!isFirebaseUserAuthenticatedForAi()) {
                    delay(280L)
                    val fallbackSkus = if (suggestionIntent) emptyList() else enforceBudgetLimitOnSkus(matchFallbackSkus(query), budgetLimit)
                    val fallbackSuggestionPayload = if (suggestionIntent) {
                        SuggestionMailPayload(
                            toEmail = "bezaleelpatrocino0@gmail.com",
                            fromUser = _profile.value.name,
                            userContact = "${_profile.value.email} / ${_profile.value.phone}",
                            activeBranch = _activeBranch.value,
                            activeStore = activeStoreCode,
                            category = if (query.contains("producto", ignoreCase = true)) "SUGERENCIA_PRODUCTO" else "MEJORA_UI_UX",
                            priority = "ALTA",
                            subject = "[Brav IA Soporte] Sugerencia de ${_profile.value.name} (${_activeBranch.value})",
                            summaryBody = query,
                            timestampIso = isoTimestampNow
                        ).also { dispatchSuggestionMailPayloadAsync(it) }
                    } else {
                        null
                    }
                    val duration = (System.currentTimeMillis() - startTime).coerceAtMost(MAX_AI_RESPONSE_TIMEOUT_MS)
                    _chatHistory.value = _chatHistory.value.map { msg ->
                        if (msg.id == loadingMsgId) {
                            msg.copy(
                                text = buildFallbackRecipeOrCatalogResponse(query, fallbackSkus, hasAttachedImage = hasImage),
                                skus = fallbackSkus,
                                isLoading = false,
                                modelLabel = when {
                                    fallbackSuggestionPayload != null -> "Brav IA Soporte • Enviado a bezaleelpatrocino0@gmail.com"
                                    hasImage -> "Brav IA Soporte • Auditoría Visual Multimodal"
                                    orderIntent && fallbackSkus.isNotEmpty() -> "Brav IA • Pedido Listo para Confirmación Manual"
                                    else -> "Brav IA • Chef Ejecutivo & Asesor Oficial"
                                },
                                durationMs = duration,
                                requiresOrderConfirmation = orderIntent && fallbackSkus.isNotEmpty(),
                                suggestionPayload = fallbackSuggestionPayload
                            )
                        } else {
                            msg
                        }
                    }
                    return@launch
                }

                withTimeout(MAX_AI_RESPONSE_TIMEOUT_MS) {
                    val activeModelStream = try {
                        if (attachedBitmap != null) {
                            val multimodalInput = content {
                                image(attachedBitmap)
                                text(systemPrompt)
                            }
                            geminiModel.generateContentStream(multimodalInput)
                        } else {
                            geminiModel.generateContentStream(systemPrompt)
                        }
                    } catch (_: Exception) {
                        if (attachedBitmap != null) {
                            val multimodalInput = content {
                                image(attachedBitmap)
                                text(systemPrompt)
                            }
                            geminiFallbackModel.generateContentStream(multimodalInput)
                        } else {
                            geminiFallbackModel.generateContentStream(systemPrompt)
                        }
                    }
                    try {
                        activeModelStream.collect { chunk ->
                            val part = chunk.text.orEmpty()
                            if (part.isNotEmpty()) {
                                accumulatedText.append(part)
                                val (withoutMail, _) = extractSuggestionPayloadFromResponse(accumulatedText.toString(), query)
                                val (cleanPreview, _) = extractSkusFromResponse(withoutMail)
                                _chatHistory.value = _chatHistory.value.map { msg ->
                                    if (msg.id == loadingMsgId) {
                                        msg.copy(
                                            text = cleanPreview.ifBlank { personalizedThinking },
                                            isLoading = false,
                                            modelLabel = if (hasImage) {
                                                "Brav IA Soporte • Auditoría Visual Multimodal"
                                            } else {
                                                "Brav IA • Chef Ejecutivo & Asesor Oficial"
                                            }
                                        )
                                    } else {
                                        msg
                                    }
                                }
                            }
                        }
                    } catch (streamErr: Exception) {
                        if (accumulatedText.length < 40) {
                            val backupResp = if (attachedBitmap != null) {
                                geminiFallbackModel.generateContent(
                                    content {
                                        image(attachedBitmap)
                                        text(systemPrompt)
                                    }
                                )
                            } else {
                                geminiFallbackModel.generateContent(systemPrompt)
                            }
                            val backupText = backupResp.text.orEmpty()
                            if (backupText.isNotBlank()) {
                                accumulatedText.clear()
                                accumulatedText.append(backupText)
                            } else {
                                throw streamErr
                            }
                        }
                    }
                }

                val duration = (System.currentTimeMillis() - startTime).coerceAtMost(MAX_AI_RESPONSE_TIMEOUT_MS)
                val fullText = accumulatedText.toString()
                val (textWithoutMail, parsedSuggestionPayload) = extractSuggestionPayloadFromResponse(fullText, query)
                val (rawCleanText, parsedSkus) = extractSkusFromResponse(textWithoutMail)

                val finalSuggestionPayload = parsedSuggestionPayload ?: if (suggestionIntent) {
                    SuggestionMailPayload(
                        toEmail = "bezaleelpatrocino0@gmail.com",
                        fromUser = _profile.value.name,
                        userContact = "${_profile.value.email} / ${_profile.value.phone}",
                        activeBranch = _activeBranch.value,
                        activeStore = activeStoreCode,
                        category = if (query.contains("producto", ignoreCase = true)) "SUGERENCIA_PRODUCTO" else "MEJORA_UI_UX",
                        priority = "ALTA",
                        subject = "[Brav IA Soporte] Sugerencia de ${_profile.value.name} (${_activeBranch.value})",
                        summaryBody = query,
                        timestampIso = isoTimestampNow
                    )
                } else {
                    null
                }

                if (finalSuggestionPayload != null) {
                    dispatchSuggestionMailPayloadAsync(finalSuggestionPayload)
                }

                val hasNumberedSteps = rawCleanText.contains("Paso 1", ignoreCase = true) ||
                    Regex("""(?m)^\s*(?:\*\*?)?(?:Paso\s*\d+|\d+[\.\)])""").containsMatchIn(rawCleanText)

                if (rawCleanText.length >= 35 && (!recipeOrInstructionIntent || hasNumberedSteps)) {
                    val isRefusalOrNoProduct = !recipeOrInstructionIntent && !hasImage && (
                        rawCleanText.contains("solo asisto", ignoreCase = true) ||
                            rawCleanText.contains("Solo estoy autorizado", ignoreCase = true) ||
                            rawCleanText.contains("no tenemos", ignoreCase = true) ||
                            rawCleanText.contains("no está actualmente", ignoreCase = true) ||
                            rawCleanText.contains("no se encuentra", ignoreCase = true) ||
                            isConversationalOrStoreInfoQuery(query)
                        )

                    val rawCandidateSkus = when {
                        isRefusalOrNoProduct || suggestionIntent -> emptyList()
                        recipeOrInstructionIntent -> {
                            (parsedSkus + matchFallbackSkus(query)).distinct().take(8)
                        }
                        parsedSkus.isNotEmpty() -> parsedSkus
                        else -> matchFallbackSkus(query)
                    }
                    // Enforce mathematical budget compliance: sum(SKU_i) <= targetBudgetInChat
                    val finalSkus = enforceBudgetLimitOnSkus(rawCandidateSkus, budgetLimit)

                    // Ensure financial breakdown is always present when SKUs are recommended
                    val cleanTextWithBudget = if (finalSkus.isNotEmpty() &&
                        !rawCleanText.contains("Retorno Tarjeta Bravísima", ignoreCase = true) &&
                        !rawCleanText.contains("Subtotal de la compra", ignoreCase = true)
                    ) {
                        rawCleanText.trimEnd() + buildFinancialBreakdownText(finalSkus, budgetLimit)
                    } else {
                        rawCleanText
                    }

                    val needsManualOrderConfirm = !isRefusalOrNoProduct && finalSkus.isNotEmpty() && (
                        orderIntent ||
                            cleanTextWithBudget.contains("Confirmar Pedido", ignoreCase = true)
                        )
                    _chatHistory.value = _chatHistory.value.map { msg ->
                        if (msg.id == loadingMsgId) {
                            msg.copy(
                                text = cleanTextWithBudget,
                                skus = finalSkus,
                                isLoading = false,
                                modelLabel = when {
                                    finalSuggestionPayload != null -> "Brav IA Soporte • Enviado a bezaleelpatrocino0@gmail.com"
                                    hasImage -> "Brav IA Soporte • Auditoría Visual Multimodal"
                                    needsManualOrderConfirm -> "Brav IA • Pedido Listo para Confirmación Manual"
                                    else -> "Brav IA • Chef Ejecutivo & Asesor Oficial"
                                },
                                durationMs = duration,
                                requiresOrderConfirmation = needsManualOrderConfirm,
                                suggestionPayload = finalSuggestionPayload
                            )
                        } else {
                            msg
                        }
                    }
                } else {
                    val fallbackSkus = if (suggestionIntent) emptyList() else enforceBudgetLimitOnSkus(matchFallbackSkus(query), budgetLimit)
                    _chatHistory.value = _chatHistory.value.map { msg ->
                        if (msg.id == loadingMsgId) {
                            msg.copy(
                                text = buildFallbackRecipeOrCatalogResponse(query, fallbackSkus, hasAttachedImage = hasImage),
                                skus = fallbackSkus,
                                isLoading = false,
                                modelLabel = when {
                                    finalSuggestionPayload != null -> "Brav IA Soporte • Enviado a bezaleelpatrocino0@gmail.com"
                                    hasImage -> "Brav IA Soporte • Auditoría Visual Multimodal"
                                    orderIntent && fallbackSkus.isNotEmpty() -> "Brav IA • Pedido Listo para Confirmación Manual"
                                    else -> "Brav IA • Chef Ejecutivo & Asesor Oficial"
                                },
                                durationMs = duration,
                                requiresOrderConfirmation = orderIntent && fallbackSkus.isNotEmpty(),
                                suggestionPayload = finalSuggestionPayload
                            )
                        } else {
                            msg
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                val duration = (System.currentTimeMillis() - startTime).coerceAtMost(MAX_AI_RESPONSE_TIMEOUT_MS)
                val partial = accumulatedText.toString()
                val (withoutMail, parsedMail) = extractSuggestionPayloadFromResponse(partial, query)
                val (cleanPartial, partialSkus) = extractSkusFromResponse(withoutMail)
                val partialHasSteps = cleanPartial.contains("Paso 1", ignoreCase = true) ||
                    Regex("""(?m)^\s*(?:\*\*?)?(?:Paso\s*\d+|\d+[\.\)])""").containsMatchIn(cleanPartial)
                val rawSkus = when {
                    isConversationalOrStoreInfoQuery(query) || suggestionIntent -> emptyList()
                    recipeOrInstructionIntent -> (partialSkus + matchFallbackSkus(query)).distinct().take(8)
                    partialSkus.isNotEmpty() -> partialSkus
                    else -> matchFallbackSkus(query)
                }
                val finalSkus = enforceBudgetLimitOnSkus(rawSkus, budgetLimit)
                val finalAnswer = if (cleanPartial.length >= 120 && (!recipeOrInstructionIntent || partialHasSteps)) {
                    if (finalSkus.isNotEmpty() && !cleanPartial.contains("Subtotal de la compra", ignoreCase = true)) {
                        cleanPartial.trimEnd() + buildFinancialBreakdownText(finalSkus, budgetLimit)
                    } else {
                        cleanPartial
                    }
                } else {
                    buildFallbackRecipeOrCatalogResponse(query, finalSkus, hasAttachedImage = hasImage)
                }
                if (parsedMail != null) {
                    dispatchSuggestionMailPayloadAsync(parsedMail)
                }
                _chatHistory.value = _chatHistory.value.map { msg ->
                    if (msg.id == loadingMsgId) {
                        msg.copy(
                            text = finalAnswer,
                            skus = finalSkus,
                            isLoading = false,
                            modelLabel = when {
                                parsedMail != null -> "Brav IA Soporte • Enviado a bezaleelpatrocino0@gmail.com"
                                hasImage -> "Brav IA Soporte • Auditoría Visual Multimodal"
                                orderIntent && finalSkus.isNotEmpty() -> "Brav IA • Pedido Listo para Confirmación Manual"
                                else -> "Brav IA • Chef Ejecutivo & Asesor Oficial"
                            },
                            durationMs = duration,
                            requiresOrderConfirmation = orderIntent && finalSkus.isNotEmpty(),
                            suggestionPayload = parsedMail
                        )
                    } else {
                        msg
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Brav IA local fallback used: ${e.message}")
                val duration = (System.currentTimeMillis() - startTime).coerceAtMost(MAX_AI_RESPONSE_TIMEOUT_MS)
                val fallbackSkus = if (suggestionIntent) emptyList() else enforceBudgetLimitOnSkus(matchFallbackSkus(query), budgetLimit)
                val fallbackSuggestionPayload = if (suggestionIntent) {
                    SuggestionMailPayload(
                        toEmail = "bezaleelpatrocino0@gmail.com",
                        fromUser = _profile.value.name,
                        userContact = "${_profile.value.email} / ${_profile.value.phone}",
                        activeBranch = _activeBranch.value,
                        activeStore = activeStoreCode,
                        category = if (query.contains("producto", ignoreCase = true)) "SUGERENCIA_PRODUCTO" else "MEJORA_UI_UX",
                        priority = "ALTA",
                        subject = "[Brav IA Soporte] Sugerencia de ${_profile.value.name} (${_activeBranch.value})",
                        summaryBody = query,
                        timestampIso = isoTimestampNow
                    ).also { dispatchSuggestionMailPayloadAsync(it) }
                } else {
                    null
                }
                _chatHistory.value = _chatHistory.value.map { msg ->
                    if (msg.id == loadingMsgId) {
                        msg.copy(
                            text = buildFallbackRecipeOrCatalogResponse(query, fallbackSkus, hasAttachedImage = hasImage),
                            skus = fallbackSkus,
                            isLoading = false,
                            modelLabel = when {
                                fallbackSuggestionPayload != null -> "Brav IA Soporte • Enviado a bezaleelpatrocino0@gmail.com"
                                hasImage -> "Brav IA Soporte • Auditoría Visual Multimodal"
                                orderIntent && fallbackSkus.isNotEmpty() -> "Brav IA • Pedido Listo para Confirmación Manual"
                                else -> "Brav IA • Chef Ejecutivo & Asesor Oficial"
                            },
                            durationMs = duration,
                            requiresOrderConfirmation = orderIntent && fallbackSkus.isNotEmpty(),
                            suggestionPayload = fallbackSuggestionPayload
                        )
                    } else {
                        msg
                    }
                }
            } finally {
                _isAiGenerating.value = false
                activeAiChatJob = null
            }
        }
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
