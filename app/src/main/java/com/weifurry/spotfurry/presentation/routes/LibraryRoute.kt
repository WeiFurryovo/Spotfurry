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
import com.weifurry.spotfurry.presentation.components.TrackCard
import com.weifurry.spotfurry.presentation.model.Playlist
import com.weifurry.spotfurry.presentation.player.SpotfurryState

@Composable
internal fun LibraryRoute(
    state: SpotfurryState,
    onOpenNowPlaying: () -> Unit,
    onOpenAppleMusic: () -> Unit,
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
            item {
                TrackCard(
                    title = "Apple Music 实验",
                    body = "检测 MusicKit SDK，登录后测试在手表内在线播放",
                    onClick = onOpenAppleMusic,
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
