package com.example.notifications

import com.example.platform.Date
import com.example.platform.Locale
import com.example.platform.Log
import com.example.platform.PlatformContext
import com.example.platform.PlatformInfo
import com.example.platform.PushPlatform
import com.example.platform.SimpleDateFormat
import com.example.platform.System

import com.example.model.FcmTopicSubscription
import com.example.model.PushNotificationCategory
import com.example.model.PushNotificationRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "BravoFCMService"

object BravoPushNotificationCenter {
    const val CHANNEL_ORDER_UPDATES = "bravova_order_updates"
    const val CHANNEL_EXCLUSIVE_OFFERS = "bravova_exclusive_offers"

    const val TOPIC_OFFERS_BRAVO = "ofertas_exclusivas_bravo"
    const val TOPIC_OFFERS_ARCA = "ofertas_arca_petshop"
    const val TOPIC_ORDERS_REALTIME = "pedidos_tiempo_real"

    const val EXTRA_OPEN_SCREEN = "extra_fcm_open_screen"
    const val EXTRA_PRODUCT_SKU = "extra_fcm_product_sku"
    const val EXTRA_ORDER_ID = "extra_fcm_order_id"

    private val _fcmToken = MutableStateFlow("Inicializando token FCM...")
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()

    private val _topics = MutableStateFlow(
        listOf(
            FcmTopicSubscription(
                topicKey = TOPIC_ORDERS_REALTIME,
                title = "Estado de Pedidos en Tiempo Real",
                subtitle = "Confirmación, empaque, salida del repartidor y llegada a tu puerta",
                enabled = true
            ),
            FcmTopicSubscription(
                topicKey = TOPIC_OFFERS_BRAVO,
                title = "Ofertas Exclusivas Supermercados Bravo",
                subtitle = "Descuentos relámpago en carnes, frescos, despensa y retornos Bravísima",
                enabled = true
            ),
            FcmTopicSubscription(
                topicKey = TOPIC_OFFERS_ARCA,
                title = "Promociones El Arca Petshop",
                subtitle = "Alertas de alimentos premium, snacks y farmacia veterinaria",
                enabled = true
            )
        )
    )
    val topics: StateFlow<List<FcmTopicSubscription>> = _topics.asStateFlow()

    private val _notifications = MutableStateFlow(
        listOf(
            PushNotificationRecord(
                id = "fcm_init_1",
                category = PushNotificationCategory.ORDER_STATUS,
                title = "🛵 Pedido #ORD-88412 En Ruta (34 km/h)",
                body = "Carlos Rodríguez salió de Bravo Winston Churchill y está a 6 min de tu dirección en Piantini.",
                timestampLabel = "Hace 2 min",
                topic = TOPIC_ORDERS_REALTIME,
                targetOrderId = "ORD-88412",
                progressPct = 72,
                isRead = false
            ),
            PushNotificationRecord(
                id = "fcm_init_2",
                category = PushNotificationCategory.EXCLUSIVE_OFFER,
                title = "🔥 Oferta Exclusiva Bravo: Salami Super Especial Induveca",
                body = "Llévate el Salami Super Especial 1.5 lb a RD$315.00 + doble retorno en tu Tarjeta Bravísima por hoy.",
                timestampLabel = "Hace 18 min",
                topic = TOPIC_OFFERS_BRAVO,
                targetSku = "BRV-096",
                isRead = false
            ),
            PushNotificationRecord(
                id = "fcm_init_3",
                category = PushNotificationCategory.EXCLUSIVE_OFFER,
                title = "🐾 Flash Deal El Arca: Alimento Adulto Cordero 15 lb",
                body = "15% de ahorro directo en nutrición premium para perro adulto (RD$1,850.00) con entrega express gratis.",
                timestampLabel = "Hace 1 hora",
                topic = TOPIC_OFFERS_ARCA,
                targetSku = "ARC-001",
                isRead = true
            )
        )
    )
    val notifications: StateFlow<List<PushNotificationRecord>> = _notifications.asStateFlow()

    fun initialize(context: PlatformContext) {
        ensureNotificationChannels(context)
        val fallbackToken = "fcm-bravova-${PlatformInfo.deviceModel.lowercase().replace(" ", "-")}-active"
        try {
            PushPlatform.fetchToken { token ->
                _fcmToken.value = if (!token.isNullOrBlank()) token else fallbackToken
            }
            _topics.value.forEach { sub ->
                if (sub.enabled) {
                    PushPlatform.subscribe(sub.topicKey)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "FCM initialization completed in local fallback mode: ${e.message}")
            _fcmToken.value = fallbackToken
        }
    }

    fun updateFcmToken(token: String) {
        if (token.isNotBlank()) {
            _fcmToken.value = token
        }
    }

    fun setTopicEnabled(topicKey: String, enabled: Boolean) {
        _topics.value = _topics.value.map { item ->
            if (item.topicKey == topicKey) item.copy(enabled = enabled) else item
        }
        try {
            if (enabled) {
                PushPlatform.subscribe(topicKey)
            } else {
                PushPlatform.unsubscribe(topicKey)
            }
        } catch (e: Exception) {
            Log.d(TAG, "FCM topic subscription local update for $topicKey: ${e.message}")
        }
    }

    fun markAllAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun markNotificationRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    fun hasNotificationPermission(context: PlatformContext): Boolean =
        PushPlatform.hasNotificationPermission(context)

    fun ensureNotificationChannels(context: PlatformContext) {
        PushPlatform.ensureChannels(context)
    }

    fun dispatchPushNotification(
        context: PlatformContext,
        category: PushNotificationCategory,
        title: String,
        body: String,
        topic: String = if (category == PushNotificationCategory.ORDER_STATUS) TOPIC_ORDERS_REALTIME else TOPIC_OFFERS_BRAVO,
        targetOrderId: String? = null,
        targetSku: String? = null,
        progressPct: Int? = null,
        postSystemNotification: Boolean = true
    ): PushNotificationRecord {
        ensureNotificationChannels(context)
        val timeFmt = SimpleDateFormat("h:mm a", Locale.US).format(Date())
        val record = PushNotificationRecord(
            id = "fcm_${System.nanoTime()}",
            category = category,
            title = title,
            body = body,
            timestampLabel = "Ahora • $timeFmt",
            topic = topic,
            targetOrderId = targetOrderId,
            targetSku = targetSku,
            progressPct = progressPct,
            isRead = false
        )
        _notifications.value = listOf(record) + _notifications.value.take(24)

        if (postSystemNotification && hasNotificationPermission(context)) {
            try {
                val openScreen = when {
                    category == PushNotificationCategory.ORDER_STATUS -> "DELIVERY"
                    !targetSku.isNullOrBlank() -> "PRODUCT"
                    else -> "HOME"
                }
                PushPlatform.postSystemNotification(
                    context = context,
                    id = record.id,
                    isOrderUpdate = category == PushNotificationCategory.ORDER_STATUS,
                    title = title,
                    body = body,
                    openScreen = openScreen,
                    targetOrderId = targetOrderId,
                    targetSku = targetSku,
                    progressPct = progressPct
                )
            } catch (e: Exception) {
                Log.d(TAG, "System notification skipped: ${e.message}")
            }
        }
        return record
    }
}

