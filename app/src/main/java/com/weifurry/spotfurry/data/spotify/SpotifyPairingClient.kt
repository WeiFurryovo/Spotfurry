package com.weifurry.spotfurry.data.spotify

import com.weifurry.spotfurry.data.auth.PairingHttpClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

internal class SpotifyPairingClient(
    private val httpClient: PairingHttpClient = PairingHttpClient()
) {
    suspend fun startPairing(authBaseUrl: String): Result<SpotifyPairingStartResponse> =
        runCatching {
            require(authBaseUrl.isNotBlank()) { "缺少 Spotify 授权后端地址" }
            httpClient.requestJson(
                url = httpClient.endpoint(authBaseUrl, "/api/spotify/pairing/start"),
                method = "POST"
            ) {
                decodeFromString<SpotifyPairingStartResponse>(it)
            }
        }.mapFailure("无法创建 Spotify 扫码登录会话")

    suspend fun checkStatus(
        authBaseUrl: String,
        sessionId: String,
        watchSecret: String
    ): Result<SpotifyPairingStatusResponse> =
        runCatching {
            require(authBaseUrl.isNotBlank()) { "缺少 Spotify 授权后端地址" }
            require(sessionId.isNotBlank()) { "缺少 Spotify sessionId" }
            require(watchSecret.isNotBlank()) { "缺少 Spotify watchSecret" }

            httpClient.requestJson(
                url =
                    httpClient.endpoint(
                        authBaseUrl,
                        "/api/spotify/pairing/status?sessionId=${httpClient.urlEncode(sessionId)}"
                    ),
                method = "GET",
                bearerToken = watchSecret
            ) {
                decodeFromString<SpotifyPairingStatusResponse>(it)
            }
        }.mapFailure("无法读取 Spotify 扫码登录状态")

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
