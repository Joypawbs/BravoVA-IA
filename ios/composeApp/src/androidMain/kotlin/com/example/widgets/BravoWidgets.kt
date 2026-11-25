package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.util.Calendar

private val DAY_THEMES = listOf(
    "Domingo Familiar & Embutidos", "Lunes de Frutas & Vegetales", "Martes de Carnes & Mariscos",
    "Miércoles de Despensa & Bebidas", "Jueves de Lácteos & Panadería", "Viernes de Limpieza & Hogar",
    "Sábado de Hogar & Cuidado Personal"
)

private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context, 0,
    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

/** Widget "Ofertas del día": muestra el tema de hoy con los colores dinámicos del teléfono. */
class OffersWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val theme = DAY_THEMES[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_offers).apply {
                setTextViewText(R.id.widget_theme, theme)
                setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            }
            manager.updateAppWidget(id, views)
        }
    }
}

/** Widget "Bravísima": acceso directo a la tarjeta. */
class BravisimaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_bravisima).apply {
                setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            }
            manager.updateAppWidget(id, views)
        }
    }
}
