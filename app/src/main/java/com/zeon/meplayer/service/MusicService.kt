package com.zeon.meplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.DynamicsProcessing
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zeon.meplayer.MainActivity
import com.zeon.meplayer.R
import com.zeon.meplayer.manager.PlaybackManager
import com.zeon.meplayer.model.Audio

/**
 * Background service responsible for audio playback.
 * It manages a MediaPlayer instance, maintains the current playlist,
 * handles shuffle mode, and communicates with the UI through callbacks.
 */
class MusicService : Service() {
    private val binder = MusicBinder()
    private lateinit var playbackManager: PlaybackManager
    private val becomingNoisyReceiver = BecomingNoisyReceiver()

    inner class MusicBinder : Binder() {
        fun getPlaybackManager(): PlaybackManager = playbackManager
    }

    override fun onCreate() {
        super.onCreate()
        playbackManager = PlaybackManager(this).apply {
            onPlaybackStarted = { song -> updateNotification(song, isPlaying = true) }
            onPlaybackPaused = { updateNotification(null, isPlaying = false) }
        }
        createNotificationChannel()
        registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(becomingNoisyReceiver)
        playbackManager.release()
    }

    private fun updateNotification(song: Audio?, isPlaying: Boolean) {
        if (song == null) {
            stopForeground(false)
            return
        }
        val notification = createNotification(song)
        if (isPlaying) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(song: Audio): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private inner class BecomingNoisyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (playbackManager.state.value.isPlaying) {
                    playbackManager.pauseMusic()
                }
            }
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "MusicPlayerChannel"
    }
}