package com.zeon.meplayer.presentation.screen.playlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeon.meplayer.R
import com.zeon.meplayer.data.repository.PlaylistRepository
import com.zeon.meplayer.domain.model.Playlist
import com.zeon.meplayer.presentation.screen.main.components.ModeSelector
import com.zeon.meplayer.presentation.screen.main.model.MainContentMode
import com.zeon.meplayer.presentation.screen.playlist.viewmodel.PlaylistsViewModel

@Composable
fun PlaylistsList(
    viewModel: PlaylistsViewModel,
    onPlaylistClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val playlists by viewModel.playlists.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = bottomPadding + 8.dp
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = stringResource(R.string.no_playlists),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = bottomPadding + 8.dp
            )
        ) {
            items(playlists) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) },
                    onDelete = {
                        playlistToDelete = playlist
                        showDeleteDialog = true
                    }
                )
            }
        }
    }

    if (showDeleteDialog && playlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_playlist_title)) },
            text = { Text(stringResource(R.string.delete_playlist_confirmation, playlistToDelete!!.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(playlistToDelete!!.id)
                        showDeleteDialog = false
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    playlistToDelete = null
                }) {
                    Text(stringResource(R.string.delete_cancel))
                }
            }
        )
    }
}