package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Android siempre tiene mapa real:
 * - Con MAPS_API_KEY usa Google Maps.
 * - Sin clave usa MapLibre con los mapas libres de OpenFreeMap (sin cuenta, sin cobro).
 */
actual fun nativeDeliveryMapAvailable(): Boolean = true

private val hasGoogleKey: Boolean get() = BuildConfig.MAPS_API_KEY.isNotBlank()

@Composable
actual fun NativeDeliveryMap(
    branch: DeliveryMapPin,
    home: DeliveryMapPin,
    driver: DeliveryMapPin,
    routeColor: Color,
    darkMode: Boolean,
    modifier: Modifier,
    travelledPath: List<DeliveryMapPin>,
    remainingPath: List<DeliveryMapPin>
) {
    val done = travelledPath.takeIf { it.size >= 2 } ?: listOf(branch, driver)
    val left = remainingPath.takeIf { it.size >= 2 } ?: listOf(driver, home)
    if (hasGoogleKey) {
        GoogleDeliveryMap(branch, home, driver, routeColor, darkMode, modifier, done, left)
    } else {
        LibreDeliveryMap(branch, home, driver, routeColor, darkMode, modifier, done, left)
    }
}

// ───────────────────────── MapLibre + OpenFreeMap ─────────────────────────

private const val OFM_LIGHT = "https://tiles.openfreemap.org/styles/liberty"
private const val OFM_DARK = "https://tiles.openfreemap.org/styles/dark"
private const val SRC_ROUTE = "bravova-route"
private const val SRC_PINS = "bravova-pins"

private fun routeCollection(donePath: List<DeliveryMapPin>, leftPath: List<DeliveryMapPin>): FeatureCollection {
    val done = Feature.fromGeometry(
        LineString.fromLngLats(donePath.map { Point.fromLngLat(it.lng, it.lat) })
    ).apply { addStringProperty("part", "done") }
    val left = Feature.fromGeometry(
        LineString.fromLngLats(leftPath.map { Point.fromLngLat(it.lng, it.lat) })
    ).apply { addStringProperty("part", "left") }
    return FeatureCollection.fromFeatures(listOf(done, left))
}

private fun pinsCollection(branch: DeliveryMapPin, home: DeliveryMapPin, driver: DeliveryMapPin): FeatureCollection =
    FeatureCollection.fromFeatures(
        listOf(
            Feature.fromGeometry(Point.fromLngLat(branch.lng, branch.lat)).apply { addStringProperty("kind", "branch") },
            Feature.fromGeometry(Point.fromLngLat(home.lng, home.lat)).apply { addStringProperty("kind", "home") },
            Feature.fromGeometry(Point.fromLngLat(driver.lng, driver.lat)).apply { addStringProperty("kind", "driver") }
        )
    )

@Composable
private fun LibreDeliveryMap(
    branch: DeliveryMapPin,
    home: DeliveryMapPin,
    driver: DeliveryMapPin,
    routeColor: Color,
    darkMode: Boolean,
    modifier: Modifier,
    donePath: List<DeliveryMapPin>,
    leftPath: List<DeliveryMapPin>
) {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context.applicationContext)
        // onStart/onResume los llama el observador del ciclo de vida de abajo (al registrarse recibe
        // los eventos que ya pasaron), así no se llaman dos veces
        MapView(context).apply { onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }

    // Pausa el mapa cuando la app pasa a segundo plano y lo reanuda al volver (ahorra batería)
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    DisposableEffect(mapView) {
        mapView.getMapAsync { m ->
            m.uiSettings.isRotateGesturesEnabled = false
            m.uiSettings.isTiltGesturesEnabled = false
            m.uiSettings.isCompassEnabled = false
            map = m
        }
        onDispose {
            runCatching {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        }
    }

    // Estilo claro u oscuro según el tema; al cargar, agrega ruta y pines
    LaunchedEffect(map, darkMode) {
        val m = map ?: return@LaunchedEffect
        style = null
        m.setStyle(Style.Builder().fromUri(if (darkMode) OFM_DARK else OFM_LIGHT)) { loaded ->
            val route = routeColor.toArgb()
            loaded.addSource(GeoJsonSource(SRC_ROUTE, routeCollection(donePath, leftPath)))
            loaded.addSource(GeoJsonSource(SRC_PINS, pinsCollection(branch, home, driver)))
            loaded.addLayer(
                LineLayer("bravova-route-line", SRC_ROUTE).withProperties(
                    PropertyFactory.lineColor(route),
                    PropertyFactory.lineWidth(6f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineOpacity(
                        org.maplibre.android.style.expressions.Expression.match(
                            org.maplibre.android.style.expressions.Expression.get("part"),
                            org.maplibre.android.style.expressions.Expression.literal(0.45f),
                            org.maplibre.android.style.expressions.Expression.stop("left", 1f)
                        )
                    )
                )
            )
            loaded.addLayer(
                CircleLayer("bravova-pins-dot", SRC_PINS).withProperties(
                    PropertyFactory.circleRadius(9f),
                    PropertyFactory.circleStrokeWidth(3f),
                    PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                    PropertyFactory.circleColor(
                        org.maplibre.android.style.expressions.Expression.match(
                            org.maplibre.android.style.expressions.Expression.get("kind"),
                            org.maplibre.android.style.expressions.Expression.color(route),
                            org.maplibre.android.style.expressions.Expression.stop(
                                "branch",
                                org.maplibre.android.style.expressions.Expression.color(0xFFE31837.toInt())
                            ),
                            org.maplibre.android.style.expressions.Expression.stop(
                                "home",
                                org.maplibre.android.style.expressions.Expression.color(0xFF2563EB.toInt())
                            )
                        )
                    )
                )
            )
            style = loaded
        }
        val bounds = org.maplibre.android.geometry.LatLngBounds.Builder()
            .include(org.maplibre.android.geometry.LatLng(branch.lat, branch.lng))
            .include(org.maplibre.android.geometry.LatLng(home.lat, home.lng))
            .include(org.maplibre.android.geometry.LatLng(driver.lat, driver.lng))
            .build()
        runCatching {
            m.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    // El repartidor se mueve: actualiza ruta y pines sin recargar el estilo
    LaunchedEffect(style, driver.lat, driver.lng, leftPath.size) {
        val s = style ?: return@LaunchedEffect
        (s.getSource(SRC_ROUTE) as? GeoJsonSource)?.setGeoJson(routeCollection(donePath, leftPath))
        (s.getSource(SRC_PINS) as? GeoJsonSource)?.setGeoJson(pinsCollection(branch, home, driver))
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

// ───────────────────────── Google Maps (con clave) ─────────────────────────

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
private fun GoogleDeliveryMap(
    branch: DeliveryMapPin,
    home: DeliveryMapPin,
    driver: DeliveryMapPin,
    routeColor: Color,
    darkMode: Boolean,
    modifier: Modifier,
    donePath: List<DeliveryMapPin>,
    leftPath: List<DeliveryMapPin>
) {
    val b = LatLng(branch.lat, branch.lng)
    val h = LatLng(home.lat, home.lng)
    val d = LatLng(driver.lat, driver.lng)
    val camera = rememberCameraPositionState()
    val driverState = remember { MarkerState(d) }
    driverState.position = d

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
        Polyline(points = donePath.map { LatLng(it.lat, it.lng) }, color = routeColor.copy(alpha = 0.45f), width = 12f)
        Polyline(points = leftPath.map { LatLng(it.lat, it.lng) }, color = routeColor, width = 14f)
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
