package com.missionbreak56.musicplayer.ui

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Song
import com.missionbreak56.musicplayer.playback.PlaybackQueue
import com.missionbreak56.musicplayer.playback.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Brings the rest of the app up to date after a song's tags or cover were rewritten. */
object SongRefresh {

    suspend fun afterSave(context: Context, song: Song, coverChanged: Boolean) {
        val app = context.applicationContext
        withContext(Dispatchers.IO) {
            // A custom cover kept by the app would still hide the cover now stored in the file.
            if (coverChanged) SongArt.remove(app, song.id)
            rescan(app, song.path)
        }
        withContext(Dispatchers.Main) { ArtLoader.invalidate(song) }
        Library.load(app, force = true)
        withContext(Dispatchers.Main) {
            Library.song(song.id)?.let { ArtLoader.invalidate(it) }
            refreshPlayer(app, song.id)
        }
    }

    /** Asks MediaStore to read the file again, and waits (a few seconds at most) for it. */
    private suspend fun rescan(context: Context, path: String) {
        if (path.isBlank()) return
        withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine<Unit> { cont ->
                MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ ->
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    private fun refreshPlayer(context: Context, songId: Long) {
        val holder = arrayOfNulls<PlayerConnection>(1)
        val connection = PlayerConnection(context) { controller ->
            controller.sendCustomCommand(PlaybackQueue.REFRESH_SONG, Bundle().apply { putLong("id", songId) })
            Handler(Looper.getMainLooper()).postDelayed({ holder[0]?.release() }, 2_000L)
        }
        holder[0] = connection
        connection.connect()
    }
}
