package com.zeon.meplayer.presentation.screen.player

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.zeon.meplayer.core.playback.PlaybackManager
import com.zeon.meplayer.domain.model.Audio
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PlayerViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: PlayerViewModel
    private lateinit var mockPlaybackManager: PlaybackManager
    private val stateFlow = MutableStateFlow(PlaybackManager.PlaybackState())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockPlaybackManager = mockk(relaxed = true)
        every { mockPlaybackManager.state } returns stateFlow.asStateFlow()
        viewModel = PlayerViewModel()
    }

    @After
    fun tearDown() {
        viewModel.detach()
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `attach subscribes to playback state and updates flows`() = runTest {
        // Given
        val testSong = Audio(1, "path", "title", "artist", 10000)
        stateFlow.value = PlaybackManager.PlaybackState(
            currentSong = testSong,
            isPlaying = true,
            currentPosition = 5000L,
            duration = 10000L,
            shuffleEnabled = true,
            isMuted = true
        )

        // When
        viewModel.attach(mockPlaybackManager)

        // Then
        TestCase.assertEquals(testSong, viewModel.currentSong.value)
        TestCase.assertTrue(viewModel.isPlaying.value)
        TestCase.assertEquals(5000L, viewModel.currentPosition.value)
        TestCase.assertEquals(10000L, viewModel.duration.value)
        TestCase.assertTrue(viewModel.shuffleEnabled.value)
        TestCase.assertTrue(viewModel.isMuted.value)
    }

    @Test
    fun `state updates are collected correctly`() = runTest {
        // Given
        viewModel.attach(mockPlaybackManager)

        // When
        stateFlow.value = stateFlow.value.copy(isPlaying = true)

        // Then
        TestCase.assertEquals(true, viewModel.isPlaying.value)
    }

    @Test
    fun `detach cancels collection`() = runTest {
        // Given
        viewModel.attach(mockPlaybackManager)
        viewModel.detach()

        // When
        stateFlow.value = stateFlow.value.copy(isPlaying = true)

        // Then
        TestCase.assertEquals(false, viewModel.isPlaying.value)
    }

    @Test
    fun `playPause calls pause when playing`() = runTest {
        // Given
        stateFlow.value = stateFlow.value.copy(isPlaying = true)
        viewModel.attach(mockPlaybackManager)

        // When
        viewModel.playPause()

        // Then
        verify(exactly = 1) { mockPlaybackManager.pauseMusic() }
        verify(exactly = 0) { mockPlaybackManager.startMusic() }
    }

    @Test
    fun `playPause calls start when not playing`() = runTest {
        // Given
        stateFlow.value = stateFlow.value.copy(isPlaying = false)
        viewModel.attach(mockPlaybackManager)

        // When
        viewModel.playPause()

        // Then
        verify(exactly = 1) { mockPlaybackManager.startMusic() }
        verify(exactly = 0) { mockPlaybackManager.pauseMusic() }
    }

    @Test
    fun `playNext delegates to manager`() = runTest {
        // Given
        viewModel.attach(mockPlaybackManager)

        // When
        viewModel.playNext()

        // Then
        verify(exactly = 1) { mockPlaybackManager.playNext() }
    }

    @Test
    fun `playPrevious delegates to manager`() = runTest {
        // Given
        viewModel.attach(mockPlaybackManager)

        // When
        viewModel.playPrevious()

        // Then
        verify(exactly = 1) { mockPlaybackManager.playPrevious() }
    }

    @Test
    fun `seekTo delegates to manager`() = runTest {
        // Given
        viewModel.attach(mockPlaybackManager)

        // When
        viewModel.seekTo(12345L)

        // Then
        verify(exactly = 1) { mockPlaybackManager.seekTo(12345L) }
    }

    @Test
    fun `toggleShuffle delegates to manager`() = runTest {
        // Given
        viewModel.attach(mockPlaybackManager)

        // When
        viewModel.toggleShuffle()

        // Then
        verify(exactly = 1) { mockPlaybackManager.toggleShuffle() }
    }

    @Test
    fun `toggleMute delegates to manager`() = runTest {
        // Given
        viewModel.attach(mockPlaybackManager)

        // When
        viewModel.toggleMute()

        // Then
        verify(exactly = 1) { mockPlaybackManager.toggleMute() }
    }
}