package com.weifurry.spotfurry.data.spotify

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal class SpotifyPlaybackClient(
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
        },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun currentPlayback(accessToken: String): Result<SpotifyPlaybackSnapshot?> =
        runCatching {
            require(accessToken.isNotBlank()) { "缺少 Spotify access token" }

            val response =
                requestSpotifyApi(
                    accessToken = accessToken,
                    method = "GET",
                    pathAndQuery = "/me/player"
                )
            if (response.statusCode == HTTP_NO_CONTENT) {
                return@runCatching null
            }
            json.decodeFromString<SpotifyPlaybackResponse>(response.body).toSnapshot()
        }.mapFailure("无法读取 Spotify 当前播放")

    suspend fun resume(accessToken: String): Result<Unit> =
        sendPlaybackCommand(
            accessToken = accessToken,
            method = "PUT",
            pathAndQuery = "/me/player/play",
            errorPrefix = "无法继续 Spotify 播放"
        )

    suspend fun pause(accessToken: String): Result<Unit> =
        sendPlaybackCommand(
            accessToken = accessToken,
            method = "PUT",
            pathAndQuery = "/me/player/pause",
            errorPrefix = "无法暂停 Spotify 播放"
        )

    suspend fun skipNext(accessToken: String): Result<Unit> =
        sendPlaybackCommand(
            accessToken = accessToken,
            method = "POST",
            pathAndQuery = "/me/player/next",
            errorPrefix = "无法切到 Spotify 下一首"
        )

    suspend fun skipPrevious(accessToken: String): Result<Unit> =
        sendPlaybackCommand(
            accessToken = accessToken,
            method = "POST",
            pathAndQuery = "/me/player/previous",
            errorPrefix = "无法回到 Spotify 上一首"
        )

    suspend fun setVolume(
        accessToken: String,
        volumePercent: Int
    ): Result<Unit> =
        sendPlaybackCommand(
            accessToken = accessToken,
            method = "PUT",
            pathAndQuery = "/me/player/volume?volume_percent=${volumePercent.coerceIn(0, 100)}",
            errorPrefix = "无法调整 Spotify 音量"
        )

    suspend fun setShuffle(
        accessToken: String,
        enabled: Boolean
    ): Result<Unit> =
        sendPlaybackCommand(
            accessToken = accessToken,
            method = "PUT",
            pathAndQuery = "/me/player/shuffle?state=$enabled",
            errorPrefix = "无法切换 Spotify 随机播放"
        )

    suspend fun setRepeatMode(
        accessToken: String,
        repeatMode: SpotifyRepeatMode
    ): Result<Unit> =
        sendPlaybackCommand(
            accessToken = accessToken,
            method = "PUT",
            pathAndQuery = "/me/player/repeat?state=${repeatMode.apiValue}",
            errorPrefix = "无法切换 Spotify 重复播放"
        )

    private fun SpotifyPlaybackResponse.toSnapshot(): SpotifyPlaybackSnapshot? {
        val currentItem = item ?: return null
        val title = currentItem.name.trim().ifBlank { return null }
        val durationMs = currentItem.durationMs.coerceAtLeast(0)
        val durationSeconds = (durationMs / 1000L).toInt().coerceAtLeast(0)
        val progress =
            if (durationMs > 0L) {
                progressMs.toFloat().coerceAtLeast(0f) / durationMs.toFloat()
            } else {
                0f
            }

        return SpotifyPlaybackSnapshot(
            id = currentItem.uri ?: currentItem.id?.let { "spotify:$it" } ?: "spotify:$title",
            title = title,
            artist = currentItem.artistLabel(),
            durationSeconds = durationSeconds,
            progress = progress.coerceIn(0f, 1f),
            isPlaying = isPlaying,
            shuffleEnabled = shuffleState,
            repeatMode = repeatState.toSpotifyRepeatMode(),
            volumePercent = device?.volumePercent?.coerceIn(0, 100),
            deviceName = device?.name?.trim().orEmpty().ifBlank { null }
        )
    }

    private fun SpotifyPlaybackItem.artistLabel(): String =
        artists
            .mapNotNull { it.name?.trim()?.takeIf(String::isNotBlank) }
            .joinToString(" / ")
            .ifBlank { "Spotify" }

    private suspend fun sendPlaybackCommand(
        accessToken: String,
        method: String,
        pathAndQuery: String,
        errorPrefix: String
    ): Result<Unit> =
        runCatching {
            require(accessToken.isNotBlank()) { "缺少 Spotify access token" }
            requestSpotifyApi(
                accessToken = accessToken,
                method = method,
                pathAndQuery = pathAndQuery
            )
            Unit
        }.mapFailure(errorPrefix)

    private suspend fun requestSpotifyApi(
        accessToken: String,
        method: String,
        pathAndQuery: String
    ): SpotifyApiResponse =
        withContext(dispatcher) {
            val connection =
                (URL("$SPOTIFY_API_BASE$pathAndQuery").openConnection() as HttpURLConnection)
            try {
                connection.requestMethod = method
                connection.connectTimeout = CONNECTION_TIMEOUT_MS
                connection.readTimeout = CONNECTION_TIMEOUT_MS
                connection.setRequestProperty("accept", "application/json")
                connection.setRequestProperty("authorization", "Bearer $accessToken")

                val statusCode = connection.responseCode
                val body = connection.readResponseBody(statusCode)
                if (statusCode !in 200..299) {
                    throw SpotifyApiException(
                        statusCode = statusCode,
                        detail = body.ifBlank { "HTTP $statusCode" }
                    )
                }

                SpotifyApiResponse(
                    statusCode = statusCode,
                    body = body
                )
            } finally {
                connection.disconnect()
            }
        }

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

    private companion object {
        const val SPOTIFY_API_BASE = "https://api.spotify.com/v1"
        const val CONNECTION_TIMEOUT_MS = 10_000
        const val HTTP_NO_CONTENT = 204
    }
}

