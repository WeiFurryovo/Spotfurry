package com.weifurry.spotfurry.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.weifurry.spotfurry.presentation.theme.SpotfurryTheme
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpotfurryApp()
        }
    }
}

@Serializable
sealed interface SpotfurryScreen : NavKey

@Serializable
data object HomeScreen : SpotfurryScreen

@Serializable
data object NowPlayingScreen : SpotfurryScreen

@Serializable
data object LibraryScreen : SpotfurryScreen

@Serializable
data object QueueScreen : SpotfurryScreen

@Composable
fun SpotfurryApp() {
    val appState = remember { SpotfurryState.preview() }
    val backStack = rememberNavBackStack(HomeScreen)

    LaunchedEffect(appState.isPlaying, appState.currentTrack.id) {
        while (appState.isPlaying) {
            delay(1_200)
            appState.advancePreview(0.04f)
        }
    }

    SpotfurryTheme {
        AppScaffold {
            val entryProvider =
                remember(appState) {
                    entryProvider<NavKey> {
                        entry<HomeScreen> {
                            HomeRoute(
                                state = appState,
                                onOpenNowPlaying = { backStack.add(NowPlayingScreen) },
                                onOpenLibrary = { backStack.add(LibraryScreen) },
                                onOpenQueue = { backStack.add(QueueScreen) },
                                onPlayTrack = { track ->
                                    appState.playTrack(track)
                                    backStack.add(NowPlayingScreen)
                                }
                            )
                        }
                        entry<NowPlayingScreen> {
                            NowPlayingRoute(
                                state = appState,
                                onOpenQueue = { backStack.add(QueueScreen) },
                                onOpenLibrary = { backStack.add(LibraryScreen) }
                            )
                        }
                        entry<LibraryScreen> {
                            LibraryRoute(
                                state = appState,
                                onOpenNowPlaying = { backStack.add(NowPlayingScreen) },
                                onQueueLoaded = { playlist ->
                                    appState.loadPlaylist(playlist)
                                    backStack.add(NowPlayingScreen)
                                }
                            )
                        }
                        entry<QueueScreen> {
                            QueueRoute(
                                state = appState,
                                onSelectTrack = { track ->
                                    appState.playTrack(track)
                                    backStack.add(NowPlayingScreen)
                                }
                            )
                        }
                    }
                }

            val swipeDismissableSceneStrategy = rememberSwipeDismissableSceneStrategy<NavKey>()

            NavDisplay(
                backStack = backStack,
                entryProvider = entryProvider,
                sceneStrategies = listOf(swipeDismissableSceneStrategy)
            )
        }
    }
}

@Composable
private fun HomeRoute(
    state: SpotfurryState,
    onOpenNowPlaying: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenQueue: () -> Unit,
    onPlayTrack: (Track) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = onOpenNowPlaying,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text(if (state.isPlaying) "Playing" else "Resume")
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ListHeaderDefaults.minimumTopListContentPadding
                            ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text("Spotfurry")
                }
            }
            item {
                TrackCard(
                    title = state.currentTrack.title,
                    body = "${state.currentTrack.artist}  ${state.playbackSummary}",
                    onClick = onOpenNowPlaying,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "Library",
                    detail = "${state.playlists.size} mixes ready",
                    onClick = onOpenLibrary,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "Queue",
                    detail = "${state.queue.size} tracks in rotation",
                    onClick = onOpenQueue,
                    transformationSpec = transformationSpec
                )
            }
            item {
                SectionHeader(
                    label = "Discover",
                    transformationSpec = transformationSpec
                )
            }
            state.discoverTracks.forEach { track ->
                item {
                    TrackCard(
                        title = track.title,
                        body = "${track.artist}  ${track.durationLabel}",
                        onClick = { onPlayTrack(track) },
                        transformationSpec = transformationSpec
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingRoute(
    state: SpotfurryState,
    onOpenQueue: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = state::togglePlayPause,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text(if (state.isPlaying) "Pause" else "Play")
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ListHeaderDefaults.minimumTopListContentPadding
                            ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text("Now Playing")
                }
            }
            item {
                TrackCard(
                    title = state.currentTrack.title,
                    body = "${state.currentTrack.artist}\n${state.playbackSummary}",
                    onClick = {},
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "Next Track",
                    detail = state.nextTrackLabel,
                    onClick = state::skipNext,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "Previous Track",
                    detail = "Restart or jump back in the queue",
                    onClick = state::skipPrevious,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = if (state.isLiked) "Liked" else "Like Track",
                    detail = "Save this song to your watch favorites",
                    onClick = state::toggleLike,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = if (state.shuffleEnabled) "Shuffle On" else "Shuffle Off",
                    detail = "Mode: ${state.repeatMode.label}",
                    onClick = state::toggleShuffle,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "Repeat",
                    detail = "Current mode: ${state.repeatMode.label}",
                    onClick = state::cycleRepeat,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "Volume +5",
                    detail = "Current volume ${state.volumePercent}%",
                    onClick = { state.changeVolume(5) },
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "Volume -5",
                    detail = "Current volume ${state.volumePercent}%",
                    onClick = { state.changeVolume(-5) },
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "Open Queue",
                    detail = "${state.queue.size} tracks lined up",
                    onClick = onOpenQueue,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "Browse Library",
                    detail = "Swap the current queue to a new mix",
                    onClick = onOpenLibrary,
                    transformationSpec = transformationSpec
                )
            }
        }
    }
}

