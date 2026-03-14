package com.zeon.meplayer.core.playback

import com.zeon.meplayer.domain.model.Audio
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class PlaylistManagerTest {

    private lateinit var manager: PlaylistManager
    private val song1 = Audio(1, "/path/1", "Title1", "Artist1", 180000)
    private val song2 = Audio(2, "/path/2", "Title2", "Artist2", 200000)
    private val song3 = Audio(3, "/path/3", "Title3", "Artist3", 210000)
    private val song4 = Audio(4, "/path/4", "Title4", "Artist4", 220000)

    @Before
    fun setUp() {
        manager = PlaylistManager()
    }

    @Test
    fun `initial state`() {
        assertTrue(manager.musicList.isEmpty())
        assertEquals(-1, manager.currentIndex)
        assertFalse(manager.isShuffleEnabled())
        assertNull(manager.getCurrentSong())
        assertFalse(manager.hasCurrentSong())
    }

    @Test
    fun `setList without currentSongId`() {
        val list = listOf(song1, song2, song3)
        manager.setList(list)

        assertEquals(list, manager.musicList)
        assertEquals(-1, manager.currentIndex)
        assertFalse(manager.hasCurrentSong())
        assertNull(manager.getCurrentSong())
    }

    @Test
    fun `setList with existing currentSongId`() {
        val list = listOf(song1, song2, song3)
        manager.setList(list, currentSongId = 2)

        assertEquals(list, manager.musicList)
        assertEquals(1, manager.currentIndex)
        assertEquals(song2, manager.getCurrentSong())
        assertTrue(manager.hasCurrentSong())
    }

    @Test
    fun `setList with non-existing currentSongId`() {
        val list = listOf(song1, song2, song3)
        manager.setList(list, currentSongId = 99)

        assertEquals(list, manager.musicList)
        assertEquals(-1, manager.currentIndex)
        assertFalse(manager.hasCurrentSong())
    }

    @Test
    fun `setList with empty list`() {
        manager.setList(emptyList())
        assertTrue(manager.musicList.isEmpty())
        assertEquals(-1, manager.currentIndex)
    }

    @Test
    fun `setList when shuffle enabled rebuilds order with current index`() {
        val list = listOf(song1, song2, song3, song4)
        manager.setList(list, currentSongId = 3)
        manager.toggleShuffle()

        val order = manager.getShuffleOrderForTest()
        assertNotNull(order)
        assertEquals(2, order!![0])
        assertEquals(list.indices.toSet(), order.toSet())
    }

    @Test
    fun `setList when shuffle enabled but currentIndex invalid uses 0 as start`() {
        val list = listOf(song1, song2, song3)
        manager.setList(list)
        manager.toggleShuffle()

        val order = manager.getShuffleOrderForTest()
        assertNotNull(order)
        assertEquals(0, order!![0])
        assertEquals(list.indices.toSet(), order.toSet())
    }

    @Test
    fun `setList when shuffle enabled and list empty does not build order`() {
        manager.toggleShuffle()
        manager.setList(emptyList())

        assertNull(manager.getShuffleOrderForTest())
    }

    @Test
    fun `setCurrentIndex with valid index`() {
        manager.setList(listOf(song1, song2, song3))
        manager.setCurrentIndex(1)
        assertEquals(1, manager.currentIndex)
        assertEquals(song2, manager.getCurrentSong())
    }

    @Test
    fun `setCurrentIndex with -1`() {
        manager.setList(listOf(song1, song2, song3))
        manager.setCurrentIndex(-1)
        assertEquals(-1, manager.currentIndex)
        assertNull(manager.getCurrentSong())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `setCurrentIndex throws on index out of bounds - above`() {
        manager.setList(listOf(song1, song2, song3))
        manager.setCurrentIndex(3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `setCurrentIndex throws on index out of bounds - below -1`() {
        manager.setList(listOf(song1, song2, song3))
        manager.setCurrentIndex(-2)
    }

    @Test
    fun `setCurrentIndex on empty list only allows -1`() {
        manager.setList(emptyList())
        manager.setCurrentIndex(-1)
        try {
            manager.setCurrentIndex(0)
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun `getCurrentSong returns correct song`() {
        manager.setList(listOf(song1, song2, song3), currentSongId = 2)
        assertEquals(song2, manager.getCurrentSong())
        manager.setCurrentIndex(0)
        assertEquals(song1, manager.getCurrentSong())
    }

    @Test
    fun `hasCurrentSong returns true only when index in range`() {
        manager.setList(listOf(song1, song2, song3))
        assertFalse(manager.hasCurrentSong())
        manager.setCurrentIndex(1)
        assertTrue(manager.hasCurrentSong())
        manager.setCurrentIndex(-1)
        assertFalse(manager.hasCurrentSong())
    }

    @Test
    fun `playNext without shuffle`() {
        manager.setList(listOf(song1, song2, song3), currentSongId = 1)

        assertEquals(1, manager.playNext())
        manager.setCurrentIndex(1)
        assertEquals(2, manager.playNext())
        manager.setCurrentIndex(2)
        assertEquals(2, manager.playNext())
        manager.setCurrentIndex(-1)
        assertNull(manager.playNext())
    }

    @Test
    fun `playPrevious without shuffle`() {
        manager.setList(listOf(song1, song2, song3), currentSongId = 3)

        assertEquals(1, manager.playPrevious())
        manager.setCurrentIndex(1)
        assertEquals(0, manager.playPrevious())
        manager.setCurrentIndex(0)
        assertEquals(0, manager.playPrevious())
        manager.setCurrentIndex(-1)
        assertNull(manager.playPrevious())
    }

    @Test
    fun `playNext and playPrevious with single song`() {
        manager.setList(listOf(song1), currentSongId = 1)
        assertEquals(0, manager.playNext())
        assertEquals(0, manager.playPrevious())
    }

    @Test
    fun `playNext and playPrevious with empty list`() {
        manager.setList(emptyList())
        assertNull(manager.playNext())
        assertNull(manager.playPrevious())
    }

    @Test
    fun `toggleShuffle enables and disables shuffle`() {
        assertFalse(manager.isShuffleEnabled())
        assertTrue(manager.toggleShuffle())
        assertTrue(manager.isShuffleEnabled())
        assertFalse(manager.toggleShuffle())
        assertFalse(manager.isShuffleEnabled())
    }

    @Test
    fun `toggleShuffle on non-empty list builds order when enabling`() {
        manager.setList(listOf(song1, song2, song3), currentSongId = 2)
        manager.toggleShuffle()
        val order = manager.getShuffleOrderForTest()
        assertNotNull(order)
        assertEquals(1, order!![0])
        assertEquals(setOf(0, 1, 2), order.toSet())
    }

    @Test
    fun `toggleShuffle on empty list does not build order`() {
        manager.toggleShuffle()
        assertNull(manager.getShuffleOrderForTest())
    }

    @Test
    fun `toggleShuffle disables and clears order`() {
        manager.setList(listOf(song1, song2, song3))
        manager.toggleShuffle()
        assertNotNull(manager.getShuffleOrderForTest())
        manager.toggleShuffle()
        assertFalse(manager.isShuffleEnabled())
        assertNull(manager.getShuffleOrderForTest())
    }

    @Test
    fun `playNext with shuffle returns next indices and updates shuffleIndex`() {
        manager.setList(listOf(song1, song2, song3), currentSongId = 1)
        manager.toggleShuffle()
        val firstNext = manager.playNext() ?: error("should not be null")
        val secondNext = manager.playNext() ?: error("should not be null")
        val thirdNext = manager.playNext() ?: error("should not be null")
        val fourthNext = manager.playNext() ?: error("should not be null")
        assertEquals(thirdNext, fourthNext)
    }

    @Test
    fun `playNext with shuffle on single song`() {
        manager.setList(listOf(song1), currentSongId = 1)
        manager.toggleShuffle()
        val order = manager.getShuffleOrderForTest()
        assertNotNull(order)
        assertEquals(listOf(0), order)

        assertEquals(0, manager.playNext())
        assertEquals(0, manager.playNext())
    }

    @Test
    fun `playNext with shuffle after exhausting order rebuilds and returns second element`() {
        manager.setList(listOf(song1, song2), currentSongId = 1)
        manager.toggleShuffle()
        assertEquals(1, manager.playNext())
        assertEquals(1, manager.playNext())
        assertEquals(1, manager.playNext())
    }

    @Test
    fun `playPrevious with shuffle on two-song list`() {
        manager.setList(listOf(song1, song2), currentSongId = 1)
        manager.toggleShuffle()
        assertEquals(1, manager.playPrevious())
        assertEquals(0, manager.playPrevious())
        assertEquals(1, manager.playPrevious())
    }

    @Test
    fun `playPrevious with shuffle on single song`() {
        manager.setList(listOf(song1), currentSongId = 1)
        manager.toggleShuffle()
        assertEquals(0, manager.playPrevious())
        assertEquals(0, manager.playPrevious())
    }

    @Test
    fun `setCurrentIndex does not update shuffle order`() {
        manager.setList(listOf(song1, song2, song3), currentSongId = 1)
        manager.toggleShuffle()
        val orderBefore = manager.getShuffleOrderForTest()!!.toList()
        manager.setCurrentIndex(2)
        assertEquals(orderBefore, manager.getShuffleOrderForTest())
    }

    @Test
    fun `setList when shuffle enabled rebuilds order using new list`() {
        manager.setList(listOf(song1, song2, song3), currentSongId = 2)
        manager.toggleShuffle()
        val oldOrder = manager.getShuffleOrderForTest()!!.toList()
        manager.setList(listOf(song1, song2, song3, song4), currentSongId = 2)
        val newOrder = manager.getShuffleOrderForTest()!!
        assertNotEquals(oldOrder, newOrder)
        assertEquals(4, newOrder.size)
        assertEquals(1, newOrder[0])
        assertEquals(setOf(0, 1, 2, 3), newOrder.toSet())
    }

    @Test
    fun `playNext returns null when no current song`() {
        manager.setList(listOf(song1, song2, song3))
        assertNull(manager.playNext())
        assertNull(manager.playPrevious())
    }

    @Test
    fun `shuffle methods work when list becomes empty after enabling shuffle`() {
        manager.setList(listOf(song1, song2, song3), currentSongId = 1)
        manager.toggleShuffle()
        manager.setList(emptyList())
        assertNull(manager.playNext())
        assertNull(manager.playPrevious())
        assertNull(manager.getShuffleOrderForTest())
    }
}

/**
 * Extension function to access private shuffleOrder for testing.
 * Uses reflection – acceptable for unit tests in the same module.
 */
fun PlaylistManager.getShuffleOrderForTest(): MutableList<Int>? {
    val field = this::class.java.getDeclaredField("shuffleOrder")
    field.isAccessible = true
    return field.get(this) as? MutableList<Int>
}