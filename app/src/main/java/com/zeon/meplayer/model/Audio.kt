package com.zeon.meplayer.model

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

data class Audio(
    val id: Long,
    val path: String,
    val title: String,
    val artist: String,
    val duration: Long
) {
    val uri: Uri
        get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
}