@Composable
private fun LibraryRoute(
    state: SpotfurryState,
    onOpenNowPlaying: () -> Unit,
    onQueueLoaded: (Playlist) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = onOpenNowPlaying,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text("Player")
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ListHeaderDefaults.minimumTopListContentPadding
                            ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text("Library")
                }
            }
            item {
                TrackCard(
                    title = "Pinned Mixes",
                    body = "${state.playlists.size} ready-made queues for a watch session",
                    onClick = {},
                    transformationSpec = transformationSpec
                )
            }
            state.playlists.forEach { playlist ->
                item {
                    TrackCard(
                        title = playlist.name,
                        body = "${playlist.subtitle}\n${playlist.tracks.size} tracks",
                        onClick = { onQueueLoaded(playlist) },
                        transformationSpec = transformationSpec
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRoute(
    state: SpotfurryState,
    onSelectTrack: (Track) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = state::skipNext,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text("Skip")
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ListHeaderDefaults.minimumTopListContentPadding
                            ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text("Queue")
                }
            }
            state.queue.forEach { track ->
                item {
                    TrackCard(
                        title = track.title,
                        body =
                            if (track.id == state.currentTrack.id) {
                                "${track.artist}  ${track.durationLabel}\nPlaying now"
                            } else {
                                "${track.artist}  ${track.durationLabel}"
                            },
                        onClick = { onSelectTrack(track) },
                        transformationSpec = transformationSpec
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    label: String,
    transformationSpec: androidx.wear.compose.material3.lazy.TransformationSpec
) {
    ListHeader(
        modifier =
            Modifier
                .fillMaxWidth(),
        transformation = SurfaceTransformation(transformationSpec)
    ) {
        Text(label)
    }
}

@Composable
private fun ActionRowButton(
    label: String,
    detail: String,
    onClick: () -> Unit,
    transformationSpec: androidx.wear.compose.material3.lazy.TransformationSpec
) {
    TitleCard(
        title = { Text(label) },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        transformation = SurfaceTransformation(transformationSpec)
    ) {
        Text(detail)
    }
}

@Composable
private fun TrackCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    transformationSpec: androidx.wear.compose.material3.lazy.TransformationSpec
) {
    TitleCard(
        title = { Text(title) },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        transformation = SurfaceTransformation(transformationSpec)
    ) {
        Text(body)
    }
}

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int
) {
    val durationLabel: String
        get() = formatClock(durationSeconds)
}

data class Playlist(
    val id: String,
    val name: String,
    val subtitle: String,
    val tracks: List<Track>
)

enum class RepeatMode(val label: String) {
    Off("Off"),
    Queue("Queue"),
    Track("Track")
}

private class SpotfurryState private constructor(
    val playlists: List<Playlist>,
    initialQueue: List<Track>,
    initialTrackId: String
) {
    var queue by mutableStateOf(initialQueue)
        private set

    var currentTrackId by mutableStateOf(initialTrackId)
        private set

    var isPlaying by mutableStateOf(true)
        private set

    var isLiked by mutableStateOf(false)
        private set

    var shuffleEnabled by mutableStateOf(false)
        private set

    var repeatMode by mutableStateOf(RepeatMode.Queue)
        private set

    var progress by mutableStateOf(0.22f)
        private set

    var volumePercent by mutableStateOf(72)
        private set

    val currentTrack: Track
        get() = queue.firstOrNull { it.id == currentTrackId } ?: queue.first()

    val discoverTracks: List<Track> =
        playlists
            .flatMap { playlist -> playlist.tracks }
            .distinctBy { track -> track.id }
            .take(5)

    val playbackSummary: String
        get() {
            val elapsed = (currentTrack.durationSeconds * progress).toInt()
            val stateLabel = if (isPlaying) "Playing" else "Paused"
            return "$stateLabel  ${formatClock(elapsed)} / ${currentTrack.durationLabel}"
        }

    val nextTrackLabel: String
        get() {
            if (queue.size < 2) {
                return "No alternate track in the current queue"
            }

            val nextIndex =
                when {
                    shuffleEnabled && queue.size > 1 -> (currentIndex() + 2) % queue.size
                    else -> (currentIndex() + 1) % queue.size
                }

            val nextTrack = queue[nextIndex]
            return "${nextTrack.title}  ${nextTrack.artist}"
        }

    fun playTrack(track: Track) {
        currentTrackId = track.id
        progress = 0f
        isPlaying = true
    }

    fun loadPlaylist(playlist: Playlist) {
        queue = playlist.tracks
        currentTrackId = playlist.tracks.first().id
        progress = 0f
        isPlaying = true
        isLiked = false
    }

    fun togglePlayPause() {
        isPlaying = !isPlaying
    }

    fun skipNext() {
        if (queue.isEmpty()) {
            return
        }

        val nextIndex =
            when {
                repeatMode == RepeatMode.Track -> currentIndex()
                shuffleEnabled && queue.size > 1 -> (currentIndex() + 2) % queue.size
                else -> (currentIndex() + 1) % queue.size
            }

        currentTrackId = queue[nextIndex].id
        progress = 0f
        isPlaying = true
    }

    fun skipPrevious() {
        if (progress > 0.12f) {
            progress = 0f
            return
        }

        val previousIndex =
            when {
                queue.size <= 1 -> 0
                shuffleEnabled -> (currentIndex() - 2).floorMod(queue.size)
                else -> (currentIndex() - 1).floorMod(queue.size)
            }

        currentTrackId = queue[previousIndex].id
        progress = 0f
        isPlaying = true
    }

    fun toggleLike() {
        isLiked = !isLiked
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
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

    fun advancePreview(step: Float) {
        val nextProgress = progress + step
        if (nextProgress < 1f) {
            progress = nextProgress
            return
        }

        when (repeatMode) {
            RepeatMode.Track -> progress = 0f
            RepeatMode.Off,
            RepeatMode.Queue -> skipNext()
        }
    }

    private fun currentIndex(): Int {
        val index = queue.indexOfFirst { it.id == currentTrackId }
        return if (index == -1) 0 else index
    }

    companion object {
        fun preview(): SpotfurryState {
            val playlists =
                listOf(
                    Playlist(
                        id = "night-drive",
                        name = "Night Drive",
                        subtitle = "Warm synths for a quick city ride",
                        tracks =
                            listOf(
                                Track("t1", "Afterglow Circuit", "Nova Pulse", 214),
                                Track("t2", "Moonlit Exit", "Velvet Signal", 201),
                                Track("t3", "Static Hearts", "Aster Lane", 226),
                                Track("t4", "Glass Highway", "Sora Echo", 192)
                            )
                    ),
                    Playlist(
                        id = "focus-loop",
                        name = "Focus Loop",
                        subtitle = "A calm queue for short wrist sessions",
                        tracks =
                            listOf(
                                Track("t5", "Soft Orbit", "Cinder Vale", 178),
                                Track("t6", "Quiet Binary", "North Current", 242),
                                Track("t7", "Luma Script", "Halo Drift", 206),
                                Track("t8", "Clean Room", "Aerial Bloom", 187)
                            )
                    ),
                    Playlist(
                        id = "run-mode",
                        name = "Run Mode",
                        subtitle = "Fast hooks with enough punch for movement",
                        tracks =
                            listOf(
                                Track("t9", "Sparkline", "Kite Atlas", 181),
                                Track("t10", "Velocity Bloom", "June Arcade", 195),
                                Track("t11", "Pulse Parallel", "Echo Harbor", 204),
                                Track("t12", "Neon Sprint", "Rivet Youth", 176)
                            )
                    )
                )

            val initialQueue = playlists.first().tracks
            return SpotfurryState(
                playlists = playlists,
                initialQueue = initialQueue,
                initialTrackId = initialQueue.first().id
            )
        }
    }
}

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other

private fun formatClock(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}
