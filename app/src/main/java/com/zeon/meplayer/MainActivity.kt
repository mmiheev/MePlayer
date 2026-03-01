package com.zeon.meplayer

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zeon.meplayer.repository.MusicRepository
import com.zeon.meplayer.service.MusicServiceConnection
import com.zeon.meplayer.ui.main.MainScreen
import com.zeon.meplayer.ui.player.PlayerScreen
import com.zeon.meplayer.ui.settings.SettingsScreen
import com.zeon.meplayer.ui.theme.MePlayerTheme
import com.zeon.meplayer.utils.PermissionHandler
import com.zeon.meplayer.viewmodel.PlayerViewModel
import com.zeon.meplayer.viewmodel.ThemeViewModel

/**
 * Main Activity of the MePlayer application.
 * It manages runtime permissions, binds to the MusicService, loads audio files,
 * and sets up the Compose UI with navigation.
 */
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val musicRepository = MusicRepository(this)

    private val serviceConnection = MusicServiceConnection(this) { playbackManager ->
        playbackManager.musicList = musicRepository.musicList.value
        playerViewModel.attach(playbackManager)
    }

    private val permissionHandler = PermissionHandler(
        activity = this,
        musicRepository = musicRepository,
        onListUpdated = { updatedList ->
            serviceConnection.getPlaybackManager()?.musicList = updatedList
        }
    )

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionHandler.checkPermissionAndLoadMusic()

        setContent {
            val isDarkTheme by themeViewModel.isDarkTheme
            val musicList by musicRepository.musicList.collectAsState()

            MePlayerTheme(
                darkTheme = isDarkTheme,
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
                            onSongSelect = { position ->
                                val manager = serviceConnection.getPlaybackManager()
                                if (manager == null) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        R.string.service_not_ready,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    manager.playMusic(position)
                                    navController.navigate(Screen.PLAYER)
                                }
                            },
                            onSongDelete = { audio -> permissionHandler.deleteAudio(audio) },
                            onNavigateToSettings = { navController.navigate(Screen.SETTINGS) },
                            onNavigateToPlayer = { navController.navigate(Screen.PLAYER) }
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
}

object Screen {
    const val MAIN = "main"
    const val PLAYER = "player"
    const val SETTINGS = "settings"
}