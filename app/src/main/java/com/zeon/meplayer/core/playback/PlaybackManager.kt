package com.zeon.meplayer.core.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zeon.meplayer.data.local.datastore.LastPlayedPreferences
import com.zeon.meplayer.domain.model.Audio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Manages audio playback.
 * Controls ExoPlayer, playback state, playlist, shuffle, mute, and position updates.
 */
open class PlaybackManager(context: Context) {
    private val stateManager = PlaybackStateManager()
    private val playlistManager = PlaylistManager()
    private val playerController = PlayerController(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionUpdateJob: Job? = null

    private var lastPlayedManager: LastPlayedStateManager? = null

    var onPlaybackStarted: ((Audio) -> Unit)? = null
    var onPlaybackPaused: (() -> Unit)? = null
    var audioFocusHandler: (() -> Boolean)? = null

    init {
        playerController.onPlaybackStateChanged = { playbackState ->
            when (playbackState) {
                Player.STATE_ENDED -> playNext()
                Player.STATE_READY -> stateManager.updateDuration(playerController.duration)
                Player.STATE_IDLE -> resetFlags()
            }
        }

        playerController.onMediaItemTransition = { mediaItem, reason ->
            updateCurrentSongInfo()
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
            ) {
                getCurrentSong()?.let { onPlaybackStarted?.invoke(it) }
            }
        }

        playerController.onPlaybackResumed = {
            stateManager.updateIsPlaying(true)
            startPositionUpdates()
            getCurrentSong()?.let { onPlaybackStarted?.invoke(it) }
        }

        playerController.onPlaybackPaused = {
            stateManager.updateIsPlaying(false)
            stopPositionUpdates()
            onPlaybackPaused?.invoke()
        }

        playerController.onPositionDiscontinuitySkip = { }
    }

    private var allSongs: List<Audio> = emptyList()

    fun setAllSongs(songs: List<Audio>) {
        allSongs = songs
    }

    fun setPlaylist(songs: List<Audio>) {
        musicList = songs
        if (!playlistManager.hasCurrentSong()) {
            playerController.stop()
            stateManager.resetToEmpty()
        }
    }

    fun resetToAllSongs() {
        if (allSongs.isNotEmpty()) musicList = allSongs
    }

    val state: StateFlow<PlaybackState> = stateManager.state

    var musicList: List<Audio>
        get() = playlistManager.musicList
        set(value) {
            val currentSongId = getCurrentSong()?.id
            playlistManager.setList(value, currentSongId)

            if (!playlistManager.hasCurrentSong()) {
                playerController.stop()
                stateManager.resetToEmpty()
                stopPositionUpdates()
            }

            lastPlayedManager?.let { restoreLastPlayedState() }
        }

    fun playMusic(index: Int) {
        if (!playlistManager.musicList.indices.contains(index)) return
        if (playlistManager.currentIndex == index && playerController.isPlaying) return
        if (audioFocusHandler?.invoke() == false) return

        playlistManager.setCurrentIndex(index)

        val song = playlistManager.getCurrentSong()!!
        stateManager.prepareForPlayback(song)
        stateManager.updateShuffleEnabled(playlistManager.isShuffleEnabled())

        playerController.play(song.path)
        playerController.setMuted(stateManager.state.value.isMuted)

        saveCurrentState()
    }

    open fun pauseMusic() {
        playerController.pause()
        saveCurrentState()
    }

    open fun startMusic() {
        if (!playerController.isPlaying && playlistManager.hasCurrentSong()) {
            if (audioFocusHandler?.invoke() == false) return
            playerController.start()
        }
    }

    open fun playNext() {
        val nextIndex = playlistManager.playNext() ?: return
        playMusic(nextIndex)
    }

    open fun playPrevious() {
        val prevIndex = playlistManager.playPrevious() ?: return
        playMusic(prevIndex)
    }

    open fun seekTo(position: Long) {
        playerController.seekTo(position)
        stateManager.updateCurrentPosition(position)
        saveCurrentState()
    }

    open fun toggleShuffle() {
        val enabled = playlistManager.toggleShuffle()
        stateManager.updateShuffleEnabled(enabled)
        saveCurrentState()
    }

    open fun toggleMute() {
        val isMuted = !state.value.isMuted
        playerController.setMuted(isMuted)
        stateManager.updateIsMuted(isMuted)
        saveCurrentState()
    }

    fun release() {
        saveCurrentState()
        scope.cancel()
        playerController.release()
    }

    fun setLastPlayedPreferences(prefs: LastPlayedPreferences) {
        lastPlayedManager = LastPlayedStateManager(prefs, scope)
        if (playlistManager.musicList.isNotEmpty()) {
            restoreLastPlayedState()
        }
    }

    fun getPlayer(): ExoPlayer = playerController.getExoPlayer()

    private fun getCurrentSong(): Audio? = playlistManager.getCurrentSong()

    private fun updateCurrentSongInfo() {
        val song = getCurrentSong()
        stateManager.updateCurrentSong(song)
        stateManager.updateDuration(playerController.duration)
        stateManager.updateCurrentPosition(playerController.currentPosition)
    }

    private fun resetFlags() {}

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (playerController.isPlaying) {
                stateManager.updateCurrentPosition(playerController.currentPosition)
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun restoreLastPlayedState() {
        lastPlayedManager?.restoreIfNeeded(
            playlistManager = playlistManager,
            playerController = playerController,
            stateManager = stateManager
        ) {
            updateCurrentSongInfo()
        }
    }

    private fun saveCurrentState() {
        lastPlayedManager?.saveCurrent(playlistManager, playerController)
    }

    companion object {
        private const val POSITION_UPDATE_INTERVAL_MS = 500L
    }
}