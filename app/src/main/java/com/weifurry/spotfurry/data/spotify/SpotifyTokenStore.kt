package com.weifurry.spotfurry.data.spotify

import android.content.Context

internal class SpotifyTokenStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            "spotfurry_spotify",
            Context.MODE_PRIVATE
        )

    val hasValidAccessToken: Boolean
        get() = validAccessToken().isNotBlank()

    fun validAccessToken(nowMillis: Long = System.currentTimeMillis()): String {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, "").orEmpty()
        val expiresAtMillis = preferences.getLong(KEY_EXPIRES_AT_MILLIS, 0L)
        return accessToken.takeIf {
            it.isNotBlank() && expiresAtMillis > nowMillis + TOKEN_EXPIRY_SKEW_MS
        }.orEmpty()
    }

    fun saveAccessToken(
        accessToken: String,
        expiresInSeconds: Int
    ) {
        val expiresAtMillis =
            System.currentTimeMillis() + expiresInSeconds.coerceAtLeast(1) * 1000L
        preferences
            .edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putLong(KEY_EXPIRES_AT_MILLIS, expiresAtMillis)
            .apply()
    }

    fun clear() {
        preferences
            .edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_EXPIRES_AT_MILLIS)
            .apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_EXPIRES_AT_MILLIS = "expires_at_millis"
        const val TOKEN_EXPIRY_SKEW_MS = 60_000L
    }
}
