package com.example.ui.screens

import org.jetbrains.compose.resources.stringResource

import com.example.resources.*

import com.example.ui.theme.Tone

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.ui.theme.ArcaEmerald
import com.example.ui.theme.BravoCtaGradient
import androidx.compose.material.icons.outlined.GridView
import com.example.ui.components.LocalDataSaver
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import com.example.ui.components.GlassBarAction
import com.example.ui.components.GlassTopBar
import com.example.ui.components.LargeTitle
import com.example.ui.components.productHeroImage
import com.example.ui.components.ZoomableProductImage
import com.example.ui.components.SeasonalCombosRow
import com.example.ui.components.PriceAlertRow
import com.example.platform.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SearchOff
import com.example.ui.components.matchesSearch
import com.example.ui.components.unitPrice
import com.example.ui.components.cheaperAlternative
import com.example.ui.components.formatUnitPrice
import com.example.ui.components.applySearchFilters
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.style.TextDecoration
import com.example.data.ProductDescriptionProvider
import com.example.model.CatalogSection
import com.example.model.DailyRotatingOffer
import com.example.model.Product
import com.example.model.StoreMode
import com.example.model.nutritionProfile
import com.example.ui.components.AnimatedPurchaseButton
import com.example.ui.components.BravoAiPlasmaOrb
import com.example.ui.components.BravoArcaHeaderSwitcher
import com.example.ui.components.BravoArcaSelectorMenuOverlay
import com.example.ui.components.BravoUnderline
import com.example.ui.components.BravoUnifiedIcon
import com.example.ui.components.DiagonalMetadataStrip
import com.example.ui.components.FloatingHeaderCapsule
import com.example.ui.components.HapticFeedbackManager
import com.example.ui.components.HapticStyle
import com.example.ui.components.LocalTranslucentHazeState
import com.example.ui.components.PredictiveCatalogSearchBar
import com.example.ui.components.ProductArtworkView
import com.example.ui.components.SectionLineIcon
import com.example.ui.components.TranslucentCard
import com.example.ui.components.VerticalProductCard
import com.example.ui.components.bravoAiAura
import com.example.ui.components.performCrispHaptic
import com.example.ui.components.translucentBackdropEffect
import com.example.ui.components.translucentBackdropSource
import com.example.ui.theme.ArcaTeal
import com.example.ui.theme.PlusJakartaSans
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay

private fun formatCurrency(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount)
}

