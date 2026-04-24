package com.weifurry.spotfurry.data.applemusic

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.random.Random

internal const val DEFAULT_APPLE_MUSIC_PAIRING_BASE_URL =
    "https://spotfurry.invalid/apple-music/pair"

internal data class AppleMusicPairingSession(
    val code: String,
    val expiresAtEpochMillis: Long
) {
    companion object {
        private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        private const val CODE_LENGTH = 8
        private const val CODE_GROUP_SIZE = 4
        private const val TTL_MILLIS = 5 * 60 * 1000L

        fun create(
            nowEpochMillis: Long = System.currentTimeMillis(),
            random: Random = Random.Default
        ): AppleMusicPairingSession {
            val rawCode =
                buildString(capacity = CODE_LENGTH) {
                    repeat(CODE_LENGTH) {
                        append(ALPHABET[random.nextInt(ALPHABET.length)])
                    }
                }

            return AppleMusicPairingSession(
                code = rawCode.chunked(CODE_GROUP_SIZE).joinToString(separator = "-"),
                expiresAtEpochMillis = nowEpochMillis + TTL_MILLIS
            )
        }
    }
}

internal fun buildAppleMusicPairingUrl(
    baseUrl: String,
    session: AppleMusicPairingSession
): String {
    val normalizedBaseUrl =
        baseUrl
            .trim()
            .ifBlank { DEFAULT_APPLE_MUSIC_PAIRING_BASE_URL }

    val separator =
        when {
            normalizedBaseUrl.endsWith("?") || normalizedBaseUrl.endsWith("&") -> ""
            "?" in normalizedBaseUrl -> "&"
            else -> "?"
        }

    return buildString {
        append(normalizedBaseUrl)
        append(separator)
        append("code=")
        append(urlEncode(session.code))
        append("&provider=apple_music")
    }
}

private fun urlEncode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name())
