package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.FcmTopicSubscription
import com.example.model.PushNotificationCategory
import com.example.model.PushNotificationRecord
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                body = "Llévate el Salami Super Especial 1.5 lb a RD$ 315.00 + doble retorno en tu Tarjeta Bravísima por hoy.",
                timestampLabel = "Hace 18 min",
                topic = TOPIC_OFFERS_BRAVO,
                targetSku = "BRV-096",
                isRead = false
            ),
            PushNotificationRecord(
                id = "fcm_init_3",
                category = PushNotificationCategory.EXCLUSIVE_OFFER,
                title = "🐾 Flash Deal El Arca: Alimento Adulto Cordero 15 lb",
                body = "15% de ahorro directo en nutrición premium para perro adulto (RD$ 1,850.00) con entrega express gratis.",
                timestampLabel = "Hace 1 hora",
                topic = TOPIC_OFFERS_ARCA,
                targetSku = "ARC-001",
                isRead = true
            )
        )
    )
    val notifications: StateFlow<List<PushNotificationRecord>> = _notifications.asStateFlow()

    fun initialize(context: Context) {
        ensureNotificationChannels(context)
        try {
            @Suppress("DEPRECATION")
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        _fcmToken.value = token
                    } else {
                        _fcmToken.value = "fcm-bravova-${Build.MODEL.lowercase().replace(" ", "-")}-active"
                    }
                } else {
                    _fcmToken.value = "fcm-bravova-${Build.MODEL.lowercase().replace(" ", "-")}-active"
                }
            }
            _topics.value.forEach { sub ->
                if (sub.enabled) {
                    FirebaseMessaging.getInstance().subscribeToTopic(sub.topicKey)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "FCM initialization completed in local fallback mode: ${e.message}")
            _fcmToken.value = "fcm-bravova-${Build.MODEL.lowercase().replace(" ", "-")}-active"
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
                FirebaseMessaging.getInstance().subscribeToTopic(topicKey)
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topicKey)
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

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val ordersChannel = NotificationChannel(
                CHANNEL_ORDER_UPDATES,
                "Estado de Pedidos en Tiempo Real",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas en vivo de confirmación, preparación y ubicación del repartidor BravoVa"
                enableVibration(true)
            }

            val offersChannel = NotificationChannel(
                CHANNEL_EXCLUSIVE_OFFERS,
                "Ofertas Exclusivas Bravo y El Arca",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Promociones exclusivas, precios especiales en RD$ y bonos de Tarjeta Bravísima"
                enableVibration(true)
            }

            manager.createNotificationChannels(listOf(ordersChannel, offersChannel))
        }
    }

    fun dispatchPushNotification(
        context: Context,
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
                val channelId = if (category == PushNotificationCategory.ORDER_STATUS) {
                    CHANNEL_ORDER_UPDATES
                } else {
                    CHANNEL_EXCLUSIVE_OFFERS
                }

                val tapIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    if (category == PushNotificationCategory.ORDER_STATUS) {
                        putExtra(EXTRA_OPEN_SCREEN, "DELIVERY")
                        targetOrderId?.let { putExtra(EXTRA_ORDER_ID, it) }
                    } else if (!targetSku.isNullOrBlank()) {
                        putExtra(EXTRA_OPEN_SCREEN, "PRODUCT")
                        putExtra(EXTRA_PRODUCT_SKU, targetSku)
                    } else {
                        putExtra(EXTRA_OPEN_SCREEN, "HOME")
                    }
                }

                val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    record.id.hashCode(),
                    tapIntent,
                    pendingIntentFlags
                )

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setColor(0xFFD9232A.toInt())

                if (progressPct != null) {
                    builder.setProgress(100, progressPct.coerceIn(0, 100), false)
                }

                NotificationManagerCompat.from(context).notify(
                    (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                    builder.build()
                )
            } catch (se: SecurityException) {
                Log.d(TAG, "POST_NOTIFICATIONS permission not granted yet: ${se.message}")
            } catch (e: Exception) {
                Log.d(TAG, "System notification skipped: ${e.message}")
            }
        }
        return record
    }
}

class BravoFirebaseMessagingService : FirebaseMessagingService() {

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM registration token updated: $token")
        BravoPushNotificationCenter.updateFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val rawCategory = (data["category"] ?: data["type"] ?: "").lowercase()
        val category = if (rawCategory.contains("order") || rawCategory.contains("pedido")) {
            PushNotificationCategory.ORDER_STATUS
        } else {
            PushNotificationCategory.EXCLUSIVE_OFFER
        }

        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: if (category == PushNotificationCategory.ORDER_STATUS) {
                "Actualización de tu pedido BravoVa"
            } else {
                "Oferta Exclusiva Supermercados Bravo"
            }

        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: "Abre la app para ver los detalles actualizados en tiempo real."

        val orderId = data["orderId"]
        val sku = data["sku"]
        val progress = data["progress"]?.toIntOrNull()
        val topic = remoteMessage.from?.removePrefix("/topics/")
            ?: if (category == PushNotificationCategory.ORDER_STATUS) {
                BravoPushNotificationCenter.TOPIC_ORDERS_REALTIME
            } else {
                BravoPushNotificationCenter.TOPIC_OFFERS_BRAVO
            }

        BravoPushNotificationCenter.dispatchPushNotification(
            context = applicationContext,
            category = category,
            title = title,
            body = body,
            topic = topic,
            targetOrderId = orderId,
            targetSku = sku,
            progressPct = progress,
            postSystemNotification = true
        )
    }
}
