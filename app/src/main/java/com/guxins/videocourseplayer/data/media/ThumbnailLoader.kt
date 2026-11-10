package com.guxins.videocourseplayer.data.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class ThumbnailLoader(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val semaphore = Semaphore(4)
    private val cache = object : LruCache<String, Bitmap>(maxCacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    suspend fun load(
        uri: Uri,
        key: String,
        updatedAt: Long,
        widthPx: Int = 320,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "$key@$updatedAt@$widthPx"
        cache.get(cacheKey)?.let { return@withContext it }

        semaphore.withPermit {
            cache.get(cacheKey) ?: run {
                val bitmap = runCatching {
                    resolver.loadThumbnail(
                        uri,
                        Size(widthPx, (widthPx * 9 / 16).coerceAtLeast(1)),
                        null,
                    )
                }.getOrNull()
                if (bitmap != null) {
                    cache.put(cacheKey, bitmap)
                }
                bitmap
            }
        }
    }

    private companion object {
        fun maxCacheSizeKb(): Int {
            val availableKb = (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt()
            return availableKb.coerceIn(8 * 1024, 32 * 1024)
        }
    }
}
