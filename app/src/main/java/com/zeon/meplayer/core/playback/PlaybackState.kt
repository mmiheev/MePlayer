package com.zeon.meplayer.core.playback

import com.zeon.meplayer.domain.model.Audio

data class PlaybackState(
    val currentSong: Audio? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val isMuted: Boolean = false
)