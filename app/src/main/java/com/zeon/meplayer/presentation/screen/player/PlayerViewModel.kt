package com.zeon.meplayer.presentation.screen.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeon.meplayer.core.playback.PlaybackManager
import com.zeon.meplayer.domain.model.Audio
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state of the music player.
 * It holds the current track, playback status, position, duration,
 * and shuffle mode. Communication with the [com.zeon.meplayer.core.service.MusicService] is done
 * via callbacks and a weak reference to avoid memory leaks.
 */
class PlayerViewModel : ViewModel() {
    private var playbackManager: PlaybackManager? = null
    private var stateJob: Job? = null

    private val _currentSong = MutableStateFlow<Audio?>(null)
    val currentSong: StateFlow<Audio?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    fun attach(manager: PlaybackManager) {
        if (playbackManager === manager) return
        detach()
        playbackManager = manager
        stateJob = viewModelScope.launch {
            manager.state.collect { state ->
                _currentSong.value = state.currentSong
                _isPlaying.value = state.isPlaying
                _currentPosition.value = state.currentPosition
                _duration.value = state.duration
                _shuffleEnabled.value = state.shuffleEnabled
                _isMuted.value = state.isMuted
            }
        }
    }

    fun detach() {
        stateJob?.cancel()
        stateJob = null
        playbackManager = null
    }

    fun playPause() {
        playbackManager?.let {
            if (it.state.value.isPlaying) it.pauseMusic()
            else it.startMusic()
        }
    }

    fun playNext() = playbackManager?.playNext()
    fun playPrevious() = playbackManager?.playPrevious()
    fun seekTo(position: Long) = playbackManager?.seekTo(position)
    fun toggleShuffle() = playbackManager?.toggleShuffle()
    fun toggleMute() = playbackManager?.toggleMute()

    override fun onCleared() {
        super.onCleared()
        detach()
    }
}