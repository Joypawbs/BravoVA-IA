package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKOverlayProtocol
import platform.MapKit.MKOverlayRenderer
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKPolyline
import platform.MapKit.MKPolylineRenderer
import platform.MapKit.MKMapPointForCoordinate
import platform.MapKit.MKMapRectUnion
import platform.MapKit.MKMapRectMake
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIUserInterfaceStyle
import platform.darwin.NSObject
import kotlinx.cinterop.useContents

actual fun nativeDeliveryMapAvailable(): Boolean = true

private fun Color.toUi(): UIColor =
    UIColor.colorWithRed(red.toDouble(), green.toDouble(), blue.toDouble(), alpha.toDouble())

/** Dibuja la ruta y colorea los marcadores (sucursal, casa, repartidor). */
private class DeliveryMapDelegate : NSObject(), MKMapViewDelegateProtocol {
    var routeColor: UIColor = UIColor.systemGreenColor
    var doneRoute: MKPolyline? = null
    var branchPin: MKPointAnnotation? = null
    var homePin: MKPointAnnotation? = null

    @kotlinx.cinterop.ObjCSignatureOverride
    override fun mapView(mapView: MKMapView, rendererForOverlay: MKOverlayProtocol): MKOverlayRenderer {
        val line = rendererForOverlay as? MKPolyline ?: return MKOverlayRenderer(rendererForOverlay)
        return MKPolylineRenderer(line).apply {
            val done = line == doneRoute
            strokeColor = if (done) routeColor.colorWithAlphaComponent(0.45) else routeColor
            lineWidth = if (done) 4.0 else 5.5
            lineCap = platform.CoreGraphics.CGLineCap.kCGLineCapRound
        }
    }

    @kotlinx.cinterop.ObjCSignatureOverride
    override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {
        val point = viewForAnnotation as? MKPointAnnotation ?: return null
        val id = when (point) { branchPin -> "branch"; homePin -> "home"; else -> "driver" }
        val view = (mapView.dequeueReusableAnnotationViewWithIdentifier(id) as? MKMarkerAnnotationView)
            ?: MKMarkerAnnotationView(annotation = point, reuseIdentifier = id)
        view.annotation = point
        when (id) {
            "branch" -> { view.markerTintColor = UIColor.systemRedColor; view.glyphText = "B" }
            "home" -> { view.markerTintColor = UIColor.systemBlueColor; view.glyphText = "⌂" }
            else -> { view.markerTintColor = routeColor; view.glyphText = "🛵"; view.displayPriority = 1000f }
        }
        return view
    }
}

private class DeliveryMapHolder {
    val map = MKMapView()
    val delegate = DeliveryMapDelegate()
    val branchPin = MKPointAnnotation()
    val homePin = MKPointAnnotation()
    val driverPin = MKPointAnnotation()
    var framed = false

    init {
        delegate.branchPin = branchPin
        delegate.homePin = homePin
        map.delegate = delegate
        map.showsCompass = false
        map.pointOfInterestFilter = null
        map.addAnnotations(listOf(branchPin, homePin, driverPin))
    }
}

private fun coord(p: DeliveryMapPin): CValue<CLLocationCoordinate2D> = CLLocationCoordinate2DMake(p.lat, p.lng)

private fun polyline(a: DeliveryMapPin, b: DeliveryMapPin): MKPolyline = memScoped {
    val pts = allocArray<CLLocationCoordinate2D>(2)
    pts[0].latitude = a.lat; pts[0].longitude = a.lng
    pts[1].latitude = b.lat; pts[1].longitude = b.lng
    MKPolyline.polylineWithCoordinates(pts, 2u)
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeDeliveryMap(
    branch: DeliveryMapPin,
    home: DeliveryMapPin,
    driver: DeliveryMapPin,
    routeColor: Color,
    darkMode: Boolean,
    modifier: Modifier
) {
    val holder = remember { DeliveryMapHolder() }
    UIKitView(
        factory = { holder.map },
        modifier = modifier,
        update = { map ->
            holder.delegate.routeColor = routeColor.toUi()
            map.overrideUserInterfaceStyle =
                if (darkMode) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
            holder.branchPin.setTitle(branch.title); holder.branchPin.setCoordinate(coord(branch))
            holder.homePin.setTitle(home.title); holder.homePin.setCoordinate(coord(home))
            holder.driverPin.setTitle(driver.title); holder.driverPin.setCoordinate(coord(driver))

            map.removeOverlays(map.overlays)
            val done = polyline(branch, driver)
            holder.delegate.doneRoute = done
            map.addOverlay(done)
            map.addOverlay(polyline(driver, home))

            if (!holder.framed) {
                holder.framed = true
                val rect = listOf(branch, home, driver)
                    .map { MKPointAnnotationRect(it) }
                    .reduce { acc, r -> MKMapRectUnion(acc, r) }
                map.setVisibleMapRect(rect, UIEdgeInsetsMake(70.0, 50.0, 70.0, 50.0), animated = false)
            }
        }
    )
}

private fun MKPointAnnotationRect(p: DeliveryMapPin) =
    MKMapPointForCoordinate(coord(p)).useContents { MKMapRectMake(x, y, 0.0, 0.0) }
