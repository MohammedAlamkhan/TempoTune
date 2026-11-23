package com.alam.tempotune

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alam.tempotune.data.MusicLibrary
import com.alam.tempotune.data.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LibraryTab {
    SONGS,
    ARTISTS,
    ALBUMS,
}

data class ArtistGroup(
    val name: String,
    val trackCount: Int,
    val sampleTrack: Track,
)

data class AlbumGroup(
    val name: String,
    val artist: String,
    val trackCount: Int,
    val sampleTrack: Track,
)

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val errorMessage: String? = null,
    val query: String = "",
    val tab: LibraryTab = LibraryTab.SONGS,
    val artistFilter: String? = null,
    val albumFilter: String? = null,
    val formatFilter: String? = null,
) {
    private val normalizedQuery: String
        get() = query.trim()

    val availableFormats: List<String>
        get() = tracks
            .map { it.format }
            .distinct()
            .sortedWith(
                compareBy(
                    { formatRank(it) },
                    { it },
                ),
            )

    val filteredSongs: List<Track>
        get() {
            val needle = normalizedQuery
            return tracks.filter { track ->
                track.matches(needle) &&
                    (artistFilter == null || track.artist == artistFilter) &&
                    (albumFilter == null || track.album == albumFilter) &&
                    (formatFilter == null || track.format == formatFilter)
            }
        }

    val artistGroups: List<ArtistGroup>
        get() {
            val needle = normalizedQuery
            return tracks
                .filter { track ->
                    (formatFilter == null || track.format == formatFilter) &&
                        (needle.isEmpty() || track.artist.contains(needle, ignoreCase = true))
                }
                .groupBy { it.artist }
                .map { (artist, songs) ->
                    ArtistGroup(
                        name = artist,
                        trackCount = songs.size,
                        sampleTrack = songs.first(),
                    )
                }
                .sortedBy { it.name.lowercase() }
        }

    val albumGroups: List<AlbumGroup>
        get() {
            val needle = normalizedQuery
            return tracks
                .filter { track ->
                    (formatFilter == null || track.format == formatFilter) &&
                        (
                            needle.isEmpty() ||
                                track.album?.contains(needle, ignoreCase = true) == true ||
                                track.artist.contains(needle, ignoreCase = true)
                            )
                }
                .groupBy { it.album ?: UNKNOWN_ALBUM_KEY }
                .map { (album, songs) ->
                    val first = songs.first()
                    AlbumGroup(
                        name = album,
                        artist = first.artist,
                        trackCount = songs.size,
                        sampleTrack = first,
                    )
                }
                .sortedBy { it.name.lowercase() }
        }

    val hasActiveFilter: Boolean
        get() = query.isNotBlank() ||
            artistFilter != null ||
            albumFilter != null ||
            formatFilter != null

    val currentListCount: Int
        get() = when (tab) {
            LibraryTab.SONGS -> filteredSongs.size
            LibraryTab.ARTISTS -> artistGroups.size
            LibraryTab.ALBUMS -> albumGroups.size
        }

    private fun Track.matches(needle: String): Boolean {
        if (needle.isEmpty()) return true
        return title.contains(needle, ignoreCase = true) ||
            artist.contains(needle, ignoreCase = true) ||
            album?.contains(needle, ignoreCase = true) == true
    }

    private fun formatRank(format: String): Int {
        val index = FORMAT_ORDER.indexOf(format)
        return if (index >= 0) index else FORMAT_ORDER.size
    }

    private companion object {
        const val UNKNOWN_ALBUM_KEY = "__unknown_album__"
        val FORMAT_ORDER = listOf(
            "FLAC",
            "MP3",
            "M4A",
            "AAC",
            "OGG",
            "OPUS",
            "WAV",
            "AIFF",
            "WMA",
            "MKA",
            "AMR",
        )
    }
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val musicLibrary = MusicLibrary(application)
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadIfNeeded() {
        if (_uiState.value.hasLoaded || loadJob?.isActive == true) return
        refresh()
    }

    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = runCatching { musicLibrary.loadTracks() }
            result.fold(
                onSuccess = { tracks ->
                    _uiState.value = _uiState.value.copy(
                        tracks = tracks,
                        isLoading = false,
                        hasLoaded = true,
                        errorMessage = null,
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasLoaded = true,
                        errorMessage = getApplication<Application>().getString(
                            R.string.library_error,
                        ),
                    )
                },
            )
        }
    }

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun setTab(tab: LibraryTab) {
        if (tab == _uiState.value.tab) return
        _uiState.value = _uiState.value.copy(
            tab = tab,
            artistFilter = null,
            albumFilter = null,
            formatFilter = null,
        )
    }

    fun setFormatFilter(format: String?) {
        _uiState.value = _uiState.value.copy(formatFilter = format)
    }

    fun selectArtist(artist: String) {
        _uiState.value = _uiState.value.copy(
            tab = LibraryTab.SONGS,
            query = "",
            artistFilter = artist,
            albumFilter = null,
            formatFilter = null,
        )
    }

    fun selectAlbum(album: String) {
        _uiState.value = _uiState.value.copy(
            tab = LibraryTab.SONGS,
            query = "",
            albumFilter = album,
            artistFilter = null,
            formatFilter = null,
        )
    }

    fun clearArtistFilter() {
        _uiState.value = _uiState.value.copy(artistFilter = null)
    }

    fun clearAlbumFilter() {
        _uiState.value = _uiState.value.copy(albumFilter = null)
    }

    fun clearFormatFilter() {
        _uiState.value = _uiState.value.copy(formatFilter = null)
    }

    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            query = "",
            artistFilter = null,
            albumFilter = null,
            formatFilter = null,
        )
    }
}
