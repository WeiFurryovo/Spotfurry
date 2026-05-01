package com.weifurry.spotfurry.presentation.routes

import androidx.compose.runtime.Composable
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.weifurry.spotfurry.presentation.components.ActionRowButton
import com.weifurry.spotfurry.presentation.components.ListSectionHeader
import com.weifurry.spotfurry.presentation.components.TrackCard
import com.weifurry.spotfurry.presentation.player.SpotfurryState

@Composable
internal fun NowPlayingRoute(
    state: SpotfurryState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = onTogglePlayPause,
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
            item(key = "now-playing-header") {
                ListSectionHeader("当前播放")
            }
            item(key = "now-playing-track") {
                TrackCard(
                    title = state.currentTrack.title,
                    body = "${state.currentTrack.artist}\n${state.playbackSummary}",
                    onClick = {}
                )
            }
            item(key = "next") {
                ActionRowButton(
                    label = "下一首",
                    detail = state.nextTrackLabel,
                    onClick = onSkipNext
                )
            }
            item(key = "previous") {
                ActionRowButton(
                    label = "上一首",
                    detail = "重新播放当前歌曲或返回上一首",
                    onClick = onSkipPrevious
                )
            }
            item(key = "like") {
                ActionRowButton(
                    label = if (state.isLiked) "已喜欢" else "喜欢歌曲",
                    detail = "把这首歌加入手表收藏",
                    onClick = state::toggleLike
                )
            }
            item(key = "shuffle") {
                ActionRowButton(
                    label = if (state.shuffleEnabled) "随机已开" else "随机已关",
                    detail = "模式：${state.repeatMode.label}",
                    onClick = onToggleShuffle
                )
            }
            item(key = "repeat") {
                ActionRowButton(
                    label = "重复播放",
                    detail = "当前模式：${state.repeatMode.label}",
                    onClick = onCycleRepeat
                )
            }
            item(key = "volume-up") {
                ActionRowButton(
                    label = "音量 +5",
                    detail = "当前音量 ${state.volumePercent}%",
                    onClick = { onVolumeChange(5) }
                )
            }
            item(key = "volume-down") {
                ActionRowButton(
                    label = "音量 -5",
                    detail = "当前音量 ${state.volumePercent}%",
                    onClick = { onVolumeChange(-5) }
                )
            }
            item(key = "open-queue") {
                ActionRowButton(
                    label = "打开队列",
                    detail = "队列中共有 ${state.queue.size} 首歌",
                    onClick = onOpenQueue
                )
            }
            item(key = "open-library") {
                ActionRowButton(
                    label = "浏览音乐库",
                    detail = "切换到新的播放列表",
                    onClick = onOpenLibrary
                )
            }
        }
    }
}
