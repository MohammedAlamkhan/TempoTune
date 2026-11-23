package com.alam.tempotune

import com.alam.tempotune.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFiltersTest {

    private val tracks = listOf(
        track("Nocturne in Blue", "Aria Lane", "Midnight Sketches", "FLAC"),
        track("Paper Moons", "Aria Lane", "Midnight Sketches", "MP3"),
        track("Amber", "Kite & Co.", "Soft Focus", "FLAC"),
        track("Tidal", "Nova Park", "Bloom", "MP3"),
    )

    @Test
    fun searchMatchesTitleArtistAndAlbum() {
        val state = LibraryUiState(tracks = tracks)

        assertEquals(1, state.copy(query = "nocturne").filteredSongs.size)
        assertEquals(2, state.copy(query = "aria").filteredSongs.size)
        assertEquals(1, state.copy(query = "soft focus").filteredSongs.size)
        assertEquals(1, state.copy(query = "NOVA").filteredSongs.size)
    }

    @Test
    fun artistAndFormatFiltersCombine() {
        val state = LibraryUiState(
            tracks = tracks,
            artistFilter = "Aria Lane",
            formatFilter = "FLAC",
        )

        assertEquals(1, state.filteredSongs.size)
        assertEquals("Nocturne in Blue", state.filteredSongs.first().title)
        assertTrue(state.hasActiveFilter)
    }

    @Test
    fun clearingFiltersReturnsEverySong() {
        val state = LibraryUiState(
            tracks = tracks,
            query = "aria",
            artistFilter = "Aria Lane",
            formatFilter = "MP3",
        )
        val cleared = state.copy(query = "", artistFilter = null, formatFilter = null)

        assertEquals(4, cleared.filteredSongs.size)
        assertFalse(cleared.hasActiveFilter)
    }

    @Test
    fun artistsAreGroupedAndSorted() {
        val groups = LibraryUiState(tracks = tracks).artistGroups

        assertEquals(listOf("Aria Lane", "Kite & Co.", "Nova Park"), groups.map { it.name })
        assertEquals(2, groups.first().trackCount)
    }

    @Test
    fun artistSearchFiltersGroups() {
        val groups = LibraryUiState(tracks = tracks, query = "kite").artistGroups

        assertEquals(1, groups.size)
        assertEquals("Kite & Co.", groups.single().name)
    }

    @Test
    fun albumsAreGroupedAndSearchableByArtist() {
        val state = LibraryUiState(tracks = tracks, query = "aria")
        val albums = state.albumGroups

        assertEquals(1, albums.size)
        assertEquals("Midnight Sketches", albums.single().name)
        assertEquals(2, albums.single().trackCount)
    }

    @Test
    fun formatOptionsAreOrderedWithFlacFirst() {
        val formats = LibraryUiState(tracks = tracks).availableFormats

        assertEquals(listOf("FLAC", "MP3"), formats)
    }

    @Test
    fun currentListCountTracksTheActiveTab() {
        val songsState = LibraryUiState(tracks = tracks, query = "aria")
        val artistsState = songsState.copy(tab = LibraryTab.ARTISTS)
        val albumsState = songsState.copy(tab = LibraryTab.ALBUMS)

        assertEquals(2, songsState.currentListCount)
        assertEquals(1, artistsState.currentListCount)
        assertEquals(1, albumsState.currentListCount)
    }

    private fun track(title: String, artist: String, album: String, format: String): Track {
        return Track(
            uri = "content://test/${title.hashCode()}",
            title = title,
            artist = artist,
            durationMs = 42_000L,
            format = format,
            album = album,
        )
    }
}
