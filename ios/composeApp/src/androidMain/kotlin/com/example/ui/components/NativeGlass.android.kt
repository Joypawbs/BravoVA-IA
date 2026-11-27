package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.platform.AppContext

/** En Android el vidrio se dibuja con Compose (LiquidGlass); no hay vista nativa. */
actual fun nativeLiquidGlassAvailable(): Boolean = false

@Composable
actual fun NativeGlassTopBar(spec: GlassTopBarSpec, onBack: () -> Unit, onAction: (Int) -> Unit, modifier: Modifier) {}

@Composable
actual fun NativeGlassDock(
    symbols: List<String>,
    labels: List<String>,
    selectedIndex: Int,
    accent: Color,
    darkMode: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    minimized: Boolean,
    onSearch: () -> Unit
) {}

@Composable
actual fun NativeGlassCartBar(count: Int, total: String, accent: Color, darkMode: Boolean, onTap: () -> Unit, modifier: Modifier) {}

@Composable
actual fun NativeGlassToast(text: String, symbol: String, accent: Color, darkMode: Boolean, modifier: Modifier) {}

@Composable
actual fun NativeGlassSearchField(
    text: String,
    placeholder: String,
    accent: Color,
    darkMode: Boolean,
    trailingSymbol: String,
    focusSignal: Int,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFocus: (Boolean) -> Unit,
    onTrailing: () -> Unit,
    modifier: Modifier
) {}

@Composable
actual fun NativeGlassBudgetPanel(
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    chips: List<String>,
    accent: Color,
    darkMode: Boolean,
    busy: Boolean,
    onChange: (Int) -> Unit,
    onAction: (Int) -> Unit,
    modifier: Modifier
) {}

/** Android: el pedido en curso se muestra como notificación de progreso en vivo (Live Update en Android 16). */
actual object PlatformExtras {
    private const val CHANNEL = "bravova_pedido_en_vivo"
    private const val ID = 4242
    private var orderId = ""
    private var branch = ""

    actual fun presentQuickView(
        title: String, subtitle: String, price: String, imageUrl: String, isFavorite: Boolean, accentArgb: Int,
        onAdd: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit
    ): Boolean = false

    actual fun startDeliveryActivity(orderId: String, branch: String, stage: String, etaMinutes: Int, progress: Double) {
        this.orderId = orderId
        this.branch = branch
        post(stage, etaMinutes, progress)
    }

    actual fun updateDeliveryActivity(stage: String, etaMinutes: Int, progress: Double) {
        if (orderId.isNotEmpty()) post(stage, etaMinutes, progress)
    }

    actual fun endDeliveryActivity() {
        runCatching { NotificationManagerCompat.from(AppContext.instance).cancel(ID) }
        orderId = ""
    }

    @SuppressLint("MissingPermission")
    private fun post(stage: String, etaMinutes: Int, progress: Double) {
        val context = AppContext.instance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Pedido en curso", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Progreso en vivo de tu pedido BravoVa"
                    setSound(null, null)
                }
            )
        }
        val open = PendingIntent.getActivity(
            context, ID,
            // Tocar la notificación abre el seguimiento del pedido
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("bravova://pedido"), context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val done = progress >= 1.0
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(if (done) "Pedido #$orderId entregado" else "$stage • llega en ~$etaMinutes min")
            .setContentText("Desde $branch")
            .setSubText("#$orderId")
            .setOnlyAlertOnce(true)
            .setOngoing(!done)
            .setAutoCancel(done)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setColor(0xFFE1251B.toInt())
            .setContentIntent(open)
            .setProgress(100, (progress * 100).toInt().coerceIn(0, 100), false)
        // Android 16: pide mostrarlo como "Live Update" (chip en la barra de estado)
        builder.extras.putBoolean("android.requestPromotedOngoing", true)
        runCatching { NotificationManagerCompat.from(context).notify(ID, builder.build()) }
    }
}
