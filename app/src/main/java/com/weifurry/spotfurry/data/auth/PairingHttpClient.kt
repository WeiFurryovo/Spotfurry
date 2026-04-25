package com.weifurry.spotfurry.data.auth

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class PairingHttpClient(
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
        },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun <T> requestJson(
        url: URL,
        method: String,
        bearerToken: String? = null,
        decode: Json.(String) -> T
    ): T =
        withContext(dispatcher) {
            val connection = (url.openConnection() as HttpURLConnection)
            try {
                connection.requestMethod = method
                connection.connectTimeout = CONNECTION_TIMEOUT_MS
                connection.readTimeout = CONNECTION_TIMEOUT_MS
                connection.setRequestProperty("accept", "application/json")
                bearerToken?.let {
                    connection.setRequestProperty("authorization", "Bearer $it")
                }

                val statusCode = connection.responseCode
                val body = connection.readResponseBody(statusCode)
                if (statusCode !in 200..299) {
                    throw IllegalStateException(body.ifBlank { "HTTP $statusCode" })
                }

                json.decode(body)
            } finally {
                connection.disconnect()
            }
        }

    fun endpoint(
        baseUrl: String,
        pathAndQuery: String
    ): URL =
        URL("${baseUrl.trimEnd('/')}$pathAndQuery")

    fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun HttpURLConnection.readResponseBody(statusCode: Int): String {
        val stream =
            if (statusCode in 200..299) {
                inputStream
            } else {
                errorStream
            }

        return stream
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
    }

    private companion object {
        const val CONNECTION_TIMEOUT_MS = 10_000
    }
}
