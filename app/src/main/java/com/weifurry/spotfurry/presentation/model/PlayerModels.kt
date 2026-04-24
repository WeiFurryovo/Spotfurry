package com.weifurry.spotfurry.presentation.model

internal data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int
) {
    val durationLabel: String
        get() = formatClock(durationSeconds)
}

internal data class Playlist(
    val id: String,
    val name: String,
    val subtitle: String,
    val tracks: List<Track>
)

internal enum class RepeatMode(val label: String) {
    Off("关闭"),
    Queue("列表循环"),
    Track("单曲循环")
}

internal fun formatClock(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}
