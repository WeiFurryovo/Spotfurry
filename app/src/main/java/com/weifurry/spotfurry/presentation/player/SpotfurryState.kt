package com.weifurry.spotfurry.presentation.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.weifurry.spotfurry.presentation.model.Playlist
import com.weifurry.spotfurry.presentation.model.RepeatMode
import com.weifurry.spotfurry.presentation.model.Track
import com.weifurry.spotfurry.presentation.model.formatClock

private val EmptyTrack =
    Track(
        id = "empty",
        title = "暂无播放",
        artist = "等待播放内容",
        durationSeconds = 0
    )

internal class SpotfurryState internal constructor(
    val playlists: List<Playlist>,
    initialQueue: List<Track>,
    initialTrackId: String,
    private val shuffleTrackIds: (List<String>) -> List<String> = { it.shuffled() }
) {
    var queue by mutableStateOf(initialQueue)
        private set

    var currentTrackId by mutableStateOf(initialTrackId)
        private set

    var isPlaying by mutableStateOf(true)
        private set

    private var likedTrackIds by mutableStateOf(emptySet<String>())

    val isLiked: Boolean
        get() = queue.isNotEmpty() && currentTrack.id in likedTrackIds

    private var shuffleOrder by mutableStateOf(emptyList<String>())

    private var shufflePosition by mutableIntStateOf(0)

    var shuffleEnabled by mutableStateOf(false)
        private set

    var repeatMode by mutableStateOf(RepeatMode.Queue)
        private set

    var progress by mutableFloatStateOf(0.22f)
        private set

    var volumePercent by mutableIntStateOf(72)
        private set

    val currentTrack: Track
        get() = queue.firstOrNull { it.id == currentTrackId } ?: queue.firstOrNull() ?: EmptyTrack

    val playbackSummary: String
        get() {
            if (queue.isEmpty()) {
                return "暂无播放内容"
            }

            val stateLabel = if (isPlaying) "正在播放" else "已暂停"
            return "$stateLabel  $timeRangeLabel"
        }

    val timeRangeLabel: String
        get() {
            if (queue.isEmpty()) {
                return "0:00 / 0:00"
            }

            val elapsed =
                (currentTrack.durationSeconds * progress)
                    .toInt()
                    .coerceIn(0, currentTrack.durationSeconds)
            return "${formatClock(elapsed)} / ${currentTrack.durationLabel}"
        }

    val nextTrackLabel: String
        get() {
            if (queue.size < 2) {
                return "当前队列中没有其他歌曲"
            }

            val nextIndex =
                nextTrackId()
                    ?.let { id -> queue.indexOfFirst { it.id == id } }
                    ?.takeIf { it != -1 }
                    ?: return "已到队列末尾"

            val nextTrack = queue[nextIndex]
            return "${nextTrack.title}  ${nextTrack.artist}"
        }

    fun playTrack(track: Track) {
        currentTrackId = track.id
        progress = 0f
        isPlaying = true
        rebuildShuffleOrderIfNeeded()
    }

    fun loadPlaylist(playlist: Playlist) {
        queue = playlist.tracks
        currentTrackId = playlist.tracks.firstOrNull()?.id ?: EmptyTrack.id
        progress = 0f
        isPlaying = playlist.tracks.isNotEmpty()
        rebuildShuffleOrderIfNeeded()
    }

    fun togglePlayPause() {
        if (queue.isEmpty()) {
            return
        }

        if (!isPlaying && progress >= 1f) {
            progress = 0f
        }
        isPlaying = !isPlaying
    }

    fun skipNext() {
        if (queue.isEmpty()) {
            return
        }

        ensureShuffleOrder()
        val nextTrackId = nextTrackId()
        if (nextTrackId == null) {
            progress = 1f
            isPlaying = false
            return
        }

        currentTrackId = nextTrackId
        progress = 0f
        isPlaying = true
        syncShufflePosition()
    }

    fun skipPrevious() {
        if (queue.isEmpty()) {
            return
        }

        if (progress > 0.12f) {
            progress = 0f
            return
        }

        ensureShuffleOrder()
        val previousTrackId = previousTrackId() ?: currentTrackId

        currentTrackId = previousTrackId
        progress = 0f
        isPlaying = true
        syncShufflePosition()
    }

    fun toggleLike() {
        if (queue.isEmpty()) {
            return
        }

        likedTrackIds =
            if (isLiked) {
                likedTrackIds - currentTrack.id
            } else {
                likedTrackIds + currentTrack.id
            }
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        if (shuffleEnabled) {
            rebuildShuffleOrder()
        } else {
            shuffleOrder = emptyList()
            shufflePosition = 0
        }
    }

    fun cycleRepeat() {
        repeatMode =
            when (repeatMode) {
                RepeatMode.Off -> RepeatMode.Queue
                RepeatMode.Queue -> RepeatMode.Track
                RepeatMode.Track -> RepeatMode.Off
            }
    }

    fun changeVolume(delta: Int) {
        volumePercent = (volumePercent + delta).coerceIn(0, 100)
    }

    fun advancePreviewBySeconds(seconds: Int) {
        if (queue.isEmpty()) {
            return
        }

        val durationSeconds = currentTrack.durationSeconds.coerceAtLeast(1)
        val nextProgress = progress + seconds.toFloat() / durationSeconds
        if (nextProgress < 1f) {
            progress = nextProgress
            return
        }

        when (repeatMode) {
            RepeatMode.Track -> progress = 0f
            RepeatMode.Queue -> skipNext()
            RepeatMode.Off -> {
                ensureShuffleOrder()
                if (nextTrackId() == null) {
                    progress = 1f
                    isPlaying = false
                } else {
                    skipNext()
                }
            }
        }
    }

    private fun nextTrackId(): String? {
        if (queue.isEmpty()) {
            return null
        }

        if (repeatMode == RepeatMode.Track) {
            return currentTrackId
        }

        if (shuffleEnabled) {
            val order = activeShuffleOrder()
            val nextPosition = currentShufflePosition(order) + 1
            if (nextPosition < order.size) {
                return order[nextPosition]
            }
            return if (repeatMode == RepeatMode.Queue) order.firstOrNull() else null
        }

        val nextIndex = currentIndex() + 1
        if (nextIndex < queue.size) {
            return queue[nextIndex].id
        }
        return if (repeatMode == RepeatMode.Queue) queue.firstOrNull()?.id else null
    }

    private fun previousTrackId(): String? {
        if (queue.isEmpty()) {
            return null
        }

        if (repeatMode == RepeatMode.Track) {
            return currentTrackId
        }

        if (shuffleEnabled) {
            val order = activeShuffleOrder()
            val previousPosition = currentShufflePosition(order) - 1
            if (previousPosition >= 0) {
                return order[previousPosition]
            }
            return if (repeatMode == RepeatMode.Queue) order.lastOrNull() else null
        }

        val previousIndex = currentIndex() - 1
        if (previousIndex >= 0) {
            return queue[previousIndex].id
        }
        return if (repeatMode == RepeatMode.Queue) queue.lastOrNull()?.id else null
    }

    private fun ensureShuffleOrder() {
        if (!shuffleEnabled) {
            return
        }

        val queueIds = queue.map { it.id }
        if (shuffleOrder.toSet() != queueIds.toSet() || currentTrackId !in shuffleOrder) {
            rebuildShuffleOrder()
            return
        }

        syncShufflePosition()
    }

    private fun activeShuffleOrder(): List<String> {
        val queueIds = queue.map { it.id }
        if (shuffleOrder.toSet() == queueIds.toSet() && currentTrackId in shuffleOrder) {
            return shuffleOrder
        }

        return listOf(currentTrack.id) + queueIds.filterNot { it == currentTrack.id }
    }

    private fun currentShufflePosition(order: List<String>): Int =
        order.indexOf(currentTrackId).takeIf { it >= 0 } ?: 0

    private fun rebuildShuffleOrderIfNeeded() {
        if (shuffleEnabled) {
            rebuildShuffleOrder()
        }
    }

    private fun rebuildShuffleOrder() {
        val queueIds = queue.map { it.id }
        if (queueIds.isEmpty()) {
            shuffleOrder = emptyList()
            shufflePosition = 0
            return
        }

        val currentId = currentTrack.id
        val remainingIds = queueIds.filterNot { it == currentId }
        shuffleOrder = listOf(currentId) + shuffleTrackIds(remainingIds)
        shufflePosition = 0
    }

    private fun syncShufflePosition() {
        if (!shuffleEnabled) {
            return
        }

        shufflePosition = shuffleOrder.indexOf(currentTrackId).takeIf { it >= 0 } ?: 0
    }

    private fun currentIndex(): Int {
        val index = queue.indexOfFirst { it.id == currentTrackId }
        return if (index == -1) 0 else index
    }

    companion object {
        fun preview(): SpotfurryState {
            val playlists = previewPlaylists()
            val initialQueue = playlists.first().tracks
            return SpotfurryState(
                playlists = playlists,
                initialQueue = initialQueue,
                initialTrackId = initialQueue.first().id
            )
        }
    }
}
