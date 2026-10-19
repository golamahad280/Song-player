package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PlaylistEntity
import com.example.data.local.SongDatabase
import com.example.data.local.SongEntity
import com.example.data.local.UserProfileEntity
import com.example.data.remote.LyricsAndArtFetcher
import com.example.data.scanner.AudioScanner
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.UserProfile
import com.example.player.PlayerManager
import com.example.player.RepeatMode
import com.example.ui.components.NavDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.service.StorageScanWorker
import java.util.concurrent.TimeUnit

class SongPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val playerManager = PlayerManager.getInstance(application)
    private val database = SongDatabase.getInstance(application)
    private val songDao = database.songDao()
    private val userProfileDao = database.userProfileDao()
    private val playlistDao = database.playlistDao()
    private val fetcher = LyricsAndArtFetcher(application, songDao)

    // Player state exposed from PlayerManager
    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs
    val queue: StateFlow<List<Song>> = playerManager.queue
    val isShuffle: StateFlow<Boolean> = playerManager.isShuffle
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val dominantColor: StateFlow<Long> = playerManager.dominantColor
    val sleepTimerRemainingSeconds: StateFlow<Int?> = playerManager.sleepTimerRemainingSeconds

    // App Navigation & Sheet state
    private val _currentDestination = MutableStateFlow(NavDestination.HOME)
    val currentDestination: StateFlow<NavDestination> = _currentDestination.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _isQueueExpanded = MutableStateFlow(false)
    val isQueueExpanded: StateFlow<Boolean> = _isQueueExpanded.asStateFlow()

    private val _isProfileManagerOpen = MutableStateFlow(false)
    val isProfileManagerOpen: StateFlow<Boolean> = _isProfileManagerOpen.asStateFlow()

    private val _selectedPlaylistForDetail = MutableStateFlow<Playlist?>(null)
    val selectedPlaylistForDetail: StateFlow<Playlist?> = _selectedPlaylistForDetail.asStateFlow()

    private val _songToAddToPlaylist = MutableStateFlow<Song?>(null)
    val songToAddToPlaylist: StateFlow<Song?> = _songToAddToPlaylist.asStateFlow()

    // Songs library
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isFetchingLyrics = MutableStateFlow(false)
    val isFetchingLyrics: StateFlow<Boolean> = _isFetchingLyrics.asStateFlow()

    private val _importProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val importProgress: StateFlow<Pair<Int, Int>?> = _importProgress.asStateFlow()

    // Profiles state
    val profiles: StateFlow<List<UserProfile>> = userProfileDao.getAllProfiles()
        .map { entities ->
            entities.map {
                UserProfile(
                    id = it.id,
                    name = it.name,
                    avatarColor = it.avatarColor,
                    isCurrent = it.isCurrent,
                    createdAt = it.createdAt
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentProfile: StateFlow<UserProfile?> = userProfileDao.getCurrentProfileFlow()
        .map { entity ->
            entity?.let {
                UserProfile(
                    id = it.id,
                    name = it.name,
                    avatarColor = it.avatarColor,
                    isCurrent = it.isCurrent,
                    createdAt = it.createdAt
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Playlists state for active profile
    @OptIn(ExperimentalCoroutinesApi::class)
    val playlists: StateFlow<List<Playlist>> = currentProfile.flatMapLatest { profile ->
        if (profile == null) flowOf(emptyList())
        else playlistDao.getPlaylistsForProfile(profile.id).map { entities ->
            entities.map {
                Playlist(
                    id = it.id,
                    profileId = it.profileId,
                    name = it.name,
                    description = it.description,
                    songIds = it.getSongIdList(),
                    createdAt = it.createdAt
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Schedule periodic automatic storage scan every 6 hours
        try {
            val periodicWork = PeriodicWorkRequestBuilder<StorageScanWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(application).enqueueUniquePeriodicWork(
                "StorageScanPeriodicWork",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
        } catch (_: Exception) {}

        // Observe Room DB songs
        viewModelScope.launch {
            songDao.getAllSongs().collectLatest { entities ->
                if (entities.isNotEmpty()) {
                    _songs.value = entities.map { entity ->
                        Song(
                            id = entity.id,
                            title = entity.title,
                            artist = entity.artist,
                            album = entity.album,
                            durationMs = entity.durationMs,
                            contentUri = entity.contentUri,
                            filePath = entity.filePath,
                            albumId = entity.albumId,
                            dateAdded = entity.dateAdded,
                            year = entity.year,
                            playCount = entity.playCount,
                            isFavorite = entity.isFavorite,
                            lyrics = entity.lyrics,
                            isSyncedLyrics = entity.isSyncedLyrics,
                            customArtworkUrl = entity.customArtworkUrl,
                            dominantColor = entity.dominantColor
                        )
                    }
                } else {
                    scanAudioFiles()
                }
            }
        }

        // Initialize default user profile if none exist
        viewModelScope.launch(Dispatchers.IO) {
            val count = userProfileDao.getProfileCount()
            if (count == 0) {
                val defaultProfile = UserProfileEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Primary Listener",
                    avatarColor = 0xFF1ED760,
                    isCurrent = true
                )
                userProfileDao.insertOrUpdateProfile(defaultProfile)

                // Create a sample favorite playlist for the default profile
                playlistDao.insertOrUpdatePlaylist(
                    PlaylistEntity(
                        profileId = defaultProfile.id,
                        name = "Night Drive",
                        description = "Atmospheric late night beats & synthwave",
                        songIds = "9001,9002,9004"
                    )
                )
            }
        }
    }

    fun scanAudioFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val scanned = AudioScanner.scanLocalAudio(getApplication())
                val entities = scanned.map { song ->
                    val existing = songDao.getSongById(song.id)
                    SongEntity(
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        album = song.album,
                        durationMs = song.durationMs,
                        contentUri = song.contentUri,
                        filePath = song.filePath,
                        albumId = song.albumId,
                        dateAdded = song.dateAdded,
                        year = song.year,
                        playCount = existing?.playCount ?: song.playCount,
                        isFavorite = existing?.isFavorite ?: song.isFavorite,
                        lyrics = existing?.lyrics ?: song.lyrics,
                        isSyncedLyrics = existing?.isSyncedLyrics ?: song.isSyncedLyrics,
                        customArtworkUrl = existing?.customArtworkUrl ?: song.customArtworkUrl,
                        dominantColor = existing?.dominantColor ?: song.dominantColor
                    )
                }
                songDao.insertOrUpdateSongs(entities)
            } finally {
                _isScanning.value = false
            }
        }
    }

    // Profile Management
    fun createProfile(name: String, colorHex: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = name.trim().ifBlank { "User" }
            val newProfile = UserProfileEntity(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                avatarColor = colorHex,
                isCurrent = false
            )
            userProfileDao.insertOrUpdateProfile(newProfile)
            userProfileDao.switchProfile(newProfile.id)
        }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userProfileDao.switchProfile(profileId)
            _selectedPlaylistForDetail.value = null
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = userProfileDao.getCurrentProfile()
            playlistDao.deleteAllPlaylistsForProfile(profileId)
            userProfileDao.deleteProfile(profileId)

            // If we deleted the active profile, switch to the first remaining profile
            if (all?.id == profileId) {
                val remaining = userProfileDao.getAllProfiles()
                viewModelScope.launch {
                    remaining.collectLatest { list ->
                        if (list.isNotEmpty()) {
                            userProfileDao.switchProfile(list.first().id)
                        } else {
                            val fallback = UserProfileEntity(
                                id = UUID.randomUUID().toString(),
                                name = "Main Listener",
                                avatarColor = 0xFF1ED760,
                                isCurrent = true
                            )
                            userProfileDao.insertOrUpdateProfile(fallback)
                        }
                    }
                }
            }
        }
    }

    fun openProfileManager() {
        _isProfileManagerOpen.value = true
    }

    fun closeProfileManager() {
        _isProfileManagerOpen.value = false
    }

    // Playlist Management
    fun createPlaylist(name: String, description: String = "") {
        val profile = currentProfile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = PlaylistEntity(
                profileId = profile.id,
                name = name.trim().ifBlank { "My Playlist" },
                description = description.trim(),
                songIds = ""
            )
            playlistDao.insertOrUpdatePlaylist(playlist)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.deletePlaylist(playlistId)
            if (_selectedPlaylistForDetail.value?.id == playlistId) {
                _selectedPlaylistForDetail.value = null
            }
        }
    }

    fun updatePlaylistDetails(playlistId: Long, newName: String, newDescription: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.updatePlaylistDetails(playlistId, newName.trim(), newDescription.trim())
            val existing = _selectedPlaylistForDetail.value
            if (existing?.id == playlistId) {
                _selectedPlaylistForDetail.value = existing.copy(name = newName.trim(), description = newDescription.trim())
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = playlistDao.getPlaylistById(playlistId) ?: return@launch
            val ids = playlist.getSongIdList().toMutableList()
            if (!ids.contains(songId)) {
                ids.add(songId)
                val joined = ids.joinToString(",")
                playlistDao.updatePlaylistSongs(playlistId, joined)

                if (_selectedPlaylistForDetail.value?.id == playlistId) {
                    _selectedPlaylistForDetail.value = _selectedPlaylistForDetail.value?.copy(songIds = ids)
                }
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = playlistDao.getPlaylistById(playlistId) ?: return@launch
            val ids = playlist.getSongIdList().toMutableList()
            ids.remove(songId)
            val joined = ids.joinToString(",")
            playlistDao.updatePlaylistSongs(playlistId, joined)

            if (_selectedPlaylistForDetail.value?.id == playlistId) {
                _selectedPlaylistForDetail.value = _selectedPlaylistForDetail.value?.copy(songIds = ids)
            }
        }
    }

    fun reorderPlaylistSong(playlistId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = playlistDao.getPlaylistById(playlistId) ?: return@launch
            val ids = playlist.getSongIdList().toMutableList()
            if (fromIndex in ids.indices && toIndex in ids.indices) {
                val moved = ids.removeAt(fromIndex)
                ids.add(toIndex, moved)
                val joined = ids.joinToString(",")
                playlistDao.updatePlaylistSongs(playlistId, joined)

                if (_selectedPlaylistForDetail.value?.id == playlistId) {
                    _selectedPlaylistForDetail.value = _selectedPlaylistForDetail.value?.copy(songIds = ids)
                }
            }
        }
    }

    fun openPlaylistDetail(playlist: Playlist) {
        _selectedPlaylistForDetail.value = playlist
    }

    fun closePlaylistDetail() {
        _selectedPlaylistForDetail.value = null
    }

    fun showAddToPlaylistDialog(song: Song) {
        _songToAddToPlaylist.value = song
    }

    fun dismissAddToPlaylistDialog() {
        _songToAddToPlaylist.value = null
    }

    fun playPlaylist(playlist: Playlist) {
        val playlistSongs = playlist.songIds.mapNotNull { id ->
            _songs.value.find { it.id == id }
        }
        if (playlistSongs.isNotEmpty()) {
            playerManager.playSong(playlistSongs.first(), playlistSongs)
        }
    }

    fun shufflePlaylist(playlist: Playlist) {
        val playlistSongs = playlist.songIds.mapNotNull { id ->
            _songs.value.find { it.id == id }
        }.shuffled()
        if (playlistSongs.isNotEmpty()) {
            playerManager.playSong(playlistSongs.first(), playlistSongs)
        }
    }

    // Navigation and player controls
    fun navigateTo(destination: NavDestination) {
        _currentDestination.value = destination
    }

    fun openNowPlaying() {
        _isNowPlayingExpanded.value = true
    }

    fun closeNowPlaying() {
        _isNowPlayingExpanded.value = false
    }

    fun openQueue() {
        _isQueueExpanded.value = true
    }

    fun closeQueue() {
        _isQueueExpanded.value = false
    }

    fun playSong(song: Song, newQueue: List<Song>? = null) {
        playerManager.playSong(song, newQueue ?: _songs.value)
    }

    fun playAll(list: List<Song>) {
        if (list.isNotEmpty()) {
            playerManager.playSong(list.first(), list)
        }
    }

    fun shuffleAll(list: List<Song>) {
        if (list.isNotEmpty()) {
            val shuffled = list.shuffled()
            playerManager.playSong(shuffled.first(), shuffled)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun next() {
        playerManager.next()
    }

    fun previous() {
        playerManager.previous()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playerManager.cycleRepeatMode()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            songDao.toggleFavorite(song.id)
            val updated = song.copy(isFavorite = !song.isFavorite)
            if (currentSong.value?.id == song.id) {
                playerManager.playSong(updated, playerManager.queue.value)
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playerManager.reorderQueue(fromIndex, toIndex)
    }

    fun removeFromQueue(index: Int) {
        playerManager.removeFromQueue(index)
    }

    fun saveLyrics(songId: Long, customLyrics: String, isSynced: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            fetcher.saveManualLyrics(songId, customLyrics, isSynced)
            val current = currentSong.value
            if (current?.id == songId) {
                playerManager.playSong(current.copy(lyrics = customLyrics, isSyncedLyrics = isSynced), playerManager.queue.value)
            }
        }
    }

    fun fetchOnlineLyricsForCurrentSong() {
        val current = currentSong.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingLyrics.value = true
            try {
                val (lyrics, isSynced) = fetcher.fetchLyricsForSong(current)
                if (!lyrics.isNullOrBlank()) {
                    playerManager.playSong(current.copy(lyrics = lyrics, isSyncedLyrics = isSynced), playerManager.queue.value)
                }
            } finally {
                _isFetchingLyrics.value = false
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
    }

    fun setSleepTimerEndOfTrack() {
        playerManager.setSleepTimerEndOfTrack()
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
    }

    fun importAudioFromUri(uri: android.net.Uri) {
        importAudioFromUris(listOf(uri))
    }

    fun importAudioFromUris(uris: List<android.net.Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val total = uris.size
            _importProgress.value = Pair(0, total)
            val newSongs = mutableListOf<Song>()
            val entities = mutableListOf<SongEntity>()

            for ((index, uri) in uris.withIndex()) {
                try {
                    try {
                        val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        getApplication<Application>().contentResolver.takePersistableUriPermission(uri, takeFlags)
                    } catch (_: Exception) {}

                    val newSong = AudioScanner.parseUriMetadata(getApplication(), uri)
                    val existing = songDao.getSongById(newSong.id)
                    val entity = SongEntity(
                        id = newSong.id,
                        title = newSong.title,
                        artist = newSong.artist,
                        album = newSong.album,
                        durationMs = newSong.durationMs,
                        contentUri = newSong.contentUri,
                        filePath = newSong.filePath,
                        albumId = newSong.albumId,
                        dateAdded = newSong.dateAdded,
                        year = newSong.year,
                        playCount = existing?.playCount ?: 0,
                        isFavorite = existing?.isFavorite ?: false,
                        lyrics = existing?.lyrics,
                        isSyncedLyrics = existing?.isSyncedLyrics ?: false,
                        customArtworkUrl = existing?.customArtworkUrl
                    )
                    entities.add(entity)
                    newSongs.add(newSong)
                } catch (e: Exception) {
                    android.util.Log.e("SongPlayerViewModel", "Error importing uri $uri: ${e.message}")
                }
                _importProgress.value = Pair(index + 1, total)
            }

            if (entities.isNotEmpty()) {
                songDao.insertOrUpdateSongs(entities)
                val updatedList = newSongs + _songs.value.filter { existingSong -> !newSongs.any { it.id == existingSong.id } }
                _songs.value = updatedList

                if (newSongs.isNotEmpty()) {
                    playerManager.addToQueue(newSongs.first())
                }
            }

            kotlinx.coroutines.delay(1000)
            _importProgress.value = null
        }
    }
}
