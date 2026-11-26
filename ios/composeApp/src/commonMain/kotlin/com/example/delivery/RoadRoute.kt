package com.example.delivery

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ruta por calles entre la sucursal y el cliente.
 *
 * Para qué: el mapa dibuja esta línea (no una recta) y el repartidor avanza sobre ella.
 * Cómo: [points] son vértices (lat, lng); [cumKm] guarda la distancia acumulada hasta cada
 * vértice, así ubicar al repartidor en "X km recorridos" es una búsqueda + interpolación.
 * Cada [RouteLeg] es un tramo con nombre de calle y velocidad media (distancia ÷ duración
 * que devuelve el enrutador), y el ETA sale de sumar tiempo = distancia ÷ velocidad.
 */
data class RoadRoute(
    val points: List<Pair<Double, Double>>,
    val cumKm: List<Double>,
    val legs: List<RouteLeg>,
    val isStreetRouted: Boolean
) {
    val totalKm: Double get() = cumKm.lastOrNull() ?: 0.0

    /** Posición (lat, lng) tras recorrer [km] desde la sucursal. */
    fun pointAt(km: Double): Pair<Double, Double> {
        if (points.isEmpty()) return 0.0 to 0.0
        if (km <= 0.0 || points.size == 1) return points.first()
        if (km >= totalKm) return points.last()
        var lo = 0
        var hi = cumKm.lastIndex
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (cumKm[mid] <= km) lo = mid else hi = mid
        }
        val span = cumKm[hi] - cumKm[lo]
        val t = if (span <= 1e-9) 0.0 else (km - cumKm[lo]) / span
        val a = points[lo]
        val b = points[hi]
        return (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)
    }

    /** Tramo de calle en el que está el kilómetro [km]. */
    fun legAt(km: Double): RouteLeg? =
        legs.firstOrNull { km < it.endKm } ?: legs.lastOrNull()

    /** Minutos que faltan desde [km]: suma de (distancia restante de cada tramo ÷ su velocidad). */
    fun remainingMinutes(km: Double, trafficFactor: Double = 1.0): Double {
        var minutes = 0.0
        for (leg in legs) {
            if (leg.endKm <= km) continue
            val from = max(km, leg.startKm)
            val dist = leg.endKm - from
            if (dist <= 0) continue
            minutes += dist / leg.speedKmh.coerceAtLeast(5.0) * 60.0
        }
        return minutes * trafficFactor
    }

    /** Vértices desde [km] hasta el final (la parte de la ruta que aún falta). */
    fun remainingPath(km: Double): List<Pair<Double, Double>> {
        if (points.isEmpty()) return emptyList()
        val out = ArrayList<Pair<Double, Double>>()
        out += pointAt(km)
        for (i in points.indices) if (cumKm[i] > km) out += points[i]
        return out
    }

    /** Vértices desde el inicio hasta [km] (la parte ya recorrida). */
    fun travelledPath(km: Double): List<Pair<Double, Double>> {
        if (points.isEmpty()) return emptyList()
        val out = ArrayList<Pair<Double, Double>>()
        for (i in points.indices) if (cumKm[i] < km) out += points[i]
        out += pointAt(km)
        return out
    }

    companion object {
        /** Construye la ruta calculando distancias acumuladas entre vértices. */
        fun fromPoints(
            points: List<Pair<Double, Double>>,
            legs: List<RouteLeg>,
            isStreetRouted: Boolean
        ): RoadRoute {
            val cum = ArrayList<Double>(points.size)
            var acc = 0.0
            points.forEachIndexed { i, p ->
                if (i > 0) acc += haversine(points[i - 1].first, points[i - 1].second, p.first, p.second)
                cum += acc
            }
            // Ajusta los tramos para que cubran exactamente la geometría.
            val total = acc
            val fixedLegs = if (legs.isEmpty()) {
                listOf(RouteLeg(0.0, total, "Ruta hacia tu dirección", 26.0))
            } else {
                val legTotal = legs.last().endKm.takeIf { it > 0 } ?: total
                val k = if (legTotal > 0) total / legTotal else 1.0
                legs.map { it.copy(startKm = it.startKm * k, endKm = min(total, it.endKm * k)) }
            }
            return RoadRoute(points, cum, fixedLegs, isStreetRouted)
        }

        /**
         * Respaldo sin conexión: una curva suave en "L" (como calles en cuadrícula) en vez de
         * una recta. La velocidad media urbana es 26 km/h.
         */
        fun fallback(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): RoadRoute {
            val corner = fromLat to toLng
            val pts = ArrayList<Pair<Double, Double>>()
            val steps = 12
            for (i in 0..steps) {
                val t = i / steps.toDouble()
                pts += (fromLat + (corner.first - fromLat) * t) to (fromLng + (corner.second - fromLng) * t)
            }
            for (i in 1..steps) {
                val t = i / steps.toDouble()
                pts += (corner.first + (toLat - corner.first) * t) to (corner.second + (toLng - corner.second) * t)
            }
            val half = haversine(fromLat, fromLng, corner.first, corner.second)
            val total = half + haversine(corner.first, corner.second, toLat, toLng)
            val legs = listOf(
                RouteLeg(0.0, half, "Avenida principal desde la sucursal", 32.0),
                RouteLeg(half, total, "Calles residenciales hacia tu dirección", 22.0)
            )
            return fromPoints(pts, legs, isStreetRouted = false)
        }

        private const val DEG = kotlin.math.PI / 180.0

        fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6371.0
            val dLat = (lat2 - lat1) * DEG
            val dLng = (lng2 - lng1) * DEG
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * DEG) * cos(lat2 * DEG) * sin(dLng / 2) * sin(dLng / 2)
            return r * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}

/** Tramo de la ruta: de [startKm] a [endKm], por la calle [street], a [speedKmh] de media. */
data class RouteLeg(
    val startKm: Double,
    val endKm: Double,
    val street: String,
    val speedKmh: Double
)

/**
 * Ruta activa compartida entre el ViewModel (que la calcula) y el mapa (que la dibuja).
 * Así el mapa no necesita recibir la ruta por todos los parámetros de la pantalla.
 */
object DeliveryRouteStore {
    val route = kotlinx.coroutines.flow.MutableStateFlow<RoadRoute?>(null)
}
