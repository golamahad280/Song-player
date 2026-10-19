package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.UserProfile
import com.example.ui.components.SongListItem
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCardBackground
import com.example.ui.theme.SpotifyCardHover
import com.example.ui.theme.SpotifyDivider
import com.example.ui.theme.SpotifyElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

enum class SortOption(val displayName: String) {
    NEWEST("Newest to Oldest"),
    OLDEST("Oldest to Newest"),
    ALPHABETICAL("Alphabetical (A-Z)"),
    ARTIST("Artist"),
    ALBUM("Album"),
    PLAY_COUNT("Most Played")
}

@Composable
fun LibraryScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    currentProfile: UserProfile?,
    playlists: List<Playlist>,
    onSongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onRescanStorage: () -> Unit,
    onOpenProfileManager: () -> Unit,
    onOpenPlaylistDetail: (Playlist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    isScanning: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedSort by remember { mutableStateOf(SortOption.NEWEST) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Playlists") }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val filteredAndSortedSongs by remember(songs, selectedSort, selectedTab) {
        derivedStateOf {
            val filtered = when (selectedTab) {
                "Favorites" -> songs.filter { it.isFavorite }
                else -> songs
            }

            when (selectedSort) {
                SortOption.NEWEST -> filtered.sortedByDescending { it.dateAdded }
                SortOption.OLDEST -> filtered.sortedBy { it.dateAdded }
                SortOption.ALPHABETICAL -> filtered.sortedBy { it.title.lowercase() }
                SortOption.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
                SortOption.ALBUM -> filtered.sortedBy { it.album.lowercase() }
                SortOption.PLAY_COUNT -> filtered.sortedByDescending { it.playCount }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .testTag("library_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        // Library Top Header with Profile Avatar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onOpenProfileManager() }
                        .padding(vertical = 4.dp)
                        .testTag("library_profile_header_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(currentProfile?.avatarColor ?: 0xFF1ED760)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentProfile?.initials ?: "U",
                            color = SpotifyBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Your Library",
                            color = SpotifyTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentProfile?.name ?: "Main Listener"} • Switch profile",
                            color = SpotifyGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Rescan device storage button
                IconButton(
                    onClick = onRescanStorage,
                    modifier = Modifier.testTag("rescan_storage_button")
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Rescan Local Audio",
                        tint = SpotifyTextPrimary
                    )
                }
            }
        }

        // Filter chips (Playlists, All Tracks, Favorites)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Playlists", "All Tracks", "Favorites").forEach { tab ->
                    val isSelected = selectedTab == tab
                    AssistChip(
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                tab,
                                color = if (isSelected) SpotifyBlack else SpotifyTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) SpotifyGreen else SpotifyElevated
                        ),
                        border = null,
                        modifier = Modifier.testTag("library_tab_${tab.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }

        if (selectedTab == "Playlists") {
            // New Playlist Action Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showCreatePlaylistDialog = true }
                        .padding(vertical = 8.dp)
                        .testTag("create_playlist_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SpotifyElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Playlist",
                            tint = SpotifyGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Create Playlist",
                            color = SpotifyTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Save custom mixes for ${currentProfile?.name ?: "this profile"}",
                            color = SpotifyTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Playlists List
            if (playlists.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No playlists in this profile yet",
                            color = SpotifyTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create a playlist and add your favorite tracks",
                            color = SpotifyTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(playlists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPlaylistDetail(playlist) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("playlist_row_${playlist.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SpotifyElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = SpotifyTextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                color = SpotifyTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Playlist • ${playlist.songCount} songs",
                                color = SpotifyTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Quick Sort Row & Play/Shuffle Action Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sort Dropdown Button
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isSortMenuExpanded = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("sort_dropdown_trigger"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = SpotifyTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedSort.displayName,
                                color = SpotifyTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = SpotifyTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isSortMenuExpanded,
                            onDismissRequest = { isSortMenuExpanded = false },
                            modifier = Modifier.background(SpotifyCardBackground)
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option.displayName,
                                            color = if (selectedSort == option) SpotifyGreen else SpotifyTextPrimary,
                                            fontWeight = if (selectedSort == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedSort = option
                                        isSortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Play / Shuffle All Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onPlayAll(filteredAndSortedSongs) },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("play_all_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SpotifyBlack, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", color = SpotifyBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { onShuffleAll(filteredAndSortedSongs) },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("shuffle_all_button")
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = SpotifyTextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Summary item
            item {
                Text(
                    text = "${filteredAndSortedSongs.size} tracks found",
                    color = SpotifyTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Track List
            if (filteredAndSortedSongs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No songs found in this view",
                            color = SpotifyTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan storage or adjust filters to view songs",
                            color = SpotifyTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredAndSortedSongs) { song ->
                    SongListItem(
                        song = song,
                        isPlaying = isPlaying,
                        isSelected = song.id == currentSong?.id,
                        onClick = { onSongClick(song) },
                        onToggleFavorite = { onToggleFavorite(song) },
                        onMoreClick = { onAddToPlaylist(song) }
                    )
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        var playlistNameInput by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showCreatePlaylistDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(16.dp)),
                color = SpotifyCardBackground
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Give your playlist a name",
                        color = SpotifyTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        placeholder = { Text("My Playlist #1", color = SpotifyTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SpotifyElevated,
                            unfocusedContainerColor = SpotifyElevated,
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = SpotifyDivider,
                            focusedTextColor = SpotifyTextPrimary,
                            unfocusedTextColor = SpotifyTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("new_playlist_input")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showCreatePlaylistDialog = false }) {
                            Text("Cancel", color = SpotifyTextSecondary)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                if (playlistNameInput.isNotBlank()) {
                                    onCreatePlaylist(playlistNameInput)
                                    showCreatePlaylistDialog = false
                                }
                            },
                            enabled = playlistNameInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            modifier = Modifier.testTag("confirm_create_playlist_button")
                        ) {
                            Text("Create", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
