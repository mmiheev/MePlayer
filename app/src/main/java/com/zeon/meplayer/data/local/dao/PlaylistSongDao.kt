package com.zeon.meplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zeon.meplayer.data.local.entity.PlaylistSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistSongDao {
    @Insert
    suspend fun insertSong(playlistSong: PlaylistSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSongs(playlistSongs: List<PlaylistSongEntity>)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<Long>>

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteAllSongs(playlistId: Long)

    @Delete
    suspend fun deleteSong(playlistSong: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deleteSongById(playlistId: Long, songId: Long)

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int?
}