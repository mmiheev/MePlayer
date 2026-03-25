package com.zeon.meplayer.presentation.screen.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import com.zeon.meplayer.presentation.screen.playlist.components.SelectableMusicListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsToPlaylistScreen(
    playlistId: Long?,
    playlistRepository: PlaylistRepository,
    allSongs: List<Audio>,
    onSongsSelected: (List<Long>) -> Unit,
    onBack: () -> Unit
) {
    var existingSongIds by remember { mutableStateOf(setOf<Long>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(playlistId) {
        if (playlistId != null) {
            val ids = playlistRepository.getPlaylistSongIds(playlistId)
            existingSongIds = ids.toSet()
            isLoading = false
        }
    }

    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }
    LaunchedEffect(existingSongIds) {
        if (!isLoading) {
            selectedSongIds = existingSongIds
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_songs)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSongsSelected(selectedSongIds.toList())
                            onBack()
                        },
                        enabled = selectedSongIds.isNotEmpty() || selectedSongIds != existingSongIds
                    ) {
                        Text(stringResource(R.string.add))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                items(allSongs) { song ->
                    SelectableMusicListItem(
                        song = song,
                        isSelected = song.id in selectedSongIds,
                        onSelectChange = { isSelected ->
                            selectedSongIds = if (isSelected) {
                                selectedSongIds + song.id
                            } else {
                                selectedSongIds - song.id
                            }
                        }
                    )
                }
            }
        }
    }
}