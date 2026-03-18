package com.zeon.meplayer.presentation.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeon.meplayer.R
import com.zeon.meplayer.core.playback.PlaybackManager
import com.zeon.meplayer.data.repository.PlaylistRepository
import com.zeon.meplayer.domain.model.Audio
import com.zeon.meplayer.presentation.screen.main.components.ModeSelector
import com.zeon.meplayer.presentation.screen.main.components.PlayingBar
import com.zeon.meplayer.presentation.screen.main.components.SongsList
import com.zeon.meplayer.presentation.screen.main.model.MainContentMode
import com.zeon.meplayer.presentation.screen.player.PlayerViewModel
import com.zeon.meplayer.presentation.screen.playlist.components.PlaylistSelectionBottomSheet
import com.zeon.meplayer.presentation.screen.playlist.components.PlaylistsList
import com.zeon.meplayer.presentation.screen.playlist.viewmodel.PlaylistsViewModel
import com.zeon.meplayer.presentation.theme.AppGradients
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    musicList: List<Audio>,
    playerViewModel: PlayerViewModel,
    playlistRepository: PlaylistRepository,
    playbackManager: PlaybackManager?,
    onSongSelect: (Int) -> Unit,
    onSongDelete: (Audio) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onPlaylistClick: (Long) -> Unit
) {
    val currentSong by playerViewModel.currentSong.collectAsState()
    val bottomPadding = if (currentSong != null) 100.dp else 0.dp
    val coroutineScope = rememberCoroutineScope()

    val playlistsViewModel: PlaylistsViewModel = viewModel(
        factory = PlaylistsViewModel.provideFactory(
            playlistRepository,
            onPlaylistDeleted = { deletedId ->
                coroutineScope.launch {
                    val currentSong = playbackManager?.state?.value?.currentSong
                    if (currentSong != null) {
                        val isInPlaylist =
                            playlistRepository.isSongInPlaylist(deletedId, currentSong.id)
                        if (!isInPlaylist) {
                            playbackManager.resetToAllSongs()
                        }
                    }
                }
            }
        )
    )
    val playlists by playlistsViewModel.playlists.collectAsState()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    var selectedMode by remember { mutableStateOf(MainContentMode.SONGS) }

    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Audio?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<Audio?>(null) }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    brush = if (isSystemInDarkTheme()) AppGradients.darkGradient
                                    else AppGradients.primaryGradient
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedMode == MainContentMode.SONGS) {
                                Spacer(modifier = Modifier.width(8.dp))
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = pluralStringResource(
                                                R.plurals.song_count,
                                                musicList.size,
                                                musicList.size
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        leadingIconContentColor = MaterialTheme.colorScheme.onPrimary,
                                        labelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    border = null,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
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
                        if (selectedMode == MainContentMode.SONGS) {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search_icon)
                                )
                            }
                        }
                        if (selectedMode == MainContentMode.PLAYLISTS) {
                            IconButton(onClick = { showCreatePlaylistDialog = true }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.create_playlist)
                                )
                            }
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_icon)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ModeSelector(
                    selectedMode = selectedMode,
                    onModeSelected = { newMode ->
                        selectedMode = newMode
                        if (newMode == MainContentMode.PLAYLISTS) {
                            isSearching = false
                            searchQuery = ""
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )

                when (selectedMode) {
                    MainContentMode.SONGS -> {
                        SongsList(
                            songs = filteredList,
                            currentSong = currentSong,
                            onSongClick = { index, songs ->
                                val originalIndex =
                                    musicList.indexOfFirst { it.id == songs[index].id }
                                if (originalIndex != -1) {
                                    onSongSelect(originalIndex)
                                }
                            },
                            onAddToPlaylist = { song ->
                                selectedSongForPlaylist = song
                                showPlaylistBottomSheet = true
                            },
                            onDelete = { song ->
                                songToDelete = song
                                showDeleteDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            bottomPadding = bottomPadding
                        )
                    }

                    MainContentMode.PLAYLISTS -> {
                        PlaylistsList(
                            viewModel = playlistsViewModel,
                            onPlaylistClick = onPlaylistClick,
                            modifier = Modifier.weight(1f),
                            bottomPadding = bottomPadding
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = currentSong != null,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PlayingBar(
                    currentSong = currentSong,
                    isPlaying = playerViewModel.isPlaying.collectAsState().value,
                    currentPosition = playerViewModel.currentPosition.collectAsState().value,
                    duration = playerViewModel.duration.collectAsState().value,
                    onPlayPause = { playerViewModel.playPause() },
                    onNext = { playerViewModel.playNext() },
                    onPrevious = { playerViewModel.playPrevious() },
                    onClick = onNavigateToPlayer,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .navigationBarsPadding()
                )
            }
        }

        if (showPlaylistBottomSheet && selectedSongForPlaylist != null) {
            PlaylistSelectionBottomSheet(
                playlists = playlists,
                onDismiss = { showPlaylistBottomSheet = false },
                onPlaylistSelected = { playlistId ->
                    playlistsViewModel.addSongsToPlaylist(
                        playlistId,
                        listOf(selectedSongForPlaylist!!.id)
                    )
                    showPlaylistBottomSheet = false
                }
            )
        }

        if (showDeleteDialog && songToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_title)) },
                text = { Text(stringResource(R.string.delete_confirmation, songToDelete!!.title)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onSongDelete(songToDelete!!)
                            showDeleteDialog = false
                            songToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        songToDelete = null
                    }) {
                        Text(stringResource(R.string.delete_cancel))
                    }
                }
            )
        }

        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
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
                                playlistsViewModel.createPlaylist(newPlaylistName)
                                newPlaylistName = ""
                                showCreatePlaylistDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.create))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}