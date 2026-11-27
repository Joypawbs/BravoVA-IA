package com.example.notifications

import com.example.platform.AppContext
import com.example.platform.Log
import com.example.model.PushNotificationCategory
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private const val TAG = "BravoFCMService"

class BravoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        AppContext.init(applicationContext)
    }

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
