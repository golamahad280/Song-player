package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarColor: Long = 0xFF1ED760,
    val isCurrent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
