package com.zeon.meplayer.manager

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zeon.meplayer.model.Audio
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

class PlaybackManager(context: Context) {

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> playNext()
                Player.STATE_READY -> {
                    _state.update { it.copy(duration = player.duration.toInt()) }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val song = getCurrentSong()
            _state.update {
                it.copy(
                    currentSong = song,
                    duration = player.duration.toInt(),
                    currentPosition = player.currentPosition.toInt()
                )
            }
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                song?.let { onPlaybackStarted?.invoke(it) }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPositionUpdates()
                getCurrentSong()?.let { onPlaybackStarted?.invoke(it) }
            } else {
                stopPositionUpdates()
                onPlaybackPaused?.invoke()
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
            updateShuffleOrderIfNeeded()
        }

    private var currentIndex = -1

    private var shuffleEnabled = false
    private var shuffleOrder: MutableList<Int>? = null
    private var shuffleIndex = 0

    private var isMuted = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionJob: Job? = null

    var onPlaybackStarted: ((Audio) -> Unit)? = null
    var onPlaybackPaused: (() -> Unit)? = null

    init { player.volume = 1f }

    private fun getCurrentSong(): Audio? =
        if (currentIndex in musicList.indices) musicList[currentIndex] else null

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (player.isPlaying) {
                _state.update { it.copy(currentPosition = player.currentPosition.toInt()) }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    fun playMusic(index: Int) {
        if (musicList.isEmpty()) return
        if (currentIndex == index && player.isPlaying) return

        currentIndex = index
        val song = musicList[index]

        _state.update {
            it.copy(
                currentSong = song,
                isPlaying = true,
                currentPosition = 0,
                duration = 0,
                shuffleEnabled = shuffleEnabled,
                isMuted = isMuted
            )
        }

        val mediaItem = MediaItem.fromUri(song.path)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        player.volume = if (isMuted) 0f else 1f
    }

    fun pauseMusic() { player.pause() }

    fun startMusic() {
        if (!player.isPlaying && currentIndex != -1) { player.play() }
    }

    fun playNext() {
        if (musicList.isEmpty() || currentIndex == -1) return

        val nextIndex = if (shuffleEnabled) getNextShuffleIndex() else (currentIndex + 1) % musicList.size
        playMusic(nextIndex)
    }

    fun playPrevious() {
        if (musicList.isEmpty() || currentIndex == -1) return

        val prevIndex = if (shuffleEnabled) getPreviousShuffleIndex() else {
            if (currentIndex - 1 < 0) musicList.size - 1 else currentIndex - 1
        }
        playMusic(prevIndex)
    }

    fun seekTo(position: Int) {
        player.seekTo(position.toLong())
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
            return if (shuffleOrder!!.size > 1) shuffleOrder!![1] else currentIndex
        }
    }

    private fun getPreviousShuffleIndex(): Int {
        val order = shuffleOrder ?: return (if (currentIndex - 1 < 0) musicList.size - 1 else currentIndex - 1)
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
            buildShuffleOrder(if (currentIndex in musicList.indices) currentIndex else 0)
        }
    }

    fun release() {
        scope.cancel()
        player.release()
    }

    data class PlaybackState(
        val currentSong: Audio? = null,
        val isPlaying: Boolean = false,
        val currentPosition: Int = 0,
        val duration: Int = 0,
        val shuffleEnabled: Boolean = false,
        val isMuted: Boolean = false
    )
}