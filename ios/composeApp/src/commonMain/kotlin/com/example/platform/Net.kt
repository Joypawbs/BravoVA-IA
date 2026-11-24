package com.example.platform

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Shared Ktor client (OkHttp engine on Android, Darwin engine on iOS). */
val sharedHttpClient: HttpClient by lazy {
    HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        expectSuccess = false
    }
}

data class HttpTextResponse(val code: Int, val body: String)

suspend fun httpGetText(url: String, headers: Map<String, String> = emptyMap()): HttpTextResponse {
    val resp = sharedHttpClient.get(url) {
        headers.forEach { (k, v) -> header(k, v) }
    }
    return HttpTextResponse(resp.status.value, resp.bodyAsText())
}

suspend fun httpPostJson(url: String, json: String, headers: Map<String, String> = emptyMap()): HttpTextResponse {
    val resp = sharedHttpClient.post(url) {
        contentType(ContentType.Application.Json)
        headers.forEach { (k, v) -> header(k, v) }
        setBody(json)
    }
    return HttpTextResponse(resp.status.value, resp.bodyAsText())
}

/** java.net.URLEncoder.encode(value, "UTF-8") equivalent (spaces become '+'). */
object URLEncoder {
    fun encode(value: String, @Suppress("UNUSED_PARAMETER") charset: String = "UTF-8"): String {
        val sb = StringBuilder()
        for (b in value.encodeToByteArray()) {
            val c = (b.toInt() and 0xFF)
            val ch = c.toChar()
            when {
                ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '.' || ch == '-' || ch == '*' || ch == '_' ->
                    sb.append(ch)
                ch == ' ' -> sb.append('+')
                else -> {
                    sb.append('%')
                    sb.append("0123456789ABCDEF"[c shr 4])
                    sb.append("0123456789ABCDEF"[c and 0x0F])
                }
            }
        }
        return sb.toString()
    }
}
