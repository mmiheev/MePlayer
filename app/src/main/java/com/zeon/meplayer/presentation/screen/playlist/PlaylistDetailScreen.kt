package com.zeon.meplayer.presentation.screen.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeon.meplayer.R
import com.zeon.meplayer.data.repository.PlaylistRepository
import com.zeon.meplayer.presentation.screen.playlist.viewmodel.PlaylistsViewModel
import com.zeon.meplayer.domain.model.Audio
import com.zeon.meplayer.presentation.screen.player.PlayerViewModel
import com.zeon.meplayer.presentation.screen.playlist.components.TrackAction
import com.zeon.meplayer.presentation.screen.playlist.components.TrackListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long?,
    playlistRepository: PlaylistRepository,
    playerViewModel: PlayerViewModel,
    onSongClick: (Int, List<Audio>) -> Unit,
    onAddSongs: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: PlaylistsViewModel = viewModel(
        factory = PlaylistsViewModel.provideFactory(playlistRepository)
    )
    val songs by viewModel.selectedPlaylistSongs.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()

    var showRemoveDialog by remember { mutableStateOf(false) }
    var songToRemove by remember { mutableStateOf<Audio?>(null) }

    LaunchedEffect(playlistId) {
        if (playlistId != null) {
            viewModel.loadPlaylistSongs(playlistId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.playlist_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onAddSongs) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_songs))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.empty_playlist))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                itemsIndexed(songs) { index, song ->
                    TrackListItem(
                        song = song,
                        isNowPlaying = (currentSong?.id == song.id),
                        onClick = { onSongClick(index, songs) },
                        actions = listOf(
                            TrackAction.RemoveFromPlaylist {
                                songToRemove = song
                                showRemoveDialog = true
                            }
                        )
                    )
                }
            }

            if (showRemoveDialog && songToRemove != null) {
                AlertDialog(
                    onDismissRequest = { showRemoveDialog = false },
                    title = { Text(stringResource(R.string.remove_from_playlist_title)) },
                    text = { Text(stringResource(R.string.remove_from_playlist_confirmation, songToRemove!!.title)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.removeSongFromPlaylist(playlistId!!, songToRemove!!.id)
                                showRemoveDialog = false
                                songToRemove = null
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.remove))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showRemoveDialog = false
                            songToRemove = null
                        }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}