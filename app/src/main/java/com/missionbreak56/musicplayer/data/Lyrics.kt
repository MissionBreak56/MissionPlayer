package com.missionbreak56.musicplayer.data

import android.content.Context
import java.io.File

data class LyricLine(val timeMs: Long, val text: String)

/** Parses `.lrc` files. Plain text is returned with timestamps of -1 (not synced). */
object LrcParser {

    private val tag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    fun parse(raw: String): List<LyricLine> {
        val synced = ArrayList<LyricLine>()
        val plain = ArrayList<LyricLine>()
        for (rawLine in raw.lineSequence()) {
            val line = rawLine.trim().trimStart('\uFEFF')
            if (line.isEmpty()) continue
            val matches = tag.findAll(line).toList()
            if (matches.isEmpty()) {
                // [ar:Artist], [ti:Title] ... are metadata, not lyrics
                if (line.startsWith("[") && line.contains(":") && line.endsWith("]")) continue
                plain += LyricLine(-1L, line)
                continue
            }
            val text = line.replace(tag, "").trim()
            for (m in matches) {
                val minutes = m.groupValues[1].toLong()
                val seconds = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val millis = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.take(3).toLong()
                }
                synced += LyricLine(minutes * 60_000 + seconds * 1_000 + millis, text)
            }
        }
        return if (synced.isNotEmpty()) synced.sortedBy { it.timeMs } else plain
    }
}

/** Lyrics the user attached to a song, kept in the app's private storage. */
object LyricsStore {

    private fun file(context: Context, songId: Long): File =
        File(File(context.filesDir, "lyrics"), "$songId.lrc")

    fun has(context: Context, songId: Long): Boolean = file(context, songId).exists()

    fun load(context: Context, songId: Long): List<LyricLine> {
        val f = file(context, songId)
        if (!f.exists()) return emptyList()
        return try {
            LrcParser.parse(f.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, songId: Long, text: String) {
        val f = file(context, songId)
        f.parentFile?.mkdirs()
        f.writeText(text)
    }

    fun delete(context: Context, songId: Long) {
        file(context, songId).delete()
    }
}
