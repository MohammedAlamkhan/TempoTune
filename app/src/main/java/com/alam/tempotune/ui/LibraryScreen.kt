package com.alam.tempotune.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alam.tempotune.AlbumGroup
import com.alam.tempotune.ArtistGroup
import com.alam.tempotune.LibraryTab
import com.alam.tempotune.LibraryUiState
import com.alam.tempotune.PlayerUiState
import com.alam.tempotune.R
import com.alam.tempotune.audio.PlaybackMath
import com.alam.tempotune.data.ArtworkRepository
import com.alam.tempotune.data.ThemePreference
import com.alam.tempotune.data.Track
import kotlin.math.abs

@Composable
fun LibraryScreen(
    libraryState: LibraryUiState,
    playerState: PlayerUiState,
    hasAudioPermission: Boolean,
    permissionDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onTabChange: (LibraryTab) -> Unit,
    onFormatFilter: (String?) -> Unit,
    onSelectArtist: (String) -> Unit,
    onSelectAlbum: (String) -> Unit,
    onClearArtistFilter: () -> Unit,
    onClearAlbumFilter: () -> Unit,
    onClearAllFilters: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val isSongsTab = libraryState.tab == LibraryTab.SONGS
    val isArtistsTab = libraryState.tab == LibraryTab.ARTISTS
    val isAlbumsTab = libraryState.tab == LibraryTab.ALBUMS

    // Recompute only the grouping needed by the visible tab, so searching a
    // large library never pays for the other two lists.
    val filteredSongs = remember(
        libraryState.tracks,
        libraryState.query,
        libraryState.artistFilter,
        libraryState.albumFilter,
        libraryState.formatFilter,
        isSongsTab,
    ) {
        if (isSongsTab) libraryState.filteredSongs else emptyList()
    }
    val artistGroups = remember(libraryState.tracks, libraryState.query, isArtistsTab) {
        if (isArtistsTab) libraryState.artistGroups else emptyList()
    }
    val albumGroups = remember(libraryState.tracks, libraryState.query, isAlbumsTab) {
        if (isAlbumsTab) libraryState.albumGroups else emptyList()
    }
    val availableFormats = remember(libraryState.tracks) {
        libraryState.availableFormats
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        LibraryTopBar(
            isLoading = libraryState.isLoading,
            showRefresh = hasAudioPermission,
            onRefresh = onRefresh,
            currentTheme = playerState.themePreference,
            onThemeChange = onThemeChange,
        )

        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.library_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        val subtitle = librarySubtitle(
            state = libraryState,
            hasPermission = hasAudioPermission,
            filteredSongsCount = filteredSongs.size,
            artistGroupsCount = artistGroups.size,
            albumGroupsCount = albumGroups.size,
        )
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
            )
        }

        if (!hasAudioPermission) {
            PermissionContent(
                denied = permissionDenied,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            Spacer(Modifier.height(16.dp))
            SearchField(
                query = libraryState.query,
                onQueryChange = onQueryChange,
                onSearch = { focusManager.clearFocus() },
            )
            Spacer(Modifier.height(12.dp))
            LibraryTabs(
                selected = libraryState.tab,
                onSelect = {
                    focusManager.clearFocus()
                    onTabChange(it)
                },
            )

            if (libraryState.tab == LibraryTab.SONGS &&
                (
                    availableFormats.size > 1 ||
                        libraryState.artistFilter != null ||
                        libraryState.albumFilter != null ||
                        libraryState.formatFilter != null
                    )
            ) {
                Spacer(Modifier.height(8.dp))
                FilterRow(
                    state = libraryState,
                    availableFormats = availableFormats,
                    onFormatFilter = onFormatFilter,
                    onClearArtistFilter = onClearArtistFilter,
                    onClearAlbumFilter = onClearAlbumFilter,
                    onClearAllFilters = onClearAllFilters,
                )
            }

            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    libraryState.errorMessage != null -> {
                        LibraryErrorContent(
                            message = libraryState.errorMessage,
                            onRetry = onRefresh,
                        )
                    }

                    libraryState.isLoading && !libraryState.hasLoaded -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                            )
                        }
                    }

                    libraryState.tracks.isEmpty() -> {
                        EmptyLibraryContent(onRefresh = onRefresh, compact = imeVisible)
                    }

                    libraryState.tab == LibraryTab.SONGS && filteredSongs.isEmpty() -> {
                        NoResultsContent(
                            onClearFilters = onClearAllFilters,
                            compact = imeVisible,
                        )
                    }

                    libraryState.tab == LibraryTab.ARTISTS && artistGroups.isEmpty() -> {
                        NoResultsContent(
                            onClearFilters = onClearAllFilters,
                            compact = imeVisible,
                        )
                    }

                    libraryState.tab == LibraryTab.ALBUMS && albumGroups.isEmpty() -> {
                        NoResultsContent(
                            onClearFilters = onClearAllFilters,
                            compact = imeVisible,
                        )
                    }

                    else -> {
                        when (libraryState.tab) {
                            LibraryTab.SONGS -> SongList(
                                songs = filteredSongs,
                                currentTrackUri = playerState.currentTrack?.uri,
                                listState = listState,
                                onSongClick = { index ->
                                    focusManager.clearFocus()
                                    onPlayTracks(filteredSongs, index)
                                },
                            )

                            LibraryTab.ARTISTS -> ArtistList(
                                artists = artistGroups,
                                listState = listState,
                                onArtistClick = onSelectArtist,
                            )

                            LibraryTab.ALBUMS -> AlbumList(
                                albums = albumGroups,
                                listState = listState,
                                onAlbumClick = onSelectAlbum,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = playerState.currentTrack != null && !imeVisible,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(180)),
            ) {
                MiniPlayer(
                    playerState = playerState,
                    onOpenPlayer = onOpenPlayer,
                    onTogglePlayPause = onTogglePlayPause,
                )
            }
        }
    }
}

