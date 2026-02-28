package com.zeon.meplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeon.meplayer.model.Audio
import com.zeon.meplayer.service.MusicService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * ViewModel responsible for managing the state of the music player.
 * It holds the current track, playback status, position, duration,
 * and shuffle mode. Communication with the [MusicService] is done
 * via callbacks and a weak reference to avoid memory leaks.
 */
class PlayerViewModel : ViewModel() {

    private var musicServiceRef = WeakReference<MusicService>(null)
    private var musicService: MusicService?
        get() = musicServiceRef.get()
        set(value) {
            musicServiceRef = WeakReference(value)
        }

    private var positionUpdateJob: Job? = null

    private val _currentSong = MutableStateFlow<Audio?>(null)
    val currentSong: StateFlow<Audio?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val shuffleListener = object : MusicService.OnShuffleChangeListener {
        override fun onShuffleChanged(enabled: Boolean) {
            _shuffleEnabled.value = enabled
        }
    }

    private val muteListener = object : MusicService.OnMuteChangeListener {
        override fun onMuteChanged(muted: Boolean) {
            _isMuted.value = muted
        }
    }

    private val callback = object : MusicService.MusicServiceCallback {
        override fun onSongChanged(position: Int, isPlaying: Boolean) {
            _currentSong.value = musicService?.musicList?.getOrNull(position)
            _isPlaying.value = isPlaying
            _duration.value = musicService?.getDuration() ?: 0
            updatePositionUpdating()
        }
    }

    fun setService(service: MusicService) {
        musicService?.removeCallback(callback)
        musicService?.removeShuffleListener(shuffleListener)
        musicService?.removeMuteListener(muteListener)

        musicService = service
        service.addCallback(callback)
        service.addShuffleListener(shuffleListener)
        service.addMuteListener(muteListener)

        _currentSong.value = service.getCurrentSong()
        _isPlaying.value = service.isPlaying()
        _isMuted.value = service.isMuted()
        _duration.value = service.getDuration()
        _shuffleEnabled.value = service.isShuffleEnabled()

        updatePositionUpdating()
    }

    private fun updatePositionUpdating() {
        if (_isPlaying.value) {
            startUpdatingPosition()
        } else {
            stopUpdatingPosition()
        }
    }

    private fun startUpdatingPosition() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                _currentPosition.value = musicService?.getCurrentPosition() ?: 0
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopUpdatingPosition() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun toggleShuffle() { musicService?.toggleShuffle() }

    fun seekTo(position: Int) {
        musicService?.seekTo(position)
        _currentPosition.value = position
    }

    fun playPause() {
        musicService?.let {
            if (it.isPlaying()) {
                it.pauseMusic()
                stopUpdatingPosition()
            } else {
                it.startMusic()
                startUpdatingPosition()
            }
        }
    }

    fun playNext() { musicService?.playNext() }

    fun playPrevious() { musicService?.playPrevious() }

    fun toggleMute() {
        musicService?.toggleMute()
    }

    override fun onCleared() {
        super.onCleared()
        musicService?.removeCallback(callback)
        musicService?.removeShuffleListener(shuffleListener)
        musicService?.removeMuteListener(muteListener)
        stopUpdatingPosition()
        musicService = null
    }

    companion object {
        private const val POSITION_UPDATE_INTERVAL_MS = 500L
    }
}