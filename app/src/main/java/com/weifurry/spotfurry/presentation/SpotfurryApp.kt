package com.weifurry.spotfurry.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.weifurry.spotfurry.data.spotify.SpotifyApiException
import com.weifurry.spotfurry.data.spotify.SpotifyPlaybackClient
import com.weifurry.spotfurry.data.spotify.SpotifyPlaybackSnapshot
import com.weifurry.spotfurry.data.spotify.SpotifyTokenStore
import com.weifurry.spotfurry.data.spotify.SpotifyWebPlaybackConfig
import com.weifurry.spotfurry.presentation.navigation.AppleMusicScreen
import com.weifurry.spotfurry.presentation.navigation.AppleMusicPairingScreen
import com.weifurry.spotfurry.presentation.navigation.HomeScreen
import com.weifurry.spotfurry.presentation.navigation.LibraryScreen
import com.weifurry.spotfurry.presentation.navigation.NowPlayingScreen
import com.weifurry.spotfurry.presentation.navigation.QueueScreen
import com.weifurry.spotfurry.presentation.navigation.SpotfurryScreen
import com.weifurry.spotfurry.presentation.navigation.SpotifyWebPlaybackScreen
import com.weifurry.spotfurry.presentation.model.Track
import com.weifurry.spotfurry.presentation.player.SpotfurryState
import com.weifurry.spotfurry.presentation.routes.HomeRoute
import com.weifurry.spotfurry.presentation.routes.AppleMusicPairingRoute
import com.weifurry.spotfurry.presentation.routes.AppleMusicRoute
import com.weifurry.spotfurry.presentation.routes.LibraryRoute
import com.weifurry.spotfurry.presentation.routes.NowPlayingRoute
import com.weifurry.spotfurry.presentation.routes.QueueRoute
import com.weifurry.spotfurry.presentation.routes.SpotifyWebPlaybackRoute
import com.weifurry.spotfurry.presentation.theme.SpotfurryTheme
import kotlinx.coroutines.delay

@Composable
internal fun SpotfurryApp() {
    val context = LocalContext.current
    val appState = remember { SpotfurryState.preview() }
    val backStack = rememberNavBackStack(HomeScreen)
    val spotifyConfig = remember(context) { SpotifyWebPlaybackConfig.fromResources(context) }
    val spotifyTokenStore = remember(context) { SpotifyTokenStore(context) }
    val spotifyPlaybackClient = remember { SpotifyPlaybackClient() }

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

    LaunchedEffect(spotifyConfig.accessToken) {
        while (true) {
            val accessToken = spotifyConfig.accessToken.ifBlank { spotifyTokenStore.validAccessToken() }
            if (accessToken.isBlank()) {
                delay(SPOTIFY_IDLE_POLL_INTERVAL_MS)
                continue
            }

            var nextPollIntervalMs = SPOTIFY_IDLE_POLL_INTERVAL_MS
            spotifyPlaybackClient
                .currentPlayback(accessToken)
                .onSuccess { playback ->
                    if (playback == null) {
                        appState.showSpotifyPlaybackNotice(
                            title = "未检测到 Spotify 播放",
                            detail = "请先在 Spotify 里播放一首歌"
                        )
                        return@onSuccess
                    }

                    appState.syncSpotifyPlayback(
                        track = playback.toTrack(),
                        progress = playback.progress,
                        isPlaying = playback.isPlaying,
                        deviceName = playback.deviceName
                    )
                    nextPollIntervalMs =
                        if (playback.isPlaying) {
                            SPOTIFY_ACTIVE_POLL_INTERVAL_MS
                        } else {
                            SPOTIFY_IDLE_POLL_INTERVAL_MS
                        }
                }
                .onFailure { error ->
                    val apiException = error.spotifyApiException()
                    when (apiException?.statusCode) {
                        HTTP_UNAUTHORIZED -> {
                            spotifyTokenStore.clear()
                            appState.showSpotifyPlaybackNotice(
                                title = "Spotify 登录已过期",
                                detail = "请重新扫码登录"
                            )
                        }

                        HTTP_FORBIDDEN -> {
                            appState.showSpotifyPlaybackNotice(
                                title = "Spotify 权限不足",
                                detail = "清除登录后重新扫码授权"
                            )
                        }

                        else -> {
                            appState.showSpotifyPlaybackNotice(
                                title = "无法读取 Spotify",
                                detail = error.message ?: "网络或 Spotify API 异常"
                            )
                        }
                    }
                }

            delay(nextPollIntervalMs)
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
                                onOpenAppleMusic = { navigateTo(AppleMusicScreen) },
                                onOpenSpotifyWebPlayback = {
                                    navigateTo(SpotifyWebPlaybackScreen)
                                },
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
                        entry<AppleMusicScreen> {
                            AppleMusicRoute(
                                onOpenLibrary = { navigateTo(LibraryScreen) },
                                onOpenPairingLogin = { navigateTo(AppleMusicPairingScreen) }
                            )
                        }
                        entry<AppleMusicPairingScreen> {
                            AppleMusicPairingRoute(
                                onBack = {
                                    if (backStack.size > 1) {
                                        backStack.removeLastOrNull()
                                    }
                                }
                            )
                        }
                        entry<SpotifyWebPlaybackScreen> {
                            SpotifyWebPlaybackRoute(
                                onBack = {
                                    if (backStack.size > 1) {
                                        backStack.removeLastOrNull()
                                    }
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

private fun SpotifyPlaybackSnapshot.toTrack(): Track =
    Track(
        id = id,
        title = title,
        artist = artist,
        durationSeconds = durationSeconds
    )

private fun Throwable.spotifyApiException(): SpotifyApiException? {
    var current: Throwable? = this
    while (current != null) {
        if (current is SpotifyApiException) {
            return current
        }
        current = current.cause
    }
    return null
}

private const val SPOTIFY_ACTIVE_POLL_INTERVAL_MS = 5_000L
private const val SPOTIFY_IDLE_POLL_INTERVAL_MS = 10_000L
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
