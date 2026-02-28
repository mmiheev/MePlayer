package com.zeon.meplayer

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.IntentSender
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zeon.meplayer.manager.PlaybackManager
import com.zeon.meplayer.model.Audio
import com.zeon.meplayer.repository.MusicRepository
import com.zeon.meplayer.service.MusicService
import com.zeon.meplayer.ui.main.MainScreen
import com.zeon.meplayer.ui.player.PlayerScreen
import com.zeon.meplayer.ui.settings.SettingsScreen
import com.zeon.meplayer.ui.theme.MePlayerTheme
import com.zeon.meplayer.viewmodel.PlayerViewModel
import com.zeon.meplayer.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

/**
 * Main Activity of the MePlayer application.
 * It manages runtime permissions, binds to the MusicService, loads audio files,
 * and sets up the Compose UI with navigation.
 */
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    private val musicRepository = MusicRepository(this)

    private var playbackManager: PlaybackManager? = null
    private var isServiceBound = false

    private var pendingDeleteAudio: Audio? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            playbackManager = binder.getPlaybackManager()
            isServiceBound = true

            playbackManager?.musicList = musicRepository.musicList.value

            playerViewModel.attach(playbackManager!!)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            playbackManager = null
        }
    }

    private val requestReadPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                loadMusic()
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            }
        }

    private val requestWritePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pendingDeleteAudio?.let { performDelete(it) }
            } else {
                Toast.makeText(this, getString(R.string.write_permission_needed), Toast.LENGTH_SHORT).show()
            }
            pendingDeleteAudio = null
        }

    private val deleteAudioLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                pendingDeleteAudio?.let { audio ->
                    musicRepository.confirmDelete(audio)
                    playbackManager?.musicList = musicRepository.musicList.value
                }
            } else {
                Toast.makeText(this, getString(R.string.delete_cancelled), Toast.LENGTH_SHORT).show()
            }
            pendingDeleteAudio = null
        }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissionAndLoadMusic()

        val mainRoute = getString(R.string.route_main)
        val playerRoute = getString(R.string.route_player)
        val settingsRoute = getString(R.string.route_settings)

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
                    startDestination = mainRoute
                ) {
                    composable(mainRoute) {
                        MainScreen(
                            musicList = musicList,
                            playerViewModel = playerViewModel,
                            onSongSelect = { position ->
                                if (playbackManager == null) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.service_not_ready),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    playbackManager?.playMusic(position)
                                    navController.navigate(playerRoute)
                                }
                            },
                            onSongDelete = { audio -> deleteAudio(audio) },
                            onNavigateToSettings = { navController.navigate(settingsRoute) },
                            onNavigateToPlayer = { navController.navigate(playerRoute) }
                        )
                    }
                    composable(settingsRoute) {
                        SettingsScreen(
                            navController = navController,
                            themeViewModel = themeViewModel
                        )
                    }
                    composable(playerRoute) {
                        PlayerScreen(
                            navController = navController,
                            playerViewModel = playerViewModel
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissionAndLoadMusic() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadMusic()
        } else {
            requestReadPermissionLauncher.launch(permission)
        }
    }

    private fun loadMusic() {
        lifecycleScope.launch {
            musicRepository.loadMusic()
            playbackManager?.musicList = musicRepository.musicList.value
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun deleteAudio(audio: Audio) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingDeleteAudio = audio
            val deleteRequest = MediaStore.createDeleteRequest(contentResolver, listOf(audio.uri))
            try {
                deleteAudioLauncher.launch(IntentSenderRequest.Builder(deleteRequest.intentSender).build())
            } catch (e: IntentSender.SendIntentException) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.delete_start_failed), Toast.LENGTH_SHORT).show()
                pendingDeleteAudio = null
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    performDelete(audio)
                } else {
                    pendingDeleteAudio = audio
                    requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            } else {
                performDelete(audio)
            }
        }
    }

    private fun performDelete(audio: Audio) {
        lifecycleScope.launch {
            val deleted = musicRepository.deleteAudio(audio)
            if (deleted) {
                playbackManager?.musicList = musicRepository.musicList.value
            } else {
                Toast.makeText(this@MainActivity, R.string.delete_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, connection, BIND_AUTO_CREATE)
            startService(intent)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            playerViewModel.detach()
            unbindService(connection)
            isServiceBound = false
        }
    }
}