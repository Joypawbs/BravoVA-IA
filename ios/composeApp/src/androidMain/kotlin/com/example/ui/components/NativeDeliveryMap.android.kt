package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.BuildConfig
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

actual fun nativeDeliveryMapAvailable(): Boolean = BuildConfig.MAPS_API_KEY.isNotBlank()

// Estilo oscuro sencillo para Google Maps (colores de la app en modo oscuro)
private const val DARK_MAP_STYLE = """[
 {"elementType":"geometry","stylers":[{"color":"#131c2e"}]},
 {"elementType":"labels.text.fill","stylers":[{"color":"#cbd5e1"}]},
 {"elementType":"labels.text.stroke","stylers":[{"color":"#0c1320"}]},
 {"featureType":"road","elementType":"geometry","stylers":[{"color":"#283548"}]},
 {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0e2238"}]},
 {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#102a22"}]}
]"""

@Composable
actual fun NativeDeliveryMap(
    branch: DeliveryMapPin,
    home: DeliveryMapPin,
    driver: DeliveryMapPin,
    routeColor: Color,
    darkMode: Boolean,
    modifier: Modifier
) {
    val b = LatLng(branch.lat, branch.lng)
    val h = LatLng(home.lat, home.lng)
    val d = LatLng(driver.lat, driver.lng)
    val camera = rememberCameraPositionState()
    val driverState = remember { MarkerState(d) }
    driverState.position = d

    // Encuadra sucursal, casa y repartidor al abrir
    LaunchedEffect(branch, home) {
        val bounds = LatLngBounds.builder().include(b).include(h).include(d).build()
        runCatching { camera.move(CameraUpdateFactory.newLatLngBounds(bounds, 120)) }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = MapProperties(
            mapStyleOptions = if (darkMode) MapStyleOptions(DARK_MAP_STYLE) else null
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false
        )
    ) {
        Polyline(points = listOf(b, d), color = routeColor.copy(alpha = 0.45f), width = 12f)
        Polyline(points = listOf(d, h), color = routeColor, width = 14f)
        Marker(
            state = remember(b) { MarkerState(b) },
            title = branch.title,
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        )
        Marker(
            state = remember(h) { MarkerState(h) },
            title = home.title,
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
        )
        Marker(
            state = driverState,
            title = driver.title,
            icon = BitmapDescriptorFactory.defaultMarker(hueOf(routeColor))
        )
    }
}

private fun hueOf(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}
