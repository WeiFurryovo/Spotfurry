package com.weifurry.spotfurry.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation as surfaceTransformation
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
                                onOpenQueue = { backStack.add(QueueScreen) }
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
    onOpenQueue: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color(0xFF4D79BE),
                                        Color(0xFF7BA4D8)
                                    )
                            )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x2FFFFFFF),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallIconBubble(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    onClick = onOpenQueue,
                    size = 38.dp,
                    iconSize = 18.dp
                )
                SmallIconBubble(
                    icon = Icons.Filled.LibraryMusic,
                    onClick = onOpenLibrary,
                    size = 38.dp,
                    iconSize = 18.dp
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = 28.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.74f)
                            .clickable(onClick = onOpenNowPlaying),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.currentTrack.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        text = state.currentTrack.artist,
                        fontSize = 13.sp,
                        color = Color(0xDDE8F0FF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = state.playbackSummary,
                        fontSize = 11.sp,
                        color = Color(0xFFF2F7FF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.size(18.dp))

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SecondaryPlayerButton(
                        icon = Icons.Filled.SkipPrevious,
                        onClick = state::skipPrevious,
                        size = 48.dp,
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = 10.dp, y = 18.dp)
                    )
                    PrimaryPlayerButton(
                        icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        onClick = state::togglePlayPause,
                        size = 82.dp
                    )
                    SecondaryPlayerButton(
                        icon = Icons.Filled.SkipNext,
                        onClick = state::skipNext,
                        size = 48.dp,
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = (-10).dp, y = 18.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(0.58f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallIconBubble(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        onClick = { state.changeVolume(5) },
                        size = 36.dp,
                        iconSize = 18.dp
                    )
                    SmallIconBubble(
                        icon = Icons.Filled.Shuffle,
                        onClick = state::toggleShuffle,
                        size = 36.dp,
                        iconSize = 18.dp
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
                Text(if (state.isPlaying) "暂停" else "播放")
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
                    transformation = surfaceTransformation(transformationSpec)
                ) {
                    Text("当前播放")
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
                    label = "下一首",
                    detail = state.nextTrackLabel,
                    onClick = state::skipNext,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "上一首",
                    detail = "重新播放当前歌曲或返回上一首",
                    onClick = state::skipPrevious,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = if (state.isLiked) "已喜欢" else "喜欢歌曲",
                    detail = "把这首歌加入手表收藏",
                    onClick = state::toggleLike,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = if (state.shuffleEnabled) "随机已开" else "随机已关",
                    detail = "模式：${state.repeatMode.label}",
                    onClick = state::toggleShuffle,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "重复播放",
                    detail = "当前模式：${state.repeatMode.label}",
                    onClick = state::cycleRepeat,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "音量 +5",
                    detail = "当前音量 ${state.volumePercent}%",
                    onClick = { state.changeVolume(5) },
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "音量 -5",
                    detail = "当前音量 ${state.volumePercent}%",
                    onClick = { state.changeVolume(-5) },
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "打开队列",
                    detail = "队列中共有 ${state.queue.size} 首歌",
                    onClick = onOpenQueue,
                    transformationSpec = transformationSpec
                )
            }
            item {
                ActionRowButton(
                    label = "浏览音乐库",
                    detail = "切换到新的播放列表",
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
                Text("播放器")
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
                    transformation = surfaceTransformation(transformationSpec)
                ) {
                    Text("音乐库")
                }
            }
            item {
                TrackCard(
                    title = "固定歌单",
                    body = "已准备 ${state.playlists.size} 个适合手表场景的歌单",
                    onClick = {},
                    transformationSpec = transformationSpec
                )
            }
            state.playlists.forEach { playlist ->
                item {
                    TrackCard(
                        title = playlist.name,
                        body = "${playlist.subtitle}\n共 ${playlist.tracks.size} 首歌",
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
                Text("跳过")
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
                    transformation = surfaceTransformation(transformationSpec)
                ) {
                    Text("播放队列")
                }
            }
            state.queue.forEach { track ->
                item {
                    TrackCard(
                        title = track.title,
                        body =
                            if (track.id == state.currentTrack.id) {
                                "${track.artist}  ${track.durationLabel}\n正在播放"
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
private fun TransformingLazyColumnItemScope.ActionRowButton(
    label: String,
    detail: String,
    onClick: () -> Unit,
    transformationSpec: androidx.wear.compose.material3.lazy.TransformationSpec
) {
    TitleCard(
        title = { Text(label) },
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .transformedHeight(this, transformationSpec),
        transformation = surfaceTransformation(transformationSpec)
    ) {
        Text(detail)
    }
}

@Composable
private fun TransformingLazyColumnItemScope.TrackCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    transformationSpec: androidx.wear.compose.material3.lazy.TransformationSpec
) {
    TitleCard(
        title = { Text(title) },
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .transformedHeight(this, transformationSpec),
        transformation = surfaceTransformation(transformationSpec)
    ) {
        Text(body)
    }
}

@Composable
private fun PrimaryPlayerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 90.dp
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color(0xFFF8FBFF),
                                    Color(0xFFEAF2FE)
                                )
                        )
                )
                .border(
                    width = 1.dp,
                    color = Color(0x4DFFFFFF),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF406FAF),
            modifier = Modifier.size(size * 0.42f)
        )
    }
}

@Composable
private fun SecondaryPlayerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0x22FFFFFF))
                .border(
                    width = 1.dp,
                    color = Color(0x14FFFFFF),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

@Composable
private fun SmallIconBubble(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 19.dp
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0x22FFFFFF))
                .border(
                    width = 1.dp,
                    color = Color(0x14FFFFFF),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
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
    Off("关闭"),
    Queue("列表循环"),
    Track("单曲循环")
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

    val playbackSummary: String
        get() {
            val elapsed = (currentTrack.durationSeconds * progress).toInt()
            val stateLabel = if (isPlaying) "正在播放" else "已暂停"
            return "$stateLabel  ${formatClock(elapsed)} / ${currentTrack.durationLabel}"
        }

    val nextTrackLabel: String
        get() {
            if (queue.size < 2) {
                return "当前队列中没有其他歌曲"
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
                        name = "夜行驾驶",
                        subtitle = "适合夜晚通勤的温暖合成器旋律",
                        tracks =
                            listOf(
                                Track("t1", "余晖电路", "新星脉冲", 214),
                                Track("t2", "月光出口", "丝绒信号", 201),
                                Track("t3", "静电心跳", "紫苑小径", 226),
                                Track("t4", "玻璃公路", "天穹回声", 192)
                            )
                    ),
                    Playlist(
                        id = "focus-loop",
                        name = "专注循环",
                        subtitle = "适合短时专注的平静播放队列",
                        tracks =
                            listOf(
                                Track("t5", "柔和轨道", "灰烬山谷", 178),
                                Track("t6", "静默二进制", "北方气流", 242),
                                Track("t7", "流明脚本", "光环漂流", 206),
                                Track("t8", "洁净房间", "高空绽放", 187)
                            )
                    ),
                    Playlist(
                        id = "run-mode",
                        name = "跑步模式",
                        subtitle = "适合运动节奏的高能旋律",
                        tracks =
                            listOf(
                                Track("t9", "火花轨迹", "风筝星图", 181),
                                Track("t10", "速度绽放", "六月街机", 195),
                                Track("t11", "脉冲并行", "回声港湾", 204),
                                Track("t12", "霓虹冲刺", "铆钉青春", 176)
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
