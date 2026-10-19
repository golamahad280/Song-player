package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.SongDao
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LyricsAndArtFetcher(
    private val context: Context,
    private val songDao: SongDao
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "LyricsAndArtFetcher"
    }

    /**
     * Attempts to fetch synced lyrics from LRCLIB. If cached in Room DB, returns immediately offline.
     */
    suspend fun fetchLyricsForSong(song: Song): Pair<String?, Boolean> = withContext(Dispatchers.IO) {
        // 1. Check local DB cache first (ensures seamless offline playback)
        val cached = songDao.getSongById(song.id)
        if (!cached?.lyrics.isNullOrBlank()) {
            return@withContext Pair(cached?.lyrics, cached?.isSyncedLyrics ?: false)
        }

        // If song already has lyrics attached
        if (!song.lyrics.isNullOrBlank()) {
            songDao.updateLyrics(song.id, song.lyrics, song.isSyncedLyrics)
            return@withContext Pair(song.lyrics, song.isSyncedLyrics)
        }

        // 2. Fetch online via LRCLIB public API (zero-authentication, high quality synced LRC)
        val cleanTitle = song.title.replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)"), "").trim()
        val cleanArtist = song.artist.replace(Regex("(?i)feat\\..*|ft\\..*"), "").trim()

        try {
            val query = URLEncoder.encode("$cleanArtist $cleanTitle", "UTF-8")
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
                            val firstMatch = array.getJSONObject(0)
                            val syncedLyrics = firstMatch.optString("syncedLyrics")
                            val plainLyrics = firstMatch.optString("plainLyrics")

                            val resultLyrics = when {
                                syncedLyrics.isNotBlank() -> syncedLyrics
                                plainLyrics.isNotBlank() -> plainLyrics
                                else -> null
                            }

                            val isSynced = syncedLyrics.isNotBlank()

                            if (resultLyrics != null) {
                                // Cache to Room DB for 100% offline playback
                                songDao.updateLyrics(song.id, resultLyrics, isSynced)
                                return@withContext Pair(resultLyrics, isSynced)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "LRCLIB lyrics fetch failed: ${e.message}")
        }

        return@withContext Pair(null, false)
    }

    /**
     * Attempts to fetch high-resolution album cover art from iTunes Search API.
     * Caches the image URL in Room DB.
     */
    suspend fun fetchArtworkForSong(song: Song): String? = withContext(Dispatchers.IO) {
        val cached = songDao.getSongById(song.id)
        if (!cached?.customArtworkUrl.isNullOrBlank()) {
            return@withContext cached?.customArtworkUrl
        }

        if (!song.customArtworkUrl.isNullOrBlank()) {
            songDao.updateCustomArtwork(song.id, song.customArtworkUrl)
            return@withContext song.customArtworkUrl
        }

        val cleanTitle = song.title.replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)"), "").trim()
        val cleanArtist = song.artist.replace(Regex("(?i)feat\\..*|ft\\..*"), "").trim()

        try {
            val term = URLEncoder.encode("$cleanArtist $cleanTitle", "UTF-8")
            val itunesUrl = "https://itunes.apple.com/search?term=$term&entity=song&limit=1"

            val request = Request.Builder()
                .url(itunesUrl)
                .header("User-Agent", "SongPlayer-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val results = json.optJSONArray("results")
                        if (results != null && results.length() > 0) {
                            val first = results.getJSONObject(0)
                            val rawUrl = first.optString("artworkUrl100")
                            if (rawUrl.isNotBlank()) {
                                // Convert 100x100 to ultra crisp 600x600 high-res album artwork
                                val highRes = rawUrl.replace("100x100bb", "600x600bb")
                                songDao.updateCustomArtwork(song.id, highRes)
                                return@withContext highRes
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "iTunes artwork fetch failed: ${e.message}")
        }

        return@withContext null
    }

    suspend fun saveManualLyrics(songId: Long, customLyrics: String, isSynced: Boolean) = withContext(Dispatchers.IO) {
        songDao.updateLyrics(songId, customLyrics, isSynced)
    }
}
