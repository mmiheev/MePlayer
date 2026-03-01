package com.zeon.meplayer.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.zeon.meplayer.manager.PlaybackManager

class MusicServiceConnection(
    private val context: Context,
    private val onConnected: (PlaybackManager) -> Unit
) {
    private var playbackManager: PlaybackManager? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            playbackManager = binder.getPlaybackManager()
            isBound = true
            playbackManager?.let { onConnected.invoke(it) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            playbackManager = null
        }
    }

    fun bind() {
        Intent(context, MusicService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            context.startService(intent)
        }
    }

    fun unbind() {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
            playbackManager = null
        }
    }

    fun getPlaybackManager(): PlaybackManager? = playbackManager
}