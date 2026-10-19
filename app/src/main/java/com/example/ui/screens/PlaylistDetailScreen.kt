package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCardBackground
import com.example.ui.theme.SpotifyDivider
import com.example.ui.theme.SpotifyElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    allSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onReorderSong: (Int, Int) -> Unit,
    onUpdateDetails: (String, String) -> Unit,
    onDeletePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var isEditDialogOpen by remember { mutableStateOf(false) }

    val playlistSongs by remember(playlist.songIds, allSongs) {
        derivedStateOf {
            playlist.songIds.mapNotNull { id ->
                allSongs.find { it.id == id }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("playlist_detail_screen")
    ) {
        // Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SpotifyTextPrimary
                )
            }

            Row {
                IconButton(onClick = { isEditDialogOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Playlist",
                        tint = SpotifyTextSecondary
                    )
                }

                IconButton(onClick = onDeletePlaylist) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Playlist",
                        tint = SpotifyTextMuted
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Playlist Hero Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        val firstCover = playlistSongs.firstOrNull { !it.customArtworkUrl.isNullOrBlank() }?.customArtworkUrl
                        if (firstCover != null) {
                            AsyncImage(
                                model = firstCover,
                                contentDescription = playlist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(90.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = SpotifyTextSecondary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            color = SpotifyTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (playlist.description.isNotBlank()) {
                            Text(
                                text = playlist.description,
                                color = SpotifyTextSecondary,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Text(
                            text = "${playlistSongs.size} tracks • Custom Order",
                            color = SpotifyTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Play / Shuffle Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onPlayAll(playlistSongs) },
                        enabled = playlistSongs.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).testTag("play_playlist_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SpotifyBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onShuffleAll(playlistSongs) },
                        enabled = playlistSongs.isNotEmpty(),
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).testTag("shuffle_playlist_button")
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, tint = SpotifyTextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", color = SpotifyTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Song list with reorder and remove actions
            if (playlistSongs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "This playlist is empty",
                            color = SpotifyTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the '...' menu on any song in Home, Search, or Library to add it here.",
                            color = SpotifyTextMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(playlistSongs) { index, song ->
                    val isCurrent = song.id == currentSong?.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) SpotifyCardBackground else Color.Transparent)
                            .clickable { onSongClick(song, playlistSongs) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("playlist_track_$index"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Order index
                        Text(
                            text = "${index + 1}",
                            color = SpotifyTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp)
                        )

                        // Artwork
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SpotifyElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!song.customArtworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = song.customArtworkUrl,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(42.dp)
                                )
                            } else {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifyTextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = if (isCurrent) SpotifyGreen else SpotifyTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = SpotifyTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Reorder track up
                        IconButton(
                            onClick = { if (index > 0) onReorderSong(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Move Up",
                                tint = if (index > 0) SpotifyTextSecondary else SpotifyTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Reorder track down
                        IconButton(
                            onClick = { if (index < playlistSongs.size - 1) onReorderSong(index, index + 1) },
                            enabled = index < playlistSongs.size - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Move Down",
                                tint = if (index < playlistSongs.size - 1) SpotifyTextSecondary else SpotifyTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Remove track from playlist
                        IconButton(
                            onClick = { onRemoveSong(song.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.RemoveCircleOutline,
                                contentDescription = "Remove from playlist",
                                tint = SpotifyTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }

    // Edit Playlist Dialog
    if (isEditDialogOpen) {
        var editName by remember { mutableStateOf(playlist.name) }
        var editDescription by remember { mutableStateOf(playlist.description) }

        Dialog(onDismissRequest = { isEditDialogOpen = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(16.dp)),
                color = SpotifyCardBackground
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Edit Playlist",
                        color = SpotifyTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Playlist Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SpotifyTextPrimary,
                            unfocusedTextColor = SpotifyTextPrimary,
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = SpotifyDivider
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SpotifyTextPrimary,
                            unfocusedTextColor = SpotifyTextPrimary,
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = SpotifyDivider
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { isEditDialogOpen = false }) {
                            Text("Cancel", color = SpotifyTextSecondary)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                onUpdateDetails(editName, editDescription)
                                isEditDialogOpen = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                        ) {
                            Text("Save", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
