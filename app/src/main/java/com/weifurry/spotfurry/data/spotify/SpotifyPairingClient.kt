package com.weifurry.spotfurry.data.spotify

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class SpotifyPairingClient(
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
        }
) {
    suspend fun startPairing(authBaseUrl: String): Result<SpotifyPairingStartResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(authBaseUrl.isNotBlank()) { "缺少 Spotify 授权后端地址" }
                requestJson<SpotifyPairingStartResponse>(
                    url = endpoint(authBaseUrl, "/api/spotify/pairing/start"),
                    method = "POST"
                )
            }.mapFailure("无法创建 Spotify 扫码登录会话")
        }

    suspend fun checkStatus(
        authBaseUrl: String,
        sessionId: String,
        watchSecret: String
    ): Result<SpotifyPairingStatusResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(authBaseUrl.isNotBlank()) { "缺少 Spotify 授权后端地址" }
                require(sessionId.isNotBlank()) { "缺少 Spotify sessionId" }
                require(watchSecret.isNotBlank()) { "缺少 Spotify watchSecret" }

                requestJson<SpotifyPairingStatusResponse>(
                    url =
                        endpoint(
                            authBaseUrl,
                            "/api/spotify/pairing/status?sessionId=${urlEncode(sessionId)}"
                        ),
                    method = "GET",
                    bearerToken = watchSecret
                )
            }.mapFailure("无法读取 Spotify 扫码登录状态")
        }

    private inline fun <reified T> requestJson(
        url: URL,
        method: String,
        bearerToken: String? = null
    ): T {
        val connection = (url.openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("accept", "application/json")
            bearerToken?.let {
                connection.setRequestProperty("authorization", "Bearer $it")
            }

            val statusCode = connection.responseCode
            val body = connection.readResponseBody(statusCode)
            if (statusCode !in 200..299) {
                throw IllegalStateException(body.ifBlank { "HTTP $statusCode" })
            }

            return json.decodeFromString<T>(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun endpoint(
        baseUrl: String,
        pathAndQuery: String
    ): URL =
        URL("${baseUrl.trimEnd('/')}$pathAndQuery")

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

    private fun <T> Result<T>.mapFailure(prefix: String): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException("$prefix：${it.message}", it)) }
        )
}

@Serializable
internal data class SpotifyPairingStartResponse(
    val sessionId: String,
    val watchSecret: String,
    val code: String,
    val pairUrl: String,
    val expiresAt: Long,
    val pollAfterMs: Int
)

@Serializable
internal data class SpotifyPairingStatusResponse(
    val status: String,
    val expiresAt: Long? = null,
    val accessToken: String? = null,
    val expiresIn: Int? = null,
    val tokenType: String? = null,
    val scope: String? = null,
    val error: String? = null
)

private fun urlEncode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name())
