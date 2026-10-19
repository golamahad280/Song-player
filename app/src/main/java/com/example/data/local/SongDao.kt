package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSongs(songs: List<SongEntity>)

    @Query("UPDATE songs SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END WHERE id = :songId")
    suspend fun toggleFavorite(songId: Long)

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long)

    @Query("UPDATE songs SET lyrics = :lyrics, isSyncedLyrics = :isSynced WHERE id = :songId")
    suspend fun updateLyrics(songId: Long, lyrics: String, isSynced: Boolean)

    @Query("UPDATE songs SET customArtworkUrl = :artworkUrl WHERE id = :songId")
    suspend fun updateCustomArtwork(songId: Long, artworkUrl: String)

    @Query("UPDATE songs SET dominantColor = :colorHex WHERE id = :songId")
    suspend fun updateDominantColor(songId: Long, colorHex: Long)
}
