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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.ui.components.SpotifyCoverArt
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCardBackground
import com.example.ui.theme.SpotifyElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

@Composable
fun QueueScreen(
    currentSong: Song?,
    queue: List<Song>,
    onClose: () -> Unit,
    onSongClick: (Song) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("queue_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close Queue", tint = SpotifyTextPrimary)
            }

            Text(
                text = "Play Queue",
                color = SpotifyTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.size(48.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Now Playing Section
            if (currentSong != null) {
                item {
                    Text(
                        text = "Now Playing",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyCardBackground)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpotifyCoverArt(
                            artworkUrl = currentSong.customArtworkUrl,
                            title = currentSong.title,
                            artist = currentSong.artist,
                            dominantColorHex = currentSong.dominantColor,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(48.dp),
                            initialsSize = 13.sp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSong.title,
                                color = SpotifyGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentSong.artist,
                                color = SpotifyTextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Next In Queue Header
            item {
                Text(
                    text = "Next In Queue (${queue.size} tracks)",
                    color = SpotifyTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }

            if (queue.isEmpty()) {
                item {
                    Text(
                        text = "Your queue is empty. Tap any song to play and build your queue.",
                        color = SpotifyTextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                itemsIndexed(queue) { index, song ->
                    val isCurrent = song.id == currentSong?.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) SpotifyCardBackground else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { onSongClick(song) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("queue_item_$index"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = SpotifyTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp)
                        )

                        SpotifyCoverArt(
                            artworkUrl = song.customArtworkUrl,
                            title = song.title,
                            artist = song.artist,
                            dominantColorHex = song.dominantColor,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.size(40.dp),
                            initialsSize = 11.sp
                        )

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

                        // Reorder Up
                        IconButton(
                            onClick = { if (index > 0) onMoveUp(index) },
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

                        // Reorder Down
                        IconButton(
                            onClick = { if (index < queue.size - 1) onMoveDown(index) },
                            enabled = index < queue.size - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Move Down",
                                tint = if (index < queue.size - 1) SpotifyTextSecondary else SpotifyTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Remove
                        IconButton(
                            onClick = { onRemoveFromQueue(index) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Remove",
                                tint = SpotifyTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
