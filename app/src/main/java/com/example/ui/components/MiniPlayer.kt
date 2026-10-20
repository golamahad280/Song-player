package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import coil.compose.AsyncImage
import com.example.model.Song
import com.example.ui.theme.SpotifyCardHover
import com.example.ui.theme.SpotifyElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    progressFraction: Float,
    onPlayPauseClick: () -> Unit,
    onExpandClick: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    val draggableState = rememberDraggableState { delta ->
        horizontalDragOffset += delta
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(song.dominantColor ?: 0xFF242424))
            .clickable { onExpandClick() }
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (horizontalDragOffset < -80f) {
                        onNextTrack()
                    } else if (horizontalDragOffset > 80f) {
                        onPreviousTrack()
                    }
                    horizontalDragOffset = 0f
                }
            )
            .testTag("floating_mini_player")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                SpotifyCoverArt(
                    artworkUrl = song.customArtworkUrl,
                    title = song.title,
                    artist = song.artist,
                    dominantColorHex = song.dominantColor,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(42.dp),
                    initialsSize = 12.sp
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Title and Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = SpotifyTextPrimary,
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

                // Heart
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(40.dp).testTag("mini_player_fav_button")
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) SpotifyGreen else SpotifyTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Play / Pause
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(40.dp).testTag("mini_player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = SpotifyTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Spotify Green Progress Bar Line at bottom of mini-player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color(0x33FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction.coerceIn(0f, 1f))
                        .height(2.5.dp)
                        .background(SpotifyGreen)
                )
            }
        }
    }
}