@Composable
fun HomeScreen(
    storeMode: StoreMode,
    darkMode: Boolean,
    sections: List<CatalogSection>,
    allProducts: List<Product>,
    searchQuery: String,
    cartQtyProvider: (String) -> Int,
    isFavoriteProvider: (String) -> Boolean = { false },
    onToggleFavorite: ((String) -> Unit)? = null,
    isOfflineMode: Boolean = false,
    isGuestMode: Boolean = false,
    cacheStatusLabel: String = "Catálogo oficial al día",
    areMenusVisible: Boolean = true,
    dailyRotatingOffers: List<DailyRotatingOffer> = emptyList(),
    onRotateOfferDay: (Int) -> Unit = {},
    onTriggerDailyOfferPush: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onSelectStoreMode: (StoreMode) -> Unit,
    onOpenScanner: () -> Unit,
    onNavigateToBravIa: (String?) -> Unit,
    onNavigateToDelivery: () -> Unit,
    onOpenMyLists: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onAddCombo: (String, List<String>) -> Unit = { _, _ -> },
    recentProducts: List<Product> = emptyList(),
    lastOrder: com.example.model.OrderRecord? = null,
    onRepeatOrder: (String) -> Unit = {},
    onOpenCategory: (String) -> Unit,
    onOpenProduct: (String) -> Unit,
    onChangeQty: (String, Int) -> Unit
) {
    val view = currentPlatformView()
    val isArca = storeMode == StoreMode.ARCA
    val primaryColor = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary

    var localSearchInput by rememberSaveable { mutableStateOf(searchQuery) }
    var debouncedQuery by rememberSaveable { mutableStateOf(searchQuery) }
    var showMiniSearchPreview by remember { mutableStateOf(false) }

    val productsByCategory = remember(allProducts) {
        allProducts.groupBy { it.category }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery != localSearchInput && searchQuery.isEmpty()) {
            localSearchInput = ""
            debouncedQuery = ""
            showMiniSearchPreview = false
        }
    }

    LaunchedEffect(localSearchInput) {
        if (localSearchInput.isBlank()) {
            debouncedQuery = ""
            onSearchQueryChange("")
            showMiniSearchPreview = false
        } else {
            delay(300L)
            debouncedQuery = localSearchInput.trim()
            onSearchQueryChange(debouncedQuery)
            showMiniSearchPreview = true
        }
    }

    // Búsqueda sin tildes, con filtro de ofertas y orden por precio
    var onlyOffers by rememberSaveable { mutableStateOf(false) }
    var searchSort by remember { mutableStateOf(com.example.ui.components.SearchSort.RELEVANCE) }
    val offerSkus = remember(dailyRotatingOffers) { dailyRotatingOffers.map { it.sku }.toSet() }
    val filteredProducts = remember(debouncedQuery, allProducts, onlyOffers, searchSort, offerSkus) {
        val q = debouncedQuery.trim()
        if (q.isEmpty()) emptyList()
        else allProducts.filter { p -> p.matchesSearch(q) }.applySearchFilters(onlyOffers, searchSort, offerSkus)
    }
    // Guarda la búsqueda como reciente cuando el usuario se detiene en ella
    LaunchedEffect(debouncedQuery) {
        if (debouncedQuery.isNotBlank()) {
            delay(1500L)
            com.example.ui.components.RecentSearches.add(debouncedQuery)
        }
    }

    val searchRows = remember(filteredProducts) {
        filteredProducts.chunked(3)
    }

    val miniSearchSuggestions = remember(isArca) {
        if (isArca) {
            listOf("Alimento", "Salmón", "Arena", "Snacks", "Antipulgas")
        } else {
            listOf("Salami", "Arroz", "Plátano", "Queso", "Leche", "Papel")
        }
    }

    val hazeState = LocalTranslucentHazeState.current
    val storeMenuHazeState = rememberHazeState(blurEnabled = true)
    var isStoreMenuExpanded by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    // Botón Buscar de la barra de pestañas: enfoca el campo de búsqueda del inicio
    val homeSearchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val searchRequest = com.example.ui.components.SearchFocusRequest.serial
    LaunchedEffect(searchRequest) {
        if (searchRequest > 0) {
            delay(120)
            runCatching { homeSearchFocus.requestFocus() }
        }
    }
    val showTopHeader = areMenusVisible || isSearchFocused
    val homeListState = rememberLazyListState()
    val isHomeScrolled by remember {
        derivedStateOf {
            homeListState.firstVisibleItemIndex > 0 || homeListState.firstVisibleItemScrollOffset > 28
        }
    }
    val homeExploreTarget = remember(sections.size, searchRows.size, debouncedQuery) {
        if (debouncedQuery.isNotEmpty()) searchRows.size.coerceAtLeast(1) else sections.size.coerceAtLeast(1)
    }
    val homeScrollProgress by animateFloatAsState(
        targetValue = (
            (homeListState.firstVisibleItemIndex.toFloat() + (homeListState.firstVisibleItemScrollOffset / 450f)) /
                homeExploreTarget.toFloat()
            ).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 160),
        label = "home_scroll_progress"
    )
    val headerVerticalPadding by animateDpAsState(
        targetValue = if (isHomeScrolled) 3.dp else 6.dp,
        animationSpec = tween(durationMillis = 180),
        label = "home_header_vertical_pad"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .translucentBackdropSource(
                    hazeState = storeMenuHazeState,
                    zIndex = 0f,
                    enabled = isStoreMenuExpanded && showTopHeader
                )
        ) {
            AnimatedVisibility(
                visible = showTopHeader,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
                exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(160)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = headerVerticalPadding),
                    verticalArrangement = Arrangement.spacedBy(if (isHomeScrolled) 4.dp else 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BravoArcaHeaderSwitcher(
                            activeStore = storeMode,
                            darkMode = darkMode,
                            onSelectStore = onSelectStoreMode,
                            expanded = isStoreMenuExpanded,
                            onExpandedChange = { isStoreMenuExpanded = it }
                        )

                    Surface(
                        onClick = {
                            HapticFeedbackManager.onNavigationClick(view)
                            onNavigateToDelivery()
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = Tone.success.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Tone.success.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("home_delivery_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            BravoUnifiedIcon(
                                iconKey = "delivery",
                                tint = Tone.success,
                                size = 15.dp
                            )
                            Text(
                                text = "En ruta",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Tone.success,
                                maxLines = 1
                            )
                        }
                    }
                }

                PredictiveCatalogSearchBar(
                    query = localSearchInput,
                    onQueryChange = { newText ->
                        localSearchInput = newText
                    },
                    catalogProducts = allProducts,
                    onSelectPredictedProduct = { prod ->
                        localSearchInput = prod.name
                        debouncedQuery = prod.name
                        onSearchQueryChange(prod.name)
                    },
                    placeholderText = if (isArca) "Buscar productos, marcas o mascotas..." else "Buscar productos, marcas o recetas...",
                    darkMode = darkMode,
                    isArca = isArca,
                    testTag = "home_search_input",
                    onFocusChanged = { isSearchFocused = it },
                    focusRequester = homeSearchFocus,
                    nativeTrailingSymbol = "barcode.viewfinder",
                    onNativeTrailing = { onOpenScanner() },
                    modifier = Modifier.bravoAiAura(
                        cornerRadius = 999.dp,
                        enabled = localSearchInput.isNotBlank(),
                        borderWidth = 1.4.dp,
                        haloAlpha = 0.20f
                    ),
                    trailingActionContent = {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    performCrispHaptic(view, HapticStyle.CONFIRM)
                                    onOpenScanner()
                                }
                                .testTag("home_scanner_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Escanear código de barras",
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(homeScrollProgress.coerceAtLeast(0.06f))
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                }
            }
        }

        // Deslizar hacia abajo para actualizar el catálogo
        var isRefreshingHome by remember { mutableStateOf(false) }
        val refreshScope = rememberCoroutineScope()
        PullToRefreshBox(
            isRefreshing = isRefreshingHome,
            onRefresh = {
                isRefreshingHome = true
                performCrispHaptic(view, HapticStyle.TICK)
                onRefresh()
                refreshScope.launch {
                    delay(900)
                    isRefreshingHome = false
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            LazyColumn(
                state = homeListState,
                modifier = Modifier
                    .fillMaxSize()
                    .translucentBackdropSource(hazeState, zIndex = 1f)
                    .testTag("home_screen_list"),
                contentPadding = PaddingValues(top = 4.dp, bottom = com.example.ui.components.LocalBottomChromeInset.current)
            ) {
                if (debouncedQuery.isEmpty() && isSearchFocused) {
                    item(key = "search_suggestions", contentType = "suggestions") {
                        com.example.ui.components.SearchSuggestionRow(
                            popular = miniSearchSuggestions,
                            onPick = { picked ->
                                performCrispHaptic(view, HapticStyle.TICK)
                                localSearchInput = picked
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                }
                if (debouncedQuery.isNotEmpty()) {
                    item(key = "search_header", contentType = "header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(Res.string.search_results_for, debouncedQuery),
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            BravoUnderline(isArca = isArca)
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.ui.components.SearchFilterRow(
                                onlyOffers = onlyOffers,
                                onToggleOffers = {
                                    performCrispHaptic(view, HapticStyle.TICK)
                                    onlyOffers = !onlyOffers
                                },
                                sort = searchSort,
                                onSortChange = {
                                    performCrispHaptic(view, HapticStyle.TICK)
                                    searchSort = it
                                },
                                accent = primaryColor
                            )
                        }
                    }

                    if (filteredProducts.isEmpty()) {
                        item(key = "search_empty", contentType = "empty") {
                            TranslucentCard(
                                darkMode = darkMode,
                                isArca = isArca,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    com.example.ui.components.EmptyStateBadge(
                                        icon = Icons.Default.SearchOff,
                                        accent = primaryColor
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(Res.string.search_empty_title, debouncedQuery),
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(Res.string.search_empty_body),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    AnimatedPurchaseButton(
                                        text = stringResource(Res.string.search_empty_cta),
                                        icon = Icons.Default.AutoAwesome,
                                        primaryColor = primaryColor,
                                        onClick = { onNavigateToBravIa("¿Qué producto del catálogo me recomiendas como alternativa a $debouncedQuery?") }
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = searchRows,
                            key = { row -> row.first().sku },
                            contentType = { "search_row" }
                        ) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { prod ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        VerticalProductCard(
                                            product = prod,
                                            qtyInCart = cartQtyProvider(prod.sku),
                                            isArca = isArca,
                                            darkMode = darkMode,
                                            onOpenDetail = { onOpenProduct(prod.sku) },
                                            onIncrement = { onChangeQty(prod.sku, 1) },
                                            onDecrement = { onChangeQty(prod.sku, -1) },
                                            isFavorite = isFavoriteProvider(prod.sku),
                                            onToggleFavorite = onToggleFavorite?.let { fn -> { fn(prod.sku) } },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {

                    // Recordatorio: repetir la última compra con un toque
                    if (lastOrder != null && lastOrder.skus.isNotEmpty()) {
                        // Pedido aún en camino: "¿Repetimos…? • En ruta ahora" no tenía sentido;
                        // se muestra el seguimiento en su lugar
                        val activeStage = com.example.model.OrderDeliveryStage.fromStatusString(lastOrder.status)
                        val orderInProgress = activeStage != com.example.model.OrderDeliveryStage.DELIVERED
                        item(key = "home_repeat_last_order", contentType = "repeat_order") {
                            TranslucentCard(
                                darkMode = darkMode,
                                isArca = isArca,
                                cornerRadius = 20.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .padding(top = 14.dp)
                                    .testTag("home_repeat_order_card")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    com.example.ui.components.EmptyStateBadge(
                                        icon = if (orderInProgress) Icons.Default.LocalShipping else Icons.Default.Replay,
                                        accent = primaryColor,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = if (orderInProgress) "Tu pedido va en camino" else stringResource(Res.string.repeat_title),
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (orderInProgress) "${activeStage.statusText} • ${lastOrder.skus.size} productos" else stringResource(Res.string.repeat_products, lastOrder.skus.size, lastOrder.date),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        onClick = {
                                            performCrispHaptic(view, HapticStyle.CONFIRM)
                                            if (orderInProgress) onNavigateToDelivery() else onRepeatOrder(lastOrder.id)
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        color = primaryColor,
                                        modifier = Modifier.testTag("home_repeat_order_button")
                                    ) {
                                        Text(
                                            text = if (orderInProgress) "Ver" else stringResource(Res.string.repeat_button),
                                            color = Color.White,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (recentProducts.isNotEmpty()) {
                    item(key = "home_recent_products", contentType = "section_carousel") {
                        Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                            Text(
                                text = stringResource(Res.string.recent_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            Text(
                                text = stringResource(Res.string.recent_subtitle),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(recentProducts, key = { "recent_" + it.sku }) { product ->
                                    VerticalProductCard(
                                        product = product,
                                        qtyInCart = cartQtyProvider(product.sku),
                                        isArca = isArca,
                                        darkMode = darkMode,
                                        onOpenDetail = { onOpenProduct(product.sku) },
                                        onIncrement = { onChangeQty(product.sku, 1) },
                                        onDecrement = { onChangeQty(product.sku, -1) },
                                        isFavorite = isFavoriteProvider(product.sku),
                                        onToggleFavorite = onToggleFavorite?.let { fn -> { fn(product.sku) } },
                                        modifier = Modifier.width(140.dp),
                                        heroTransition = false
                                    )
                                }
                            }
                        }
                    }

                    // 2.0: las ofertas del día van después de lo que más se recompra
                    if (dailyRotatingOffers.isNotEmpty()) {
                        item(key = "home_daily_rotating_offers_carousel", contentType = "daily_offers") {
                            DailyRotatingOffersCarouselSection(
                                dailyRotatingOffers = dailyRotatingOffers,
                                allProducts = allProducts,
                                darkMode = darkMode,
                                isArca = isArca,
                                primaryColor = primaryColor,
                                onRotateOfferDay = onRotateOfferDay,
                                onTriggerDailyOfferPush = onTriggerDailyOfferPush,
                                onOpenProduct = onOpenProduct,
                                onChangeQty = onChangeQty,
                                sectionTag = "home_daily_rotating_offers_section",
                                rotateButtonTag = "home_rotate_daily_offer_button"
                            )
                        }
                    }
                }

                item(key = "home_seasonal_combos", contentType = "combos") {
                    SeasonalCombosRow(
                        catalog = allProducts,
                        accent = primaryColor,
                        onAddCombo = { combo, products ->
                            performCrispHaptic(view, HapticStyle.CONFIRM)
                            onAddCombo(combo.title, products.map { it.sku })
                        }
                    )
                }

                // Banner "Todas las categorías", como en la app oficial
                    item(key = "home_all_categories_banner", contentType = "banner") {
                        val bannerShape = RoundedCornerShape(16.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .height(64.dp)
                                .clip(bannerShape)
                                .background(Brush.horizontalGradient(if (isArca) listOf(ArcaEmerald, ArcaTeal) else BravoCtaGradient))
                                .clickable {
                                    HapticFeedbackManager.onNavigationClick(view)
                                    onOpenCatalog()
                                }
                                .padding(horizontal = 18.dp)
                                .testTag("home_all_categories_banner"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TODAS LAS CATEGORÍAS",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    items(
                        items = sections,
                        key = { it.id },
                        contentType = { "section_carousel" }
                    ) { section ->
                        val sectionProducts = productsByCategory[section.name].orEmpty()
                        if (sectionProducts.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = section.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable {
                                                HapticFeedbackManager.onNavigationClick(view)
                                                onOpenCategory(section.name)
                                            }
                                            .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
                                            .testTag("see_all_${section.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Ver todos",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = primaryColor,
                                            maxLines = 1
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = sectionProducts,
                                        key = { it.sku },
                                        contentType = { "product_card" }
                                    ) { product ->
                                        VerticalProductCard(
                                            product = product,
                                            qtyInCart = cartQtyProvider(product.sku),
                                            isArca = isArca,
                                            darkMode = darkMode,
                                            onOpenDetail = { onOpenProduct(product.sku) },
                                            onIncrement = { onChangeQty(product.sku, 1) },
                                            onDecrement = { onChangeQty(product.sku, -1) },
                                            isFavorite = isFavoriteProvider(product.sku),
                                            onToggleFavorite = onToggleFavorite?.let { fn -> { fn(product.sku) } },
                                            modifier = Modifier.width(140.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "home_ai_footer", contentType = "ai_banner") {
                        TranslucentCard(
                            darkMode = darkMode,
                            isArca = isArca,
                            cornerRadius = 22.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 28.dp)
                                .bravoAiAura(cornerRadius = 22.dp, borderWidth = 1.6.dp, haloAlpha = 0.22f)
                                .testTag("home_ai_hero_banner"),
                            onClick = {
                                HapticFeedbackManager.onAssistantTrigger(view)
                                onNavigateToBravIa(null)
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BravoAiPlasmaOrb(size = 38.dp, isActive = true)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = if (isArca) {
                                                "Asesoría Nutricional Brav IA"
                                            } else {
                                                "Compra con Brav IA"
                                            },
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        BravoUnderline(isArca = isArca)
                                    }
                                }
                                Text(
                                    text = "Brav IA recomienda productos del catálogo con precios reales, tiene un recetario verificado y puede pedir por ti cuando tú lo confirmas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AnimatedPurchaseButton(
                                    text = "Abrir Brav IA",
                                    icon = Icons.Default.AutoAwesome,
                                    primaryColor = primaryColor,
                                    onClick = {
                                        HapticFeedbackManager.onAssistantTrigger(view)
                                        onNavigateToBravIa(null)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Surface(
                                    onClick = {
                                        HapticFeedbackManager.onNavigationClick(view)
                                        onOpenMyLists()
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = primaryColor.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_open_smart_lists_button")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BravoUnifiedIcon(
                                            iconKey = "list",
                                            tint = primaryColor,
                                            size = 16.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Mis listas inteligentes",
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
                }
            }
        }
        }

        BravoArcaSelectorMenuOverlay(
            expanded = isStoreMenuExpanded && showTopHeader,
            activeStore = storeMode,
            darkMode = darkMode,
            menuHazeState = storeMenuHazeState,
            onDismiss = { isStoreMenuExpanded = false },
            onSelectStore = { mode ->
                isStoreMenuExpanded = false
                onSelectStoreMode(mode)
            }
        )
    }
}

@Composable
fun CatalogScreen(
    storeMode: StoreMode,
    darkMode: Boolean,
    sections: List<CatalogSection>,
    allProducts: List<Product>,
    filterQuery: String,
    isOfflineMode: Boolean = false,
    cacheStatusLabel: String = "Catálogo oficial al día",
    areMenusVisible: Boolean = true,
    dailyRotatingOffers: List<DailyRotatingOffer> = emptyList(),
    onRotateOfferDay: (Int) -> Unit = {},
    onTriggerDailyOfferPush: () -> Unit = {},
    onOpenProduct: (String) -> Unit = {},
    onChangeQty: (String, Int) -> Unit = { _, _ -> },
    onFilterQueryChange: (String) -> Unit,
    onToggleStoreMode: () -> Unit,
    onSelectStoreMode: (StoreMode) -> Unit = { onToggleStoreMode() },
    onOpenCategory: (String) -> Unit
) {
    val view = currentPlatformView()
    val isArca = storeMode == StoreMode.ARCA
    val primaryColor = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary

    var localFilter by rememberSaveable { mutableStateOf(filterQuery) }
    var debouncedFilter by rememberSaveable { mutableStateOf(filterQuery) }

    val countsByCategory = remember(allProducts) {
        allProducts.groupingBy { it.category }.eachCount()
    }

    LaunchedEffect(filterQuery) {
        if (filterQuery.isEmpty() && localFilter.isNotEmpty()) {
            localFilter = ""
            debouncedFilter = ""
        }
    }

    LaunchedEffect(localFilter) {
        if (localFilter.isBlank()) {
            debouncedFilter = ""
            onFilterQueryChange("")
        } else {
            delay(300L)
            debouncedFilter = localFilter.trim()
            onFilterQueryChange(debouncedFilter)
        }
    }

    val filteredSections = remember(debouncedFilter, sections, allProducts) {
        val q = com.example.ui.components.normalizeForSearch(debouncedFilter)
        if (q.isEmpty()) sections
        else sections.filter { sec ->
            com.example.ui.components.normalizeForSearch(sec.name).contains(q) ||
                allProducts.any { it.category == sec.name && it.matchesSearch(q) }
        }
    }

    val hazeState = LocalTranslucentHazeState.current
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var isFilterFocused by remember { mutableStateOf(false) }
    val showTopHeader = areMenusVisible || isFilterFocused
    val catalogListState = rememberLazyListState()
    val isCatalogScrolled by remember {
        derivedStateOf {
            catalogListState.firstVisibleItemIndex > 0 || catalogListState.firstVisibleItemScrollOffset > 28
        }
    }
    val catalogScrollProgress by animateFloatAsState(
        targetValue = (
            (catalogListState.firstVisibleItemIndex.toFloat() + (catalogListState.firstVisibleItemScrollOffset / 450f)) /
                filteredSections.size.coerceAtLeast(1).toFloat()
            ).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 160),
        label = "catalog_scroll_progress"
    )
    val headerTopPadding by animateDpAsState(
        targetValue = if (showTopHeader) with(density) { headerHeightPx.toDp() } else 8.dp,
        animationSpec = tween(durationMillis = 220),
        label = "catalog_header_padding"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = catalogListState,
            modifier = Modifier
                .fillMaxSize()
                .translucentBackdropSource(hazeState, zIndex = 1f)
                .testTag("catalog_screen_list"),
            contentPadding = PaddingValues(top = headerTopPadding + 4.dp, bottom = com.example.ui.components.LocalBottomChromeInset.current),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (debouncedFilter.isEmpty() && dailyRotatingOffers.isNotEmpty()) {
                item(key = "catalog_daily_rotating_offers_carousel", contentType = "daily_offers") {
                    DailyRotatingOffersCarouselSection(
                        dailyRotatingOffers = dailyRotatingOffers,
                        allProducts = allProducts,
                        darkMode = darkMode,
                        isArca = isArca,
                        primaryColor = primaryColor,
                        onRotateOfferDay = onRotateOfferDay,
                        onTriggerDailyOfferPush = onTriggerDailyOfferPush,
                        onOpenProduct = onOpenProduct,
                        onChangeQty = onChangeQty,
                        sectionTag = "daily_rotating_offers_section",
                        rotateButtonTag = "rotate_daily_offer_button"
                    )
                }
            }

            item(key = "catalog_pasillos_header", contentType = "header") {
                // Mismo estilo que "Ofertas del día": título de sección a la izquierda, sin subrayado
                // (el subrayado de marca queda solo en el título grande de la pantalla)
                Text(
                    text = if (isArca) "Departamentos" else "Pasillos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            items(
                items = filteredSections,
                key = { it.id },
                contentType = { "catalog_section_card" }
            ) { sec ->
                val count = countsByCategory[sec.name] ?: 0
                TranslucentCard(
                    darkMode = darkMode,
                    isArca = isArca,
                    cornerRadius = 18.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("catalog_section_${sec.id}"),
                    onClick = { onOpenCategory(sec.name) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            SectionLineIcon(iconType = sec.iconType, isArca = isArca)
                            Column {
                                Text(
                                    text = sec.name,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                DiagonalMetadataStrip(
                                    items = listOf("$count productos"),
                                    slashColor = primaryColor,
                                    fontSizeSp = 12f
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Abrir sección",
                            tint = primaryColor
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showTopHeader,
            enter = expandVertically(animationSpec = tween(240)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(240)) + fadeOut(animationSpec = tween(170)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { headerHeightPx = it.height }
        ) {
            val headerBg = MaterialTheme.colorScheme.background
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
            ) {
                val catalogTitle = if (isArca) "Catálogo El Arca" else "Catálogo"
                // Título grande arriba; al hacer scroll se convierte en la barra compacta
                AnimatedContent(
                    targetState = isCatalogScrolled,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(260)) { -it / 4 })
                            .togetherWith(fadeOut(tween(160)))
                    },
                    label = "catalog_title"
                ) { scrolled ->
                    if (scrolled) {
                        com.example.ui.components.GlassPresence(transition.targetState == androidx.compose.animation.EnterExitState.Visible) { GlassTopBar(darkMode = darkMode, accent = primaryColor, title = catalogTitle) }
                    } else {
                        LargeTitle(title = catalogTitle, accent = primaryColor)
                    }
                }
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp)) {
                        PredictiveCatalogSearchBar(
                            query = localFilter,
                            onQueryChange = { localFilter = it },
                            catalogProducts = allProducts,
                            onSelectPredictedProduct = { prod ->
                                localFilter = prod.name
                                debouncedFilter = prod.name
                                onFilterQueryChange(prod.name)
                            },
                            placeholderText = "Buscar productos o marcas",
                            darkMode = darkMode,
                            isArca = isArca,
                            testTag = "catalog_filter_input",
                            onFocusChanged = { isFilterFocused = it }
                        )
                }
            }
        }
    }
}

@Composable
fun CategoryGridScreen(
    categoryName: String,
    products: List<Product>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    storeMode: StoreMode,
    darkMode: Boolean,
    cartQtyProvider: (String) -> Int,
    isFavoriteProvider: (String) -> Boolean = { false },
    onToggleFavorite: ((String) -> Unit)? = null,
    areMenusVisible: Boolean = true,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onChangeQty: (String, Int) -> Unit
) {
    val view = currentPlatformView()
    val isArca = storeMode == StoreMode.ARCA
    val primaryColor = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary

    var localQuery by rememberSaveable { mutableStateOf(searchQuery) }
    var debouncedQuery by rememberSaveable { mutableStateOf(searchQuery) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isEmpty() && localQuery.isNotEmpty()) {
            localQuery = ""
            debouncedQuery = ""
        }
    }

    LaunchedEffect(localQuery) {
        if (localQuery.isBlank()) {
            debouncedQuery = ""
            onSearchQueryChange("")
        } else {
            delay(300L)
            debouncedQuery = localQuery.trim()
            onSearchQueryChange(debouncedQuery)
        }
    }

    val catProducts = remember(categoryName, products) {
        products.filter { it.category == categoryName }
    }

    val filteredItems = remember(debouncedQuery, catProducts) {
        val q = debouncedQuery.trim()
        if (q.isEmpty()) catProducts
        else catProducts.filter { it.matchesSearch(q) }
    }

    val rows = remember(filteredItems) {
        filteredItems.chunked(3)
    }

    val hazeState = LocalTranslucentHazeState.current
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val showTopHeader = areMenusVisible || isSearchFocused
    val categoryListState = rememberLazyListState()
    val isCategoryScrolled by remember {
        derivedStateOf {
            categoryListState.firstVisibleItemIndex > 0 || categoryListState.firstVisibleItemScrollOffset > 28
        }
    }
    val categoryScrollProgress by animateFloatAsState(
        targetValue = (
            (categoryListState.firstVisibleItemIndex.toFloat() + (categoryListState.firstVisibleItemScrollOffset / 450f)) /
                rows.size.coerceAtLeast(1).toFloat()
            ).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 160),
        label = "category_scroll_progress"
    )
    val headerTopPadding by animateDpAsState(
        targetValue = if (showTopHeader) with(density) { headerHeightPx.toDp() } else 8.dp,
        animationSpec = tween(durationMillis = 220),
        label = "category_header_padding"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = categoryListState,
            modifier = Modifier
                .fillMaxSize()
                .translucentBackdropSource(hazeState, zIndex = 1f)
                .testTag("category_grid_screen"),
            contentPadding = PaddingValues(top = headerTopPadding + 6.dp, bottom = com.example.ui.components.LocalBottomChromeInset.current)
        ) {
            items(
                items = rows,
                key = { row -> row.first().sku },
                contentType = { "category_row" }
            ) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { prod ->
                        Box(modifier = Modifier.weight(1f)) {
                            VerticalProductCard(
                                product = prod,
                                qtyInCart = cartQtyProvider(prod.sku),
                                isArca = isArca,
                                darkMode = darkMode,
                                onOpenDetail = { onOpenProduct(prod.sku) },
                                onIncrement = { onChangeQty(prod.sku, 1) },
                                onDecrement = { onChangeQty(prod.sku, -1) },
                                isFavorite = isFavoriteProvider(prod.sku),
                                onToggleFavorite = onToggleFavorite?.let { fn -> { fn(prod.sku) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showTopHeader,
            enter = expandVertically(animationSpec = tween(240)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(240)) + fadeOut(animationSpec = tween(170)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { headerHeightPx = it.height }
        ) {
            val headerBg = MaterialTheme.colorScheme.background
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
            ) {
                GlassTopBar(
                    darkMode = darkMode,
                    accent = primaryColor,
                    title = categoryName,
                    onBack = {
                        HapticFeedbackManager.onNavigationClick(view)
                        onBack()
                    },
                    backLabel = "Volver al catálogo",
                    backTestTag = "category_back_button"
                )
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp)) {
                    PredictiveCatalogSearchBar(
                        query = localQuery,
                        onQueryChange = { localQuery = it },
                        catalogProducts = catProducts,
                        onSelectPredictedProduct = { prod ->
                            localQuery = prod.name
                            debouncedQuery = prod.name
                            onSearchQueryChange(prod.name)
                        },
                        placeholderText = "Buscar en $categoryName",
                        darkMode = darkMode,
                        isArca = isArca,
                        testTag = "category_search_input",
                        onFocusChanged = { isSearchFocused = it }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDetailScreen(
    product: Product,
    qtyInCart: Int,
    totalCartCount: Int,
    isFavorite: Boolean,
    storeMode: StoreMode,
    darkMode: Boolean,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenCategoryFilter: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onChangeQty: (Int) -> Unit,
    onRemoveCompletely: () -> Unit,
    onAskBravIa: () -> Unit,
    hasPriceAlert: Boolean = false,
    onTogglePriceAlert: () -> Unit = {},
    allProducts: List<Product> = emptyList(),
    onOpenProduct: (String) -> Unit = {}
) {
    val view = currentPlatformView()
    val isArca = storeMode == StoreMode.ARCA
    val primaryColor = if (isArca) ArcaTeal else MaterialTheme.colorScheme.primary
    val effectiveQty = if (qtyInCart > 0) qtyInCart else 1
    val accumulatedPrice = product.price * effectiveQty
    val richDescription = remember(product.sku) { ProductDescriptionProvider.describe(product) }

    val hazeState = LocalTranslucentHazeState.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("product_detail_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .translucentBackdropSource(hazeState, zIndex = 1f),
            contentPadding = PaddingValues(top = 74.dp, bottom = com.example.ui.components.LocalBottomChromeInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "detail_main_card", contentType = "product_card") {
                TranslucentCard(
                darkMode = darkMode,
                isArca = isArca,
                cornerRadius = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                performCrispHaptic(view, HapticStyle.TICK)
                                onOpenCategoryFilter(product.category)
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = primaryColor.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filtrar por categoría",
                                    tint = primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                DiagonalMetadataStrip(
                                    items = listOf(
                                        product.category,
                                        if (product.isBravo) "Marca Bravo" else product.badge.ifBlank { "Oficial" }
                                    ),
                                    slashColor = primaryColor,
                                    fontSizeSp = 12f
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    performCrispHaptic(view, HapticStyle.TICK)
                                    onShare()
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Compartir producto",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    performCrispHaptic(view, HapticStyle.CONFIRM)
                                    onToggleFavorite()
                                },
                                shape = CircleShape,
                                color = if (isFavorite) Tone.dangerSoft else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isFavorite) BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.4f)) else null,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("detail_favorite_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorito",
                                        tint = if (isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        var showZoom by remember { mutableStateOf(false) }
                        ProductArtworkView(
                            product = product,
                            modifier = Modifier
                                .size(200.dp)
                                .productHeroImage(product.sku)
                                .clickable { showZoom = true },
                            cornerRadius = 18.dp
                        )
                        if (showZoom) ZoomableProductImage(product = product, onDismiss = { showZoom = false })
                    }

                    // La categoría ya sale en la ficha de arriba; aquí no se repite
                    Text(
                        text = "ITBIS incluido",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = product.name,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "RD$${formatCurrency(product.price)}.00",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 25.sp,
                                color = primaryColor
                            )
                            Text(
                                text = "/ ${product.unit}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }

                    // Precio por libra / litro / unidad y, si existe, una opción más barata parecida
                    val unitPriceInfo = remember(product.sku, product.price) { product.unitPrice() }
                    val cheaper = remember(product.sku, allProducts) { product.cheaperAlternative(allProducts) }
                    if (unitPriceInfo != null) {
                        Text(
                            text = stringResource(Res.string.unit_price_equals, formatUnitPrice(unitPriceInfo.first, unitPriceInfo.second)),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("unit_price_label")
                        )
                    }
                    if (cheaper != null) {
                        val (alt, pct) = cheaper
                        val altUnit = alt.unitPrice()
                        Surface(
                            onClick = {
                                performCrispHaptic(view, HapticStyle.TICK)
                                onOpenProduct(alt.sku)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = Tone.success.copy(alpha = 0.10f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cheaper_alternative_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Savings, contentDescription = null, tint = Tone.success, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Ahorra $pct% por ${altUnit?.second?.label ?: "unidad"}: ${alt.name}" +
                                        (altUnit?.let { " (${formatUnitPrice(it.first, it.second)})" } ?: ""),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Tone.successStrong,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Tone.success, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    // Texto largo alineado a la izquierda (WWDC25: tipografía a la izquierda; un
                    // párrafo centrado se lee peor). El título sigue el estilo de las demás secciones.
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Tone.success,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Descripción",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = richDescription.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            richDescription.highlights.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = primaryColor.copy(alpha = if (darkMode) 0.18f else 0.09f),
                                    border = BorderStroke(0.75.dp, primaryColor.copy(alpha = 0.28f))
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (darkMode) 0.55f else 0.65f),
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = richDescription.usageOrStorageTip,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }

        item(key = "detail_nutrition_ingredients_card", contentType = "nutrition_card") {
            val nut = remember(product.sku) { product.nutritionProfile() }
            TranslucentCard(
                darkMode = darkMode,
                isArca = isArca,
                cornerRadius = 22.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("detail_nutrition_card")
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
                            text = "Nutrición e ingredientes (estimado)",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        BravoUnderline(isArca = isArca)
                        // Estos valores se aproximan según el tipo de producto, no salen de su etiqueta.
                        Text(
                            text = "Valores aproximados según el tipo de producto. Si tienes alergias o sigues una dieta médica, revisa la etiqueta del empaque.",
                            fontFamily = PlusJakartaSans,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = primaryColor.copy(alpha = if (darkMode) 0.20f else 0.10f),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BravoUnifiedIcon(
                                    iconKey = "sparkle",
                                    tint = primaryColor,
                                    size = 15.dp
                                )
                                Text(
                                    text = if (nut.caloriesKcal != null) {
                                        "≈ ${nut.caloriesKcal} kcal • ${nut.caloriesPortionLabel}"
                                    } else {
                                        nut.caloriesPortionLabel
                                    },
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = primaryColor
                                )
                            }
                        }

                        if (nut.proteinG != null && nut.carbsG != null && nut.fatG != null) {
                            DiagonalMetadataStrip(
                                items = listOf(
                                    "Prot: ${nut.proteinG}",
                                    "Carb: ${nut.carbsG}",
                                    "Grasa: ${nut.fatG}"
                                ),
                                slashColor = primaryColor,
                                fontSizeSp = 12f
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Ingredientes",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = nut.ingredients,
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Solo avisamos posibles alérgenos; nunca afirmamos "libre de" sin la etiqueta real.
                        nut.allergens.filterNot { it.contains("Libre", ignoreCase = true) }.forEach { allergen ->
                            val isFree = false
                            val badgeColor = Tone.warning
                            Surface(
                                shape = CircleShape,
                                color = badgeColor.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.38f))
                            ) {
                                Text(
                                    text = if (isFree) allergen else "Puede contener: $allergen",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        nut.dietaryTags.forEach { diet ->
                            Surface(
                                shape = CircleShape,
                                color = primaryColor.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.30f))
                            ) {
                                Text(
                                    text = diet,
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

        item(key = "detail_price_alert", contentType = "price_alert") {
            PriceAlertRow(
                active = hasPriceAlert,
                accent = primaryColor,
                darkMode = darkMode,
                onToggle = {
                    performCrispHaptic(view, HapticStyle.TICK)
                    onTogglePriceAlert()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
            )
        }

        item(key = "detail_purchase_card", contentType = "purchase_card") {
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                            DiagonalMetadataStrip(
                                items = listOf(
                                    "CANTIDAD",
                                    if (qtyInCart > 0) "$qtyInCart en carrito" else "Selecciona unidades"
                                ),
                                slashColor = primaryColor,
                                fontSizeSp = 12f
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Total: RD$${formatCurrency(accumulatedPrice)}.00",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    HapticFeedbackManager.onDecrementOrRemove(view, isFullRemove = qtyInCart <= 1)
                                    if (qtyInCart > 0) onChangeQty(-1)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Disminuir",
                                    tint = primaryColor,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Text(
                                text = "$effectiveQty",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = {
                                    HapticFeedbackManager.onAddToCart(view)
                                    onChangeQty(1)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor)
                                    .testTag("detail_increment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Aumentar",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (qtyInCart > 0) {
                            Surface(
                                onClick = {
                                    HapticFeedbackManager.onDecrementOrRemove(view, isFullRemove = true)
                                    onRemoveCompletely()
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = if (darkMode) Color(0xFF4C0519) else Tone.dangerSoft,
                                border = BorderStroke(1.5.dp, Color(0xFFE11D48)),
                                modifier = Modifier
                                    .weight(0.95f)
                                    .testTag("detail_remove_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remover",
                                        tint = if (darkMode) Color(0xFFFDA4AF) else Tone.dangerStrong,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Remover",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = if (darkMode) Color(0xFFFDA4AF) else Tone.dangerStrong
                                    )
                                }
                            }
                        }

                        AnimatedPurchaseButton(
                            text = if (qtyInCart == 0) "Agregar • RD$${formatCurrency(product.price)}" else "Ir al pago • RD$${formatCurrency(accumulatedPrice)}",
                            icon = Icons.Default.ShoppingCart,
                            primaryColor = primaryColor,
                            onClick = {
                                if (qtyInCart == 0) {
                                    HapticFeedbackManager.onAddToCart(view)
                                    onChangeQty(1)
                                } else {
                                    HapticFeedbackManager.onNavigationClick(view)
                                    onOpenCart()
                                }
                            },
                            modifier = Modifier.weight(1.4f),
                            testTag = "detail_primary_cart_action"
                        )
                    }
                }
            }
        }

        item(key = "detail_ai_option", contentType = "ai_option") {
            Surface(
                onClick = {
                    HapticFeedbackManager.onAssistantTrigger(view)
                    onAskBravIa()
                },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("detail_ask_bravia_button")
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
                                items = listOf("OPCIÓN SUGERIDA", "Brav IA"),
                                slashColor = primaryColor,
                                fontSizeSp = 12f
                            )
                            Text(
                                text = "Pregúntale a Brav IA por recetas o alergias",
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

        GlassTopBar(
            darkMode = darkMode,
            accent = primaryColor,
            title = "Producto",
            modifier = Modifier.align(Alignment.TopCenter),
            onBack = {
                HapticFeedbackManager.onNavigationClick(view)
                onBack()
            },
            backTestTag = "detail_back_button",
            actions = listOf(
                GlassBarAction(
                    symbol = "cart", icon = Icons.Default.ShoppingCart, label = "Ver carrito",
                    badge = if (totalCartCount > 0) totalCartCount.toString() else null,
                    testTag = "detail_cart_button",
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
private fun DailyRotatingOffersCarouselSection(
    dailyRotatingOffers: List<DailyRotatingOffer>,
    allProducts: List<Product>,
    darkMode: Boolean,
    isArca: Boolean,
    primaryColor: Color,
    onRotateOfferDay: (Int) -> Unit,
    onTriggerDailyOfferPush: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onChangeQty: (String, Int) -> Unit,
    sectionTag: String = "daily_rotating_offers_section",
    rotateButtonTag: String = "rotate_daily_offer_button"
) {
    val view = currentPlatformView()
    val activeDayLabel = dailyRotatingOffers.firstOrNull()?.dayLabel ?: "Hoy"
    val activeThemeSubtitle = dailyRotatingOffers.firstOrNull()?.subtitle?.substringBefore("•")?.trim()
        ?: "Ofertas Especiales del Día"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
            .testTag(sectionTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Encabezado como el resto de secciones: título a la izquierda y acciones compactas a la derecha.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ofertas del día",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$activeDayLabel • $activeThemeSubtitle",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            listOf(
                Triple(Icons.Filled.Refresh, "Ver ofertas del siguiente día", rotateButtonTag),
                Triple(Icons.Filled.NotificationsActive, "Avisarme de la oferta del día", "daily_offer_push_button")
            ).forEachIndexed { index, (icon, label, tag) ->
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable {
                            if (index == 0) {
                                performCrispHaptic(view, HapticStyle.TICK)
                                onRotateOfferDay(1)
                            } else {
                                performCrispHaptic(view, HapticStyle.CONFIRM)
                                onTriggerDailyOfferPush()
                            }
                        }
                        .testTag(tag),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = label, tint = primaryColor, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Carrusel que avanza solo cada 4 s (se pausa si el usuario lo está moviendo)
        val offersRowState = rememberLazyListState()
        val dataSaver = LocalDataSaver.current
        LaunchedEffect(dailyRotatingOffers.size, dataSaver) {
            // No avanza solo con ahorro de datos o con "reducir movimiento" activado
            if (dataSaver || dailyRotatingOffers.size < 2 || com.example.platform.systemPrefersReducedMotion()) return@LaunchedEffect
            while (true) {
                delay(4000)
                if (!offersRowState.isScrollInProgress) {
                    val next = (offersRowState.firstVisibleItemIndex + 1) % dailyRotatingOffers.size
                    // Desplazamiento 0: el contentPadding ya deja el margen de 14 dp. Un desplazamiento
                    // negativo empujaba la tarjeta y dejaba asomar un trozo cortado de la anterior.
                    offersRowState.animateScrollToItem(next)
                }
            }
        }
        // Margen final = ancho visible − tarjeta − margen inicial: así hasta la última tarjeta puede
        // quedar alineada a la izquierda. Sin esto, al llegar al final la fila no podía desplazarse
        // más y dejaba la tarjeta anterior cortada en el borde.
        var offersRowWidthPx by remember { mutableIntStateOf(0) }
        // El margen izquierdo va fuera de la fila (padding), no dentro (contentPadding): así la tarjeta
        // anterior no asoma por ese margen al desplazarse.
        val offersEndPadding = with(LocalDensity.current) { (offersRowWidthPx.toDp() - 220.dp).coerceAtLeast(14.dp) }
        LazyRow(
            state = offersRowState,
            flingBehavior = rememberSnapFlingBehavior(offersRowState),
            contentPadding = PaddingValues(end = offersEndPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp).onSizeChanged { offersRowWidthPx = it.width }
        ) {
            items(
                items = dailyRotatingOffers,
                key = { it.id },
                contentType = { "daily_offer_card" }
            ) { offer ->
                val prod = allProducts.find { it.sku == offer.sku }
                val categoryBadge = prod?.category ?: offer.subtitle.substringAfter("•", "").trim()
                TranslucentCard(
                    darkMode = darkMode,
                    isArca = isArca,
                    cornerRadius = 20.dp,
                    modifier = Modifier
                        .width(220.dp)
                        .testTag("daily_offer_card_${offer.sku}"),
                    onClick = { onOpenProduct(offer.sku) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = primaryColor
                            ) {
                                Text(
                                    text = "-${offer.discountPct}% ${offer.dayLabel.uppercase()}",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "RD$${offer.originalPrice}",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (prod != null) {
                                    ProductArtworkView(
                                        product = prod,
                                        cornerRadius = 12.dp,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                if (categoryBadge.isNotBlank()) {
                                    Text(
                                        text = categoryBadge,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = primaryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = offer.title,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RD$${offer.offerPrice}",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = primaryColor
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor)
                                            .clickable {
                                                HapticFeedbackManager.onAddToCart(view)
                                                onChangeQty(offer.sku, 1)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Agregar oferta",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (dailyRotatingOffers.size > 1) {
            val activeDot by remember { derivedStateOf { offersRowState.firstVisibleItemIndex } }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                dailyRotatingOffers.indices.forEach { index ->
                    val selected = index == activeDot
                    val dotWidth by animateDpAsState(if (selected) 18.dp else 6.dp, label = "offer_dot")
                    Box(
                        Modifier
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(if (selected) primaryColor else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
    }
}

