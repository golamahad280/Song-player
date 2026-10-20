package com.example.ui.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

data class SyncableLyricLine(
    val rawText: String,
    var timestampMs: Long? = null
)

/**
 * Stage 4: In-App Manual LRC Tapper & Precision Sync Editor.
 * Plays the local audio track while displaying plain lyrics line-by-line.
 * The user taps "Mark Timestamp" on each line as it plays to capture millisecond precision,
 * then saves directly to the local SQLite/Room database.
 */
@Composable
fun ManualLrcSyncEditorView(
    song: Song?,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSaveLyrics: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Tap-to-Sync, 1 = Raw Text Edit
    var rawTextEditorContent by remember { mutableStateOf(song.lyrics ?: "") }

    // Parse existing lines or plain text
    val syncLines = remember(song.lyrics) {
        val initialLines = mutableStateListOf<SyncableLyricLine>()
        val existing = song.lyrics ?: ""
        if (existing.isNotBlank()) {
            val lines = existing.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                // Check if line already has a timestamp [mm:ss.xx]
                val matcher = java.util.regex.Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{2,3}))?\\]").matcher(trimmed)
                if (matcher.find()) {
                    val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                    val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                    val fractionStr = matcher.group(3) ?: "0"
                    val fractionMs = if (fractionStr.length == 2) {
                        fractionStr.toLongOrNull()?.times(10) ?: 0L
                    } else {
                        fractionStr.take(3).padEnd(3, '0').toLongOrNull() ?: 0L
                    }
                    val timeMs = (minutes * 60 + seconds) * 1000 + fractionMs
                    val textOnly = trimmed.substring(matcher.end()).trim()
                    initialLines.add(SyncableLyricLine(rawText = textOnly, timestampMs = timeMs))
                } else {
                    initialLines.add(SyncableLyricLine(rawText = trimmed, timestampMs = null))
                }
            }
        }
        if (initialLines.isEmpty()) {
            initialLines.add(SyncableLyricLine("Line 1 of song lyrics..."))
            initialLines.add(SyncableLyricLine("Line 2 of song lyrics..."))
        }
        initialLines
    }

    var currentSyncIndex by remember {
        val firstUnsynced = syncLines.indexOfFirst { it.timestampMs == null }
        mutableIntStateOf(if (firstUnsynced >= 0) firstUnsynced else 0)
    }

    val listState = rememberLazyListState()

    // Keep current sync line centered
    LaunchedEffect(currentSyncIndex) {
        if (currentSyncIndex in syncLines.indices) {
            val target = (currentSyncIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .background(SpotifyBlack),
            color = SpotifyBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top Navigation Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tap-to-Sync Lyrics Editor",
                            color = SpotifyTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${song.title} • ${song.artist}",
                            color = SpotifyTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_sync_editor_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotifyTextPrimary)
                    }
                }

                // Tab Switcher (Tap-to-Sync vs Raw Text Edit)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SpotifyElevated,
                    contentColor = SpotifyGreen,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SpotifyGreen
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tap-to-Sync", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            // Update raw text with current sync state
                            val sb = StringBuilder()
                            syncLines.forEach { line ->
                                if (line.timestampMs != null) {
                                    val t = line.timestampMs!!
                                    val m = (t / 1000) / 60
                                    val s = (t / 1000) % 60
                                    val ms = (t % 1000) / 10
                                    sb.append("[%02d:%02d.%02d] %s\n".format(m, s, ms, line.rawText))
                                } else {
                                    sb.append("%s\n".format(line.rawText))
                                }
                            }
                            rawTextEditorContent = sb.toString().trim()
                            selectedTab = 1
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Raw Text / LRC", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // TAB 0: INTERACTIVE TAP-TO-SYNC VIEW
                    // Live Audio Player Controller Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = SpotifyCardBackground,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play/Pause & Rewind controls
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onPlayPause,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(SpotifyGreen)
                                        .testTag("sync_editor_play_pause")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = SpotifyBlack,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        val target = (currentPositionMs - 5000L).coerceAtLeast(0L)
                                        onSeekTo(target)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay5,
                                        contentDescription = "Rewind 5s",
                                        tint = SpotifyTextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Current audio position timestamp
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatTimestamp(currentPositionMs),
                                    color = SpotifyGreen,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Track: ${formatTimestamp(song.durationMs)}",
                                    color = SpotifyTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Instruction Banner
                    Text(
                        text = "Listen to audio & tap 'MARK LINE' as each phrase begins. Timestamps sync with millisecond precision.",
                        color = SpotifyTextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Scrollable List of Syncable Lines
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpotifyCardBackground)
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(syncLines) { index, line ->
                            val isCurrentTarget = index == currentSyncIndex
                            val isSynced = line.timestampMs != null

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isCurrentTarget -> SpotifyCardHover
                                            isSynced -> Color(0x151ED760)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isCurrentTarget) 1.5.dp else 0.5.dp,
                                        color = if (isCurrentTarget) SpotifyGreen else Color(0x22FFFFFF),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        currentSyncIndex = index
                                        line.timestampMs?.let { onSeekTo(it) }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Line number badge
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isCurrentTarget) SpotifyGreen else SpotifyTextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(28.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = line.rawText,
                                            color = if (isCurrentTarget) SpotifyTextPrimary else if (isSynced) SpotifyTextSecondary else SpotifyTextMuted,
                                            fontSize = 15.sp,
                                            fontWeight = if (isCurrentTarget) FontWeight.Bold else FontWeight.Medium
                                        )

                                        if (line.timestampMs != null) {
                                            Text(
                                                text = formatLrcTag(line.timestampMs!!),
                                                color = SpotifyGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Single line re-stamp button
                                IconButton(
                                    onClick = {
                                        syncLines[index] = line.copy(timestampMs = currentPositionMs)
                                        currentSyncIndex = (index + 1).coerceAtMost(syncLines.size - 1)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSynced) Icons.Default.Check else Icons.Default.FiberManualRecord,
                                        contentDescription = "Stamp line",
                                        tint = if (isSynced) SpotifyGreen else SpotifyTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prominent Bottom "MARK LINE" & "SAVE" Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mark Next Line button
                        Button(
                            onClick = {
                                if (currentSyncIndex in syncLines.indices) {
                                    val targetLine = syncLines[currentSyncIndex]
                                    syncLines[currentSyncIndex] = targetLine.copy(timestampMs = currentPositionMs)
                                    if (currentSyncIndex < syncLines.size - 1) {
                                        currentSyncIndex += 1
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("mark_current_line_button")
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = SpotifyBlack)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MARK LINE ${currentSyncIndex + 1} (${formatTimestamp(currentPositionMs)})",
                                color = SpotifyBlack,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        // Save final LRC button
                        Button(
                            onClick = {
                                val finalLrc = compileLinesToLrc(syncLines, song.durationMs)
                                onSaveLyrics(finalLrc, true)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyElevated),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("save_synced_lrc_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = SpotifyTextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", color = SpotifyTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // TAB 1: RAW TEXT / LRC EDITOR VIEW
                    OutlinedTextField(
                        value = rawTextEditorContent,
                        onValueChange = { rawTextEditorContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("raw_lrc_editor_input"),
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
                                "[00:15.00] Line one\n[00:20.50] Line two\nor enter plain lyrics text...",
                                color = SpotifyTextMuted
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (rawTextEditorContent.isNotBlank()) {
                                    rawTextEditorContent = LyricsParser.generateTimedLrc(rawTextEditorContent, song.durationMs)
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyGreen)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto-Distribute", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val hasTimestamps = rawTextEditorContent.contains(Regex("\\[\\d{1,2}:\\d{2}"))
                                val finalLrc = if (hasTimestamps) rawTextEditorContent else LyricsParser.generateTimedLrc(rawTextEditorContent, song.durationMs)
                                onSaveLyrics(finalLrc, true)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = SpotifyBlack)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save LRC", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

private fun formatLrcTag(timeMs: Long): String {
    val totalSec = (timeMs / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    val fraction = (timeMs % 1000) / 10
    return "[%02d:%02d.%02d]".format(m, s, fraction)
}

private fun compileLinesToLrc(lines: List<SyncableLyricLine>, durationMs: Long): String {
    val sb = StringBuilder()
    var lastTimeMs = 0L

    lines.forEachIndexed { index, line ->
        val timestamp = line.timestampMs ?: (lastTimeMs + 4000L).coerceAtMost(durationMs)
        lastTimeMs = timestamp
        val m = (timestamp / 1000) / 60
        val s = (timestamp / 1000) % 60
        val fraction = (timestamp % 1000) / 10
        sb.append("[%02d:%02d.%02d] %s\n".format(m, s, fraction, line.rawText))
    }
    return sb.toString().trim()
}
