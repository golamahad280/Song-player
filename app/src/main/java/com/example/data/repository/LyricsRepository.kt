package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.SongDao
import com.example.model.LyricLine
import com.example.model.LyricsParser
import com.example.util.MetadataSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Production-grade LyricsRepository with a 5-Stage Uncompromised Synced Lyrics Architecture:
 * - Stage 1: Metadata Sanitization with Unicode preservation (Bangla/Hindi/English).
 * - Stage 2: Primary Synced Engine (LRCLIB API direct millisecond LRC).
 * - Stage 3: Secondary Engine (Gemini prompted timestamp generator).
 * - Stage 4: In-app Manual LRC Tapper / Sync Editor integration.
 * - Stage 5: Permanent offline SQLite/Room DB caching for instant offline playback.
 */
class LyricsRepository(
    private val context: Context,
    private val songDao: SongDao
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "LyricsRepository"
    }

    /**
     * Retrieves synced lyrics for a song following the 5-stage architecture.
     * Offline-first: Checks local database first with zero latency.
     * Returns Pair<LrcText?, IsSynced>.
     */
    suspend fun getLyrics(
        songId: Long,
        rawTitle: String,
        rawArtist: String,
        durationMs: Long
    ): Pair<String?, Boolean> = withContext(Dispatchers.IO) {
        // STAGE 5: Check Offline Database Cache (instant zero-latency retrieval)
        try {
            val cached = songDao.getSongById(songId)
            if (!cached?.lyrics.isNullOrBlank()) {
                Log.d(TAG, "Stage 5 Offline Hit: Loaded lyrics from local Room DB for song $songId")
                return@withContext Pair(cached.lyrics, cached.isSyncedLyrics)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Local cache lookup error: ${e.message}")
        }

        // If device is offline, do not attempt network pipeline
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Offline mode detected: Skipping remote lyrics fetch")
            return@withContext Pair(null, false)
        }

        // STAGE 1: METADATA SANITIZER & UNICODE PRESERVATION
        val cleanTitle = MetadataSanitizer.cleanTitle(rawTitle)
        val cleanArtist = MetadataSanitizer.cleanArtist(rawArtist)
        val durationSec = (durationMs / 1000).toInt()

        Log.d(TAG, "Starting lyrics search for: '$cleanTitle' by '$cleanArtist' ($durationSec s)")

        var fallbackPlainLyrics: String? = null

        // STAGE 2: PRIMARY SYNCED ENGINE (LRCLIB DIRECT LRC ENGINE)
        try {
            val lrcLibResult = fetchFromLrcLib(cleanArtist, cleanTitle, durationSec)
            if (lrcLibResult != null && !lrcLibResult.first.isNullOrBlank()) {
                if (lrcLibResult.second) {
                    val syncedLrc = lrcLibResult.first!!
                    songDao.updateLyrics(songId, syncedLrc, true)
                    Log.d(TAG, "Stage 2 Success: Fetched synced LRC from LRCLIB for '$cleanTitle'")
                    return@withContext Pair(syncedLrc, true)
                } else {
                    fallbackPlainLyrics = lrcLibResult.first
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "LRCLIB primary fetch error: ${e.message}")
        }

        // Title-only query fallback on LRCLIB
        if (fallbackPlainLyrics.isNullOrBlank()) {
            try {
                val titleOnly = fetchFromLrcLib("", cleanTitle, durationSec)
                if (titleOnly != null && !titleOnly.first.isNullOrBlank()) {
                    if (titleOnly.second) {
                        val syncedLrc = titleOnly.first!!
                        songDao.updateLyrics(songId, syncedLrc, true)
                        Log.d(TAG, "Stage 2 Success: Fetched synced LRC via title query for '$cleanTitle'")
                        return@withContext Pair(syncedLrc, true)
                    } else {
                        fallbackPlainLyrics = titleOnly.first
                    }
                }
            } catch (_: Exception) {}
        }

        // STAGE 3: SECONDARY ENGINE (GEMINI PROMPTED TIMESTAMP GENERATOR)
        try {
            val geminiResult = fetchFromGeminiLrc(cleanArtist, cleanTitle, durationSec)
            if (!geminiResult.isNullOrBlank()) {
                songDao.updateLyrics(songId, geminiResult, true)
                Log.d(TAG, "Stage 3 Success: Generated timestamped LRC via Gemini for '$cleanTitle'")
                return@withContext Pair(geminiResult, true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Stage 3 Gemini LRC error: ${e.message}")
        }

        // Tertiary fallback: If plain text lyrics were discovered from LRCLIB, align with heuristic timestamps
        if (!fallbackPlainLyrics.isNullOrBlank()) {
            val timedLrc = LyricsParser.generateTimedLrc(fallbackPlainLyrics, durationMs)
            songDao.updateLyrics(songId, timedLrc, true)
            Log.d(TAG, "Aligned plain text into timed LRC for '$cleanTitle'")
            return@withContext Pair(timedLrc, true)
        }

        // Stage 3 fallback: Try lyrics.ovh plain text and align
        try {
            val ovhPlain = fetchFromLyricsOvh(cleanArtist, cleanTitle)
            if (!ovhPlain.isNullOrBlank()) {
                val timedLrc = LyricsParser.generateTimedLrc(ovhPlain, durationMs)
                songDao.updateLyrics(songId, timedLrc, true)
                Log.d(TAG, "Aligned lyrics.ovh plain text into timed LRC for '$cleanTitle'")
                return@withContext Pair(timedLrc, true)
            }
        } catch (_: Exception) {}

        return@withContext Pair(null, false)
    }

    /**
     * Primary LRCLIB Query Engine.
     */
    private fun fetchFromLrcLib(artist: String, title: String, durationSec: Int): Pair<String, Boolean>? {
        if (title.isBlank()) return null

        // 1. Exact get endpoint if artist and title are present
        if (artist.isNotBlank() && artist != "Unknown Artist") {
            try {
                val encodedTrack = URLEncoder.encode(title.trim(), "UTF-8")
                val encodedArtist = URLEncoder.encode(artist.trim(), "UTF-8")
                val getUrl = if (durationSec > 0) {
                    "https://lrclib.net/api/get?track_name=$encodedTrack&artist_name=$encodedArtist&duration=$durationSec"
                } else {
                    "https://lrclib.net/api/get?track_name=$encodedTrack&artist_name=$encodedArtist"
                }

                val request = Request.Builder()
                    .url(getUrl)
                    .header("User-Agent", "SongPlayer-Android/1.0 (https://github.com/aistudio)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val syncedLyrics = json.optString("syncedLyrics")
                            val plainLyrics = json.optString("plainLyrics")
                            if (syncedLyrics.isNotBlank()) {
                                return Pair(syncedLyrics, true)
                            } else if (plainLyrics.isNotBlank()) {
                                return Pair(plainLyrics, false)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "LRCLIB /api/get attempt: ${e.message}")
            }
        }

        // 2. Search endpoint fallback
        val queryStr = if (artist.isNotBlank() && artist != "Unknown Artist") "$artist $title" else title
        val query = URLEncoder.encode(queryStr.trim(), "UTF-8")
        val searchUrl = "https://lrclib.net/api/search?q=$query"

        val request = Request.Builder()
            .url(searchUrl)
            .header("User-Agent", "SongPlayer-Android/1.0 (https://github.com/aistudio)")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val array = JSONArray(body)
                    if (array.length() > 0) {
                        // Pass 1: Look for exact time-synced lyrics
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            val synced = item.optString("syncedLyrics")
                            if (synced.isNotBlank()) {
                                return Pair(synced, true)
                            }
                        }
                        // Pass 2: Plain text lyrics
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            val plain = item.optString("plainLyrics")
                            if (plain.isNotBlank()) {
                                return Pair(plain, false)
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Stage 3: Gemini Prompted Timestamp Generator with rigid system specification.
     */
    private fun fetchFromGeminiLrc(artist: String, title: String, durationSec: Int): String? {
        val apiKey = try {
            val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") return null

        val durSec = if (durationSec > 0) durationSec else 180
        val prompt = "You are an expert music lyrics aligner. Return the COMPLETE official lyrics for \"$title\" by \"$artist\". The exact track duration is $durSec seconds. STRICTLTY OUTPUT IN TIMESTAMPED LRC FORMAT [mm:ss.xx] WITH TIMESTAMPS DISTRIBUTED PROPORTIONALLY ACCORDING TO SONG STRUCTURE. DO NOT RETURN MARKDOWN OR PLAIN TEXT. ONLY RETURN VALID LRC LINES."

        val jsonBody = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            if (text.isNotBlank()) {
                                val cleaned = text.replace("```lrc", "")
                                    .replace("```", "")
                                    .trim()
                                if (cleaned.contains(Regex("\\[\\d{1,2}:\\d{2}"))) {
                                    return cleaned
                                }
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun fetchFromLyricsOvh(artist: String, title: String): String? {
        if (artist.isBlank() || artist == "Unknown Artist" || title.isBlank()) return null
        val encodedArtist = URLEncoder.encode(artist.trim(), "UTF-8")
        val encodedTitle = URLEncoder.encode(title.trim(), "UTF-8")
        val url = "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SongPlayer-Android/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val lyrics = json.optString("lyrics")
                    if (lyrics.isNotBlank()) return lyrics.trim()
                }
            }
        }
        return null
    }

    /**
     * Stage 4 & 5: Offline SQLite/Room persistence for manual edits or tap-to-sync output.
     */
    suspend fun saveLyrics(songId: Long, lrcContent: String, isSynced: Boolean = true) = withContext(Dispatchers.IO) {
        songDao.updateLyrics(songId, lrcContent, isSynced)
    }

    fun parseLrcLines(lrcContent: String?, durationMs: Long): List<LyricLine> {
        return LyricsParser.parseLrc(lrcContent, durationMs)
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }
}
