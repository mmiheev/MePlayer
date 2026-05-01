package com.zeon.meplayer.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.zeon.meplayer.domain.model.Audio
import com.zeon.meplayer.presentation.theme.AppGradients

@Composable
fun AlbumArt(
    audio: Audio?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gradient = if (isDark) AppGradients.darkGradient else AppGradients.primaryGradient

    @Composable
    fun Placeholder() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradient)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(120.dp)
            )
        }
    }

    if (audio?.albumArtUri == null) {
        Box(modifier = modifier) { Placeholder() }
        return
    }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(audio.albumArtUri)
            .crossfade(true)
            .build()
    )

    val state = painter.state
    Box(modifier = modifier) {
        Image(
            painter = painter,
            contentDescription = "Album art for ${audio.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
        if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
            Placeholder()
        }
    }
}