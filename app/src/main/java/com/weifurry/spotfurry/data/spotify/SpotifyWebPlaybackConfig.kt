package com.weifurry.spotfurry.data.spotify

import android.content.Context
import com.weifurry.spotfurry.R

internal data class SpotifyWebPlaybackConfig(
    val accessToken: String,
    val playbackUri: String,
    val authBaseUrl: String
) {
    val hasPlaybackUri: Boolean
        get() = playbackUri.isNotBlank()

    val hasAuthBackend: Boolean
        get() = authBaseUrl.isNotBlank()

    fun withAccessToken(accessToken: String): SpotifyWebPlaybackConfig =
        copy(accessToken = accessToken)

    companion object {
        fun fromResources(context: Context): SpotifyWebPlaybackConfig =
            SpotifyWebPlaybackConfig(
                accessToken = context.getString(R.string.spotify_web_playback_access_token),
                playbackUri = context.getString(R.string.spotify_web_playback_uri),
                authBaseUrl = context.getString(R.string.spotify_auth_base_url)
            )
    }
}
