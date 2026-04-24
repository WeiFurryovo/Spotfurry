package com.weifurry.spotfurry.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotfurryStateTest {
    private val tracks =
        listOf(
            Track("t1", "Track 1", "Artist", 100),
            Track("t2", "Track 2", "Artist", 100),
            Track("t3", "Track 3", "Artist", 100)
        )

    private val playlist = Playlist("p1", "Playlist", "Subtitle", tracks)

    private fun state(
        initialTrackId: String = tracks.first().id,
        shuffler: (List<String>) -> List<String> = { it.reversed() }
    ): SpotfurryState =
        SpotfurryState(
            playlists = listOf(playlist),
            initialQueue = tracks,
            initialTrackId = initialTrackId,
            shuffleTrackIds = shuffler
        )

    @Test
    fun repeatOffAdvancesUntilQueueEndThenStops() {
        val state = state(initialTrackId = "t2")
        state.cycleRepeat()
        state.cycleRepeat()

        state.advancePreviewBySeconds(100)

        assertEquals("t3", state.currentTrack.id)
        assertEquals(0f, state.progress, 0.0001f)
        assertTrue(state.isPlaying)

        state.advancePreviewBySeconds(100)

        assertEquals("t3", state.currentTrack.id)
        assertEquals(1f, state.progress, 0.0001f)
        assertFalse(state.isPlaying)
    }

    @Test
    fun shuffleUsesGeneratedOrderInsteadOfFixedIndexStep() {
        val state = state(initialTrackId = "t1")

        state.toggleShuffle()
        state.skipNext()
        assertEquals("t3", state.currentTrack.id)

        state.skipNext()
        assertEquals("t2", state.currentTrack.id)
    }

    @Test
    fun likedStateBelongsToCurrentTrack() {
        val state = state(initialTrackId = "t1")

        state.toggleLike()
        assertTrue(state.isLiked)

        state.skipNext()
        assertFalse(state.isLiked)

        state.skipPrevious()
        assertTrue(state.isLiked)
    }

    @Test
    fun emptyPlaylistStopsPlayback() {
        val state = state()
        val emptyPlaylist = Playlist("empty", "Empty", "No tracks", emptyList())

        state.loadPlaylist(emptyPlaylist)

        assertEquals("暂无播放", state.currentTrack.title)
        assertFalse(state.isPlaying)
        assertFalse(state.isLiked)
    }
}
