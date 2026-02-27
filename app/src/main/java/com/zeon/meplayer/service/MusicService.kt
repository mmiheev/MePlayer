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
import com.zeon.meplayer.model.Audio

/**
 * Background service responsible for audio playback.
 * It manages a MediaPlayer instance, maintains the current playlist,
 * handles shuffle mode, and communicates with the UI through callbacks.
 */
class MusicService : Service(), MediaPlayer.OnCompletionListener {

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    var musicList: List<Audio> = emptyList()
    private var currentPosition = -1
    private var dynamicsProcessing: DynamicsProcessing? = null
    private val callbacks = mutableListOf<MusicServiceCallback>()
    private var shuffleEnabled = false
    private val shuffleListeners = mutableListOf<OnShuffleChangeListener>()
    private var shuffleOrder: MutableList<Int>? = null
    private var shuffleIndex: Int = 0
    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (mediaPlayer?.isPlaying == true) {
                    pauseMusic()
                }
            }
        }
    }

    interface MusicServiceCallback {
        fun onSongChanged(position: Int, isPlaying: Boolean)
    }

    fun addCallback(callback: MusicServiceCallback) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: MusicServiceCallback) {
        callbacks.remove(callback)
    }

    private fun notifySongChanged() {
        val pos = currentPosition
        val playing = mediaPlayer?.isPlaying == true
        callbacks.forEach { it.onSongChanged(pos, playing) }
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener(this@MusicService)
        }
        createNotificationChannel()
        registerReceiver(
            becomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun playMusic(position: Int) {
        if (musicList.isEmpty()) return
        if (currentPosition == position && mediaPlayer?.isPlaying == true) return
        if (shuffleEnabled) buildShuffleOrder(startIndex = position)


        currentPosition = position
        val song = musicList[position]
        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(song.path)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
            startForeground(NOTIFICATION_ID, createNotification(song))
            notifySongChanged()
            setupDynamicsProcessing()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
        stopForeground(false)
        notifySongChanged()
    }

    fun startMusic() {
        if (mediaPlayer?.isPlaying == false && currentPosition != -1) {
            mediaPlayer?.start()
            getCurrentSong()?.let { startForeground(NOTIFICATION_ID, createNotification(it)) }
            notifySongChanged()
        }
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun playPrevious() {
        if (musicList.isEmpty() || currentPosition == -1) return

        if (shuffleEnabled && shuffleOrder != null) {
            val order = shuffleOrder ?: return
            if (shuffleIndex - 1 >= 0) {
                shuffleIndex--
                playMusic(order[shuffleIndex])
            } else {
                shuffleIndex = order.lastIndex
                playMusic(order[shuffleIndex])
            }
        } else {
            val prev = if (currentPosition - 1 < 0) musicList.size - 1 else currentPosition - 1
            playMusic(prev)
        }
    }

    fun getCurrentSong(): Audio? =
        if (currentPosition in musicList.indices) musicList[currentPosition] else null

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getCurrentPositionInList(): Int = currentPosition

    fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        currentPosition = -1
        notifySongChanged()
        stopForeground(true)
    }

    fun updateCurrentPositionAfterDeletion(deletedIndex: Int) {
        if (currentPosition > deletedIndex) {
            currentPosition -= 1
        } else if (currentPosition == deletedIndex) {
            stopMusic()
            shuffleOrder = null
            shuffleEnabled = false
            notifyShuffleChanged()
            return
        }

        if (shuffleEnabled && shuffleOrder != null) {
            val newOrder = mutableListOf<Int>()
            for (index in shuffleOrder!!) {
                when {
                    index == deletedIndex -> {}
                    index > deletedIndex -> newOrder.add(index - 1)
                    else -> newOrder.add(index)
                }
            }
            shuffleOrder = newOrder

            val newShuffleIndex = shuffleOrder!!.indexOf(currentPosition)
            if (newShuffleIndex != -1) {
                shuffleIndex = newShuffleIndex
            } else {
                buildShuffleOrder(startIndex = currentPosition)
            }

            if (shuffleOrder!!.isEmpty()) {
                shuffleEnabled = false
                shuffleOrder = null
                notifyShuffleChanged()
            }
        }
    }

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        playNext()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(becomingNoisyReceiver)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun setupDynamicsProcessing() {
        try {
            dynamicsProcessing?.release()
            dynamicsProcessing = null

            val mediaPlayer = mediaPlayer ?: return
            val audioSessionId = mediaPlayer.audioSessionId

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                Log.w(getString(R.string.music_service_tag),
                    getString(R.string.dynamics_processing_requires))
                return
            }

            val channelCount = 2
            val builder = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                channelCount,
                false,
                0,
                true,
                4,
                false,
                0,
                true
            )

            val config = builder.build()

            for (channel in 0 until channelCount) {
                val mbc = config.getMbcByChannelIndex(channel)

                for (band in 0 until mbc.bandCount) {
                    val mbcBand = mbc.getBand(band)
                    mbcBand.threshold = -20f
                    mbcBand.ratio = 4f
                    mbcBand.kneeWidth = 0f
                    mbcBand.attackTime = 5f
                    mbcBand.releaseTime = 200f
                    mbcBand.isEnabled = true
                }

                val limiter = config.getLimiterByChannelIndex(channel)
                limiter.attackTime = 1f
                limiter.releaseTime = 60f
                limiter.ratio = 10f
                limiter.threshold = -2f
                limiter.isEnabled = true
            }

            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                enabled = true
            }

            Log.d(getString(R.string.music_service_tag),
                getString(R.string.dynamics_processing_enabled))

        } catch (e: Exception) {
            Log.e(getString(R.string.music_service_tag),
                getString(R.string.dynamics_processing_failed), e)
        }
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

    interface OnShuffleChangeListener {
        fun onShuffleChanged(enabled: Boolean)
    }

    fun addShuffleListener(listener: OnShuffleChangeListener) {
        shuffleListeners.add(listener)
    }

    fun removeShuffleListener(listener: OnShuffleChangeListener) {
        shuffleListeners.remove(listener)
    }

    private fun notifyShuffleChanged() {
        shuffleListeners.forEach { it.onShuffleChanged(shuffleEnabled) }
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        if (shuffleEnabled) {
            buildShuffleOrder(startIndex = currentPosition)
        } else {
            shuffleOrder = null
        }
        notifyShuffleChanged()
    }

    private fun buildShuffleOrder(startIndex: Int) {
        if (musicList.isEmpty()) {
            shuffleOrder = null
            return
        }
        val indices = musicList.indices.toMutableList()
        indices.remove(startIndex)
        indices.shuffle()
        shuffleOrder = mutableListOf(startIndex).apply { addAll(indices) }
        shuffleIndex = 0
    }

    fun updateMusicList(newList: List<Audio>) {
        musicList = newList
        if (shuffleEnabled) {
            val current = if (currentPosition in musicList.indices) currentPosition else 0
            buildShuffleOrder(startIndex = current)
        } else {
            shuffleOrder = null
        }
    }

    fun isShuffleEnabled(): Boolean = shuffleEnabled

    fun playNext() {
        if (musicList.isEmpty() || currentPosition == -1) return

        if (shuffleEnabled && shuffleOrder != null) {
            val order = shuffleOrder ?: return
            if (shuffleIndex + 1 < order.size) {
                shuffleIndex++
                playMusic(order[shuffleIndex])
            } else {
                val current = currentPosition
                buildShuffleOrder(startIndex = current)
                if (shuffleOrder!!.size > 1) {
                    shuffleIndex = 1
                    playMusic(shuffleOrder!![shuffleIndex])
                } else {
                    playMusic(current)
                }
            }
        } else {
            val next = (currentPosition + 1) % musicList.size
            playMusic(next)
        }
    }

    companion object {
        private const val CHANNEL_ID = "MusicPlayerChannel"
        private const val NOTIFICATION_ID = 1
    }
}