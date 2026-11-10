package com.guxins.videocourseplayer.data.media

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.guxins.videocourseplayer.data.model.Album
import com.guxins.videocourseplayer.data.model.VideoItem
import com.guxins.videocourseplayer.util.NaturalOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MediaRepository {
    suspend fun getAlbums(): List<Album>

    suspend fun getVideos(album: Album): List<VideoItem>
}

class MediaStoreRepository(
    private val context: Context,
) : MediaRepository {
    private val collection: Uri =
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    override suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.VOLUME_NAME,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DATE_MODIFIED,
        )
        val selection = "${MediaStore.Video.Media.IS_PENDING} = 0 AND " +
            "${MediaStore.Video.Media.IS_TRASHED} = 0"
        val order = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        val accumulators = LinkedHashMap<String, AlbumAccumulator>()
        context.contentResolver.query(collection, projection, selection, null, order)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.long(MediaStore.Video.Media._ID) ?: continue
                val volumeName = cursor.string(MediaStore.Video.Media.VOLUME_NAME)
                    ?: MediaStore.VOLUME_EXTERNAL_PRIMARY
                val bucketId = cursor.string(MediaStore.Video.Media.BUCKET_ID)
                val relativePath = cursor.string(MediaStore.Video.Media.RELATIVE_PATH)
                val bucketName = cursor.string(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: relativePath?.trim()?.trimEnd('/')?.substringAfterLast('/')
                        ?.takeIf { it.isNotEmpty() }
                    ?: "未命名相册"
                val modified = cursor.long(MediaStore.Video.Media.DATE_MODIFIED) ?: 0L
                val key = albumKey(volumeName, bucketId, relativePath, bucketName)
                val videoUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.getContentUri(volumeName),
                    id,
                )

                val accumulator = accumulators.getOrPut(key) {
                    AlbumAccumulator(
                        key = key,
                        volumeName = volumeName,
                        bucketId = bucketId,
                        name = bucketName,
                        relativePath = relativePath,
                        coverUri = videoUri,
                        coverModified = modified,
                    )
                }
                accumulator.videoCount += 1
                if (modified > accumulator.latestModified) {
                    accumulator.latestModified = modified
                    accumulator.coverUri = videoUri
                    accumulator.coverModified = modified
                }
            }
        }

        accumulators.values
            .map { it.toAlbum() }
            .sortedWith(
                compareByDescending<Album> { it.latestModified }
                    .thenComparator { left, right -> NaturalOrder.compare(left.name, right.name) },
            )
    }

    override suspend fun getVideos(album: Album): List<VideoItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_MODIFIED,
        )
        val selectionParts = mutableListOf(
            "${MediaStore.Video.Media.VOLUME_NAME} = ?",
            "${MediaStore.Video.Media.IS_PENDING} = 0",
            "${MediaStore.Video.Media.IS_TRASHED} = 0",
        )
        val selectionArgs = mutableListOf(album.volumeName)
        if (album.bucketId != null) {
            selectionParts += "${MediaStore.Video.Media.BUCKET_ID} = ?"
            selectionArgs += album.bucketId
        } else if (!album.relativePath.isNullOrBlank()) {
            selectionParts += "${MediaStore.Video.Media.RELATIVE_PATH} = ?"
            selectionArgs += album.relativePath
        } else {
            selectionParts += "${MediaStore.Video.Media.BUCKET_DISPLAY_NAME} = ?"
            selectionArgs += album.name
        }

        val videos = mutableListOf<VideoItem>()
        context.contentResolver.query(
            collection,
            projection,
            selectionParts.joinToString(" AND "),
            selectionArgs.toTypedArray(),
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.long(MediaStore.Video.Media._ID) ?: continue
                val displayName = cursor.string(MediaStore.Video.Media.DISPLAY_NAME)
                    ?.takeIf { it.isNotBlank() }
                    ?: continue
                val volumeName = album.volumeName
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.getContentUri(volumeName),
                    id,
                )
                videos += VideoItem(
                    key = "$volumeName:$id",
                    mediaId = id,
                    uri = uri,
                    albumKey = album.key,
                    displayName = displayName,
                    durationMs = cursor.long(MediaStore.Video.Media.DURATION) ?: 0L,
                    sizeBytes = cursor.long(MediaStore.Video.Media.SIZE) ?: 0L,
                    mimeType = cursor.string(MediaStore.Video.Media.MIME_TYPE),
                    lastModified = cursor.long(MediaStore.Video.Media.DATE_MODIFIED) ?: 0L,
                )
            }
        }
        videos.sortedWith { left, right ->
            val byName = NaturalOrder.compare(left.displayName, right.displayName)
            if (byName != 0) byName else left.key.compareTo(right.key)
        }
    }

    private fun albumKey(
        volumeName: String,
        bucketId: String?,
        relativePath: String?,
        bucketName: String,
    ): String = "$volumeName:${bucketId ?: relativePath ?: bucketName}"

    private data class AlbumAccumulator(
        val key: String,
        val volumeName: String,
        val bucketId: String?,
        val name: String,
        val relativePath: String?,
        var videoCount: Int = 0,
        var latestModified: Long = 0L,
        var coverUri: Uri?,
        var coverModified: Long = 0L,
    ) {
        fun toAlbum(): Album = Album(
            key = key,
            volumeName = volumeName,
            bucketId = bucketId,
            name = name,
            relativePath = relativePath,
            videoCount = videoCount,
            latestModified = latestModified,
            coverUri = coverUri,
            coverModified = coverModified,
        )
    }
}

private fun Cursor.string(column: String): String? {
    val index = getColumnIndex(column)
    if (index < 0 || isNull(index)) return null
    return getString(index)
}

private fun Cursor.long(column: String): Long? {
    val index = getColumnIndex(column)
    if (index < 0 || isNull(index)) return null
    return getLong(index)
}
