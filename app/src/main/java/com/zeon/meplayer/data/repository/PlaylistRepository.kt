package com.zeon.meplayer.data.repository

import com.zeon.meplayer.domain.model.Playlist
import com.zeon.meplayer.domain.model.PlaylistWithSongs
import com.zeon.meplayer.data.local.AppDatabase
import com.zeon.meplayer.data.local.entity.PlaylistEntity
import com.zeon.meplayer.data.local.entity.PlaylistSongEntity
import com.zeon.meplayer.domain.model.toPlaylist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class PlaylistRepository(
    private val db: AppDatabase,
    private val musicRepository: MusicRepository
) {
    private val playlistDao = db.playlistDao()
    private val playlistSongDao = db.playlistSongDao()

    fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { entities ->
            entities.map { it.toPlaylist() }
        }

    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?> =
        playlistSongDao.getSongIdsForPlaylist(playlistId).combine(
            musicRepository.musicList
        ) { songIds, allSongs ->
            val songs = allSongs.filter { it.id in songIds }
            songs.sortedBy { song -> songIds.indexOf(song.id) }
        }.map { songs ->
            val playlistEntity = playlistDao.getAllPlaylists().firstOrNull()?.find { it.id == playlistId }
            playlistEntity?.let { PlaylistWithSongs(it.toPlaylist(), songs) }
        }

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name))

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(PlaylistEntity(id = playlistId, name = ""))
        playlistSongDao.deleteAllSongs(playlistId)
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        val currentMaxPosition = playlistSongDao.getMaxPosition(playlistId) ?: -1
        val newEntries = songIds.mapIndexed { index, songId ->
            PlaylistSongEntity(playlistId, songId, currentMaxPosition + 1 + index)
        }
        playlistSongDao.insertAllSongs(newEntries)
    }


    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistSongDao.deleteSongById(playlistId, songId)
    }

    suspend fun reorderSongs(playlistId: Long, newOrder: List<Long>) {
        playlistSongDao.deleteAllSongs(playlistId)
        val entries = newOrder.mapIndexed { index, songId ->
            PlaylistSongEntity(playlistId, songId, index)
        }
        playlistSongDao.insertAllSongs(entries)
    }
}