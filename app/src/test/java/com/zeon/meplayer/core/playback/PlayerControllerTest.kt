package com.zeon.meplayer.core.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class PlayerControllerTest {

    private lateinit var mockPlayer: ExoPlayer
    private lateinit var controller: PlayerController
    private lateinit var capturedListener: Player.Listener
    private val capturedListenerSlot = slot<Player.Listener>()

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any<String>()) } returns mockk<Uri>(relaxed = true)

        mockkStatic(MediaItem::class)
        every { MediaItem.fromUri(any<String>()) } returns mockk<MediaItem>(relaxed = true)

        mockPlayer = mockk(relaxed = true)
        every { mockPlayer.addListener(capture(capturedListenerSlot)) } answers {
            capturedListener = arg(0)
        }
        controller = PlayerController(mockPlayer)
        verify { mockPlayer.addListener(any()) }

        // 🟢 Очищаем историю вызовов, чтобы убрать addListener из последовательности
        clearMocks(mockPlayer)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // === Воспроизведение ===

    @Test
    fun `play should set media, prepare, play and set switching flag`() {
        val path = "file:///test.mp3"
        controller.play(path)

        verifySequence {
            mockPlayer.setMediaItem(any())
            mockPlayer.prepare()
            mockPlayer.play()
        }

        var pausedCalled = false
        controller.onPlaybackPaused = { pausedCalled = true }

        capturedListener.onIsPlayingChanged(false)
        assertFalse(pausedCalled)

        var resumedCalled = false
        controller.onPlaybackResumed = { resumedCalled = true }
        capturedListener.onIsPlayingChanged(true)
        assertTrue(resumedCalled)

        pausedCalled = false
        capturedListener.onIsPlayingChanged(false)
        assertTrue(pausedCalled)
    }

    @Test
    fun `prepareAndPause should set media, prepare, seek, pause and set muted`() {
        val path = "path"
        val position = 5000L
        val muted = true

        controller.prepareAndPause(path, position, muted)

        verifySequence {
            mockPlayer.setMediaItem(any())
            mockPlayer.prepare()
            mockPlayer.seekTo(position)
            mockPlayer.pause()
            mockPlayer.setVolume(0f)
        }
        assertTrue(controller.isMuted)
    }

    @Test
    fun `pause should call player pause`() {
        controller.pause()
        verify(exactly = 1) { mockPlayer.pause() }
    }

    @Test
    fun `start should call player play`() {
        controller.start()
        verify(exactly = 1) { mockPlayer.play() }
    }

    @Test
    fun `stop should call player stop`() {
        controller.stop()
        verify(exactly = 1) { mockPlayer.stop() }
    }

    // === Seek ===

    @Test
    fun `seekTo should set seeking flag and call player seekTo`() {
        val position = 1234L
        controller.seekTo(position)
        verify { mockPlayer.seekTo(position) }

        val listener = capturedListener
        var skipCalled = false
        controller.onPositionDiscontinuitySkip = { skipCalled = true }

        val oldPos = mockk<Player.PositionInfo>(relaxed = true)
        val newPos = mockk<Player.PositionInfo>(relaxed = true)
        listener.onPositionDiscontinuity(oldPos, newPos, Player.DISCONTINUITY_REASON_SKIP)
        assertTrue(skipCalled)

        var pausedCalled = false
        controller.onPlaybackPaused = { pausedCalled = true }
        listener.onIsPlayingChanged(false)
        assertTrue(pausedCalled)
    }

    // === Mute ===

    @Test
    fun `setMuted should change volume and update isMuted`() {
        controller.setMuted(true)
        assertTrue(controller.isMuted)
        verify { mockPlayer.volume = 0f }

        controller.setMuted(false)
        assertFalse(controller.isMuted)
        verify { mockPlayer.volume = 1f }
    }

    // === Release ===

    @Test
    fun `release should call player release`() {
        controller.release()
        verify { mockPlayer.release() }
    }

    // === Колбэк onPlaybackStateChanged и сброс флагов при STATE_IDLE ===

    @Test
    fun `onPlaybackStateChanged should be invoked and reset flags on STATE_IDLE`() {
        val listener = capturedListener
        var capturedState = -1
        controller.onPlaybackStateChanged = { state -> capturedState = state }

        listener.onPlaybackStateChanged(Player.STATE_IDLE)
        assertEquals(Player.STATE_IDLE, capturedState)

        var pausedCalled = false
        controller.onPlaybackPaused = { pausedCalled = true }
        listener.onIsPlayingChanged(false)
        assertTrue(pausedCalled)
    }

    // === Дополнительно: геттеры ===

    @Test
    fun `isPlaying should delegate to player`() {
        every { mockPlayer.isPlaying } returns true
        assertTrue(controller.isPlaying)

        every { mockPlayer.isPlaying } returns false
        assertFalse(controller.isPlaying)
    }

    @Test
    fun `currentPosition should delegate to player`() {
        every { mockPlayer.currentPosition } returns 12345L
        assertEquals(12345L, controller.currentPosition)
    }

    @Test
    fun `duration should handle TIME_UNSET`() {
        every { mockPlayer.duration } returns C.TIME_UNSET
        assertEquals(0L, controller.duration)

        every { mockPlayer.duration } returns 180000L
        assertEquals(180000L, controller.duration)
    }

    // === onMediaItemTransition callback ===

    @Test
    fun `onMediaItemTransition should be invoked from listener`() {
        val listener = capturedListener
        val mockMediaItem = mockk<MediaItem>(relaxed = true)

        var invoked = false
        var capturedReason = -1
        controller.onMediaItemTransition = { item, reason ->
            assertNotNull(item)
            invoked = true
            capturedReason = reason
        }

        listener.onMediaItemTransition(
            mockMediaItem,
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
        )
        assertTrue(invoked)
        assertEquals(Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED, capturedReason)
    }
}