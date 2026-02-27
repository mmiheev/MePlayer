package com.zeon.meplayer.ui.utils

import android.annotation.SuppressLint

@SuppressLint("DefaultLocale")
fun formatTime(millis: Long): String {
    if (millis < 0) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}