package com.missionbreak56.musicplayer.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.data.Song

/** Saves the queue and the current song / position so playback can be resumed next time. */
object PlaybackState {

    /** Saves the whole queue (song ids) and the current position. */
    fun saveQueue(player: Player) {
        val n = player.mediaItemCount
        if (n == 0) return
        Prefs.lastQueue = (0 until n).joinToString(",") { player.getMediaItemAt(it).mediaId }
        savePosition(player)
    }

    /**
     * Puts the saved queue back (paused, at the saved position) when the player is empty,
     * e.g. after the system stopped the playback service. Returns true if a restore was requested.
     */
    fun restoreIfEmpty(c: MediaController): Boolean {
        if (c.mediaItemCount > 0 || !Library.loaded) return false
        val ids = Prefs.lastQueueIds()
        if (ids.isEmpty()) return false
        val saved = Prefs.lastIndex.coerceIn(0, ids.lastIndex)
        val songs = ArrayList<Song>()
        var index = 0
        var alive = false
        ids.forEachIndexed { i, id ->
            val song = Library.song(id)
            if (i == saved) {
                index = songs.size
                alive = song != null
            }
            if (song != null) songs.add(song)
        }
        if (songs.isEmpty()) return false
        PlaybackQueue.restore(c, songs, index.coerceAtMost(songs.lastIndex), if (alive) Prefs.lastPosition else 0L)
        return true
    }

    /** Saves only which queue entry is current and how far into it playback is. */
    fun savePosition(player: Player) {
        if (player.mediaItemCount == 0) return
        Prefs.lastIndex = player.currentMediaItemIndex
        Prefs.lastPosition = player.currentPosition.coerceAtLeast(0L)
    }
}
