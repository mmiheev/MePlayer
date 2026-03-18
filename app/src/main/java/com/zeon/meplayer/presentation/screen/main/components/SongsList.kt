package com.zeon.meplayer.presentation.screen.main.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zeon.meplayer.domain.model.Audio
import com.zeon.meplayer.presentation.screen.playlist.components.TrackAction
import com.zeon.meplayer.presentation.screen.playlist.components.TrackListItem

@Composable
fun SongsList(
    songs: List<Audio>,
    currentSong: Audio?,
    onSongClick: (Int, List<Audio>) -> Unit,
    onAddToPlaylist: (Audio) -> Unit,
    onDelete: (Audio) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = bottomPadding + 8.dp
        )
    ) {
        items(songs) { song ->
            TrackListItem(
                song = song,
                isNowPlaying = (currentSong?.id == song.id),
                onClick = {
                    val index = songs.indexOfFirst { it.id == song.id }
                    onSongClick(index, songs)
                },
                actions = listOf(
                    TrackAction.AddToPlaylist { onAddToPlaylist(song) },
                    TrackAction.DeleteFromDevice { onDelete(song) }
                )
            )
        }
    }
}