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
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
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
import com.example.ui.theme.SpotifyDivider
import com.example.ui.theme.SpotifyElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

/**
 * Production-grade Spotify-style Synced Lyrics Component:
 * - Millisecond live audio stream synchronization.
 * - Active line: Dynamic scaling, bright Spotify White (#FFFFFF) text, active glow effect.
 * - Inactive lines: Dimmed to 35% opacity gray (#59FFFFFF).
 * - Smooth vertical physics centering: Smooth animate-scrolls active line to vertical center.
 * - Tap-to-Seek: Tapping any lyric line instantly seeks the audio player to that exact timestamp.
 * - Integrated Manual Tap-to-Sync Editor & Text Editor.
 */
@Composable
fun SyncedLyricsView(
    song: Song?,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    onSaveLyrics: (String, Boolean) -> Unit,
    onFetchOnline: () -> Unit,
    isFetchingOnline: Boolean = false,
    isPlaying: Boolean = false,
    onPlayPause: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showTextEditDialog by remember { mutableStateOf(false) }
    var showTapSyncEditor by remember { mutableStateOf(false) }

    // Auto-fetch lyrics as soon as the view is opened if missing
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
                if (idx == -1) -1 else idx
            }
        }
    }

    val listState = rememberLazyListState()

    // Smooth physics vertical centering of the active line
    LaunchedEffect(activeIndex) {
        if (activeIndex in parsedLines.indices) {
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (parsedLines.isEmpty()) {
            // Empty / Fetching State
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
                        text = "Querying LRCLIB API, regional databases & AI timestamp generator",
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
                        text = "No Synced Lyrics Found",
                        color = SpotifyTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Fetch online via LRCLIB & Gemini, tap-to-sync manually, or paste lyrics.",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onFetchOnline,
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.testTag("fetch_lyrics_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SpotifyBlack)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fetch Online / AI", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showTapSyncEditor = true },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyGreen),
                                modifier = Modifier.testTag("tap_to_sync_empty_button")
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tap-to-Sync", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showTextEditDialog = true },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyTextPrimary),
                                modifier = Modifier.testTag("edit_lyrics_empty_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = SpotifyTextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Text")
                            }
                        }
                    }
                }
            }
        } else {
            // Live Real-Time Karaoke Synced Stream
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 70.dp, bottom = 140.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("synced_lyrics_list")
            ) {
                itemsIndexed(parsedLines) { index, line ->
                    val isActive = index == activeIndex

                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else Color(0x59FFFFFF),
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
                                transformOrigin = TransformOrigin(0f, 0.5f)
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
                            fontSize = if (isActive) 23.sp else 18.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            lineHeight = if (isActive) 32.sp else 26.sp,
                            style = if (isActive) {
                                LocalTextStyle.current.copy(
                                    shadow = Shadow(
                                        color = Color(0xB3FFFFFF),
                                        blurRadius = 14f
                                    )
                                )
                            } else {
                                LocalTextStyle.current
                            }
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

            // Top Quick Action Bar (Tap-to-Sync / Refresh / Text Edit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tap-to-Sync tapper button
                IconButton(
                    onClick = { showTapSyncEditor = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x77000000), CircleShape)
                        .testTag("open_tap_to_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Tap to Sync Editor",
                        tint = SpotifyGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Refresh from online
                IconButton(
                    onClick = onFetchOnline,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x77000000), CircleShape)
                        .testTag("refresh_online_lyrics_button")
                ) {
                    if (isFetchingOnline) {
                        CircularProgressIndicator(
                            color = SpotifyGreen,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh from Online",
                            tint = SpotifyTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Edit
                IconButton(
                    onClick = { showTextEditDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x77000000), CircleShape)
                        .testTag("edit_lyrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Text",
                        tint = SpotifyTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Stage 4: In-App Tap-to-Sync Editor View
    if (showTapSyncEditor) {
        ManualLrcSyncEditorView(
            song = song,
            currentPositionMs = currentPositionMs,
            isPlaying = isPlaying,
            onPlayPause = onPlayPause,
            onSeekTo = onSeekTo,
            onDismiss = { showTapSyncEditor = false },
            onSaveLyrics = { lrcText, isSynced ->
                onSaveLyrics(lrcText, isSynced)
                showTapSyncEditor = false
            }
        )
    }

    // Manual Text Editor Modal Dialog
    if (showTextEditDialog) {
        LyricsEditorDialog(
            initialLyrics = song?.lyrics ?: "",
            durationMs = song?.durationMs ?: 180000L,
            onDismiss = { showTextEditDialog = false },
            onSave = { updatedText ->
                val hasTimestamps = updatedText.contains(Regex("\\[\\d{1,2}:\\d{2}"))
                val finalLyrics = if (hasTimestamps) {
                    updatedText
                } else {
                    LyricsParser.generateTimedLrc(updatedText, song?.durationMs ?: 180000L)
                }
                onSaveLyrics(finalLyrics, true)
                showTextEditDialog = false
            }
        )
    }
}

@Composable
fun LyricsEditorDialog(
    initialLyrics: String,
    durationMs: Long,
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
                    text = "Paste or edit timestamped LRC (e.g. [01:23.45] Lyric line) or plain text lyrics. Auto-sync generates real-time timestamps.",
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Auto-sync timestamps button for plain text
                    OutlinedButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                text = LyricsParser.generateTimedLrc(text, durationMs)
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyGreen),
                        modifier = Modifier.testTag("auto_sync_lyrics_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Distribute", color = SpotifyGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyTextSecondary)
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(10.dp))

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
}
