package com.weifurry.spotfurry.data.applemusic

import com.weifurry.spotfurry.data.auth.PairingHttpClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

internal class AppleMusicPairingClient(
    private val httpClient: PairingHttpClient = PairingHttpClient()
) {
    suspend fun startPairing(authBaseUrl: String): Result<AppleMusicPairingStartResponse> =
        runCatching {
            require(authBaseUrl.isNotBlank()) { "缺少扫码登录后端地址" }
            httpClient.requestJson(
                url = httpClient.endpoint(authBaseUrl, "/api/pairing/start"),
                method = "POST"
            ) {
                decodeFromString<AppleMusicPairingStartResponse>(it)
            }
        }.mapFailure("无法创建扫码登录会话")

    suspend fun checkStatus(
        authBaseUrl: String,
        sessionId: String,
        watchSecret: String
    ): Result<AppleMusicPairingStatusResponse> =
        runCatching {
            require(authBaseUrl.isNotBlank()) { "缺少扫码登录后端地址" }
            require(sessionId.isNotBlank()) { "缺少扫码登录 sessionId" }
            require(watchSecret.isNotBlank()) { "缺少扫码登录 watchSecret" }

            httpClient.requestJson(
                url =
                    httpClient.endpoint(
                        authBaseUrl,
                        "/api/pairing/status?sessionId=${httpClient.urlEncode(sessionId)}"
                    ),
                method = "GET",
                bearerToken = watchSecret
            ) {
                decodeFromString<AppleMusicPairingStatusResponse>(it)
            }
        }.mapFailure("无法读取扫码登录状态")

    private fun <T> Result<T>.mapFailure(prefix: String): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException("$prefix：${it.message}", it)) }
        )
}

@Serializable
internal data class AppleMusicPairingStartResponse(
    val sessionId: String,
    val watchSecret: String,
    val code: String,
    val pairUrl: String,
    val expiresAt: Long,
    val pollAfterMs: Int
)

@Serializable
internal data class AppleMusicPairingStatusResponse(
    val status: String,
    val expiresAt: Long? = null,
    val musicUserToken: String? = null,
    val developerToken: String? = null,
    val developerTokenAvailable: Boolean? = null,
    val error: String? = null
)
