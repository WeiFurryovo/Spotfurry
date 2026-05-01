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
import com.weifurry.spotfurry.presentation.model.Playlist
import com.weifurry.spotfurry.presentation.player.SpotfurryState

@Composable
internal fun LibraryRoute(
    state: SpotfurryState,
    onOpenNowPlaying: () -> Unit,
    onOpenAppleMusic: () -> Unit,
    onOpenSpotifyWebPlayback: () -> Unit,
    onQueueLoaded: (Playlist) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()

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
            item(key = "library-header") {
                ListSectionHeader("音乐库")
            }
            item(key = "fixed-playlists") {
                TrackCard(
                    title = "固定歌单",
                    body = "已准备 ${state.playlists.size} 个适合手表场景的歌单",
                    onClick = {}
                )
            }
            item(key = "apple-music") {
                TrackCard(
                    title = "Apple Music 实验",
                    body = "检测 MusicKit SDK，登录后测试在手表内在线播放",
                    onClick = onOpenAppleMusic
                )
            }
            item(key = "spotify-web-playback") {
                TrackCard(
                    title = "Spotify WebView 实验",
                    body = "用 Web Playback SDK 尝试把手表变成 Spotify Connect 播放设备",
                    onClick = onOpenSpotifyWebPlayback
                )
            }
            state.playlists.forEach { playlist ->
                item(key = "playlist-${playlist.id}") {
                    TrackCard(
                        title = playlist.name,
                        body = "${playlist.subtitle}\n共 ${playlist.tracks.size} 首歌",
                        onClick = { onQueueLoaded(playlist) }
                    )
                }
            }
        }
    }
}
