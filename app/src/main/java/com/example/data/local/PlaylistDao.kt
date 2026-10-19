package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getPlaylistsForProfile(profileId: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET songIds = :songIds WHERE id = :playlistId")
    suspend fun updatePlaylistSongs(playlistId: Long, songIds: String)

    @Query("UPDATE playlists SET name = :newName, description = :newDescription WHERE id = :playlistId")
    suspend fun updatePlaylistDetails(playlistId: Long, newName: String, newDescription: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlists WHERE profileId = :profileId")
    suspend fun deleteAllPlaylistsForProfile(profileId: String)
}
