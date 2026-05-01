package com.weifurry.spotfurry.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.weifurry.spotfurry.data.spotify.SpotifyRepeatMode
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
import com.weifurry.spotfurry.presentation.model.RepeatMode
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
import kotlinx.coroutines.launch

@Composable
internal fun SpotfurryApp() {
    val context = LocalContext.current
    val appState = remember { SpotfurryState.preview() }
    val backStack = rememberNavBackStack(HomeScreen)
    val spotifyConfig = remember(context) { SpotifyWebPlaybackConfig.fromResources(context) }
    val spotifyTokenStore = remember(context) { SpotifyTokenStore(context) }
    val spotifyPlaybackClient = remember { SpotifyPlaybackClient() }
    val coroutineScope = rememberCoroutineScope()

    fun navigateTo(screen: SpotfurryScreen) {
        if (backStack.lastOrNull() != screen) {
            backStack.add(screen)
        }
    }

    fun currentSpotifyAccessToken(): String =
        spotifyConfig.accessToken.ifBlank { spotifyTokenStore.validAccessToken() }

    fun applySpotifyPlaybackFailure(error: Throwable) {
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

            HTTP_NO_ACTIVE_DEVICE -> {
                appState.showSpotifyPlaybackNotice(
                    title = "没有可用播放设备",
                    detail = "请先在 Spotify 里选择一个播放设备"
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

    suspend fun refreshSpotifyPlayback(accessToken: String): Boolean {
        var isPlaying = false
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
                    shuffleEnabled = playback.shuffleEnabled,
                    repeatMode = playback.repeatMode.toRepeatMode(),
                    volumePercent = playback.volumePercent,
                    deviceName = playback.deviceName
                )
                isPlaying = playback.isPlaying
            }
            .onFailure(::applySpotifyPlaybackFailure)

        return isPlaying
    }

    fun runSpotifyCommand(
        localAction: () -> Unit,
        command: suspend (String) -> Result<Unit>
    ) {
        if (!appState.isSpotifyPlayback) {
            localAction()
            return
        }

        val accessToken = currentSpotifyAccessToken()
        if (accessToken.isBlank()) {
            appState.showSpotifyPlaybackNotice(
                title = "Spotify 未登录",
                detail = "请重新扫码登录"
            )
            return
        }

        coroutineScope.launch {
            command(accessToken)
                .onSuccess {
                    refreshSpotifyPlayback(accessToken)
                }
                .onFailure(::applySpotifyPlaybackFailure)
        }
    }

    fun togglePlayPause() {
        val shouldPause = appState.isPlaying
        runSpotifyCommand(
            localAction = appState::togglePlayPause,
            command = { accessToken ->
                if (shouldPause) {
                    spotifyPlaybackClient.pause(accessToken)
                } else {
                    spotifyPlaybackClient.resume(accessToken)
                }
            }
        )
    }

    fun skipNext() {
        runSpotifyCommand(
            localAction = appState::skipNext,
            command = spotifyPlaybackClient::skipNext
        )
    }

    fun skipPrevious() {
        runSpotifyCommand(
            localAction = appState::skipPrevious,
            command = spotifyPlaybackClient::skipPrevious
        )
    }

    fun changeVolume(delta: Int) {
        val targetVolume = (appState.volumePercent + delta).coerceIn(0, 100)
        runSpotifyCommand(
            localAction = { appState.changeVolume(delta) },
            command = { accessToken ->
                spotifyPlaybackClient.setVolume(accessToken, targetVolume)
            }
        )
    }

    fun toggleShuffle() {
        val targetShuffle = !appState.shuffleEnabled
        runSpotifyCommand(
            localAction = appState::toggleShuffle,
            command = { accessToken ->
                spotifyPlaybackClient.setShuffle(accessToken, targetShuffle)
            }
        )
    }

    fun cycleRepeat() {
        val targetRepeat = appState.repeatMode.nextSpotifyRepeatMode()
        runSpotifyCommand(
            localAction = appState::cycleRepeat,
            command = { accessToken ->
                spotifyPlaybackClient.setRepeatMode(accessToken, targetRepeat)
            }
        )
    }

    LaunchedEffect(appState.isPlaying, appState.currentTrack.id) {
        while (appState.isPlaying) {
            delay(1_000)
            appState.advancePreviewBySeconds(1)
        }
    }

    LaunchedEffect(spotifyConfig.accessToken) {
        while (true) {
            val accessToken = currentSpotifyAccessToken()
            if (accessToken.isBlank()) {
                delay(SPOTIFY_IDLE_POLL_INTERVAL_MS)
                continue
            }

            val isPlaying = refreshSpotifyPlayback(accessToken)
            delay(
                if (isPlaying) {
                    SPOTIFY_ACTIVE_POLL_INTERVAL_MS
                } else {
                    SPOTIFY_IDLE_POLL_INTERVAL_MS
                }
            )
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
                                onTogglePlayPause = ::togglePlayPause,
                                onSkipPrevious = ::skipPrevious,
                                onSkipNext = ::skipNext,
                                onVolumeUp = { changeVolume(5) },
                                onToggleShuffle = ::toggleShuffle,
                                onOpenNowPlaying = { navigateTo(NowPlayingScreen) },
                                onOpenLibrary = { navigateTo(LibraryScreen) },
                                onOpenQueue = { navigateTo(QueueScreen) }
                            )
                        }
                        entry<NowPlayingScreen> {
                            NowPlayingRoute(
                                state = appState,
                                onTogglePlayPause = ::togglePlayPause,
                                onSkipNext = ::skipNext,
                                onSkipPrevious = ::skipPrevious,
                                onToggleShuffle = ::toggleShuffle,
                                onCycleRepeat = ::cycleRepeat,
                                onVolumeChange = ::changeVolume,
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
                                onSkipNext = ::skipNext,
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

private fun SpotifyRepeatMode.toRepeatMode(): RepeatMode =
    when (this) {
        SpotifyRepeatMode.Off -> RepeatMode.Off
        SpotifyRepeatMode.Context -> RepeatMode.Queue
        SpotifyRepeatMode.Track -> RepeatMode.Track
    }

private fun RepeatMode.nextSpotifyRepeatMode(): SpotifyRepeatMode =
    when (this) {
        RepeatMode.Off -> SpotifyRepeatMode.Context
        RepeatMode.Queue -> SpotifyRepeatMode.Track
        RepeatMode.Track -> SpotifyRepeatMode.Off
    }

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
private const val HTTP_NO_ACTIVE_DEVICE = 404
