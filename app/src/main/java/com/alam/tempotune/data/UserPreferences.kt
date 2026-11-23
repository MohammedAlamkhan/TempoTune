package com.alam.tempotune.data

import android.content.Context
import com.alam.tempotune.audio.PlaybackMath
import org.json.JSONArray
import org.json.JSONObject

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AudioSettings(
    val tempoPercent: Int = PlaybackMath.DEFAULT_TEMPO_PERCENT,
    val pitchSemitones: Int = PlaybackMath.DEFAULT_PITCH_SEMITONES,
)

data class StoredQueue(
    val tracks: List<Track>,
    val index: Int,
    val positionMs: Long,
)

class UserPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun loadTheme(): ThemePreference {
        val value = preferences.getString(KEY_THEME, ThemePreference.SYSTEM.name)
        return ThemePreference.entries.firstOrNull { it.name == value } ?: ThemePreference.SYSTEM
    }

    fun saveTheme(theme: ThemePreference) {
        preferences.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun loadAudioSettings(): AudioSettings = AudioSettings(
        tempoPercent = preferences.getInt(KEY_TEMPO, PlaybackMath.DEFAULT_TEMPO_PERCENT),
        pitchSemitones = preferences.getInt(KEY_PITCH, PlaybackMath.DEFAULT_PITCH_SEMITONES),
    )

    fun saveAudioSettings(settings: AudioSettings) {
        preferences.edit()
            .putInt(KEY_TEMPO, settings.tempoPercent)
            .putInt(KEY_PITCH, settings.pitchSemitones)
            .apply()
    }

    fun loadQueue(): StoredQueue? {
        val raw = preferences.getString(KEY_QUEUE, null) ?: return null
        return try {
            val root = JSONObject(raw)
            val array = root.optJSONArray("tracks") ?: return null
            val tracks = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    Track.fromJson(item)?.let(::add)
                }
            }
            if (tracks.isEmpty()) return null
            StoredQueue(
                tracks = tracks,
                index = root.optInt("index", 0).coerceIn(tracks.indices),
                positionMs = preferences.getLong(
                    KEY_POSITION,
                    root.optLong("positionMs", 0L),
                ).coerceAtLeast(0L),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveQueue(tracks: List<Track>, index: Int, positionMs: Long) {
        if (tracks.isEmpty()) {
            preferences.edit().remove(KEY_QUEUE).apply()
            return
        }

        // Libraries can contain thousands of tracks. Persisting a bounded
        // window around the current track keeps resume fast and small while
        // still preserving meaningful next/previous context after process
        // death. The live service keeps the full queue while it is running.
        val safeIndex = index.coerceIn(tracks.indices)
        val start = (safeIndex - PERSISTED_QUEUE_RADIUS).coerceAtLeast(0)
        val end = (start + PERSISTED_QUEUE_SIZE).coerceAtMost(tracks.size)
        val windowStart = (end - PERSISTED_QUEUE_SIZE).coerceAtLeast(0)
        val window = tracks.subList(windowStart, end)
        val windowIndex = safeIndex - windowStart

        val root = JSONObject().apply {
            put("index", windowIndex)
            put("positionMs", positionMs.coerceAtLeast(0L))
            put(
                "tracks",
                JSONArray().apply {
                    window.forEach { put(it.toJson()) }
                },
            )
        }
        preferences.edit()
            .putString(KEY_QUEUE, root.toString())
            .putLong(KEY_POSITION, positionMs.coerceAtLeast(0L))
            .apply()
    }

    fun savePosition(positionMs: Long) {
        preferences.edit().putLong(KEY_POSITION, positionMs.coerceAtLeast(0L)).apply()
    }

    private companion object {
        const val FILE_NAME = "tempotune_prefs"
        const val KEY_THEME = "theme"
        const val KEY_TEMPO = "tempo_percent"
        const val KEY_PITCH = "pitch_semitones"
        const val KEY_QUEUE = "queue"
        const val KEY_POSITION = "position"
        const val PERSISTED_QUEUE_SIZE = 200
        const val PERSISTED_QUEUE_RADIUS = PERSISTED_QUEUE_SIZE / 2
    }
}
