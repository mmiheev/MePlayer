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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zeon.meplayer.model.Audio
import com.zeon.meplayer.service.MusicService
import com.zeon.meplayer.ui.main.MainScreen
import com.zeon.meplayer.ui.player.PlayerScreen
import com.zeon.meplayer.ui.settings.SettingsScreen
import com.zeon.meplayer.ui.theme.MePlayerTheme
import com.zeon.meplayer.viewmodel.PlayerViewModel
import com.zeon.meplayer.viewmodel.ThemeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main Activity of the MePlayer application.
 * It manages runtime permissions, binds to the MusicService, loads audio files,
 * and sets up the Compose UI with navigation.
 */
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    private var musicService: MusicService? = null
    private var isServiceBound = false
    private val musicList = mutableStateListOf<Audio>()

    private var pendingDeleteAudio: Audio? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isServiceBound = true
            musicService?.musicList = musicList
            playerViewModel.setService(musicService!!)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            musicService = null
        }
    }

    private val requestReadPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                loadMusic()
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private val deleteAudioLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                pendingDeleteAudio?.let { audio ->
                    removeAudioFromList(audio)
                }
            } else {
                Toast.makeText(this, getString(R.string.delete_cancelled), Toast.LENGTH_SHORT)
                    .show()
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
                                if (musicService == null) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.service_not_ready),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    musicService?.playMusic(position)
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

        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadMusic()
        } else {
            requestReadPermissionLauncher.launch(permission)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun deleteAudio(audio: Audio) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingDeleteAudio = audio
            val deleteRequest = MediaStore.createDeleteRequest(contentResolver, listOf(audio.uri))
            try {
                deleteAudioLauncher.launch(
                    IntentSenderRequest.Builder(deleteRequest.intentSender).build()
                )
            } catch (e: IntentSender.SendIntentException) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.delete_start_failed), Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    performDelete(audio)
                } else {
                    pendingDeleteAudio = audio
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_WRITE_PERMISSION
                    )
                }
            } else {
                performDelete(audio)
            }
        }
    }

    private fun performDelete(audio: Audio) {
        try {
            val rowsDeleted = contentResolver.delete(audio.uri, null, null)
            if (rowsDeleted > 0) {
                removeAudioFromList(audio)
            } else {
                Toast.makeText(this, getString(R.string.delete_failed), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                getString(R.string.delete_error, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun removeAudioFromList(audio: Audio) {
        val index = musicList.indexOfFirst { it.id == audio.id }
        if (index != -1) {
            musicList.removeAt(index)
            if (isServiceBound && musicService != null) {
                musicService!!.updateCurrentPositionAfterDeletion(index)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadMusic()
                } else {
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT)
                        .show()
                }
            }

            REQUEST_WRITE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pendingDeleteAudio?.let { performDelete(it) }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.write_permission_needed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                pendingDeleteAudio = null
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DELETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                pendingDeleteAudio?.let { audio ->
                    removeAudioFromList(audio)
                }
            } else {
                Toast.makeText(this, getString(R.string.delete_cancelled), Toast.LENGTH_SHORT)
                    .show()
            }
            pendingDeleteAudio = null
        }
    }

    private fun loadMusic() {

        val unknownTitle = getString(R.string.unknown_title)
        val unknownArtist = getString(R.string.unknown_artist)

        lifecycleScope.launch(Dispatchers.IO) {
            val contentResolver = contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            val cursor = contentResolver.query(uri, null, selection, null, sortOrder)

            val newList = mutableListOf<Audio>()
            cursor?.use {
                val idColumn = it.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val dataColumn = it.getColumnIndex(MediaStore.Audio.Media.DATA)
                val durationColumn = it.getColumnIndex(MediaStore.Audio.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: unknownTitle
                    val artist = it.getString(artistColumn) ?: unknownArtist
                    val path = it.getString(dataColumn)
                    val duration = it.getLong(durationColumn)

                    newList.add(Audio(id, path, title, artist, duration))
                }
            }
            withContext(Dispatchers.Main) {
                musicList.clear()
                musicList.addAll(newList)
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
            unbindService(connection)
            isServiceBound = false
        }
    }

    companion object {
        private const val REQUEST_PERMISSION = 1
        private const val DELETE_REQUEST_CODE = 100
        private const val REQUEST_WRITE_PERMISSION = 2
    }
}