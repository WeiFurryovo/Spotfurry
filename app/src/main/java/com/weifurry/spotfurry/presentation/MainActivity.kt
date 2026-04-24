package com.weifurry.spotfurry.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

    fun navigateTo(screen: SpotfurryScreen) {
        if (backStack.lastOrNull() != screen) {
            backStack.add(screen)
        }
    }

    LaunchedEffect(appState.isPlaying, appState.currentTrack.id) {
        while (appState.isPlaying) {
            delay(1_000)
            appState.advancePreviewBySeconds(1)
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
                                onOpenNowPlaying = { navigateTo(NowPlayingScreen) },
                                onOpenLibrary = { navigateTo(LibraryScreen) },
                                onOpenQueue = { navigateTo(QueueScreen) }
                            )
                        }
                        entry<NowPlayingScreen> {
                            NowPlayingRoute(
                                state = appState,
                                onOpenQueue = { navigateTo(QueueScreen) },
                                onOpenLibrary = { navigateTo(LibraryScreen) }
                            )
                        }
                        entry<LibraryScreen> {
                            LibraryRoute(
                                state = appState,
                                onOpenNowPlaying = { navigateTo(NowPlayingScreen) },
                                onQueueLoaded = { playlist ->
                                    appState.loadPlaylist(playlist)
                                    navigateTo(NowPlayingScreen)
                                }
                            )
                        }
                        entry<QueueScreen> {
                            QueueRoute(
                                state = appState,
                                onSelectTrack = { track ->
                                    appState.playTrack(track)
                                    navigateTo(NowPlayingScreen)
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
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0xFF171717),
                                    Color(0xFF060606),
                                    Color(0xFF000000)
                                )
                        )
                    )
        ) {
            val compact = maxWidth < 220.dp
            val mainSize = if (compact) 58.dp else 66.dp
            val sideSize = if (compact) 34.dp else 38.dp
            val utilitySize = if (compact) 22.dp else 24.dp
            val progressRingSize = if (compact) 72.dp else 80.dp
            val sideOffset = if (compact) 62.dp else 74.dp
            val transportOffsetY = if (compact) 24.dp else 34.dp
            val utilityRowOffsetY = if (compact) 46.dp else 48.dp
            val utilitySpacing = if (compact) 10.dp else 14.dp
            val topPadding = if (compact) 18.dp else 24.dp

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
            ) {
                SmallIconBubble(
                    icon = Icons.Filled.LibraryMusic,
                    onClick = onOpenLibrary,
                    contentDescription = "打开音乐库",
                    size = 28.dp,
                    iconSize = 12.dp,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = topPadding + 10.dp, end = 32.dp),
                    bubbleColor = Color(0xFF1D1D1D),
                    borderColor = Color(0xFF2A2A2A),
                    iconTint = Color(0xFFDCDCDC)
                )

                Column(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(0.72f)
                            .padding(top = topPadding)
                            .clickable(onClick = onOpenNowPlaying),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatusPill(
                        text = if (state.isPlaying) "正在播放" else "已暂停",
                        active = state.isPlaying
                    )
                    Text(
                        text = state.currentTrack.title,
                        modifier = Modifier.padding(top = 7.dp),
                        fontSize = if (compact) 17.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFF2F2F2)
                    )
                    Text(
                        text = state.currentTrack.artist,
                        modifier = Modifier.padding(top = 3.dp),
                        fontSize = if (compact) 11.sp else 12.sp,
                        color = Color(0xFFB5B5B5),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .offset(y = transportOffsetY)
                ) {
                    ProgressRing(
                        progress = state.progress,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(progressRingSize)
                    )

                    PrimaryPlayerButton(
                        icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        onClick = state::togglePlayPause,
                        contentDescription = if (state.isPlaying) "暂停" else "播放",
                        modifier = Modifier.align(Alignment.Center),
                        size = mainSize
                    )

                    SecondaryPlayerButton(
                        icon = Icons.Filled.SkipPrevious,
                        onClick = state::skipPrevious,
                        contentDescription = "上一首",
                        size = sideSize,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .offset(x = -sideOffset)
                    )

                    SecondaryPlayerButton(
                        icon = Icons.Filled.SkipNext,
                        onClick = state::skipNext,
                        contentDescription = "下一首",
                        size = sideSize,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .offset(x = sideOffset)
                    )

                    Row(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .offset(y = utilityRowOffsetY)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(utilitySpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallIconBubble(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            onClick = { state.changeVolume(5) },
                            contentDescription = "音量加大",
                            size = utilitySize,
                            iconSize = if (compact) 14.dp else 15.dp,
                            bubbleColor = Color(0xFF1D1D1D),
                            borderColor = Color(0xFF2A2A2A),
                            iconTint = Color(0xFFD0D0D0)
                        )
                        SmallIconBubble(
                            icon = Icons.Filled.Shuffle,
                            onClick = state::toggleShuffle,
                            contentDescription = if (state.shuffleEnabled) "关闭随机播放" else "开启随机播放",
                            size = utilitySize,
                            iconSize = if (compact) 14.dp else 15.dp,
                            highlighted = state.shuffleEnabled,
                            bubbleColor = Color(0xFF1D1D1D),
                            borderColor = Color(0xFF2A2A2A),
                            iconTint = if (state.shuffleEnabled) Color.White else Color(0xFFD0D0D0)
                        )
                        SmallIconBubble(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            onClick = onOpenQueue,
                            contentDescription = "打开播放队列",
                            size = utilitySize,
                            iconSize = if (compact) 14.dp else 15.dp,
                            bubbleColor = Color(0xFF1D1D1D),
                            borderColor = Color(0xFF2A2A2A),
                            iconTint = Color(0xFFD0D0D0)
                        )
                    }
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
private fun StatusPill(
    text: String,
    active: Boolean
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(if (active) Color(0xFF142018) else Color(0xFF181818))
                .border(
                    width = 1.dp,
                    color = if (active) Color(0xFF24442E) else Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            color = if (active) Color(0xFF99F7B2) else Color(0xFF9A9A9A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val inset = strokeWidth / 2
        val arcSize =
            Size(
                width = size.width - strokeWidth,
                height = size.height - strokeWidth
            )
        val topLeft = Offset(inset, inset)

        drawArc(
            color = Color(0xFF252525),
            startAngle = -220f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF8CFFAF),
            startAngle = -220f,
            sweepAngle = 260f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun PrimaryPlayerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 90.dp
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF2B2B2B))
                .border(
                    width = 1.dp,
                    color = Color(0xFF373737),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFFF0F0F0),
            modifier = Modifier.size(size * 0.42f)
        )
    }
}

@Composable
private fun SecondaryPlayerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF1F1F1F))
                .border(
                    width = 1.dp,
                    color = Color(0xFF2B2B2B),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFFD8D8D8),
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

@Composable
private fun SmallIconBubble(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 19.dp,
    highlighted: Boolean = false,
    bubbleColor: Color = Color(0xFF212121),
    borderColor: Color = Color(0xFF2D2D2D),
    iconTint: Color = Color(0xFFE5E5E5)
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (highlighted) {
                        Color(0xFF303030)
                    } else {
                        bubbleColor
                    }
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
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

private val EmptyTrack =
    Track(
        id = "empty",
        title = "暂无播放",
        artist = "等待播放内容",
        durationSeconds = 0
    )

enum class RepeatMode(val label: String) {
    Off("关闭"),
    Queue("列表循环"),
    Track("单曲循环")
}

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

private fun formatClock(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}
