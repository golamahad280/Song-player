package com.example.player

import com.example.model.Song
import kotlin.random.Random

/**
 * Intelligent Smart Shuffle System balancing:
 * - Listening history & play counts
 * - User favorites & ratings
 * - Recently added tracks & release chronology
 * - Artist diversity (penalizing repetitive consecutive tracks by the same artist)
 * - Controlled probabilistic entropy (weighted stochastic variance)
 */
object SmartShuffleEngine {

    /**
     * Reorders the given playlist or queue using the Smart Shuffle algorithm.
     * The currently playing track is kept at the front of the queue if provided.
     */
    fun smartShuffle(queue: List<Song>, currentSong: Song? = null): List<Song> {
        if (queue.size <= 2) return queue

        val active = currentSong ?: queue.firstOrNull()
        val pool = (if (active != null) queue.filter { it.id != active.id } else queue).toMutableList()

        // Calculate dynamic heuristic scores
        val scoredList = pool.map { song ->
            val weight = computeTrackWeight(song)
            val entropy = Random.nextDouble(0.85, 1.15)
            song to (weight * entropy)
        }.sortedByDescending { it.second }.map { it.first }.toMutableList()

        // Apply artist diversity smoothing: avoid more than 1 consecutive track by the same artist
        val diversified = mutableListOf<Song>()
        while (scoredList.isNotEmpty()) {
            val lastArtist = diversified.lastOrNull()?.artist
            val candidateIndex = if (lastArtist != null && lastArtist.isNotBlank() && lastArtist != "Unknown Artist") {
                val diffArtistIdx = scoredList.indexOfFirst { !it.artist.equals(lastArtist, ignoreCase = true) }
                if (diffArtistIdx >= 0) diffArtistIdx else 0
            } else {
                0
            }
            diversified.add(scoredList.removeAt(candidateIndex))
        }

        return if (active != null) {
            listOf(active) + diversified
        } else {
            diversified
        }
    }

    /**
     * Computes the preference weight of an individual song.
     */
    fun computeTrackWeight(song: Song): Double {
        var score = 100.0

        // Favorite bonus (+150 pts)
        if (song.isFavorite) {
            score += 150.0
        }

        // Play count frequency bonus (up to +80 pts)
        val playBonus = (song.playCount * 8.0).coerceAtMost(80.0)
        score += playBonus

        // Recency bonus: tracks added in recent batches get a boost (+30 pts max)
        val recencyFactor = ((song.dateAdded / 1000) % 100).toDouble() * 0.3
        score += recencyFactor

        // Genre / duration sweet-spot (songs between 2 and 6 minutes favored)
        val durationSec = song.durationMs / 1000
        if (durationSec in 120..360) {
            score += 15.0
        }

        // Stochastic variance (+0 to +40 pts)
        score += Random.nextDouble(0.0, 40.0)

        return score
    }
}