@Composable
private fun LibraryTopBar(
    isLoading: Boolean,
    showRefresh: Boolean,
    onRefresh: () -> Unit,
    currentTheme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LogoMark()
        Spacer(Modifier.width(11.dp))
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        if (showRefresh) {
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(19.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.refresh_library),
                    )
                }
            }
        }
        ThemeMenu(
            current = currentTheme,
            onThemeChange = onThemeChange,
        )
    }
}

@Composable
private fun librarySubtitle(
    state: LibraryUiState,
    hasPermission: Boolean,
    filteredSongsCount: Int,
    artistGroupsCount: Int,
    albumGroupsCount: Int,
): String {
    if (!hasPermission) return ""
    if (state.isLoading && !state.hasLoaded) {
        return stringResource(R.string.scanning_library)
    }
    return when (state.tab) {
        LibraryTab.SONGS -> pluralStringResource(
            R.plurals.song_count,
            filteredSongsCount,
            filteredSongsCount,
        )

        LibraryTab.ARTISTS -> pluralStringResource(
            R.plurals.artist_count,
            artistGroupsCount,
            artistGroupsCount,
        )

        LibraryTab.ALBUMS -> pluralStringResource(
            R.plurals.album_count,
            albumGroupsCount,
            albumGroupsCount,
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = stringResource(R.string.search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.search_clear),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colorScheme.surfaceContainerLow,
            unfocusedContainerColor = colorScheme.surfaceContainerLow,
            disabledContainerColor = colorScheme.surfaceContainerLow,
            focusedBorderColor = colorScheme.primary.copy(alpha = 0.55f),
            unfocusedBorderColor = colorScheme.outlineVariant.copy(alpha = 0.55f),
            cursorColor = colorScheme.primary,
        ),
    )
}

