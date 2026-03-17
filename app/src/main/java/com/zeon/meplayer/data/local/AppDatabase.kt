package com.zeon.meplayer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zeon.meplayer.data.local.dao.PlaylistDao
import com.zeon.meplayer.data.local.dao.PlaylistSongDao
import com.zeon.meplayer.data.local.entity.PlaylistEntity
import com.zeon.meplayer.data.local.entity.PlaylistSongEntity

@Database(
    entities = [PlaylistEntity::class, PlaylistSongEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistSongDao(): PlaylistSongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "music_player.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}