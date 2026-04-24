package com.weifurry.spotfurry.presentation.routes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation as surfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.weifurry.spotfurry.presentation.components.ActionRowButton
import com.weifurry.spotfurry.presentation.components.TrackCard
import com.weifurry.spotfurry.presentation.player.SpotfurryState

@Composable
internal fun NowPlayingRoute(
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
