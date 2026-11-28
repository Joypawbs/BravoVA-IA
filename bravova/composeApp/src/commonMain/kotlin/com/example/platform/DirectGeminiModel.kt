package com.example.platform

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Elige el modelo de Brav IA. Con una clave de Google AI Studio (compilada en [AiKeys]) habla
 * directo con la API de Gemini: no depende de App Check, que en iPhone falla cuando la app se
 * instala sin firmar (Sideloadly/AltStore). Sin clave, usa Firebase AI Logic como antes.
 */
fun createAiModel(modelName: String, config: AiModelConfig = AiModelConfig()): GenerativeModelCompat =
    if (AiKeys.GEMINI.isNotBlank()) DirectGeminiModel(freeTierModel(modelName), config, AiKeys.GEMINI)
    else createGenerativeModel(modelName, config)

/**
 * Nivel gratuito de la API de Gemini (AI Studio, noviembre de 2026): 3.8 Flash permite solo
 * 5 por minuto y 20 al día; cada Flash Lite, 15 por minuto y 500 al día, con cupo propio.
 * Con 20 al día Brav IA se quedaba sin cupo enseguida, así que el principal pasa a 3.5 Flash
 * Lite y el de respaldo a 3.1 Flash Lite: ~1000 peticiones diarias entre los dos.
 */
private fun freeTierModel(requested: String): String = when (requested) {
    "gemini-3.8-flash" -> "gemini-3.5-flash-lite"
    "gemini-3.5-flash-lite" -> "gemini-3.1-flash-lite"
    else -> requested
}

/** True si Brav IA va directo a la API de Gemini (no necesita App Check). */
val isDirectAiEnabled: Boolean get() = AiKeys.GEMINI.isNotBlank()

private val aiHttp by lazy {
    HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 90_000
            socketTimeoutMillis = 45_000
        }
    }
}

private val aiJson = Json { ignoreUnknownKeys = true }

/** API REST de Gemini (generativelanguage.googleapis.com) con la misma interfaz que Firebase. */
private class DirectGeminiModel(
    private val modelName: String,
    private val config: AiModelConfig,
    private val apiKey: String
) : GenerativeModelCompat {

    private fun url(method: String) =
        "https://generativelanguage.googleapis.com/v1beta/models/$modelName:$method"

    /**
     * El esquema se manda mientras la API lo acepte. Si un modelo lo rechaza (HTTP 400), se
     * repite la petición sin él y no se vuelve a intentar: el modo JSON y parseReply bastan.
     */
    private var useSchema = config.jsonSchema != null

    @OptIn(ExperimentalEncodingApi::class)
    private fun body(content: AiContent): String = buildJsonObject {
        config.systemInstruction?.let { sys ->
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { add(buildJsonObject { put("text", sys) }) }
            }
        }
        putJsonArray("contents") {
            add(buildJsonObject {
                put("role", "user")
                put("parts", buildJsonArray {
                    content.image?.let { jpeg ->
                        add(buildJsonObject {
                            putJsonObject("inlineData") {
                                put("mimeType", "image/jpeg")
                                put("data", Base64.encode(jpeg))
                            }
                        })
                    }
                    if (content.text.isNotEmpty()) add(buildJsonObject { put("text", content.text) })
                })
            })
        }
        putJsonObject("generationConfig") {
            if (config.temperature >= 0) put("temperature", config.temperature)
            if (config.jsonOutput) put("responseMimeType", "application/json")
            if (useSchema) config.jsonSchema?.let { schema ->
                runCatching { aiJson.parseToJsonElement(schema) }.getOrNull()?.let { put("responseJsonSchema", it) }
            }
        }
    }.toString()

    /** 400 con esquema: se apaga el esquema y se reintenta una vez. */
    private suspend fun <T> withSchemaFallback(block: suspend () -> T): T = try {
        block()
    } catch (e: IllegalStateException) {
        if (useSchema && e.message?.startsWith("HTTP 400") == true) {
            useSchema = false
            block()
        } else throw e
    }

    /** Junta el texto de todas las partes del primer candidato. */
    private fun textOf(json: String): String? = runCatching {
        val root = aiJson.parseToJsonElement(json).jsonObject
        root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?.mapNotNull { (it as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull }
            ?.joinToString("")
    }.getOrNull()

    override fun generateContentStream(content: AiContent): Flow<AiResponseChunk> = flow {
        // El 400 llega antes del primer fragmento, así que reintentar no duplica texto
        withSchemaFallback { streamInto(this, content) }
    }

    private suspend fun streamInto(collector: kotlinx.coroutines.flow.FlowCollector<AiResponseChunk>, content: AiContent) {
        aiHttp.preparePost(url("streamGenerateContent") + "?alt=sse") {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(body(content))
        }.execute { response ->
            if (!response.status.isSuccess()) {
                // El código (403, 429, 404…) va en el mensaje para que la app diga el motivo
                throw IllegalStateException("HTTP ${response.status.value}: ${response.bodyAsText().take(300)}")
            }
            val channel = response.bodyAsChannel()
            while (true) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data:")) continue
                val text = textOf(line.removePrefix("data:").trim())
                if (!text.isNullOrEmpty()) collector.emit(AiResponseChunk(text))
            }
        }
    }

    override suspend fun generateContent(content: AiContent): AiResponseChunk = withSchemaFallback { generateOnce(content) }

    private suspend fun generateOnce(content: AiContent): AiResponseChunk {
        var text: String? = null
        aiHttp.preparePost(url("generateContent")) {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(body(content))
        }.execute { response ->
            val raw = response.bodyAsText()
            if (!response.status.isSuccess()) throw IllegalStateException("HTTP ${response.status.value}: ${raw.take(300)}")
            text = textOf(raw)
        }
        return AiResponseChunk(text)
    }
}