@Composable
private fun LibraryTabs(
    selected: LibraryTab,
    onSelect: (LibraryTab) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(CircleShape)
            .background(colorScheme.surfaceContainerLow)
            .padding(3.dp),
    ) {
        LibraryTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) colorScheme.primary else Color.Transparent,
                animationSpec = tween(180),
                label = "tab-background",
            )
            val foreground by animateColorAsState(
                targetValue = if (isSelected) {
                    colorScheme.onPrimary
                } else {
                    colorScheme.onSurfaceVariant
                },
                animationSpec = tween(180),
                label = "tab-foreground",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(background)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelect(tab) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (tab) {
                        LibraryTab.SONGS -> stringResource(R.string.tab_songs)
                        LibraryTab.ARTISTS -> stringResource(R.string.tab_artists)
                        LibraryTab.ALBUMS -> stringResource(R.string.tab_albums)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = foreground,
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    state: LibraryUiState,
    availableFormats: List<String>,
    onFormatFilter: (String?) -> Unit,
    onClearArtistFilter: () -> Unit,
    onClearAlbumFilter: () -> Unit,
    onClearAllFilters: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 1.dp),
    ) {
        state.artistFilter?.let { artist ->
            item(key = "artist-filter") {
                FilterChip(
                    selected = true,
                    onClick = onClearArtistFilter,
                    shape = RoundedCornerShape(10.dp),
                    label = {
                        Text(
                            text = stringResource(R.string.artist_filter, artist),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 210.dp),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.clear_filters),
                            modifier = Modifier.size(15.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colorScheme.primary.copy(alpha = 0.12f),
                        selectedLabelColor = colorScheme.primary,
                        selectedTrailingIconColor = colorScheme.primary,
                    ),
                )
            }
        }
        state.albumFilter?.let { album ->
            item(key = "album-filter") {
                FilterChip(
                    selected = true,
                    onClick = onClearAlbumFilter,
                    shape = RoundedCornerShape(10.dp),
                    label = {
                        Text(
                            text = stringResource(R.string.album_filter, album),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 210.dp),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.clear_filters),
                            modifier = Modifier.size(15.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colorScheme.primary.copy(alpha = 0.12f),
                        selectedLabelColor = colorScheme.primary,
                        selectedTrailingIconColor = colorScheme.primary,
                    ),
                )
            }
        }
        if (state.hasActiveFilter) {
            item(key = "clear-all") {
                TextButton(
                    onClick = onClearAllFilters,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(stringResource(R.string.clear_filters))
                }
            }
        }
        item(key = "format-all") {
            FilterChip(
                selected = state.formatFilter == null,
                onClick = { onFormatFilter(null) },
                shape = RoundedCornerShape(10.dp),
                label = { Text(stringResource(R.string.all_formats)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorScheme.primary.copy(alpha = 0.12f),
                    selectedLabelColor = colorScheme.primary,
                ),
            )
        }
        items(availableFormats, key = { "format-$it" }) { format ->
            FilterChip(
                selected = state.formatFilter == format,
                onClick = {
                    onFormatFilter(if (state.formatFilter == format) null else format)
                },
                shape = RoundedCornerShape(10.dp),
                label = { Text(format) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorScheme.primary.copy(alpha = 0.12f),
                    selectedLabelColor = colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun SongList(
    songs: List<Track>,
    currentTrackUri: String?,
    listState: LazyListState,
    onSongClick: (Int) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(
            items = songs,
            key = { _, track -> track.uri },
        ) { index, track ->
            SongRow(
                track = track,
                isCurrent = track.uri == currentTrackUri,
                onClick = { onSongClick(index) },
            )
        }
    }
}

@Composable
private fun SongRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isCurrent) colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryArtwork(
            track = track,
            size = 48.dp,
            shape = RoundedCornerShape(12.dp),
            isCurrent = isCurrent,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) colorScheme.primary else colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = listOfNotNull(track.artist, track.album).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = PlaybackMath.formatTime(track.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = track.format,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    letterSpacing = 0.8.sp,
                ),
                color = colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ArtistList(
    artists: List<ArtistGroup>,
    listState: LazyListState,
    onArtistClick: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(artists, key = { it.name }) { artist ->
            GroupRow(
                title = artist.name,
                subtitle = pluralStringResource(
                    R.plurals.track_count_plural,
                    artist.trackCount,
                    artist.trackCount,
                ),
                track = artist.sampleTrack,
                avatarLabel = artist.name.firstOrNull()?.uppercase(),
                circularArtwork = true,
                onClick = { onArtistClick(artist.name) },
            )
        }
    }
}

@Composable
private fun AlbumList(
    albums: List<AlbumGroup>,
    listState: LazyListState,
    onAlbumClick: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(albums, key = { "${it.name}-${it.artist}" }) { album ->
            GroupRow(
                title = album.name,
                subtitle = "${album.artist}  ·  " + pluralStringResource(
                    R.plurals.track_count_plural,
                    album.trackCount,
                    album.trackCount,
                ),
                track = album.sampleTrack,
                circularArtwork = false,
                onClick = { onAlbumClick(album.name) },
            )
        }
    }
}

@Composable
private fun GroupRow(
    title: String,
    subtitle: String,
    track: Track,
    circularArtwork: Boolean,
    onClick: () -> Unit,
    avatarLabel: String? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryArtwork(
            track = track,
            size = 50.dp,
            shape = if (circularArtwork) CircleShape else RoundedCornerShape(12.dp),
            fallbackLabel = avatarLabel,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colorScheme.outline,
        )
    }
}

