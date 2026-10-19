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

    fun formatTime(timeMs: Long): String {
        val totalSec = (timeMs / 1000).coerceAtLeast(0)
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
