package com.weifurry.spotfurry.data.applemusic

import android.content.Context

internal class AppleMusicTokenStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            "spotfurry_apple_music",
            Context.MODE_PRIVATE
        )

    var musicUserToken: String
        get() = preferences.getString(KEY_MUSIC_USER_TOKEN, "").orEmpty()
        set(value) {
            preferences.edit().putString(KEY_MUSIC_USER_TOKEN, value).apply()
        }

    val hasMusicUserToken: Boolean
        get() = musicUserToken.isNotBlank()

    fun clear() {
        preferences.edit().remove(KEY_MUSIC_USER_TOKEN).apply()
    }

    private companion object {
        const val KEY_MUSIC_USER_TOKEN = "music_user_token"
    }
}
