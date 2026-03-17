package com.zeon.meplayer.domain.model

import com.zeon.meplayer.data.local.entity.PlaylistEntity

data class PlaylistWithSongs(
    val playlist: Playlist,
    val songs: List<Audio>
)

fun PlaylistEntity.toPlaylist() = Playlist(id, name, createdAt)