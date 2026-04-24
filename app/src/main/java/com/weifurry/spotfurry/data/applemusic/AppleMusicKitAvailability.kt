package com.weifurry.spotfurry.data.applemusic

internal data class AppleMusicKitAvailability(
    val authenticationAvailable: Boolean,
    val playbackAvailable: Boolean,
    val missingClasses: List<String>
) {
    val sdkAvailable: Boolean
        get() = authenticationAvailable && playbackAvailable

    val label: String
        get() =
            when {
                sdkAvailable -> "Apple MusicKit SDK 已可用"
                missingClasses.isEmpty() -> "SDK 状态未知"
                else -> "缺少 ${missingClasses.size} 个 SDK 类"
            }
}
