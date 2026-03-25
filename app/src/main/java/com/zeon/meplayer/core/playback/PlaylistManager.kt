package com.zeon.meplayer.core.playback

import com.zeon.meplayer.domain.model.Audio

class PlaylistManager {
    var musicList: List<Audio> = emptyList()
        private set

    var currentIndex: Int = -1
        private set

    private var shuffleEnabled = false
    private var shuffleOrder: MutableList<Int>? = null
    private var shuffleIndex = 0

    fun setList(newList: List<Audio>, currentSongId: Long? = null) {
        musicList = newList
        currentIndex = if (currentSongId != null) {
            newList.indexOfFirst { it.id == currentSongId }
        } else {
            -1
        }
        if (shuffleEnabled) {
            buildShuffleOrder(currentIndex.takeIf { it in newList.indices } ?: 0)
        }
    }

    fun setCurrentIndex(index: Int) {
        require(index in -1..musicList.lastIndex) { "Index out of bounds" }
        currentIndex = index
    }

    fun getCurrentSong(): Audio? = musicList.getOrNull(currentIndex)

    fun hasCurrentSong(): Boolean = currentIndex in musicList.indices

    fun playNext(): Int? {
        if (!hasCurrentSong()) return null
        return if (shuffleEnabled) getNextShuffleIndex() else (currentIndex + 1).coerceAtMost(musicList.lastIndex)
    }

    fun playPrevious(): Int? {
        if (!hasCurrentSong()) return null
        return if (shuffleEnabled) getPreviousShuffleIndex() else (currentIndex - 1).coerceAtLeast(0)
    }

    fun toggleShuffle(): Boolean {
        shuffleEnabled = !shuffleEnabled
        if (shuffleEnabled) {
            buildShuffleOrder(currentIndex.takeIf { it in musicList.indices } ?: 0)
        } else {
            shuffleOrder = null
        }
        return shuffleEnabled
    }

    fun isShuffleEnabled(): Boolean = shuffleEnabled

    private fun buildShuffleOrder(startIndex: Int) {
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
        return if (shuffleIndex + 1 < order.size) {
            shuffleIndex++
            order[shuffleIndex]
        } else {
            buildShuffleOrder(currentIndex)
            shuffleOrder!!.getOrElse(1) { currentIndex }
        }
    }

    private fun getPreviousShuffleIndex(): Int {
        val order = shuffleOrder ?: return (if (currentIndex - 1 < 0) musicList.lastIndex else currentIndex - 1)
        return if (shuffleIndex - 1 >= 0) {
            shuffleIndex--
            order[shuffleIndex]
        } else {
            shuffleIndex = order.lastIndex
            order[shuffleIndex]
        }
    }
}