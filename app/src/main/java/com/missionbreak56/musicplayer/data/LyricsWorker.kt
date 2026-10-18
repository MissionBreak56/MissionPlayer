package com.missionbreak56.musicplayer.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Looks up the lyrics of every song that has none, whenever the phone is online, and stores them
 * on the device. Runs by itself in the background: nothing for the user to do.
 */
class LyricsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        Prefs.init(app)
        if (!Prefs.autoLyrics) return Result.success()

        val songs = try { MusicRepository.loadSongs(app) } catch (_: Exception) { emptyList() }
        if (songs.isEmpty()) return Result.success()

        val deadline = System.currentTimeMillis() + RUN_BUDGET_MS
        var errorsInARow = 0

        // No pause between songs: several are searched at the same time instead.
        for (batch in songs.chunked(PARALLEL)) {
            if (isStopped) return Result.retry()
            if (!Prefs.autoLyrics) return Result.success()
            if (System.currentTimeMillis() > deadline) return Result.retry() // continue right after

            val outcomes = coroutineScope {
                batch.map { song -> async(Dispatchers.IO) { process(app, song) } }.awaitAll()
            }.filterNotNull()
            if (outcomes.isEmpty()) continue

            if (outcomes.any { it == LyricsFetcher.Outcome.RATE_LIMITED }) return Result.retry()
            if (outcomes.all { it == LyricsFetcher.Outcome.ERROR }) {
                errorsInARow += outcomes.size
                if (errorsInARow >= 8) return Result.retry() // probably offline again
            } else {
                errorsInARow = 0
            }
        }
        return Result.success()
    }

    /** Null when the song needs nothing (it already has lyrics or was searched a moment ago). */
    private fun process(app: Context, song: Song): LyricsFetcher.Outcome? {
        if (LyricsStore.has(app, song.id) || !LyricsFetcher.shouldTry(app, song.id)) return null
        // Lyrics inside the file or in a .lrc next to it already work offline.
        if (!LyricsIndex.fullText(app, song).isNullOrBlank()) return null
        return LyricsFetcher.fetch(app, song)
    }

    companion object {
        private const val PERIODIC = "auto_lyrics_periodic"
        private const val NOW = "auto_lyrics_now"
        private const val PARALLEL = 4
        private const val RUN_BUDGET_MS = 8 * 60_000L // WorkManager stops jobs after ~10 min

        private val online = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Starts one search now (as soon as there is internet) and repeats it every 15 minutes,
         * the shortest interval Android allows for background jobs. Retries wait 10 seconds,
         * also the shortest allowed.
         */
        fun schedule(context: Context) {
            val wm = WorkManager.getInstance(context.applicationContext)
            wm.enqueueUniqueWork(
                NOW, ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<LyricsWorker>()
                    .setConstraints(online)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .build()
            )
            wm.enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<LyricsWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(online)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .build()
            )
        }

        fun cancel(context: Context) {
            val wm = WorkManager.getInstance(context.applicationContext)
            wm.cancelUniqueWork(NOW)
            wm.cancelUniqueWork(PERIODIC)
        }
    }
}
