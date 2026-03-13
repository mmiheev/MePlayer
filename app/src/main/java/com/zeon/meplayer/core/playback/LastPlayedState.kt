package com.zeon.meplayer.core.playback

data class LastPlayedState(
    val songId: Long,
    val position: Long,
    val shuffleEnabled: Boolean,
    val isMuted: Boolean
)