package com.alam.tempotune.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Small disk-free artwork cache. MediaStore thumbnails are fast, while the
 * embedded-picture fallback covers files and providers that do not publish
 * thumbnails.
 */
object ArtworkRepository {
    private const val CACHE_BYTES = 18 * 1024 * 1024
    private val cache = LruCache<String, Bitmap>(CACHE_BYTES)

    suspend fun load(
        context: Context,
        track: Track,
        targetSizePx: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val bucket = if (targetSizePx > 512) "large" else "small"
        val key = "${track.uri}@$bucket"
        cache.get(key)?.let { return@withContext it }

        val bitmap = runCatching {
            if (!track.artworkPath.isNullOrBlank()) {
                decodeFile(track.artworkPath, targetSizePx)
            } else {
                loadFromContent(context, Uri.parse(track.uri), targetSizePx)
            }
        }.getOrNull()

        if (bitmap != null) cache.put(key, bitmap)
        bitmap
    }

    private fun loadFromContent(context: Context, uri: Uri, targetSizePx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                context.contentResolver.loadThumbnail(uri, Size(targetSizePx, targetSizePx), null)
            }.getOrNull()?.let { return it }
        }

        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever().apply { setDataSource(context, uri) }
            val bytes = retriever.embeddedPicture ?: return null
            decodeBytes(bytes, targetSizePx)
        } catch (_: Exception) {
            null
        } finally {
            retriever?.release()
        }
    }

    private fun decodeFile(path: String, targetSizePx: Int): Bitmap? {
        val file = File(path)
        if (!file.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetSizePx)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(path, options)
    }

    private fun decodeBytes(bytes: ByteArray, targetSizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetSizePx)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun sampleSize(width: Int, height: Int, targetSizePx: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (width / sample > targetSizePx || height / sample > targetSizePx) {
            sample *= 2
        }
        return sample
    }
}
