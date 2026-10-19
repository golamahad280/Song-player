package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY createdAt ASC")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET isCurrent = 0")
    suspend fun clearActiveProfiles()

    @Query("UPDATE user_profiles SET isCurrent = 1 WHERE id = :profileId")
    suspend fun setActiveProfile(profileId: String)

    @Transaction
    suspend fun switchProfile(profileId: String) {
        clearActiveProfiles()
        setActiveProfile(profileId)
    }

    @Query("DELETE FROM user_profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: String)

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getProfileCount(): Int
}
