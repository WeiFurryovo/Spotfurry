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

            withContext(dispatcher) {
                val connection = (URL("$SPOTIFY_API_BASE/me/player").openConnection() as HttpURLConnection)
                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = CONNECTION_TIMEOUT_MS
                    connection.readTimeout = CONNECTION_TIMEOUT_MS
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty("authorization", "Bearer $accessToken")

                    val statusCode = connection.responseCode
                    if (statusCode == HTTP_NO_CONTENT) {
                        return@withContext null
                    }

                    val body = connection.readResponseBody(statusCode)
                    if (statusCode !in 200..299) {
                        throw SpotifyApiException(
                            statusCode = statusCode,
                            detail = body.ifBlank { "HTTP $statusCode" }
                        )
                    }

                    json.decodeFromString<SpotifyPlaybackResponse>(body).toSnapshot()
                } finally {
                    connection.disconnect()
                }
            }
        }.mapFailure("无法读取 Spotify 当前播放")

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
            deviceName = device?.name?.trim().orEmpty().ifBlank { null }
        )
    }

    private fun SpotifyPlaybackItem.artistLabel(): String =
        artists
            .mapNotNull { it.name?.trim()?.takeIf(String::isNotBlank) }
            .joinToString(" / ")
            .ifBlank { "Spotify" }

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

internal data class SpotifyPlaybackSnapshot(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val progress: Float,
    val isPlaying: Boolean,
    val deviceName: String?
)

internal class SpotifyApiException(
    val statusCode: Int,
    detail: String
) : IllegalStateException("Spotify API $statusCode：$detail")

@Serializable
private data class SpotifyPlaybackResponse(
    @SerialName("is_playing") val isPlaying: Boolean = false,
    @SerialName("progress_ms") val progressMs: Long = 0,
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
    val name: String? = null
)
