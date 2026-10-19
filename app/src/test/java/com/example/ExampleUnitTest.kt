package com.example

import com.example.model.LyricsParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun lyricsParser_parsesTimestampsAccurately() {
        val lrc = """
            [ti:Test Title]
            [ar:Test Artist]
            [00:08.50] Driving through the neon glow
            [00:13.20] Watch the city breathing slow
            [01:05.10] In this frequency we find our home
        """.trimIndent()

        val parsed = LyricsParser.parseLrc(lrc)
        assertEquals(3, parsed.size)
        assertEquals(8500L, parsed[0].timeMs)
        assertEquals("Driving through the neon glow", parsed[0].text)
        assertEquals(13200L, parsed[1].timeMs)
        assertEquals(65100L, parsed[2].timeMs)
    }

    @Test
    fun lyricsParser_formatTimeFormatsCorrectly() {
        assertEquals("00:00", LyricsParser.formatTime(0L))
        assertEquals("01:05", LyricsParser.formatTime(65000L))
        assertEquals("03:34", LyricsParser.formatTime(214000L))
    }

    @Test
    fun lyricsNormalization_cleansVideoAndRemixTags() {
        val dirtyTitle = "Blinding Lights (Official Music Video) [4K Remastered].mp3"
        val cleaned = dirtyTitle
            .replace(Regex("(?i)\\.(mp3|flac|wav|aac|m4a|ogg)$"), "")
            .replace(Regex("(?i)\\[.*?\\]"), "")
            .replace(Regex("(?i)\\((?:official|video|audio|lyrics?|lyric video|music video|hd|4k|remastered|remaster|remix|clean|explicit|version).*?\\)"), "")
            .trim()
        assertEquals("Blinding Lights", cleaned)
    }
}
