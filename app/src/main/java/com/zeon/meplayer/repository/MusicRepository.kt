package com.zeon.meplayer.repository

import android.content.Context
import android.provider.MediaStore
import com.zeon.meplayer.R
import com.zeon.meplayer.model.Audio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {
    private val _musicList = MutableStateFlow<List<Audio>>(emptyList())
    val musicList: StateFlow<List<Audio>> = _musicList.asStateFlow()

    suspend fun loadMusic() = withContext(Dispatchers.IO) {
        val list = loadFromMediaStore()
        _musicList.emit(list)
    }

    private fun loadFromMediaStore(): List<Audio> {
        val unknownTitle = context.getString(R.string.unknown_title)
        val unknownArtist = context.getString(R.string.unknown_artist)
        val contentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        val cursor = contentResolver.query(uri, null, selection, null, sortOrder)
        val list = mutableListOf<Audio>()
        cursor?.use {
            val idColumn = it.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val dataColumn = it.getColumnIndex(MediaStore.Audio.Media.DATA)
            val durationColumn = it.getColumnIndex(MediaStore.Audio.Media.DURATION)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: unknownTitle
                val artist = it.getString(artistColumn) ?: unknownArtist
                val path = it.getString(dataColumn)
                val duration = it.getLong(durationColumn)
                list.add(Audio(id, path, title, artist, duration))
            }
        }
        return list
    }

    suspend fun deleteAudio(audio: Audio): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(audio.uri, null, null)
            if (rows > 0) {
                _musicList.update { currentList ->
                    currentList.filter { it.id != audio.id }
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun confirmDelete(audio: Audio) {
        _musicList.update { currentList ->
            currentList.filter { it.id != audio.id }
        }
    }
}