package com.guxins.videocourseplayer.util

import java.util.Locale

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSeconds = durationMs / 1000L
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun formatSize(sizeBytes: Long): String {
    if (sizeBytes <= 0L) return ""
    val megabytes = sizeBytes / (1024.0 * 1024.0)
    return if (megabytes >= 1024) {
        String.format(Locale.US, "%.1f GB", megabytes / 1024)
    } else {
        String.format(Locale.US, "%.1f MB", megabytes)
    }
}
