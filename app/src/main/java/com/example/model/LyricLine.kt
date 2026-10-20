package com.example.model

import java.util.regex.Pattern

data class LyricLine(
    val timeMs: Long,
    val text: String
)

object LyricsParser {
    private val TIME_TAG_REGEX = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{2,3}))?\\]")

    fun parseLrc(lrcContent: String?, defaultDurationMs: Long = 0L): List<LyricLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()

        val lines = lrcContent.lines()
        val parsed = mutableListOf<LyricLine>()
        var isAnyTimestampFound = false

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            // Skip ID3/LRC header metadata like [ar:Artist], [ti:Title], [al:Album]
            if (line.startsWith("[ar:") || line.startsWith("[ti:") ||
                line.startsWith("[al:") || line.startsWith("[by:") ||
                line.startsWith("[offset:") || line.startsWith("[length:")
            ) {
                continue
            }

            val matcher = TIME_TAG_REGEX.matcher(line)
            var lastMatchEnd = 0
            val lineTimestamps = mutableListOf<Long>()

            while (matcher.find()) {
                isAnyTimestampFound = true
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val fractionStr = matcher.group(3) ?: "0"
                val fractionMs = if (fractionStr.length == 2) {
                    fractionStr.toLongOrNull()?.times(10) ?: 0L
                } else {
                    fractionStr.take(3).padEnd(3, '0').toLongOrNull() ?: 0L
                }

                val totalMs = (minutes * 60 + seconds) * 1000 + fractionMs
                lineTimestamps.add(totalMs)
                lastMatchEnd = matcher.end()
            }

            val lyricText = line.substring(lastMatchEnd).trim()
            if (lyricText.isNotEmpty() || lineTimestamps.isNotEmpty()) {
                for (timestamp in lineTimestamps) {
                    parsed.add(LyricLine(timeMs = timestamp, text = lyricText))
                }
            }
        }

        // If no timestamp tags were found, convert plain text to evenly spaced approximate lines
        if (!isAnyTimestampFound) {
            val plainLines = lines.map { it.trim() }.filter { it.isNotEmpty() }
            if (plainLines.isNotEmpty()) {
                val step = if (defaultDurationMs > 0) defaultDurationMs / (plainLines.size + 1) else 4000L
                return plainLines.mapIndexed { index, text ->
                    LyricLine(timeMs = (index + 1) * step, text = text)
                }
            }
        }

        return parsed.sortedBy { it.timeMs }
    }

    /**
     * Converts plain lyrics text into a precisely timestamped [mm:ss.xx] .lrc format
     * distributed evenly across the song's playback duration.
     */
    fun generateTimedLrc(plainLyrics: String, durationMs: Long): String {
        val rawLines = plainLyrics.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("Paroles de la chanson") && !it.contains("lyrics submitted by", ignoreCase = true) }

        if (rawLines.isEmpty()) return plainLyrics

        val totalMs = if (durationMs > 10000L) durationMs else 180000L
        val startOffset = 6000L // 6s musical intro
        val availableTime = (totalMs - startOffset - 6000L).coerceAtLeast(12000L)
        val step = (availableTime / rawLines.size).coerceIn(1800L, 6500L)

        val sb = StringBuilder()
        rawLines.forEachIndexed { index, line ->
            val timestamp = startOffset + (index * step)
            val minutes = (timestamp / 1000) / 60
            val seconds = (timestamp / 1000) % 60
            val fraction = (timestamp % 1000) / 10
            sb.append("[%02d:%02d.%02d] %s\n".format(minutes, seconds, fraction, line))
        }
        return sb.toString().trim()
    }

    fun formatTime(timeMs: Long): String {
        val totalSec = (timeMs / 1000).coerceAtLeast(0)
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
