package com.weifurry.spotfurry.data.applemusic

import android.content.Context
import com.weifurry.spotfurry.R

internal data class AppleMusicConfig(
    val developerToken: String,
    val testSongId: String,
    val authBaseUrl: String
) {
    val hasDeveloperToken: Boolean
        get() = developerToken.isNotBlank()

    val hasTestSongId: Boolean
        get() = testSongId.isNotBlank()

    val hasAuthBackend: Boolean
        get() = authBaseUrl.isNotBlank()

    companion object {
        fun fromResources(context: Context): AppleMusicConfig =
            AppleMusicConfig(
                developerToken = context.getString(R.string.apple_music_developer_token),
                testSongId = context.getString(R.string.apple_music_test_song_id),
                authBaseUrl = context.getString(R.string.apple_music_auth_base_url)
            )
    }
}
