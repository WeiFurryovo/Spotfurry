package com.weifurry.spotfurry.presentation.routes

import androidx.compose.runtime.Composable
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.weifurry.spotfurry.presentation.components.ListSectionHeader
import com.weifurry.spotfurry.presentation.components.TrackCard
import com.weifurry.spotfurry.presentation.model.Track
import com.weifurry.spotfurry.presentation.player.SpotfurryState

@Composable
internal fun QueueRoute(
    state: SpotfurryState,
    onSkipNext: () -> Unit,
    onSelectTrack: (Track) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = onSkipNext,
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
            item(key = "queue-header") {
                ListSectionHeader("播放队列")
            }
            state.queue.forEach { track ->
                item(key = "queue-${track.id}") {
                    TrackCard(
                        title = track.title,
                        body =
                            if (track.id == state.currentTrack.id) {
                                "${track.artist}  ${track.durationLabel}\n正在播放"
                            } else {
                                "${track.artist}  ${track.durationLabel}"
                            },
                        onClick = { onSelectTrack(track) }
                    )
                }
            }
        }
    }
}
