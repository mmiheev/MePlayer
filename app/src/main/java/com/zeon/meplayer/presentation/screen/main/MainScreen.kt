package com.zeon.meplayer.presentation.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeon.meplayer.R
import com.zeon.meplayer.data.repository.PlaylistRepository
import com.zeon.meplayer.presentation.screen.playlist.viewmodel.PlaylistsViewModel
import com.zeon.meplayer.presentation.screen.playlist.components.TrackAction
import com.zeon.meplayer.presentation.screen.playlist.components.TrackListItem
import com.zeon.meplayer.domain.model.Audio
import com.zeon.meplayer.presentation.screen.main.components.PlayingBar
import com.zeon.meplayer.presentation.screen.player.PlayerViewModel
import com.zeon.meplayer.presentation.screen.playlist.components.PlaylistSelectionBottomSheet
import com.zeon.meplayer.presentation.theme.AppGradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    musicList: List<Audio>,
    playerViewModel: PlayerViewModel,
    playlistRepository: PlaylistRepository,
    onSongSelect: (Int) -> Unit,
    onSongDelete: (Audio) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylists: () -> Unit
) {
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Audio?>(null) }
    var showDeleteDeviceDialog by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<Audio?>(null) }

    val playlistsViewModel: PlaylistsViewModel = viewModel(
        factory = PlaylistsViewModel.provideFactory(playlistRepository)
    )
    val playlists by playlistsViewModel.playlists.collectAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    val filteredList = remember(searchQuery, musicList) {
        if (searchQuery.isBlank()) {
            musicList
        } else {
            musicList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSearching) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.search_hint),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.6f
                                                ),
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                brush = if (isDarkTheme) AppGradients.darkGradient else AppGradients.primaryGradient
                            )
                        )
                    }
                },
                actions = {
                    if (isSearching) {
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close_search)
                            )
                        }
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_icon),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onNavigateToPlaylists) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_playlist_64),
                                contentDescription = stringResource(R.string.playlists),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_icon),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredList) { song ->
                    TrackListItem(
                        song = song,
                        isNowPlaying = (currentSong?.id == song.id),
                        onClick = {
                            val originalIndex = musicList.indexOfFirst { it.id == song.id }
                            if (originalIndex != -1) onSongSelect(originalIndex)
                        },
                        actions = listOf(
                            TrackAction.AddToPlaylist {
                                selectedSongForPlaylist = song
                                showPlaylistBottomSheet = true
                            },
                            TrackAction.DeleteFromDevice {
                                songToDelete = song
                                showDeleteDeviceDialog = true
                            }
                        )
                    )
                }
            }

            if (showDeleteDeviceDialog && songToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDeviceDialog = false },
                    title = { Text(stringResource(R.string.delete_title)) },
                    text = { Text(stringResource(R.string.delete_confirmation, songToDelete!!.title)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onSongDelete(songToDelete!!)
                                showDeleteDeviceDialog = false
                                songToDelete = null
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.delete_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDeleteDeviceDialog = false
                            songToDelete = null
                        }) {
                            Text(stringResource(R.string.delete_cancel))
                        }
                    }
                )
            }

            if (showPlaylistBottomSheet) {
                PlaylistSelectionBottomSheet(
                    playlists = playlists,
                    onDismiss = { showPlaylistBottomSheet = false },
                    onPlaylistSelected = { playlistId ->
                        selectedSongForPlaylist?.let { song ->
                            playlistsViewModel.addSongsToPlaylist(playlistId, listOf(song.id))
                            showPlaylistBottomSheet = false
                        }
                    }
                )
            }

            AnimatedVisibility(visible = currentSong != null) {
                PlayingBar(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onPlayPause = { playerViewModel.playPause() },
                    onNext = { playerViewModel.playNext() },
                    onPrevious = { playerViewModel.playPrevious() },
                    onClick = onNavigateToPlayer,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}