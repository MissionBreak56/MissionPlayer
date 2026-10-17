package com.missionbreak56.musicplayer.playback

import androidx.media3.common.Player
import com.missionbreak56.musicplayer.data.Prefs

/** Saves the queue and the current song / position so playback can be resumed next time. */
object PlaybackState {

    /** Saves the whole queue (song ids) and the current position. */
    fun saveQueue(player: Player) {
        val n = player.mediaItemCount
        if (n == 0) return
        Prefs.lastQueue = (0 until n).joinToString(",") { player.getMediaItemAt(it).mediaId }
        savePosition(player)
    }

    /** Saves only which queue entry is current and how far into it playback is. */
    fun savePosition(player: Player) {
        if (player.mediaItemCount == 0) return
        Prefs.lastIndex = player.currentMediaItemIndex
        Prefs.lastPosition = player.currentPosition.coerceAtLeast(0L)
    }
}
