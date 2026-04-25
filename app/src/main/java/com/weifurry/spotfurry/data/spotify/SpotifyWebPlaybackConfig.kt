package com.weifurry.spotfurry.data.spotify

import android.content.Context
import com.weifurry.spotfurry.R

internal data class SpotifyWebPlaybackConfig(
    val accessToken: String,
    val playbackUri: String
) {
    val hasAccessToken: Boolean
        get() = accessToken.isNotBlank()

    val hasPlaybackUri: Boolean
        get() = playbackUri.isNotBlank()

    companion object {
        fun fromResources(context: Context): SpotifyWebPlaybackConfig =
            SpotifyWebPlaybackConfig(
                accessToken = context.getString(R.string.spotify_web_playback_access_token),
                playbackUri = context.getString(R.string.spotify_web_playback_uri)
            )
    }
}
