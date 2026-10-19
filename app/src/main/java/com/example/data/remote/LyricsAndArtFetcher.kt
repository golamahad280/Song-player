package com.example.data.remote

import android.content.Context
import com.example.data.local.SongDao
import com.example.model.Song

/**
 * Backward-compatible wrapper delegating to the unified [LyricsService].
 */
class LyricsAndArtFetcher(
    context: Context,
    songDao: SongDao
) {
    private val lyricsService = LyricsService(context, songDao)

    suspend fun fetchLyricsForSong(song: Song): Pair<String?, Boolean> {
        return lyricsService.fetchLyricsForSong(song)
    }

    suspend fun fetchArtworkForSong(song: Song): String? {
        return lyricsService.fetchArtworkForSong(song)
    }

    suspend fun saveManualLyrics(songId: Long, customLyrics: String, isSynced: Boolean) {
        lyricsService.saveManualLyrics(songId, customLyrics, isSynced)
    }

    fun normalizeTrackTitle(raw: String): String = lyricsService.normalizeTrackTitle(raw)

    fun normalizeArtistName(raw: String): String = lyricsService.normalizeArtistName(raw)
}
