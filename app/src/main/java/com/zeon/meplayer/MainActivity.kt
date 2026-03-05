package com.zeon.meplayer

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
import com.zeon.meplayer.data.repository.MusicRepository
import com.zeon.meplayer.core.service.MusicServiceConnection
import com.zeon.meplayer.presentation.screen.main.MainScreen
import com.zeon.meplayer.presentation.screen.player.PlayerScreen
import com.zeon.meplayer.presentation.screen.settings.SettingsScreen
import com.zeon.meplayer.presentation.theme.MePlayerTheme
import com.zeon.meplayer.data.local.datastore.SortPreferences
import com.zeon.meplayer.core.permission.PermissionHandler
import com.zeon.meplayer.presentation.screen.player.PlayerViewModel
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

    private val serviceConnection = MusicServiceConnection(this) { playbackManager ->
        playbackManager.musicList = musicRepository.musicList.value
        playerViewModel.attach(playbackManager)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sortPreferences = SortPreferences(this)
        musicRepository = MusicRepository(this, sortPreferences)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                musicRepository.musicList.collect { updatedList ->
                    serviceConnection.getPlaybackManager()?.musicList = updatedList
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
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
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

    override fun onDestroy() {
        musicRepository.close()
        super.onDestroy()
    }
}

object Screen {
    const val MAIN = "main"
    const val PLAYER = "player"
    const val SETTINGS = "settings"
}