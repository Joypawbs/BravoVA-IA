package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Un punto del mapa de entrega (sucursal, casa o repartidor). */
data class DeliveryMapPin(val lat: Double, val lng: Double, val title: String)

/**
 * ¿Hay mapa real disponible en esta plataforma?
 * - iPhone: siempre (Apple Maps / MapKit no necesita clave).
 * - Android: solo si la app se compiló con una clave de Google Maps (MAPS_API_KEY).
 * Si no hay, se usa el mapa ilustrado de siempre.
 */
expect fun nativeDeliveryMapAvailable(): Boolean

/**
 * Mapa real del seguimiento del pedido: sucursal, casa, repartidor y la ruta entre ellos.
 * Usa Apple Maps en iOS y Google Maps en Android.
 */
@Composable
expect fun NativeDeliveryMap(
    branch: DeliveryMapPin,
    home: DeliveryMapPin,
    driver: DeliveryMapPin,
    routeColor: Color,
    darkMode: Boolean,
    modifier: Modifier = Modifier
)
