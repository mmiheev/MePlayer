package com.zeon.meplayer.core.playback

import com.zeon.meplayer.data.local.datastore.LastPlayedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LastPlayedStateManager(
    private val prefs: LastPlayedPreferences,
    private val scope: CoroutineScope
) {
    fun restoreIfNeeded(
        playlistManager: PlaylistManager,
        playerController: PlayerController,
        stateManager: PlaybackStateManager,
        onRestoreCompleted: () -> Unit
    ) {
        if (playlistManager.hasCurrentSong()) {
            onRestoreCompleted()
            return
        }

        scope.launch {
            val savedState = prefs.getLastPlayedState()
            if (savedState != null) {
                val index = playlistManager.musicList.indexOfFirst { it.id == savedState.songId }
                if (index != -1) {
                    playlistManager.setCurrentIndex(index)
                    stateManager.prepareForPlayback(playlistManager.getCurrentSong()!!)
                    stateManager.updateShuffleEnabled(savedState.shuffleEnabled)
                    stateManager.updateIsMuted(savedState.isMuted)

                    playerController.prepareAndPause(
                        path = playlistManager.getCurrentSong()!!.path,
                        position = savedState.position,
                        muted = savedState.isMuted
                    )

                    if (savedState.shuffleEnabled) {
                        playlistManager.toggleShuffle()
                    }

                    onRestoreCompleted()
                } else {
                    prefs.clearState()
                    onRestoreCompleted()
                }
            } else {
                onRestoreCompleted()
            }
        }
    }

    fun saveCurrent(playlistManager: PlaylistManager, playerController: PlayerController) {
        val song = playlistManager.getCurrentSong()
        if (song != null) {
            val state = LastPlayedState(
                songId = song.id,
                position = playerController.currentPosition,
                shuffleEnabled = playlistManager.isShuffleEnabled(),
                isMuted = playerController.isMuted
            )
            scope.launch { prefs.saveState(state) }
        } else {
            scope.launch { prefs.clearState() }
        }
    }
}