package com.zeon.meplayer.presentation

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zeon.meplayer.R
import com.zeon.meplayer.core.permission.PermissionHandler
import com.zeon.meplayer.core.service.MusicServiceConnection
import com.zeon.meplayer.data.local.AppDatabase
import com.zeon.meplayer.data.local.datastore.SortPreferences
import com.zeon.meplayer.data.repository.MusicRepository
import com.zeon.meplayer.data.repository.PlaylistRepository
import com.zeon.meplayer.presentation.screen.main.MainScreen
import com.zeon.meplayer.presentation.screen.player.PlayerScreen
import com.zeon.meplayer.presentation.screen.player.PlayerViewModel
import com.zeon.meplayer.presentation.screen.playlist.AddSongsToPlaylistScreen
import com.zeon.meplayer.presentation.screen.playlist.PlaylistDetailScreen
import com.zeon.meplayer.presentation.screen.playlist.PlaylistsScreen
import com.zeon.meplayer.presentation.screen.settings.SettingsScreen
import com.zeon.meplayer.presentation.theme.MePlayerTheme
import com.zeon.meplayer.presentation.viewmodel.rememberThemeViewModel
import kotlinx.coroutines.launch

/**
 * Main Activity of the MePlayer application.
 * It manages runtime permissions, binds to the MusicService, loads audio files,
 * and sets up the Compose UI with navigation.
 */
class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private lateinit var musicRepository: MusicRepository
    private lateinit var sortPreferences: SortPreferences
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var playlistRepository: PlaylistRepository

    private val serviceConnection = MusicServiceConnection(this) { playbackManager ->
        playbackManager.musicList = musicRepository.musicList.value
        playerViewModel.attach(playbackManager)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sortPreferences = SortPreferences(this)
        musicRepository = MusicRepository(this, sortPreferences)

        val db = AppDatabase.getInstance(this)
        playlistRepository = PlaylistRepository(db, musicRepository)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                musicRepository.musicList.collect { updatedList ->
                    serviceConnection.getPlaybackManager()?.setAllSongs(updatedList)
                }
            }
        }

        permissionHandler = PermissionHandler(
            activity = this,
            musicRepository = musicRepository,
        )

        permissionHandler.checkPermissionAndLoadMusic()

        setContent {
            val themeViewModel = rememberThemeViewModel()
            val musicList by musicRepository.musicList.collectAsState()
            val themeMode by themeViewModel.themeMode.collectAsState()
            val isLoading by themeViewModel.isLoading.collectAsState()

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            } else
                MePlayerTheme(
                    themeMode = themeMode,
                    dynamicColor = false
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Screen.MAIN
                    ) {
                        composable(Screen.MAIN) {
                            MainScreen(
                                musicList = musicList,
                                playerViewModel = playerViewModel,
                                playlistRepository = playlistRepository,
                                playbackManager = serviceConnection.getPlaybackManager(),
                                onSongSelect = { position ->
                                    val manager = serviceConnection.getPlaybackManager()
                                    if (manager != null) {
                                        manager.musicList = musicList
                                        if (manager.canPlayAt(position)) {
                                            manager.playMusic(position)
                                            navController.navigate(Screen.PLAYER) {
                                                popUpTo(Screen.MAIN) { inclusive = false }
                                            }
                                        } else {
                                            Toast.makeText(
                                                this@MainActivity,
                                                getString(R.string.not_play),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@MainActivity,
                                            R.string.service_not_ready,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onSongDelete = { audio -> permissionHandler.deleteAudio(audio) },
                                onNavigateToSettings = { navController.navigate(Screen.SETTINGS) },
                                onNavigateToPlayer = { navController.navigate(Screen.PLAYER) },
                                onPlaylistClick = { playlistId ->
                                    navController.navigate("${Screen.PLAYLIST_DETAIL}/$playlistId")
                                }
                            )
                        }
                        composable(Screen.SETTINGS) {
                            SettingsScreen(
                                navController = navController,
                                themeViewModel = themeViewModel
                            )
                        }
                        composable(Screen.PLAYER) {
                            PlayerScreen(
                                navController = navController,
                                playerViewModel = playerViewModel,
                                themeViewModel = themeViewModel,
                                settingsRoute = Screen.SETTINGS
                            )
                        }
                        composable(Screen.PLAYLISTS) {
                            PlaylistsScreen(
                                playlistRepository = playlistRepository,
                                onPlaylistClick = { playlistId ->
                                    navController.navigate("${Screen.PLAYLIST_DETAIL}/$playlistId")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("${Screen.PLAYLIST_DETAIL}/{playlistId}") { backStackEntry ->
                            val playlistId =
                                backStackEntry.arguments?.getString("playlistId")?.toLongOrNull()
                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                playlistRepository = playlistRepository,
                                playerViewModel = playerViewModel,
                                onSongClick = { position, songs ->
                                    val manager = serviceConnection.getPlaybackManager()
                                    if (manager != null) {
                                        manager.setPlaylist(songs)
                                        if (manager.canPlayAt(position)) {
                                            manager.playMusic(position)
                                            navController.navigate(Screen.PLAYER)
                                        } else {
                                            Toast.makeText(
                                                this@MainActivity,
                                                getString(R.string.not_play),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@MainActivity,
                                            R.string.service_not_ready,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onAddSongs = {
                                    navController.navigate("${Screen.ADD_SONGS}/$playlistId")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("${Screen.ADD_SONGS}/{playlistId}") { backStackEntry ->
                            val playlistId =
                                backStackEntry.arguments?.getString("playlistId")?.toLongOrNull()
                            AddSongsToPlaylistScreen(
                                playlistId = playlistId,
                                playlistRepository = playlistRepository,
                                allSongs = musicList,
                                onSongsSelected = { selectedSongIds ->
                                    if (playlistId != null) {
                                        lifecycleScope.launch {
                                            try {
                                                playlistRepository.addSongsToPlaylist(
                                                    playlistId,
                                                    selectedSongIds
                                                )
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    getString(
                                                        R.string.added_songs_to_playlist,
                                                        selectedSongIds.size
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    getString(R.string.playlist_not_found),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.playlist_not_found),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        serviceConnection.bind()
    }

    override fun onStop() {
        playerViewModel.detach()
        serviceConnection.unbind()
        super.onStop()
    }

    override fun onDestroy() {
        musicRepository.close()
        super.onDestroy()
    }
}

object Screen {
    const val MAIN = "main"
    const val PLAYER = "player"
    const val SETTINGS = "settings"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlist_detail"
    const val ADD_SONGS = "add_songs"
}