package com.zeon.meplayer.core.playback

import com.zeon.meplayer.domain.model.Audio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlaybackStateManager {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun updateCurrentSong(song: Audio?) {
        _state.update { it.copy(currentSong = song) }
    }

    fun updateIsPlaying(isPlaying: Boolean) {
        _state.update { it.copy(isPlaying = isPlaying) }
    }

    fun updateCurrentPosition(position: Long) {
        _state.update { it.copy(currentPosition = position) }
    }

    fun updateDuration(duration: Long) {
        _state.update { it.copy(duration = duration) }
    }

    fun updateShuffleEnabled(enabled: Boolean) {
        _state.update { it.copy(shuffleEnabled = enabled) }
    }

    fun updateIsMuted(muted: Boolean) {
        _state.update { it.copy(isMuted = muted) }
    }

    fun resetToEmpty() {
        _state.update {
            it.copy(
                currentSong = null,
                isPlaying = false,
                currentPosition = 0,
                duration = 0
            )
        }
    }

    fun prepareForPlayback(song: Audio) {
        _state.update {
            it.copy(
                currentSong = song,
                currentPosition = 0,
                duration = 0
            )
        }
    }
}