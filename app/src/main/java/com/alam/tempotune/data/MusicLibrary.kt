package com.alam.tempotune.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.alam.tempotune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the device's indexed audio collection through MediaStore. No files are
 * copied or uploaded; the app only receives content URIs and metadata.
 */
class MusicLibrary(private val context: Context) {

    suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val selection = buildString {
            append("(")
            append(MediaStore.Audio.Media.IS_MUSIC)
            append(" != 0 OR ")
            append(MediaStore.Audio.Media.MIME_TYPE)
            append(" IN ('audio/flac','audio/x-flac','audio/mpeg'))")
            append(" AND ")
            append(MediaStore.Audio.Media.DURATION)
            append(" >= ?")
        }
        val selectionArgs = arrayOf(MINIMUM_DURATION_MS.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val tracks = buildList {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val displayNameColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DISPLAY_NAME,
                )
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                        ?.takeIf { it.isNotBlank() }
                    val title = cursor.getString(titleColumn)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                        ?: "Untitled"
                    val artist = cursor.getString(artistColumn)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: context.getString(R.string.unknown_artist)
                    val album = cursor.getString(albumColumn)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: context.getString(R.string.unknown_album)
                    val duration = cursor.getLong(durationColumn).coerceAtLeast(0L)
                    val mimeType = cursor.getString(mimeColumn)
                    val format = formatFor(displayName, mimeType)

                    add(
                        Track(
                            uri = ContentUris.withAppendedId(collection, id).toString(),
                            title = title,
                            artist = artist,
                            durationMs = duration,
                            format = format,
                            album = album,
                        ),
                    )
                }
            }
        }

        tracks.distinctBy { it.uri }
    }

    private fun formatFor(displayName: String?, mimeType: String?): String {
        val fromName = displayName
            ?.let(::formatFromUri)
            ?.takeIf { it != "AUDIO" }
        if (fromName != null) return fromName

        return when (mimeType?.lowercase()) {
            "audio/flac", "audio/x-flac" -> "FLAC"
            "audio/mpeg" -> "MP3"
            "audio/mp4", "audio/x-m4a" -> "M4A"
            "audio/aac" -> "AAC"
            "audio/ogg" -> "OGG"
            "audio/opus" -> "OPUS"
            "audio/wav", "audio/x-wav" -> "WAV"
            "audio/aiff", "audio/x-aiff" -> "AIFF"
            else -> "AUDIO"
        }
    }

    private companion object {
        const val MINIMUM_DURATION_MS = 3_000L
    }
}
