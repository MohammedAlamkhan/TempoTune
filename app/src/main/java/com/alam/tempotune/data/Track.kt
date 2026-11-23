package com.alam.tempotune.data

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import org.json.JSONObject
import java.io.File

data class Track(
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val format: String,
    val artworkPath: String? = null,
    val album: String? = null,
) {
    @OptIn(UnstableApi::class)
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setDurationMs(durationMs.takeIf { it > 0 })
            .apply {
                artworkPath?.let { path ->
                    if (File(path).isFile) {
                        setArtworkUri(Uri.fromFile(File(path)))
                    }
                }
            }
            .build()

        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("uri", uri)
        put("title", title)
        put("artist", artist)
        put("durationMs", durationMs)
        put("format", format)
        put("artworkPath", artworkPath ?: JSONObject.NULL)
        put("album", album ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(json: JSONObject): Track? {
            val uri = json.optString("uri").takeIf { it.isNotBlank() } ?: return null
            return Track(
                uri = uri,
                title = json.optString("title").ifBlank { "Untitled" },
                artist = json.optString("artist").ifBlank { "Unknown artist" },
                durationMs = json.optLong("durationMs", 0L),
                format = json.optString("format").ifBlank { "AUDIO" },
                artworkPath = json.optString("artworkPath").takeIf {
                    it.isNotBlank() && it != "null"
                },
                album = json.optString("album").takeIf {
                    it.isNotBlank() && it != "null"
                },
            )
        }
    }
}

/**
 * Rebuilds a reasonable track descriptor for media items that already live in
 * a connected playback service (for example after this activity was recreated).
 */
@OptIn(UnstableApi::class)
fun MediaItem.toTrack(): Track? {
    val mediaUri = localConfiguration?.uri?.toString() ?: return null
    val title = mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() }
        ?: mediaUri.substringAfterLast('/').substringBeforeLast('.').ifBlank { "Untitled" }
    val artist = mediaMetadata.artist?.toString()?.takeIf { it.isNotBlank() }
        ?: "Unknown artist"
    val duration = mediaMetadata.durationMs ?: 0L
    val album = mediaMetadata.albumTitle?.toString()?.takeIf { it.isNotBlank() }
    val artworkPath = mediaMetadata.artworkUri
        ?.takeIf { it.scheme == "file" }
        ?.path
    return Track(
        uri = mediaUri,
        title = title,
        artist = artist,
        durationMs = duration,
        format = formatFromUri(mediaUri),
        artworkPath = artworkPath,
        album = album,
    )
}

fun formatFromUri(uri: String): String {
    val extension = uri.substringAfterLast('/')
        .substringAfterLast('.', "")
        .lowercase()
    return when (extension) {
        "flac" -> "FLAC"
        "mp3" -> "MP3"
        "m4a", "mp4", "aac" -> extension.uppercase()
        "ogg", "oga" -> "OGG"
        "opus" -> "OPUS"
        "wav", "wave" -> "WAV"
        "aif", "aiff" -> "AIFF"
        "wma" -> "WMA"
        "mka" -> "MKA"
        else -> if (extension.length in 2..5) extension.uppercase() else "AUDIO"
    }
}