@Composable
private fun LibraryArtwork(
    track: Track,
    size: Dp,
    shape: Shape,
    isCurrent: Boolean = false,
    fallbackLabel: String? = null,
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var artwork by remember(track.uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(track.uri) {
        artwork = ArtworkRepository.load(context, track, ARTWORK_TARGET_PX)?.asImageBitmap()
    }
    val paletteIndex = abs(track.uri.hashCode()) % 3
    val palette = listOf(
        listOf(colorScheme.primaryContainer, colorScheme.tertiaryContainer),
        listOf(colorScheme.secondaryContainer, colorScheme.primaryContainer),
        listOf(colorScheme.tertiaryContainer, colorScheme.secondaryContainer),
    )[paletteIndex]

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(palette))
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        artwork?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } ?: run {
            if (fallbackLabel != null) {
                Text(
                    text = fallbackLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(size * 0.38f),
                    tint = colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                )
            }
        }

        if (isCurrent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = stringResource(R.string.current_track_marker),
                    modifier = Modifier.size(size * 0.42f),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    playerState: PlayerUiState,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
) {
    val track = playerState.currentTrack ?: return
    val colorScheme = MaterialTheme.colorScheme
    val fraction = if (playerState.durationMs > 0) {
        (playerState.positionMs.toFloat() / playerState.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val pitchLabel = when {
        playerState.pitchSemitones > 0 -> stringResource(
            R.string.pitch_positive,
            playerState.pitchSemitones,
        )

        playerState.pitchSemitones < 0 -> stringResource(
            R.string.pitch_negative,
            -playerState.pitchSemitones,
        )

        else -> stringResource(R.string.pitch_zero)
    }
    val openDescription = stringResource(R.string.open_player)
    val shape = RoundedCornerShape(22.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClickLabel = openDescription,
                onClick = onOpenPlayer,
            ),
        shape = shape,
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
        shadowElevation = 6.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(colorScheme.outlineVariant.copy(alpha = 0.28f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(colorScheme.primary),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LibraryArtwork(
                    track = track,
                    size = 44.dp,
                    shape = RoundedCornerShape(11.dp),
                )
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${track.artist}  ·  ${playerState.tempoPercent}%  ·  $pitchLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                        contentDescription = stringResource(
                            if (playerState.isPlaying) R.string.pause else R.string.play,
                        ),
                        tint = colorScheme.primary,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.outline,
                )
            }
        }
    }
}


@Composable
private fun CenteredScrollableState(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = viewportHeight)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

@Composable
private fun PermissionContent(
    denied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    CenteredScrollableState(modifier = modifier.padding(bottom = 8.dp)) {
        Box(
            modifier = Modifier
                .size(158.dp)
                .clip(RoundedCornerShape(44.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            colorScheme.primaryContainer,
                            colorScheme.tertiaryContainer,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = colorScheme.onPrimaryContainer.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.36f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawCircle(
                    color = colorScheme.onPrimaryContainer.copy(alpha = 0.07f),
                    radius = size.minDimension * 0.50f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            Icon(
                imageVector = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(58.dp),
                tint = colorScheme.primary,
            )
        }
        Spacer(Modifier.height(30.dp))
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(
                if (denied) R.string.permission_denied_body else R.string.permission_body,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 340.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
        ) {
            Text(stringResource(R.string.allow_music_access))
        }
        if (denied) {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}

@Composable
private fun EmptyLibraryContent(
    onRefresh: () -> Unit,
    compact: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    CenteredScrollableState {
        if (!compact) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = colorScheme.primary,
                )
            }
            Spacer(Modifier.height(20.dp))
        }
        Text(
            text = stringResource(R.string.no_music_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_music_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onRefresh,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(R.string.refresh_library))
        }
    }
}

@Composable
private fun NoResultsContent(
    onClearFilters: () -> Unit,
    compact: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    CenteredScrollableState {
        if (!compact) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(18.dp))
        }
        Text(
            text = stringResource(R.string.no_results_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = stringResource(R.string.no_results_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onClearFilters) {
            Text(stringResource(R.string.clear_filters))
        }
    }
}

@Composable
private fun LibraryErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    CenteredScrollableState {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}

private const val ARTWORK_TARGET_PX = 256
