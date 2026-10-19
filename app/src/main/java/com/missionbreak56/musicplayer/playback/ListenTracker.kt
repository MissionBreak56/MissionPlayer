package com.missionbreak56.musicplayer.playback

import androidx.media3.common.Player
import com.missionbreak56.musicplayer.data.Prefs

/**
 * Counts how long each song was really heard. Called once a second: while the player is
 * playing, the position moved since the last call is added to the current song. Seeking
 * (big jumps) and paused time are not counted.
 */
object ListenTracker {

    private var lastId = -1L
    private var lastPos = 0L
    private var ticks = 0
    private val pending = HashMap<Long, Long>()

    fun tick(player: Player) {
        if (!player.isPlaying) {
            lastId = -1L
            flush()
            return
        }
        val id = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val pos = player.currentPosition
        if (id == lastId) {
            val delta = pos - lastPos
            if (delta > 0L && delta <= 2500L) pending[id] = (pending[id] ?: 0L) + delta
        }
        lastId = id
        lastPos = pos
        ticks++
        if (ticks >= 5) {
            ticks = 0
            flush()
        }
    }

    /** Writes the time counted so far to storage. */
    fun flush() {
        if (pending.isEmpty()) return
        Prefs.addListened(pending)
        pending.clear()
    }

    fun stop() {
        lastId = -1L
        flush()
    }
}
