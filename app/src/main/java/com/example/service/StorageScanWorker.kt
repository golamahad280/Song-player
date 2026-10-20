package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.SongDatabase
import com.example.data.local.SongEntity
import com.example.data.scanner.AudioScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val context = applicationContext
            val database = SongDatabase.getInstance(context)
            val songDao = database.songDao()

            val existingSongs = songDao.getAllSongsList()
            val existingIds = existingSongs.map { it.id }.toSet()
            val existingPaths = existingSongs.map { it.filePath }.toSet()

            val scannedSongs = AudioScanner.scanLocalAudio(context)
            val newSongs = scannedSongs.filter { song ->
                !existingIds.contains(song.id) && (song.filePath.isBlank() || !existingPaths.contains(song.filePath))
            }

            if (newSongs.isNotEmpty()) {
                val entities = newSongs.map { song ->
                    SongEntity(
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        album = song.album,
                        durationMs = song.durationMs,
                        contentUri = song.contentUri,
                        filePath = song.filePath,
                        albumId = song.albumId,
                        dateAdded = song.dateAdded,
                        year = song.year,
                        playCount = 0,
                        isFavorite = false,
                        customArtworkUrl = song.customArtworkUrl,
                        dominantColor = song.dominantColor
                    )
                }
                songDao.insertOrUpdateSongs(entities)

                // Trigger subtle local notification
                showNotification(context, newSongs.size)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(context: Context, count: Int) {
        val channelId = "storage_scan_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Library Auto-Scan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when new audio files are automatically added"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle("Song Player Library Updated")
            .setContentText("$count new ${if (count == 1) "song was" else "songs were"} added to your library.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2024, notification)
        } catch (_: SecurityException) {}
    }
}
