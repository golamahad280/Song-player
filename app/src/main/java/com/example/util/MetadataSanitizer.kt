package com.example.util

/**
 * High-performance, uncompromised Metadata Sanitizer for track titles and artist names.
 * Strictly preserves Bengali (\u0980-\u09FF), Devanagari (\u0900-\u097F), Gurmukhi (\u0A00-\u0A7F),
 * Arabic/Urdu (\u0600-\u06FF), and other regional Unicode scripts while stripping dirty tags,
 * video markers, bitrates, and audio artifact suffixes.
 */
object MetadataSanitizer {

    private val AUDIO_EXTENSION_REGEX = Regex("(?i)\\.(mp3|flac|wav|aac|m4a|ogg|opus|wma)$")
    private val BITRATE_REGEX = Regex("(?i)\\b\\d{2,3}\\s*kbps\\b")

    private val DIRTY_BRACKET_PATTERNS = listOf(
        // English markers in brackets/parentheses
        Regex("(?i)\\[.*?(?:official|video|audio|lyrics?|lyric video|music video|hd|4k|1080p|720p|remastered|remaster|remix|8d audio|8d|download|cover|audio).*?\\]"),
        Regex("(?i)\\((?:official|video|audio|lyrics?|lyric video|music video|hd|4k|1080p|720p|remastered|remaster|remix|8d audio|8d|download|cover|audio|clean|explicit|version).*?\\)"),
        // Bengali / Bangla video & audio tags
        Regex("\\[.*?(?:ভিডিও|অফিশিয়াল|গান|লিরিক্স|অডিও|ফুল ভিডিও|লিরিক্যাল).*?\\]"),
        Regex("\\((?:ভিডিও|অফিশিয়াল|গান|লিরিক্স|অডিও|ফুল ভিডিও|লিরিক্যাল|এইচডি|৪কে).*?\\)"),
        // Hindi / Devanagari video & audio tags
        Regex("\\[.*?(?:वीडियो|ऑफ़िशियल|गाना|गीत|लिरिक्स|फुल वीडियो|रीमिक्स).*?\\]"),
        Regex("\\((?:वीडियो|ऑफ़िशियल|गाना|गीत|लिरिक्स|फुल वीडियो|रीमिक्स|एचडी).*?\\)")
    )

    private val DIRTY_KEYWORD_PATTERNS = listOf(
        Regex("(?i)\\b(official\\s*video|official\\s*audio|music\\s*video|lyric\\s*video|lyrics?\\s*video|official)\\b"),
        Regex("(?i)\\b(hd|4k|hq|1080p|720p)\\b"),
        Regex("(?i)\\b(8d\\s*audio|8d|audio\\s*video|download|full\\s*song|full\\s*audio)\\b"),
        Regex("(?i)\\b(remastered|remaster|remix|cover)\\b"),
        Regex("(?i)\\b(feat\\.|ft\\.|featuring)\\b.*")
    )

    /**
     * Cleans raw track title while strictly preserving regional Unicode characters.
     */
    fun cleanTitle(raw: String?): String {
        if (raw.isNullOrBlank()) return "Unknown Track"

        var text = raw.replace(AUDIO_EXTENSION_REGEX, "").replace('_', ' ')

        // Handle "Artist - Title" format in file names (keep the title part)
        if (text.contains(" - ")) {
            val parts = text.split(" - ")
            if (parts.size >= 2) {
                text = parts.last()
            }
        }

        // Strip bitrate tags
        text = text.replace(BITRATE_REGEX, " ")

        // Strip bracketed markers
        for (pattern in DIRTY_BRACKET_PATTERNS) {
            text = text.replace(pattern, " ")
        }

        // Strip standalone dirty keyword markers
        for (pattern in DIRTY_KEYWORD_PATTERNS) {
            text = text.replace(pattern, " ")
        }

        val cleaned = text
            .replace(Regex("[\\[\\](){}]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

        return if (cleaned.isNotBlank()) cleaned else raw.replace(AUDIO_EXTENSION_REGEX, "").trim()
    }

    /**
     * Cleans raw artist name while preserving regional Unicode characters.
     */
    fun cleanArtist(raw: String?): String {
        if (raw.isNullOrBlank()) return "Unknown Artist"

        var text = raw.replace(AUDIO_EXTENSION_REGEX, "").replace('_', ' ')

        // Handle "Artist - Title" format in file names (keep the artist part)
        if (text.contains(" - ")) {
            val parts = text.split(" - ")
            if (parts.isNotEmpty()) {
                text = parts.first()
            }
        }

        text = text.replace(BITRATE_REGEX, " ")

        val dirtyArtistPatterns = listOf(
            Regex("(?i)\\[.*?\\]|\\(.*?\\)"),
            Regex("(?i)\\b(feat\\.|ft\\.|featuring)\\b.*"),
            Regex("(?i)\\b(official|video|audio|lyrics?|remastered|remix|8d audio|8d|download|cover)\\b")
        )

        for (pattern in dirtyArtistPatterns) {
            text = text.replace(pattern, " ")
        }

        text = text.trim().replace(Regex("\\s+"), " ")

        return if (text.isBlank() || text.equals("unknown", ignoreCase = true) || text.equals("<unknown>", ignoreCase = true)) {
            "Unknown Artist"
        } else {
            text
        }
    }
}
