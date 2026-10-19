package com.example.model

data class Playlist(
    val id: Long,
    val profileId: String,
    val name: String,
    val description: String = "",
    val songIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val songCount: Int get() = songIds.size
}

data class UserProfile(
    val id: String,
    val name: String,
    val avatarColor: Long = 0xFF1ED760,
    val isCurrent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val initials: String
        get() {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return "U"
            val parts = trimmed.split(" ").filter { it.isNotEmpty() }
            return if (parts.size >= 2) {
                "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            } else {
                trimmed.take(1).uppercase()
            }
        }
}
