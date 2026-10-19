package com.example.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val filePath: String = "",
    val albumId: Long = 0L,
    val dateAdded: Long = 0L,
    val year: Int? = null,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val lyrics: String? = null,
    val isSyncedLyrics: Boolean = false,
    val customArtworkUrl: String? = null,
    val dominantColor: Long? = null
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