private data class SpotifyApiResponse(
    val statusCode: Int,
    val body: String
)

internal data class SpotifyPlaybackSnapshot(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val progress: Float,
    val isPlaying: Boolean,
    val shuffleEnabled: Boolean,
    val repeatMode: SpotifyRepeatMode,
    val volumePercent: Int?,
    val deviceName: String?
)

internal enum class SpotifyRepeatMode(val apiValue: String) {
    Off("off"),
    Context("context"),
    Track("track")
}

internal class SpotifyApiException(
    val statusCode: Int,
    detail: String
) : IllegalStateException("Spotify API $statusCode：$detail")

@Serializable
private data class SpotifyPlaybackResponse(
    @SerialName("is_playing") val isPlaying: Boolean = false,
    @SerialName("progress_ms") val progressMs: Long = 0,
    @SerialName("shuffle_state") val shuffleState: Boolean = false,
    @SerialName("repeat_state") val repeatState: String = "off",
    val item: SpotifyPlaybackItem? = null,
    val device: SpotifyPlaybackDevice? = null
)

@Serializable
private data class SpotifyPlaybackItem(
    val id: String? = null,
    val uri: String? = null,
    val name: String = "",
    @SerialName("duration_ms") val durationMs: Long = 0,
    val artists: List<SpotifyNamedEntity> = emptyList()
)

@Serializable
private data class SpotifyNamedEntity(
    val name: String? = null
)

@Serializable
private data class SpotifyPlaybackDevice(
    val name: String? = null,
    @SerialName("volume_percent") val volumePercent: Int? = null
)

private fun String.toSpotifyRepeatMode(): SpotifyRepeatMode =
    when (lowercase()) {
        "context" -> SpotifyRepeatMode.Context
        "track" -> SpotifyRepeatMode.Track
        else -> SpotifyRepeatMode.Off
    }
