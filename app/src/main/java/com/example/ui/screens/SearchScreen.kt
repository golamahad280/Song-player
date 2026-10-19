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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.model.Song
import com.example.ui.components.SongListItem
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCardBackground
import com.example.ui.theme.SpotifyDivider
import com.example.ui.theme.SpotifyElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

data class BrowseCategory(
    val title: String,
    val color: Color
)

@Composable
fun SearchScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filterChips = listOf("All", "Songs", "Artists", "Albums")

    val browseCategories = remember {
        listOf(
            BrowseCategory("Pop", Color(0xFFE13300)),
            BrowseCategory("Hip-Hop", Color(0xFFBC5900)),
            BrowseCategory("Indie & Rock", Color(0xFFE91429)),
            BrowseCategory("Electronic", Color(0xFF0D73EC)),
            BrowseCategory("Chill & Lo-Fi", Color(0xFF431F77)),
            BrowseCategory("Focus", Color(0xFF503750)),
            BrowseCategory("Workout", Color(0xFF777777)),
            BrowseCategory("Acoustic", Color(0xFF1E3264))
        )
    }

    val filteredSongs by remember(searchQuery, selectedFilter, songs) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                emptyList()
            } else {
                val q = searchQuery.trim().lowercase()
                songs.filter { song ->
                    when (selectedFilter) {
                        "Songs" -> song.title.lowercase().contains(q)
                        "Artists" -> song.artist.lowercase().contains(q)
                        "Albums" -> song.album.lowercase().contains(q)
                        else -> song.title.lowercase().contains(q) ||
                            song.artist.lowercase().contains(q) ||
                            song.album.lowercase().contains(q)
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .testTag("search_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        // Title
        item {
            Text(
                text = "Search",
                color = SpotifyTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Search Text Input
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("What do you want to listen to?", color = SpotifyBlack, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = SpotifyBlack)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SpotifyBlack)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = SpotifyBlack,
                        unfocusedTextColor = SpotifyBlack
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("search_input_field")
                )
            }
        }

        // Filter chips (visible when searching)
        if (searchQuery.isNotBlank()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterChips) { filter ->
                        val isSelected = selectedFilter == filter
                        AssistChip(
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    filter,
                                    color = if (isSelected) SpotifyBlack else SpotifyTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) SpotifyGreen else SpotifyElevated
                            ),
                            border = null
                        )
                    }
                }
            }
        }

        // Results or Browse All
        if (searchQuery.isNotBlank()) {
            if (filteredSongs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No results found for \"$searchQuery\"",
                            color = SpotifyTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please check your spelling or search for another track",
                            color = SpotifyTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredSongs) { song ->
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
        } else {
            // Browse All Genres Grid
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Browse all",
                    color = SpotifyTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (row in browseCategories.chunked(2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (cat in row) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(96.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cat.color)
                                        .clickable { searchQuery = cat.title }
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = cat.title,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
