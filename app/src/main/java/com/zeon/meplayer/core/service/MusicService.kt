package com.zeon.meplayer.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.zeon.meplayer.presentation.MainActivity
import com.zeon.meplayer.R
import com.zeon.meplayer.core.playback.PlaybackManager
import com.zeon.meplayer.data.local.datastore.LastPlayedPreferences
import com.zeon.meplayer.domain.model.Audio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Background service responsible for audio playback.
 * It manages a MediaPlayer instance, maintains the current playlist,
 * handles shuffle mode, and communicates with the UI through callbacks.
 */
class MusicService : Service() {
    private val binder = MusicBinder()
    private lateinit var playbackManager: PlaybackManager
    private val becomingNoisyReceiver = BecomingNoisyReceiver()
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isAudioFocused = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    companion object {
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "MusicPlayerChannel"
    }

    inner class MusicBinder : Binder() {
        fun getPlaybackManager(): PlaybackManager = playbackManager
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

    override fun onCreate() {
        super.onCreate()

        playbackManager = PlaybackManager(this).apply {
            onPlaybackStarted = { song -> updateNotification(song, isPlaying = true) }

            onPlaybackPaused = {
                updateNotification(null, isPlaying = false)
                abandonAudioFocus()
            }
            audioFocusHandler = ::requestAudioFocus
        }

        val lastPlayedPrefs = LastPlayedPreferences(this)
        playbackManager.setLastPlayedPreferences(lastPlayedPrefs)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        mediaSession = MediaSessionCompat(
            this,
            getString(R.string.music_service_tag)
        ).apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = playbackManager.startMusic()
                override fun onPause() = playbackManager.pauseMusic()
                override fun onSkipToNext() = playbackManager.playNext()
                override fun onSkipToPrevious() = playbackManager.playPrevious()
                override fun onSeekTo(pos: Long) = playbackManager.seekTo(pos)
                override fun onStop() = playbackManager.pauseMusic()
            })
            isActive = true
        }

        createNotificationChannel()
        registerReceiver(
            becomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )

        stateJob = scope.launch {
            playbackManager.state.collect { state ->
                updateNotification(state.currentSong, state.isPlaying)
                updateMediaSession(state.currentSong, state.isPlaying, state.currentPosition)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (playbackManager.state.value.isPlaying) {
                    playbackManager.pauseMusic()
                } else {
                    playbackManager.startMusic()
                }
            }

            ACTION_NEXT -> playbackManager.playNext()
            ACTION_PREVIOUS -> playbackManager.playPrevious()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stateJob?.cancel()
        scope.cancel()
        unregisterReceiver(becomingNoisyReceiver)
        mediaSession.isActive = false
        mediaSession.release()
        playbackManager.release()
        abandonAudioFocus()
        super.onDestroy()
    }

    private fun requestAudioFocus(): Boolean {
        if (isAudioFocused) return true

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setAcceptsDelayedFocusGain(true)
                setOnAudioFocusChangeListener(focusChangeListener)
                build()
            }.also { audioFocusRequest = it }
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        isAudioFocused = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return isAudioFocused
    }

    private fun abandonAudioFocus() {
        if (!isAudioFocused) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
        isAudioFocused = false
    }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                playbackManager.pauseMusic()
                abandonAudioFocus()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> playbackManager.pauseMusic()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> playbackManager.getPlayer().volume =
                0.2f

            AudioManager.AUDIOFOCUS_GAIN -> {
                playbackManager.getPlayer().volume =
                    if (playbackManager.state.value.isMuted) 0f else 1f
            }
        }
    }

    private fun updateNotification(song: Audio?, isPlaying: Boolean) {
        if (song == null) {
            stopForeground(false)
            return
        }
        val notification = createNotification(song, isPlaying)
        if (isPlaying) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            stopForeground(false)
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(song: Audio, isPlaying: Boolean): Notification {
        val playPauseIntent = Intent(this,
            MusicService::class.java).setAction(ACTION_PLAY_PAUSE)
        val nextIntent = Intent(this,
            MusicService::class.java).setAction(ACTION_NEXT)
        val prevIntent = Intent(this,
            MusicService::class.java).setAction(ACTION_PREVIOUS)

        val playPausePendingIntent = PendingIntent.getService(
            this, 0, playPauseIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val nextPendingIntent = PendingIntent.getService(
            this, 1, nextIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val prevPendingIntent = PendingIntent.getService(
            this, 2, prevIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(createContentIntent())
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(
                android.R.drawable.ic_media_previous,
                getString(R.string.previous),
                prevPendingIntent
            )
            .addAction(
                if (isPlaying) {
                    android.R.drawable.ic_media_pause
                } else {
                    android.R.drawable.ic_media_play
                },
                if (isPlaying) {
                    getString(R.string.pause)
                } else {
                    getString(R.string.play)
                },
                playPausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_media_next,
                getString(R.string.next),
                nextPendingIntent
            )
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
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

    private fun updateMediaSession(song: Audio?, isPlaying: Boolean, position: Long) {
        if (song != null) {
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .build()
            mediaSession.setMetadata(metadata)
        }
        val state = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (isPlaying) {
                    PlaybackStateCompat.STATE_PLAYING
                } else {
                    PlaybackStateCompat.STATE_PAUSED
                },
                position,
                1f
            )
            .build()
        mediaSession.setPlaybackState(state)
    }
}