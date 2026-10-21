package com.missionbreak56.musicplayer.data

import android.content.Context

/** Ties the pieces together: reads a song's lyric, measures its audio and moves the lyric if it is off. */
object LyricSync {

    /** Analyse this much of the beginning of the song (the first lines are enough). */
    private const val ANALYSE_MS = 160_000L

    /**
     * Looks for a sure correction of the timing of the lyric of [song]. Returns the shift that was
     * applied in ms (positive = lines now come later), or null when nothing was changed (no synced
     * lyric, audio that cannot be read, or no clear answer). Blocking, call from a background thread.
     */
    fun autoAdjust(context: Context, song: Song): Long? {
        val app = context.applicationContext
        Prefs.markLyricSyncChecked(song.id)
        val raw = LyricsIndex.fullText(app, song) ?: return null
        val lines = LrcParser.parse(raw)
        if (lines.isEmpty() || lines.first().timeMs < 0) return null
        val window = if (song.durationMs > 0) song.durationMs.coerceAtMost(ANALYSE_MS) else ANALYSE_MS
        val envelope = AudioEnvelope.compute(app, song.uri, window) ?: return null
        val result = LyricAligner.estimate(envelope, lines) ?: return null
        LyricsStore.shiftBy(app, song.id, raw, result.shiftMs)
        return result.shiftMs
    }
}
