package com.guxins.videocourseplayer.data.model

import android.net.Uri

data class Album(
    val key: String,
    val volumeName: String,
    val bucketId: String?,
    val name: String,
    val relativePath: String?,
    val videoCount: Int,
    val latestModified: Long,
    val coverUri: Uri?,
    val coverModified: Long,
)

data class VideoItem(
    val key: String,
    val mediaId: Long,
    val uri: Uri,
    val albumKey: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val lastModified: Long,
    val tags: List<String> = emptyList(),
)

enum class PlayMode {
    SEQUENTIAL,
    SINGLE_LOOP,
    ;

    companion object {
        fun fromStorage(value: String?): PlayMode =
            entries.firstOrNull { it.name == value } ?: SEQUENTIAL
    }
}

sealed interface VideoFilter {
    data object All : VideoFilter
    data object Untagged : VideoFilter
    data class ByTag(val tag: String) : VideoFilter
}

data class TagSummary(
    val name: String,
    val count: Int,
)

enum class VideoPermissionState {
    UNKNOWN,
    DENIED,
    LIMITED,
    FULL,
    ;

    val isGranted: Boolean
        get() = this == LIMITED || this == FULL
}
