package com.example.ui.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.LyricLine
import com.example.model.LyricsParser
import com.example.model.Song
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCardBackground
import com.example.ui.theme.SpotifyCardHover
import com.example.ui.theme.SpotifyDivider
import com.example.ui.theme.SpotifyElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

@Composable
fun SyncedLyricsView(
    song: Song?,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    onSaveLyrics: (String, Boolean) -> Unit,
    onFetchOnline: () -> Unit,
    isFetchingOnline: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }

    // Auto-fetch lyrics as soon as the view is opened if lyrics are not already loaded or cached
    LaunchedEffect(song?.id) {
        if (song != null && song.lyrics.isNullOrBlank() && !isFetchingOnline) {
            onFetchOnline()
        }
    }

    val parsedLines by remember(song?.lyrics) {
        derivedStateOf {
            LyricsParser.parseLrc(song?.lyrics, song?.durationMs ?: 0L)
        }
    }

    val activeIndex by remember(currentPositionMs, parsedLines) {
        derivedStateOf {
            if (parsedLines.isEmpty()) -1
            else {
                val idx = parsedLines.indexOfLast { it.timeMs <= currentPositionMs }
                if (idx == -1) 0 else idx
            }
        }
    }

    val listState = rememberLazyListState()

    // Smoothly scroll active lyric into center of view
    LaunchedEffect(activeIndex) {
        if (activeIndex in parsedLines.indices) {
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (parsedLines.isEmpty()) {
            // Empty / Loading Lyrics State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isFetchingOnline) {
                    CircularProgressIndicator(
                        color = SpotifyGreen,
                        modifier = Modifier.size(52.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Finding Synced Lyrics...",
                        color = SpotifyTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Querying LRCLIB, music databases & AI sync engine",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "No lyrics icon",
                        tint = SpotifyTextMuted,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Syncing Lyrics for this track",
                        color = SpotifyTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Once retrieved, lyrics are automatically cached for instant offline playback.",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onFetchOnline,
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.testTag("fetch_lyrics_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SpotifyBlack)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fetch & Sync", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyTextPrimary),
                            modifier = Modifier.testTag("edit_lyrics_empty_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = SpotifyTextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Lyrics")
                        }
                    }
                }
            }
        } else {
            // Live Synced Lyrics Stream
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 80.dp, bottom = 140.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize().testTag("synced_lyrics_list")
            ) {
                itemsIndexed(parsedLines) { index, line ->
                    val isActive = index == activeIndex

                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else Color(0x66FFFFFF),
                        label = "lyricTextColor"
                    )

                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 1.08f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "lyricScale"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onSeekTo(line.timeMs)
                            }
                            .padding(vertical = 4.dp)
                            .testTag("lyric_line_$index")
                    ) {
                        Text(
                            text = line.text,
                            color = textColor,
                            fontSize = if (isActive) 22.sp else 18.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            lineHeight = if (isActive) 30.sp else 24.sp
                        )

                        if (isActive && line.timeMs > 0) {
                            Text(
                                text = LyricsParser.formatTime(line.timeMs),
                                color = SpotifyGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Top action bar (Edit / Search Online)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onFetchOnline,
                    modifier = Modifier
                        .background(Color(0x66000000), CircleShape)
                        .testTag("refresh_online_lyrics_button")
                ) {
                    if (isFetchingOnline) {
                        CircularProgressIndicator(
                            color = SpotifyGreen,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Search Online Lyrics",
                            tint = SpotifyTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier
                        .background(Color(0x66000000), CircleShape)
                        .testTag("edit_lyrics_button")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Lyrics",
                        tint = SpotifyTextPrimary
                    )
                }
            }
        }
    }

    // Manual Lyrics Editor Modal
    if (showEditDialog) {
        LyricsEditorDialog(
            initialLyrics = song?.lyrics ?: "",
            onDismiss = { showEditDialog = false },
            onSave = { updatedText ->
                val hasTimestamps = updatedText.contains(Regex("\\[\\d{1,2}:\\d{2}"))
                onSaveLyrics(updatedText, hasTimestamps)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun LyricsEditorDialog(
    initialLyrics: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialLyrics) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = SpotifyCardBackground,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Lyrics & Sync",
                        color = SpotifyTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotifyTextSecondary)
                    }
                }

                Text(
                    text = "Paste or edit timestamped LRC (e.g. [01:23.45] Lyric line) or plain text lyrics. Saved directly to offline database.",
                    color = SpotifyTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Text Editor
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("lyrics_editor_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SpotifyElevated,
                        unfocusedContainerColor = SpotifyElevated,
                        focusedBorderColor = SpotifyGreen,
                        unfocusedBorderColor = SpotifyDivider,
                        focusedTextColor = SpotifyTextPrimary,
                        unfocusedTextColor = SpotifyTextPrimary
                    ),
                    placeholder = {
                        Text(
                            "[00:15.00] Enter or paste lyrics here...\n[00:20.50] Each line with timestamp or plain text.",
                            color = SpotifyTextMuted
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyTextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { onSave(text) },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                        modifier = Modifier.testTag("save_lyrics_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = SpotifyBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Lyrics", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
