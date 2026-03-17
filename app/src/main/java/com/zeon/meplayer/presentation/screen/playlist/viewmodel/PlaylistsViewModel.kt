package com.zeon.meplayer.presentation.screen.playlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zeon.meplayer.data.repository.PlaylistRepository
import com.zeon.meplayer.domain.model.Audio
import com.zeon.meplayer.domain.model.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistsViewModel(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _selectedPlaylistSongs = MutableStateFlow<List<Audio>>(emptyList())
    val selectedPlaylistSongs: StateFlow<List<Audio>> = _selectedPlaylistSongs.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllPlaylists().collect { _playlists.value = it }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            repository.getPlaylistWithSongs(playlistId).collect { playlistWithSongs ->
                _selectedPlaylistSongs.value = playlistWithSongs?.songs ?: emptyList()
            }
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            repository.addSongsToPlaylist(playlistId, songIds)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    companion object {
        fun provideFactory(repository: PlaylistRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(PlaylistsViewModel::class.java)) {
                        return PlaylistsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}