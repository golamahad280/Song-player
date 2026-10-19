package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: String,
    val name: String,
    val description: String = "",
    val songIds: String = "", // Comma-separated song IDs preserving order
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getSongIdList(): List<Long> {
        if (songIds.isBlank()) return emptyList()
        return songIds.split(",").mapNotNull { it.trim().toLongOrNull() }
    }
}
