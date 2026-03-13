package com.zeon.meplayer.core.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class PlayerController(context: Context) {
    private var isSwitchingTrack = false
    private var isSeeking = false

    var onPlaybackStateChanged: ((Int) -> Unit)? = null
    var onMediaItemTransition: ((MediaItem?, Int) -> Unit)? = null
    var onPlaybackResumed: (() -> Unit)? = null
    var onPlaybackPaused: (() -> Unit)? = null
    var onPositionDiscontinuitySkip: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> resetFlags()
            }
            onPlaybackStateChanged?.invoke(playbackState)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            onMediaItemTransition?.invoke(mediaItem, reason)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying && (isSwitchingTrack || isSeeking)) return

            if (isPlaying) {
                isSwitchingTrack = false
                isSeeking = false
                onPlaybackResumed?.invoke()
            } else {
                onPlaybackPaused?.invoke()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SKIP) {
                isSeeking = false
                onPositionDiscontinuitySkip?.invoke()
            }
        }
    }

    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(playerListener)
    }

    val isPlaying: Boolean get() = player.isPlaying
    val currentPosition: Long get() = player.currentPosition
    val duration: Long get() = if (player.duration != C.TIME_UNSET) player.duration else 0

    var isMuted: Boolean = false
        private set

    fun setMuted(muted: Boolean) {
        isMuted = muted
        player.volume = if (muted) 0f else 1f
    }

    fun prepareAndPause(path: String, position: Long, muted: Boolean) {
        val mediaItem = MediaItem.fromUri(path)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.seekTo(position)
        player.pause()
        setMuted(muted)
    }

    fun play(path: String) {
        isSwitchingTrack = true
        val mediaItem = MediaItem.fromUri(path)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun start() {
        player.play()
    }

    fun seekTo(position: Long) {
        isSeeking = true
        player.seekTo(position)
    }

    fun stop() {
        player.stop()
    }

    fun release() {
        player.release()
    }

    fun getExoPlayer(): ExoPlayer = player

    private fun resetFlags() {
        isSwitchingTrack = false
        isSeeking = false
    }
}