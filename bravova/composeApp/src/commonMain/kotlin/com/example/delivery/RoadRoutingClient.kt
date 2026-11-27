package com.example.delivery

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pide la ruta real por calles a OSRM (motor de rutas libre sobre OpenStreetMap).
 *
 * Para qué: que el repartidor siga calles reales y que el ETA use la distancia real por
 * carretera y la velocidad de cada calle. Si no hay red o el servicio falla, se usa
 * [RoadRoute.fallback] y la app sigue funcionando.
 */
object RoadRoutingClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val client by lazy {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 8_000
                connectTimeoutMillis = 5_000
            }
        }
    }
    private val cache = HashMap<String, RoadRoute>()

    suspend fun route(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): RoadRoute {
        val key = listOf(fromLat, fromLng, toLat, toLng).joinToString(",") { ((it * 10_000).toLong()).toString() }
        cache[key]?.let { return it }
        val fetched = runCatching { fetchOsrm(fromLat, fromLng, toLat, toLng) }.getOrNull()
        val route = fetched ?: RoadRoute.fallback(fromLat, fromLng, toLat, toLng)
        if (fetched != null) cache[key] = route
        return route
    }

    private suspend fun fetchOsrm(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): RoadRoute? {
        val url = "https://router.project-osrm.org/route/v1/driving/" +
            "$fromLng,$fromLat;$toLng,$toLat?overview=full&geometries=geojson&steps=true"
        val resp = client.get(url)
        if (!resp.status.isSuccess()) return null
        return parseOsrm(resp.bodyAsText())
    }

    /** Convierte la respuesta de OSRM en [RoadRoute]. Público para poder probarlo sin red. */
    fun parseOsrm(body: String): RoadRoute? {
        val root = json.parseToJsonElement(body).jsonObject
        if (root["code"]?.jsonPrimitive?.contentOrNull != "Ok") return null
        val route = root["routes"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        val coords = route["geometry"]?.jsonObject?.get("coordinates")?.jsonArray ?: return null
        val points = coords.mapNotNull { c ->
            val arr = c.jsonArray
            val lng = arr.getOrNull(0)?.jsonPrimitive?.doubleOrNull
            val lat = arr.getOrNull(1)?.jsonPrimitive?.doubleOrNull
            if (lat != null && lng != null) lat to lng else null
        }
        if (points.size < 2) return null

        val legs = ArrayList<RouteLeg>()
        var acc = 0.0
        route["legs"]?.jsonArray?.forEach { leg ->
            leg.jsonObject["steps"]?.jsonArray?.forEach { stepEl ->
                val step = stepEl as? JsonObject ?: return@forEach
                val distKm = (step["distance"]?.jsonPrimitive?.doubleOrNull ?: 0.0) / 1000.0
                val durH = (step["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0) / 3600.0
                if (distKm <= 0.0) return@forEach
                val name = step["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: step["ref"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: "Calle sin nombre"
                // OSRM da la velocidad legal/típica sin tráfico; una moto de reparto en la
                // ciudad va algo más lenta, así que se limita a un rango urbano realista.
                val speed = (if (durH > 0) distKm / durH else 26.0).coerceIn(12.0, 55.0) * 0.85
                val last = legs.lastOrNull()
                if (last != null && last.street == name) {
                    // Une pasos seguidos en la misma calle, ponderando la velocidad por distancia.
                    val d1 = last.endKm - last.startKm
                    val merged = (last.speedKmh * d1 + speed * distKm) / (d1 + distKm)
                    legs[legs.lastIndex] = last.copy(endKm = acc + distKm, speedKmh = merged)
                } else {
                    legs += RouteLeg(acc, acc + distKm, name, speed)
                }
                acc += distKm
            }
        }
        return RoadRoute.fromPoints(points, legs, isStreetRouted = true)
    }
}
