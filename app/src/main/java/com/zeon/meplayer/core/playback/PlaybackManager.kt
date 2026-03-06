package com.zeon.meplayer.core.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zeon.meplayer.domain.model.Audio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages audio playback.
 * Controls ExoPlayer, playback state, playlist, shuffle, mute, and position updates.
 */
class PlaybackManager(context: Context) {

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> playNext()
                Player.STATE_READY -> updateDuration()
                Player.STATE_IDLE -> resetFlags()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentSongInfo()
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                getCurrentSong()?.let { onPlaybackStarted?.invoke(it) }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying && (isSwitchingTrack || isSeeking)) return

            _state.update { it.copy(isPlaying = isPlaying) }

            if (isPlaying) {
                onPlaybackResumed()
            } else {
                onPlaybackPaused()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == 3) {
                isSeeking = false
            }
        }
    }

    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(playerListener)
    }

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    var musicList: List<Audio> = emptyList()
        set(value) {
            field = value
            val currentSongId = getCurrentSong()?.id
            if (currentSongId != null) {
                val newIndex = value.indexOfFirst { it.id == currentSongId }
                currentIndex = if (newIndex != -1) newIndex else -1
            } else {
                currentIndex = -1
            }

            updateShuffleOrderIfNeeded()

            if (currentIndex == -1) {
                player.stop()
                _state.update {
                    it.copy(
                        currentSong = null,
                        isPlaying = false,
                        currentPosition = 0,
                        duration = 0
                    )
                }
                stopPositionUpdates()
            }
        }

    private var currentIndex = -1

    private var shuffleEnabled = false
    private var shuffleOrder: MutableList<Int>? = null
    private var shuffleIndex = 0

    private var isMuted = false
    private var isSwitchingTrack = false
    private var isSeeking = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionUpdateJob: Job? = null

    var onPlaybackStarted: ((Audio) -> Unit)? = null
    var onPlaybackPaused: (() -> Unit)? = null

    var audioFocusHandler: (() -> Boolean)? = null

    init { player.volume = 1f }

    private fun getCurrentSong(): Audio? = musicList.getOrNull(currentIndex)

    private fun updateCurrentSongInfo() {
        val song = getCurrentSong()
        _state.update {
            it.copy(
                currentSong = song,
                duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0,
                currentPosition = player.currentPosition
            )
        }
    }

    private fun updateDuration() {
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0
        _state.update { it.copy(duration = duration) }
    }

    private fun resetFlags() {
        isSwitchingTrack = false
        isSeeking = false
    }

    private fun onPlaybackResumed() {
        isSwitchingTrack = false
        isSeeking = false
        startPositionUpdates()
        getCurrentSong()?.let { onPlaybackStarted?.invoke(it) }
    }

    private fun onPlaybackPaused() {
        stopPositionUpdates()
        onPlaybackPaused?.invoke()
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (player.isPlaying) {
                _state.update { it.copy(currentPosition = player.currentPosition) }
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun playMusic(index: Int) {
        if (musicList.isEmpty() || index !in musicList.indices) return
        if (currentIndex == index && player.isPlaying) return
        if (audioFocusHandler?.invoke() == false) return

        currentIndex = index
        val song = musicList[index]

        _state.update {
            it.copy(
                currentSong = song,
                currentPosition = 0,
                duration = 0,
                shuffleEnabled = shuffleEnabled,
                isMuted = isMuted
            )
        }

        isSwitchingTrack = true

        val mediaItem = MediaItem.fromUri(song.path)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        player.volume = if (isMuted) 0f else 1f
    }

    fun pauseMusic() {
        player.pause()
    }

    fun startMusic() {
        if (!player.isPlaying && currentIndex != -1) {
            if (audioFocusHandler?.invoke() == false) return
            player.play()
        }
    }

    fun playNext() {
        if (musicList.isEmpty() || currentIndex == -1) return
        val nextIndex = if (shuffleEnabled) getNextShuffleIndex() else (currentIndex + 1) % musicList.size
        playMusic(nextIndex)
    }

    fun playPrevious() {
        if (musicList.isEmpty() || currentIndex == -1) return
        val prevIndex = if (shuffleEnabled) getPreviousShuffleIndex() else {
            if (currentIndex - 1 < 0) musicList.lastIndex else currentIndex - 1
        }
        playMusic(prevIndex)
    }

    fun seekTo(position: Long) {
        isSeeking = true
        player.seekTo(position)
        _state.update { it.copy(currentPosition = position) }
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        if (shuffleEnabled) {
            buildShuffleOrder()
        } else {
            shuffleOrder = null
        }
        _state.update { it.copy(shuffleEnabled = shuffleEnabled) }
    }

    fun toggleMute() {
        isMuted = !isMuted
        player.volume = if (isMuted) 0f else 1f
        _state.update { it.copy(isMuted = isMuted) }
    }

    fun release() {
        scope.cancel()
        player.release()
    }

    fun getPlayer(): ExoPlayer = player

    private fun buildShuffleOrder(startIndex: Int = currentIndex) {
        if (musicList.isEmpty()) {
            shuffleOrder = null
            return
        }
        val indices = musicList.indices.toMutableList()
        indices.remove(startIndex)
        indices.shuffle()
        shuffleOrder = mutableListOf(startIndex).apply { addAll(indices) }
        shuffleIndex = 0
    }

    private fun getNextShuffleIndex(): Int {
        val order = shuffleOrder ?: return (currentIndex + 1) % musicList.size
        if (shuffleIndex + 1 < order.size) {
            shuffleIndex++
            return order[shuffleIndex]
        } else {
            buildShuffleOrder(currentIndex)
            return shuffleOrder!!.getOrElse(1) { currentIndex }
        }
    }

    private fun getPreviousShuffleIndex(): Int {
        val order = shuffleOrder ?: return (if (currentIndex - 1 < 0) musicList.lastIndex else currentIndex - 1)
        if (shuffleIndex - 1 >= 0) {
            shuffleIndex--
            return order[shuffleIndex]
        } else {
            shuffleIndex = order.lastIndex
            return order[shuffleIndex]
        }
    }

    private fun updateShuffleOrderIfNeeded() {
        if (shuffleEnabled) {
            buildShuffleOrder(currentIndex.takeIf { it in musicList.indices } ?: 0)
        }
    }

    data class PlaybackState(
        val currentSong: Audio? = null,
        val isPlaying: Boolean = false,
        val currentPosition: Long = 0L,
        val duration: Long = 0L,
        val shuffleEnabled: Boolean = false,
        val isMuted: Boolean = false
    )

    companion object {
        private const val POSITION_UPDATE_INTERVAL_MS = 500L
    }
}