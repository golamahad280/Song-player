package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NavDestination
import com.example.ui.components.ProfileManagerDialog
import com.example.ui.components.SpotifyBottomNav
import com.example.ui.nowplaying.NowPlayingSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.SongPlayerTheme
import com.example.ui.theme.SpotifyBlack
import com.example.viewmodel.SongPlayerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {

    private val viewModel: SongPlayerViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SongPlayerTheme {
                // Request runtime media audio and notification permissions
                val permissionsToRequest = buildList {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.READ_MEDIA_AUDIO)
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }

                val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

                LaunchedEffect(Unit) {
                    if (!permissionsState.allPermissionsGranted) {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                }

                SongPlayerApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SongPlayerApp(viewModel: SongPlayerViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val dominantColor by viewModel.dominantColor.collectAsStateWithLifecycle()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
    val isQueueExpanded by viewModel.isQueueExpanded.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isFetchingLyrics by viewModel.isFetchingLyrics.collectAsStateWithLifecycle()
    val sleepTimerRemainingSeconds by viewModel.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()

    // Profile & Playlist states
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isProfileManagerOpen by viewModel.isProfileManagerOpen.collectAsStateWithLifecycle()
    val selectedPlaylistForDetail by viewModel.selectedPlaylistForDetail.collectAsStateWithLifecycle()
    val songToAddToPlaylist by viewModel.songToAddToPlaylist.collectAsStateWithLifecycle()

    // Native Storage Access Framework file picker launcher for manual audio import
    val audioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.importAudioFromUri(it) }
    }

    val progressFraction = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
    ) {
        // Main Screen Content with top status bar insets
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            when (currentDestination) {
                NavDestination.HOME -> {
                    HomeScreen(
                        songs = songs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        currentProfile = currentProfile,
                        onOpenProfileManager = { viewModel.openProfileManager() },
                        onSongClick = { song -> viewModel.playSong(song) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onAddToPlaylist = { song -> viewModel.showAddToPlaylistDialog(song) },
                        onImportAudio = { audioPickerLauncher.launch("audio/*") }
                    )
                }

                NavDestination.SEARCH -> {
                    SearchScreen(
                        songs = songs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = { song -> viewModel.playSong(song) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onAddToPlaylist = { song -> viewModel.showAddToPlaylistDialog(song) }
                    )
                }

                NavDestination.LIBRARY -> {
                    LibraryScreen(
                        songs = songs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        currentProfile = currentProfile,
                        playlists = playlists,
                        onSongClick = { song -> viewModel.playSong(song) },
                        onPlayAll = { list -> viewModel.playAll(list) },
                        onShuffleAll = { list -> viewModel.shuffleAll(list) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onRescanStorage = { viewModel.scanAudioFiles() },
                        onOpenProfileManager = { viewModel.openProfileManager() },
                        onOpenPlaylistDetail = { playlist -> viewModel.openPlaylistDetail(playlist) },
                        onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                        onAddToPlaylist = { song -> viewModel.showAddToPlaylistDialog(song) },
                        onImportAudio = { audioPickerLauncher.launch("audio/*") },
                        isScanning = isScanning
                    )
                }
            }
        }

        // Bottom Controls: Floating MiniPlayer + Spotify Glassmorphism Bottom Nav Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Floating MiniPlayer (appears when a song is loaded)
            if (currentSong != null && !isNowPlayingExpanded && selectedPlaylistForDetail == null) {
                MiniPlayer(
                    song = currentSong,
                    isPlaying = isPlaying,
                    progressFraction = progressFraction,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onExpandClick = { viewModel.openNowPlaying() },
                    onNextTrack = { viewModel.next() },
                    onPreviousTrack = { viewModel.previous() },
                    onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it) } }
                )
            }

            // Glassmorphism Bottom Navigation Bar
            SpotifyBottomNav(
                currentDestination = currentDestination,
                onNavigate = { destination -> viewModel.navigateTo(destination) }
            )
        }

        // Full Screen "Now Playing" Modal Sheet Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            NowPlayingSheet(
                song = currentSong,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                dominantColorHex = dominantColor,
                onCollapse = { viewModel.closeNowPlaying() },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onSeekTo = { pos -> viewModel.seekTo(pos) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it) } },
                onOpenQueue = { viewModel.openQueue() },
                onSaveLyrics = { lyricsText, isSynced ->
                    currentSong?.let { viewModel.saveLyrics(it.id, lyricsText, isSynced) }
                },
                onFetchOnlineLyrics = { viewModel.fetchOnlineLyricsForCurrentSong() },
                isFetchingLyrics = isFetchingLyrics,
                sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
                onSetSleepTimer = { mins -> viewModel.setSleepTimer(mins) },
                onSetSleepTimerEndOfTrack = { viewModel.setSleepTimerEndOfTrack() },
                onCancelSleepTimer = { viewModel.cancelSleepTimer() }
            )
        }

        // Queue Screen Modal Overlay
        AnimatedVisibility(
            visible = isQueueExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            QueueScreen(
                currentSong = currentSong,
                queue = queue,
                onClose = { viewModel.closeQueue() },
                onSongClick = { song -> viewModel.playSong(song) },
                onMoveUp = { idx -> viewModel.moveQueueItem(idx, idx - 1) },
                onMoveDown = { idx -> viewModel.moveQueueItem(idx, idx + 1) },
                onRemoveFromQueue = { idx -> viewModel.removeFromQueue(idx) }
            )
        }

        // Playlist Detail Screen Modal Overlay
        AnimatedVisibility(
            visible = selectedPlaylistForDetail != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            selectedPlaylistForDetail?.let { playlist ->
                PlaylistDetailScreen(
                    playlist = playlist,
                    allSongs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onBack = { viewModel.closePlaylistDetail() },
                    onSongClick = { song, playlistTracks -> viewModel.playSong(song, playlistTracks) },
                    onPlayAll = { playlistTracks -> viewModel.playAll(playlistTracks) },
                    onShuffleAll = { playlistTracks -> viewModel.shuffleAll(playlistTracks) },
                    onRemoveSong = { songId -> viewModel.removeSongFromPlaylist(playlist.id, songId) },
                    onReorderSong = { from, to -> viewModel.reorderPlaylistSong(playlist.id, from, to) },
                    onUpdateDetails = { name, desc -> viewModel.updatePlaylistDetails(playlist.id, name, desc) },
                    onDeletePlaylist = { viewModel.deletePlaylist(playlist.id) }
                )
            }
        }

        // Profile Manager Dialog
        if (isProfileManagerOpen) {
            ProfileManagerDialog(
                currentProfile = currentProfile,
                profiles = profiles,
                onDismiss = { viewModel.closeProfileManager() },
                onSwitchProfile = { profileId -> viewModel.switchProfile(profileId) },
                onCreateProfile = { name, color -> viewModel.createProfile(name, color) },
                onDeleteProfile = { profileId -> viewModel.deleteProfile(profileId) }
            )
        }

        // Add to Playlist Dialog
        if (songToAddToPlaylist != null) {
            AddToPlaylistDialog(
                song = songToAddToPlaylist,
                playlists = playlists,
                onDismiss = { viewModel.dismissAddToPlaylistDialog() },
                onAddToPlaylist = { playlistId, songId ->
                    viewModel.addSongToPlaylist(playlistId, songId)
                },
                onCreatePlaylistAndAdd = { name, songId ->
                    viewModel.createPlaylist(name)
                    // Retrieve newly created or add to it
                    viewModel.playlists.value.firstOrNull { it.name == name }?.let {
                        viewModel.addSongToPlaylist(it.id, songId)
                    }
                }
            )
        }
    }
}
