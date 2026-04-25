package com.weifurry.spotfurry.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.weifurry.spotfurry.presentation.navigation.AppleMusicScreen
import com.weifurry.spotfurry.presentation.navigation.AppleMusicPairingScreen
import com.weifurry.spotfurry.presentation.navigation.HomeScreen
import com.weifurry.spotfurry.presentation.navigation.LibraryScreen
import com.weifurry.spotfurry.presentation.navigation.NowPlayingScreen
import com.weifurry.spotfurry.presentation.navigation.QueueScreen
import com.weifurry.spotfurry.presentation.navigation.SpotfurryScreen
import com.weifurry.spotfurry.presentation.navigation.SpotifyWebPlaybackScreen
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
