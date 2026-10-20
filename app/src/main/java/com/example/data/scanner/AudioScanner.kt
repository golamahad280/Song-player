package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.model.Song
import java.io.File

object AudioScanner {
    private const val TAG = "AudioScanner"

    // Supported formats specified in requirements: .mp3, .flac, .wav, .aac, .m4a, .ogg
    private val SUPPORTED_EXTENSIONS = setOf("mp3", "flac", "wav", "aac", "m4a", "ogg")

    suspend fun scanLocalAudio(context: Context): List<Song> {
        val songsList = mutableListOf<Song>()
        val contentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.YEAR
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val rawTitle = cursor.getString(titleColumn) ?: "Unknown Track"
                    val rawArtist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val rawAlbum = cursor.getString(albumColumn) ?: "Unknown Album"
                    val duration = cursor.getLong(durationColumn)
                    val data = cursor.getString(dataColumn) ?: ""
                    val albumId = cursor.getLong(albumIdColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val year = if (yearColumn >= 0) cursor.getInt(yearColumn) else null

                    // Filter only positive duration and supported audio extensions
                    val ext = data.substringAfterLast('.', "").lowercase()
                    if (duration > 5000 && (ext.isEmpty() || ext in SUPPORTED_EXTENSIONS)) {
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        songsList.add(
                            Song(
                                id = id,
                                title = cleanMetadata(rawTitle),
                                artist = if (rawArtist.equals("<unknown>", ignoreCase = true)) "Unknown Artist" else cleanMetadata(rawArtist),
                                album = if (rawAlbum.equals("<unknown>", ignoreCase = true)) "Unknown Album" else cleanMetadata(rawAlbum),
                                durationMs = duration,
                                contentUri = contentUri,
                                filePath = data,
                                albumId = albumId,
                                dateAdded = dateAdded,
                                year = year
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore: ${e.message}", e)
        }

        // Deep-scan filesystem fallback for non-indexed audio files (e.g. newly downloaded)
        try {
            val musicDir = context.getExternalFilesDir(null)
            if (musicDir != null && musicDir.exists()) {
                scanDirectoryRecursively(context, musicDir, songsList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning external files dir: ${e.message}", e)
        }

        // If running in Android emulator or fresh device without audio files, provide premium demo tracks
        if (songsList.isEmpty()) {
            songsList.addAll(getDemoTracks(context))
        }

        return songsList
    }

    private fun scanDirectoryRecursively(context: Context, dir: File, outList: MutableList<Song>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanDirectoryRecursively(context, file, outList)
            } else {
                val ext = file.extension.lowercase()
                if (ext in SUPPORTED_EXTENSIONS && !outList.any { it.filePath == file.absolutePath }) {
                    extractFileMetadata(context, file)?.let { outList.add(it) }
                }
            }
        }
    }

    private fun extractFileMetadata(context: Context, file: File): Song? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.nameWithoutExtension
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Local Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Local Album"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 180000L
            val dateAdded = file.lastModified() / 1000
            val songId = file.hashCode().toLong()

            var customArtworkUrl: String? = null
            var dominantColor: Long? = null
            val picture = retriever.embeddedPicture
            if (picture != null && picture.isNotEmpty()) {
                try {
                    val artDir = File(context.filesDir, "artwork").apply { mkdirs() }
                    val artFile = File(artDir, "art_$songId.jpg")
                    artFile.writeBytes(picture)
                    customArtworkUrl = "file://${artFile.absolutePath}"
                    dominantColor = extractDominantColor(picture)
                } catch (_: Exception) {}
            }

            Song(
                id = songId,
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                contentUri = Uri.fromFile(file).toString(),
                filePath = file.absolutePath,
                albumId = 0L,
                dateAdded = dateAdded,
                customArtworkUrl = customArtworkUrl,
                dominantColor = dominantColor
            )
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Parses metadata from a manually imported audio URI (via system Storage Access Framework).
     */
    fun parseUriMetadata(context: Context, uri: Uri): Song {
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs: Long = 180000L
        var year: Int? = null

        // Try reading display name from ContentResolver
        var displayName = "Imported Track"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }
        } catch (_: Exception) {}

        val generatedId = (uri.toString().hashCode().toLong() and 0x7FFFFFFF) + 100000L
        var customArtworkUrl: String? = null
        var dominantColor: Long? = null

        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (dur != null) {
                durationMs = dur.toLongOrNull() ?: 180000L
            }
            year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()

            val picture = retriever.embeddedPicture
            if (picture != null && picture.isNotEmpty()) {
                try {
                    val artDir = File(context.filesDir, "artwork").apply { mkdirs() }
                    val artFile = File(artDir, "art_$generatedId.jpg")
                    artFile.writeBytes(picture)
                    customArtworkUrl = "file://${artFile.absolutePath}"
                    dominantColor = extractDominantColor(picture)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract full metadata via retriever for $uri: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        val finalTitle = if (!title.isNullOrBlank()) cleanMetadata(title) else cleanMetadata(displayName)
        val finalArtist = if (!artist.isNullOrBlank() && !artist.equals("<unknown>", ignoreCase = true)) cleanMetadata(artist) else "Unknown Artist"
        val finalAlbum = if (!album.isNullOrBlank() && !album.equals("<unknown>", ignoreCase = true)) cleanMetadata(album) else "Imported Audio"

        return Song(
            id = generatedId,
            title = finalTitle,
            artist = finalArtist,
            album = finalAlbum,
            durationMs = durationMs,
            contentUri = uri.toString(),
            filePath = uri.path ?: "",
            albumId = 0L,
            dateAdded = System.currentTimeMillis() / 1000,
            year = year,
            customArtworkUrl = customArtworkUrl,
            dominantColor = dominantColor
        )
    }

    fun extractDominantColor(pictureBytes: ByteArray): Long {
        return try {
            val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 }
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size, options)
                ?: return 0xFF1DB954L
            extractDominantColorFromBitmap(bitmap)
        } catch (_: Exception) {
            0xFF1DB954L
        }
    }

    private fun extractDominantColorFromBitmap(bitmap: android.graphics.Bitmap): Long {
        return try {
            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var count = 0
            val width = bitmap.width
            val height = bitmap.height
            val stepX = (width / 16).coerceAtLeast(1)
            val stepY = (height / 16).coerceAtLeast(1)

            var maxSat = -1f
            var vibrantColor = 0xFF1DB954L
            val hsv = FloatArray(3)

            for (x in 0 until width step stepX) {
                for (y in 0 until height step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    android.graphics.Color.colorToHSV(pixel, hsv)
                    val sat = hsv[1]
                    val value = hsv[2]

                    if (sat > 0.25f && value > 0.2f && value < 0.95f) {
                        if (sat > maxSat) {
                            maxSat = sat
                            vibrantColor = 0xFF000000L or ((r.toLong() and 0xFF) shl 16) or ((g.toLong() and 0xFF) shl 8) or (b.toLong() and 0xFF)
                        }
                    }
                    totalR += r
                    totalG += g
                    totalB += b
                    count++
                }
            }
            if (maxSat > 0.25f) vibrantColor
            else if (count > 0) {
                val avgR = (totalR / count).coerceIn(30, 220)
                val avgG = (totalG / count).coerceIn(30, 220)
                val avgB = (totalB / count).coerceIn(30, 220)
                0xFF000000L or (avgR shl 16) or (avgG shl 8) or avgB
            } else {
                0xFF1DB954L
            }
        } catch (_: Exception) {
            0xFF1DB954L
        }
    }

    private fun cleanMetadata(text: String): String {
        return text.trim()
            .replace(Regex("\\.(mp3|flac|wav|aac|m4a|ogg)$", RegexOption.IGNORE_CASE), "")
    }

    /**
     * Bundled realistic Spotify demo tracks with synchronized LRC lyrics and high-res cover art.
     * Ensures immediate functionality and rich visual demo even if the test device has no stored MP3s.
     */
    fun getDemoTracks(context: Context): List<Song> {
        return listOf(
            Song(
                id = 9001L,
                title = "Midnight City Lights",
                artist = "Neon Skyline",
                album = "Electric Dreams",
                durationMs = 214000L,
                contentUri = "asset://synthwave_sample.mp3",
                filePath = "/demo/midnight_city_lights.mp3",
                albumId = 101L,
                dateAdded = 1690000000L,
                year = 2024,
                playCount = 42,
                isFavorite = true,
                customArtworkUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80",
                isSyncedLyrics = true,
                dominantColor = 0xFF7928CA,
                lyrics = """
                    [00:00.00] (Instrumental Synthwave Intro)
                    [00:08.50] Driving through the neon glow
                    [00:13.20] Watch the city breathing slow
                    [00:18.00] Echoes in the velvet night
                    [00:23.40] Guided by the purple light
                    [00:28.10] Speeding down the boulevard
                    [00:33.20] Leaving behind every scar
                    [00:38.00] Feel the bass line in the floor
                    [00:43.10] We don't look back anymore
                    [00:48.00] Oh midnight city lights, take me away
                    [00:53.40] Dancing till the breaking of the day
                    [00:58.20] In this frequency we find our home
                    [01:03.50] Underneath the synthesizer dome
                    [01:10.00] (Synthesizer Solo)
                    [01:25.00] Shadows fade into the sound
                    [01:30.20] Feet floating off the ground
                    [01:35.00] Electric heartbeats in the rain
                    [01:40.10] Washing out the yesterday's pain
                    [01:45.30] Oh midnight city lights, take me away
                    [01:50.50] Forever in this sonic cabaret
                    [02:00.00] (Outro Fade)
                """.trimIndent()
            ),
            Song(
                id = 9002L,
                title = "Starry Horizons",
                artist = "Aura Bloom",
                album = "Cosmic Reverie",
                durationMs = 198000L,
                contentUri = "asset://ambient_sample.mp3",
                filePath = "/demo/starry_horizons.mp3",
                albumId = 102L,
                dateAdded = 1691000000L,
                year = 2024,
                playCount = 29,
                isFavorite = true,
                customArtworkUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                isSyncedLyrics = true,
                dominantColor = 0xFF104080,
                lyrics = """
                    [00:00.00] (Celestial chimes intro)
                    [00:07.80] Whispers in the solar wind
                    [00:13.00] Where the galaxies begin
                    [00:18.50] Floating past the rings of gold
                    [00:24.00] Stories that the stars foretold
                    [00:29.80] Stardust falling like soft snow
                    [00:35.40] Nowhere else we need to go
                    [00:41.20] Gravity has lost its hold
                    [00:47.00] Beautiful and pure and bold
                    [00:53.00] We cross the starry horizon tonight
                    [00:59.20] Bathed in warm celestial light
                    [01:05.00] Infinite love, infinite peace
                    [01:11.20] Where time and sorrow cease
                """.trimIndent()
            ),
            Song(
                id = 9003L,
                title = "Golden Hour Groove",
                artist = "Velvet Sunset",
                album = "Sundown Sessions",
                durationMs = 186000L,
                contentUri = "asset://funk_sample.mp3",
                filePath = "/demo/golden_hour_groove.mp3",
                albumId = 103L,
                dateAdded = 1692000000L,
                year = 2023,
                playCount = 58,
                isFavorite = false,
                customArtworkUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80",
                isSyncedLyrics = true,
                dominantColor = 0xFFD97706,
                lyrics = """
                    [00:00.00] (Funky Bassline Groove)
                    [00:06.50] Sun is dipping low in the west
                    [00:11.80] Put your heavy thoughts to rest
                    [00:16.90] Golden rays on the ocean tide
                    [00:22.10] Roll the windows down, let's ride
                    [00:27.40] Catch the rhythm, feel the breeze
                    [00:32.60] Swaying palm trees in ease
                    [00:38.00] Oh this golden hour groove is right
                    [00:43.20] Warming up the summer night
                """.trimIndent()
            ),
            Song(
                id = 9004L,
                title = "Lo-Fi Coffee Rain",
                artist = "Quiet Rain",
                album = "Study Chill Vol. 3",
                durationMs = 164000L,
                contentUri = "asset://lofi_sample.mp3",
                filePath = "/demo/lofi_coffee_rain.mp3",
                albumId = 104L,
                dateAdded = 1693000000L,
                year = 2024,
                playCount = 76,
                isFavorite = true,
                customArtworkUrl = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=600&auto=format&fit=crop&q=80",
                isSyncedLyrics = true,
                dominantColor = 0xFF4B5563,
                lyrics = """
                    [00:00.00] (Warm vinyl crackle & soft rain)
                    [00:08.00] Steam rises from the cup
                    [00:14.20] Cozy room, looking up
                    [00:20.50] Drops against the window pane
                    [00:27.10] Sweet melody in the rain
                    [00:33.40] Turn the page of your favorite book
                    [00:40.00] A peaceful world in every nook
                    [00:46.80] Slowing down the busy mind
                    [00:53.00] Quiet warmth is what we find
                """.trimIndent()
            ),
            Song(
                id = 9005L,
                title = "Bass Pulse Overdrive",
                artist = "Cyber Circuit",
                album = "Subwoofer Dreams",
                durationMs = 230000L,
                contentUri = "asset://bass_sample.mp3",
                filePath = "/demo/bass_pulse_overdrive.mp3",
                albumId = 105L,
                dateAdded = 1694000000L,
                year = 2024,
                playCount = 14,
                isFavorite = false,
                customArtworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                isSyncedLyrics = true,
                dominantColor = 0xFF059669,
                lyrics = """
                    [00:00.00] (Heavy sub-bass buildup)
                    [00:10.00] Feel the pressure in your chest
                    [00:15.50] Sound system put to the test
                    [00:21.00] Laser beams cut through the smoke
                    [00:26.50] Pure energy awoke
                    [00:32.00] Drop the frequency down below
                    [00:37.50] Watch the whole stadium glow
                """.trimIndent()
            )
        )
    }
}
