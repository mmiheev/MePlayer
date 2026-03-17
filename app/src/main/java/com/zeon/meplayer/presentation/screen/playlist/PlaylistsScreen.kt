package com.zeon.meplayer.presentation.screen.playlist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeon.meplayer.R
import com.zeon.meplayer.data.repository.PlaylistRepository
import com.zeon.meplayer.domain.model.Playlist
import com.zeon.meplayer.presentation.screen.playlist.components.PlaylistItem
import com.zeon.meplayer.presentation.screen.playlist.viewmodel.PlaylistsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlistRepository: PlaylistRepository,
    onPlaylistClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: PlaylistsViewModel = viewModel(
        factory = PlaylistsViewModel.provideFactory(playlistRepository)
    )

    val playlists by viewModel.playlists.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.playlists_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.create_playlist)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) },
                    onDelete = {
                        playlistToDelete = playlist
                        showDeleteDialog = true
                    })
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text(stringResource(R.string.new_playlist)) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text(stringResource(R.string.playlist_name)) },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                viewModel.createPlaylist(newPlaylistName)
                                newPlaylistName = ""
                                showCreateDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.create))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showDeleteDialog && playlistToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_playlist_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.delete_playlist_confirmation,
                            playlistToDelete!!.name
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePlaylist(playlistToDelete!!.id)
                            showDeleteDialog = false
                            playlistToDelete = null
                        }
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
}