package com.alam.tempotune

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alam.tempotune.data.Track
import com.alam.tempotune.ui.LibraryScreen
import com.alam.tempotune.ui.PlayerScreen
import com.alam.tempotune.ui.theme.TempoTuneTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    private var audioPermissionGranted by mutableStateOf(false)
    private var audioPermissionDenied by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        audioPermissionGranted = checkAudioPermission()

        setContent {
            val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
            val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current
            val libraryListState = rememberLazyListState()
            var lastListSignature by remember { mutableStateOf<String?>(null) }

            var showPlayer by rememberSaveable { mutableStateOf(false) }
            var notificationAsked by rememberSaveable { mutableStateOf(false) }
            var pendingPlayback by remember { mutableStateOf<(() -> Unit)?>(null) }

            val audioPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { granted ->
                audioPermissionGranted = granted
                audioPermissionDenied = !granted
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) {
                pendingPlayback?.invoke()
                pendingPlayback = null
            }

            val requestAudioPermission = {
                audioPermissionLauncher.launch(requiredAudioPermission())
            }

            val openAppSettings = {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null),
                    ),
                )
            }

            val requestPlay: (List<Track>, Int) -> Unit = { tracks, index ->
                val play = { playerViewModel.playQueue(tracks, index) }
                val needsNotificationPermission =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED

                if (needsNotificationPermission && !notificationAsked) {
                    notificationAsked = true
                    pendingPlayback = play
                    notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                } else {
                    play()
                }
            }

            LaunchedEffect(audioPermissionGranted) {
                if (audioPermissionGranted) libraryViewModel.loadIfNeeded()
            }

            LaunchedEffect(playerViewModel) {
                playerViewModel.messages.collectLatest { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

            LaunchedEffect(
                libraryState.query,
                libraryState.tab,
                libraryState.artistFilter,
                libraryState.albumFilter,
                libraryState.formatFilter,
            ) {
                val signature = buildString {
                    append(libraryState.query)
                    append('|')
                    append(libraryState.tab)
                    append('|')
                    append(libraryState.artistFilter)
                    append('|')
                    append(libraryState.albumFilter)
                    append('|')
                    append(libraryState.formatFilter)
                }
                if (signature != lastListSignature) {
                    lastListSignature = signature
                    libraryListState.scrollToItem(0)
                }
            }

            BackHandler(
                enabled = showPlayer ||
                    libraryState.tab != LibraryTab.SONGS ||
                    libraryState.hasActiveFilter,
            ) {
                when {
                    showPlayer -> showPlayer = false
                    libraryState.hasActiveFilter -> libraryViewModel.clearAllFilters()
                    else -> libraryViewModel.setTab(LibraryTab.SONGS)
                }
            }

            TempoTuneTheme(themePreference = playerState.themePreference) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = showPlayer,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                if (targetState) {
                                    (
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(280),
                                        ) + fadeIn(tween(180))
                                        ) togetherWith (
                                        slideOutHorizontally(
                                            targetOffsetX = { -it / 5 },
                                            animationSpec = tween(280),
                                        ) + fadeOut(tween(160))
                                        )
                                } else {
                                    (
                                        slideInHorizontally(
                                            initialOffsetX = { -it / 5 },
                                            animationSpec = tween(260),
                                        ) + fadeIn(tween(180))
                                        ) togetherWith (
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(260),
                                        ) + fadeOut(tween(140))
                                        )
                                }
                            },
                            label = "app-screen",
                        ) { playerVisible ->
                            if (playerVisible) {
                                PlayerScreen(
                                    state = playerState,
                                    onBack = { showPlayer = false },
                                    onTogglePlayPause = playerViewModel::togglePlayPause,
                                    onSeek = playerViewModel::seekTo,
                                    onSeekBy = playerViewModel::seekBy,
                                    onPrevious = playerViewModel::skipPrevious,
                                    onNext = playerViewModel::skipNext,
                                    onPlayTrack = playerViewModel::playTrack,
                                    onTempoChange = playerViewModel::setTempoPercent,
                                    onPitchChange = playerViewModel::setPitchSemitones,
                                    onResetTempo = playerViewModel::resetTempo,
                                    onResetPitch = playerViewModel::resetPitch,
                                    onThemeChange = playerViewModel::setThemePreference,
                                )
                            } else {
                                LibraryScreen(
                                    libraryState = libraryState,
                                    playerState = playerState,
                                    hasAudioPermission = audioPermissionGranted,
                                    permissionDenied = audioPermissionDenied,
                                    onRequestPermission = requestAudioPermission,
                                    onOpenSettings = openAppSettings,
                                    onThemeChange = playerViewModel::setThemePreference,
                                    onRefresh = libraryViewModel::refresh,
                                    onQueryChange = libraryViewModel::setQuery,
                                    onTabChange = libraryViewModel::setTab,
                                    onFormatFilter = libraryViewModel::setFormatFilter,
                                    onSelectArtist = libraryViewModel::selectArtist,
                                    onSelectAlbum = libraryViewModel::selectAlbum,
                                    onClearArtistFilter = libraryViewModel::clearArtistFilter,
                                    onClearAlbumFilter = libraryViewModel::clearAlbumFilter,
                                    onClearAllFilters = libraryViewModel::clearAllFilters,
                                    onPlayTracks = requestPlay,
                                    onOpenPlayer = { showPlayer = true },
                                    onTogglePlayPause = playerViewModel::togglePlayPause,
                                    listState = libraryListState,
                                )
                            }
                        }

                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) { data ->
                            Snackbar(
                                snackbarData = data,
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                actionColor = MaterialTheme.colorScheme.inversePrimary,
                            )
                        }
                        }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val granted = checkAudioPermission()
        audioPermissionGranted = granted
        if (granted) audioPermissionDenied = false
    }

    override fun onStop() {
        playerViewModel.persistNow()
        super.onStop()
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            requiredAudioPermission(),
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requiredAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}
