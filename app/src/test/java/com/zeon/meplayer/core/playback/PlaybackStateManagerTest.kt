package com.zeon.meplayer.core.playback

import com.zeon.meplayer.domain.model.Audio
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackStateManagerTest {

    private lateinit var manager: PlaybackStateManager
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        manager = PlaybackStateManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`(): Unit = runTest {
        val state = manager.state.value
        assertEquals(null, state.currentSong)
        assertEquals(false, state.isPlaying)
        assertEquals(0L, state.currentPosition)
        assertEquals(0L, state.duration)
        assertEquals(false, state.shuffleEnabled)
        assertEquals(false, state.isMuted)
    }

    @Test
    fun `updateCurrentSong changes song`() = runTest {
        val song = Audio(1, "path", "title", "artist", 1000)
        manager.updateCurrentSong(song)
        assertEquals(song, manager.state.value.currentSong)
    }

    @Test
    fun `updateCurrentSong with null sets song to null`() = runTest {
        manager.updateCurrentSong(null)
        assertEquals(null, manager.state.value.currentSong)
    }

    @Test
    fun `updateIsPlaying changes playing flag`() = runTest {
        manager.updateIsPlaying(true)
        assertEquals(true, manager.state.value.isPlaying)
        manager.updateIsPlaying(false)
        assertEquals(false, manager.state.value.isPlaying)
    }

    @Test
    fun `updateCurrentPosition changes position`() = runTest {
        manager.updateCurrentPosition(12345)
        assertEquals(12345L, manager.state.value.currentPosition)
    }

    @Test
    fun `updateDuration changes duration`() = runTest {
        manager.updateDuration(300000)
        assertEquals(300000L, manager.state.value.duration)
    }

    @Test
    fun `updateShuffleEnabled changes shuffle flag`() = runTest {
        manager.updateShuffleEnabled(true)
        assertEquals(true, manager.state.value.shuffleEnabled)
        manager.updateShuffleEnabled(false)
        assertEquals(false, manager.state.value.shuffleEnabled)
    }

    @Test
    fun `updateIsMuted changes muted flag`() = runTest {
        manager.updateIsMuted(true)
        assertEquals(true, manager.state.value.isMuted)
        manager.updateIsMuted(false)
        assertEquals(false, manager.state.value.isMuted)
    }

    @Test
    fun `resetToEmpty resets playback fields but leaves other flags`() = runTest {
        val song = Audio(1, "p", "t", "a", 1000)
        manager.updateCurrentSong(song)
        manager.updateIsPlaying(true)
        manager.updateCurrentPosition(5000)
        manager.updateDuration(200000)
        manager.updateShuffleEnabled(true)
        manager.updateIsMuted(true)

        manager.resetToEmpty()

        val state = manager.state.value
        assertEquals(null, state.currentSong)
        assertEquals(false, state.isPlaying)
        assertEquals(0L, state.currentPosition)
        assertEquals(0L, state.duration)
        assertEquals(true, state.shuffleEnabled)
        assertEquals(true, state.isMuted)
    }

    @Test
    fun `prepareForPlayback sets song and resets position and duration`() = runTest {
        manager.updateCurrentPosition(12345)
        manager.updateDuration(67890)
        manager.updateIsPlaying(true)

        val song = Audio(2, "path2", "title2", "artist2", 2000)
        manager.prepareForPlayback(song)

        val state = manager.state.value
        assertEquals(song, state.currentSong)
        assertEquals(0L, state.currentPosition)
        assertEquals(0L, state.duration)
        assertEquals(true, state.isPlaying)
    }

    @Test
    fun `multiple updates produce distinct states`() = runTest {
        val song1 = Audio(1, "p1", "t1", "a1", 1000)
        val song2 = Audio(2, "p2", "t2", "a2", 2000)

        manager.updateCurrentSong(song1)
        assertEquals(song1, manager.state.value.currentSong)

        manager.updateCurrentSong(song2)
        assertEquals(song2, manager.state.value.currentSong)

        manager.updateIsPlaying(true)
        assertEquals(true, manager.state.value.isPlaying)

        manager.updateCurrentPosition(5000)
        assertEquals(5000L, manager.state.value.currentPosition)
    }
}