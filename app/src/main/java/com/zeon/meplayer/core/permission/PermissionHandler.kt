package com.zeon.meplayer.core.permission

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.zeon.meplayer.R
import com.zeon.meplayer.data.repository.MusicRepository
import com.zeon.meplayer.domain.model.Audio
import kotlinx.coroutines.launch

class PermissionHandler(
    private val activity: ComponentActivity,
    private val musicRepository: MusicRepository,
) {
    private var pendingDeleteAudio: Audio? = null

    private val requestReadPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                loadMusic()
            } else {
                Toast.makeText(activity, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    private val requestWritePermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pendingDeleteAudio?.let { performDelete(it) }
            } else {
                Toast.makeText(activity, R.string.write_permission_needed, Toast.LENGTH_SHORT).show()
            }
            pendingDeleteAudio = null
        }

    private val deleteAudioLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == ComponentActivity.RESULT_OK) {
                pendingDeleteAudio?.let { audio ->
                    musicRepository.confirmDelete(audio)
                }
            } else {
                Toast.makeText(activity, R.string.delete_cancelled, Toast.LENGTH_SHORT).show()
            }
            pendingDeleteAudio = null
        }

    fun checkPermissionAndLoadMusic() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            loadMusic()
        } else {
            requestReadPermissionLauncher.launch(permission)
        }
    }

    private fun loadMusic() {
        activity.lifecycleScope.launch {
            musicRepository.loadMusic()
        }
    }

    fun deleteAudio(audio: Audio) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingDeleteAudio = audio
            val deleteRequest = MediaStore.createDeleteRequest(activity.contentResolver, listOf(audio.uri))
            try {
                deleteAudioLauncher.launch(
                    IntentSenderRequest.Builder(deleteRequest.intentSender).build()
                )
            } catch (_: IntentSender.SendIntentException) {
                Toast.makeText(activity, R.string.delete_start_failed, Toast.LENGTH_SHORT).show()
                pendingDeleteAudio = null
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
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
        activity.lifecycleScope.launch {
            val deleted = musicRepository.deleteAudio(audio)
            if (!deleted) {
                Toast.makeText(activity, R.string.delete_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}