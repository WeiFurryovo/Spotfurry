package com.weifurry.spotfurry.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface SpotfurryScreen : NavKey

@Serializable
internal data object HomeScreen : SpotfurryScreen

@Serializable
internal data object NowPlayingScreen : SpotfurryScreen

@Serializable
internal data object LibraryScreen : SpotfurryScreen

@Serializable
internal data object QueueScreen : SpotfurryScreen

@Serializable
internal data object AppleMusicScreen : SpotfurryScreen
