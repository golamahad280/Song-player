package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
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
)
