package com.zeon.meplayer.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.zeon.meplayer.R
import com.zeon.meplayer.data.local.datastore.SortPreferences
import com.zeon.meplayer.domain.model.Audio
import com.zeon.meplayer.domain.model.SortOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val sortPreferences: SortPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _musicList = MutableStateFlow<List<Audio>>(emptyList())
    val musicList: StateFlow<List<Audio>> = _musicList.asStateFlow()

    private var currentSortOption: SortOption = SortOption.TITLE
    private var isFirstSort = true

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        scope.launch {
            sortPreferences.sortOptionFlow.collect { option ->
                currentSortOption = option
                if (isFirstSort) {
                    isFirstSort = false
                    loadMusic()
                } else {
                    loadMusic()
                }
            }
        }
    }

    private fun hasReadPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun loadMusic() = withContext(Dispatchers.IO) {
        if (!hasReadPermission()) {
            _musicList.emit(emptyList())
            _isLoading.value = false
            return@withContext
        }
        val list = loadFromMediaStore(currentSortOption)
        _musicList.emit(list)
        _isLoading.value = false
    }

    private fun loadFromMediaStore(sortOption: SortOption): List<Audio> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = when (sortOption) {
            SortOption.TITLE -> "${MediaStore.Audio.Media.TITLE} ASC"
            SortOption.ARTIST -> "${MediaStore.Audio.Media.ARTIST} ASC, ${MediaStore.Audio.Media.TITLE} ASC"
            SortOption.DURATION -> "${MediaStore.Audio.Media.DURATION} ASC"
            SortOption.DATE_ADDED -> "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        }

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        val unknownTitle = context.getString(R.string.unknown_title)
        val unknownArtist = context.getString(R.string.unknown_artist)
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

    fun close() {
        scope.cancel()
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
        } catch (_: Exception) {
            false
        }
    }

    fun confirmDelete(audio: Audio) {
        _musicList.update { currentList ->
            currentList.filter { it.id != audio.id }
        }
    }
}