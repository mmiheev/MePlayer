package com.zeon.meplayer.presentation.screen.playlist.components

sealed class TrackAction {
    data class AddToPlaylist(val onAdd: () -> Unit) : TrackAction()
    data class RemoveFromPlaylist(val onRemove: () -> Unit) : TrackAction()
    data class DeleteFromDevice(val onDelete: () -> Unit) : TrackAction()
}