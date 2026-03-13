package com.zeon.meplayer.core.playback

import com.zeon.meplayer.data.local.datastore.LastPlayedPreferences
import com.zeon.meplayer.domain.model.Audio
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LastPlayedStateManagerTest {

    private lateinit var prefs: LastPlayedPreferences
    private lateinit var playlistManager: PlaylistManager
    private lateinit var playerController: PlayerController
    private lateinit var stateManager: PlaybackStateManager
    private lateinit var manager: LastPlayedStateManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        prefs = mockk()
        playlistManager = mockk(relaxed = true)
        playerController = mockk(relaxed = true)
        stateManager = mockk(relaxed = true)

        val scope = CoroutineScope(testDispatcher)
        manager = LastPlayedStateManager(prefs, scope)
    }

    @Test
    fun `restoreIfNeeded should call onRestoreCompleted even if current song exists`() = runTest(testDispatcher) {
        every { playlistManager.hasCurrentSong() } returns true
        var restoreCalled = false

        manager.restoreIfNeeded(playlistManager, playerController, stateManager) {
            restoreCalled = true
        }

        advanceUntilIdle()

        assertTrue("Callback should be called", restoreCalled)
        coVerify(exactly = 0) { prefs.getLastPlayedState() }
        coVerify(exactly = 0) { prefs.saveState(any()) }
        coVerify(exactly = 0) { prefs.clearState() }
    }

    @Test
    fun `restoreIfNeeded should call onRestoreCompleted when no saved state`() = runTest(testDispatcher) {
        every { playlistManager.hasCurrentSong() } returns false
        coEvery { prefs.getLastPlayedState() } returns null
        var restoreCalled = false

        manager.restoreIfNeeded(playlistManager, playerController, stateManager) {
            restoreCalled = true
        }

        advanceUntilIdle()

        assertTrue("Callback should be called", restoreCalled)
        coVerify(exactly = 1) { prefs.getLastPlayedState() }
        coVerify(exactly = 0) { prefs.clearState() }
        verify(exactly = 0) { playlistManager.setCurrentIndex(any()) }
        verify(exactly = 0) { stateManager.prepareForPlayback(any()) }
        verify(exactly = 0) { playerController.prepareAndPause(any(), any(), any()) }
    }

    @Test
    fun `restoreIfNeeded should clear state and call onRestoreCompleted when song not found`() = runTest(testDispatcher) {
        val savedState = LastPlayedState(songId = 999L, position = 5000L, shuffleEnabled = false, isMuted = false)
        every { playlistManager.hasCurrentSong() } returns false
        coEvery { prefs.getLastPlayedState() } returns savedState
        every { playlistManager.musicList } returns emptyList()
        coEvery { prefs.clearState() } just runs
        var restoreCalled = false

        manager.restoreIfNeeded(playlistManager, playerController, stateManager) {
            restoreCalled = true
        }

        advanceUntilIdle()

        assertTrue("Callback should be called", restoreCalled)
        coVerifySequence {
            prefs.getLastPlayedState()
            prefs.clearState()
        }
        verify(exactly = 0) { playlistManager.setCurrentIndex(any()) }
        verify(exactly = 0) { stateManager.prepareForPlayback(any()) }
        verify(exactly = 0) { playerController.prepareAndPause(any(), any(), any()) }
    }

    @Test
    fun `restoreIfNeeded should restore when state exists and song found (shuffle disabled)`() = runTest(testDispatcher) {
        val savedState = LastPlayedState(songId = 1L, position = 5000L, shuffleEnabled = false, isMuted = true)
        val audio = Audio(
            id = 1L,
            path = "path/to/song",
            title = "Test Title",
            artist = "Test Artist",
            duration = 180000L,
            dateAdded = 123456L
        )
        every { playlistManager.hasCurrentSong() } returns false
        coEvery { prefs.getLastPlayedState() } returns savedState
        every { playlistManager.musicList } returns listOf(audio)
        every { playlistManager.getCurrentSong() } returns audio
        var restoreCalled = false

        manager.restoreIfNeeded(playlistManager, playerController, stateManager) {
            restoreCalled = true
        }

        advanceUntilIdle()

        assertTrue("Callback should be called", restoreCalled)
        verifySequence {
            playlistManager.hasCurrentSong()
            playlistManager.musicList
            playlistManager.setCurrentIndex(0)
            playlistManager.getCurrentSong()
            stateManager.prepareForPlayback(audio)
            stateManager.updateShuffleEnabled(false)
            stateManager.updateIsMuted(true)
            playlistManager.getCurrentSong()
            playerController.prepareAndPause("path/to/song", 5000L, true)
        }
        verify(exactly = 0) { playlistManager.toggleShuffle() }
        coVerify(exactly = 0) { prefs.clearState() }
    }

    @Test
    fun `restoreIfNeeded should restore with shuffle enabled and call toggleShuffle`() = runTest(testDispatcher) {
        val savedState = LastPlayedState(songId = 1L, position = 5000L, shuffleEnabled = true, isMuted = false)
        val audio = Audio(
            id = 1L,
            path = "path/to/song",
            title = "Test Title",
            artist = "Test Artist",
            duration = 180000L,
            dateAdded = 123456L
        )
        every { playlistManager.hasCurrentSong() } returns false
        coEvery { prefs.getLastPlayedState() } returns savedState
        every { playlistManager.musicList } returns listOf(audio)
        every { playlistManager.getCurrentSong() } returns audio
        var restoreCalled = false

        manager.restoreIfNeeded(playlistManager, playerController, stateManager) {
            restoreCalled = true
        }

        advanceUntilIdle()

        assertTrue("Callback should be called", restoreCalled)
        verifySequence {
            playlistManager.hasCurrentSong()
            playlistManager.musicList
            playlistManager.setCurrentIndex(0)
            playlistManager.getCurrentSong()
            stateManager.prepareForPlayback(audio)
            stateManager.updateShuffleEnabled(true)
            stateManager.updateIsMuted(false)
            playlistManager.getCurrentSong()
            playerController.prepareAndPause("path/to/song", 5000L, false)
            playlistManager.toggleShuffle()
        }
    }

    @Test
    fun `saveCurrent should save state when song exists`() = runTest(testDispatcher) {
        val audio = Audio(
            id = 1L,
            path = "path/to/song",
            title = "Test Title",
            artist = "Test Artist",
            duration = 180000L,
            dateAdded = 123456L
        )
        every { playlistManager.getCurrentSong() } returns audio
        every { playlistManager.isShuffleEnabled() } returns true
        every { playerController.currentPosition } returns 12345L
        every { playerController.isMuted } returns false
        coEvery { prefs.saveState(any()) } just runs

        manager.saveCurrent(playlistManager, playerController)

        advanceUntilIdle()

        coVerify { prefs.saveState(match {
            it.songId == 1L &&
                    it.position == 12345L &&
                    it.shuffleEnabled == true &&
                    !it.isMuted
        }) }
        coVerify(exactly = 0) { prefs.clearState() }
    }

    @Test
    fun `saveCurrent should clear state when no song`() = runTest(testDispatcher) {
        every { playlistManager.getCurrentSong() } returns null
        coEvery { prefs.clearState() } just runs

        manager.saveCurrent(playlistManager, playerController)

        advanceUntilIdle()

        coVerify(exactly = 1) { prefs.clearState() }
        coVerify(exactly = 0) { prefs.saveState(any()) }
    }
}