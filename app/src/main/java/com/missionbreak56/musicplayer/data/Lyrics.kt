package com.missionbreak56.musicplayer.data

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

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

    fun raw(context: Context, songId: Long): String? {
        val f = file(context, songId)
        if (!f.exists()) return null
        return try { f.readText() } catch (_: Exception) { null }
    }

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
        LyricsIndex.invalidate(songId)
    }

    fun delete(context: Context, songId: Long) {
        file(context, songId).delete()
        LyricsIndex.invalidate(songId)
    }
}

/**
 * Reads lyrics stored inside the audio file itself (ID3 USLT, FLAC / Ogg Vorbis comments,
 * MP4 ©lyr) or in a `.lrc` file with the same name next to it. Best effort, never throws.
 */
object EmbeddedLyrics {

    private const val MAX_TAG = 6 * 1024 * 1024

    fun read(context: Context, song: Song): String? {
        val inline = try {
            context.contentResolver.openInputStream(song.uri)?.use { readFromStream(it) }
        } catch (_: Exception) {
            null
        }
        if (!inline.isNullOrBlank()) return inline
        return sidecar(song)
    }

    private fun sidecar(song: Song): String? {
        if (song.path.isBlank()) return null
        return try {
            val base = song.path.substringBeforeLast('.', song.path)
            listOf("$base.lrc", "$base.LRC", "$base.txt").map { File(it) }
                .firstOrNull { it.isFile && it.canRead() && it.length() < 1_000_000 }
                ?.readText()
        } catch (_: Exception) {
            null
        }
    }

    private fun readFromStream(input: InputStream): String? {
        val stream = BufferedInputStream(input, 64 * 1024)
        stream.mark(16)
        val head = ByteArray(10)
        val n = readFully(stream, head, 10)
        stream.reset()
        if (n < 8) return null
        return when {
            head[0] == 'I'.code.toByte() && head[1] == 'D'.code.toByte() && head[2] == '3'.code.toByte() ->
                id3(stream)
            head[0] == 'f'.code.toByte() && head[1] == 'L'.code.toByte() && head[2] == 'a'.code.toByte() ->
                flac(stream)
            head[0] == 'O'.code.toByte() && head[1] == 'g'.code.toByte() -> scanVorbis(stream)
            head[4] == 'f'.code.toByte() && head[5] == 't'.code.toByte() && head[6] == 'y'.code.toByte() ->
                mp4(stream)
            else -> null
        }
    }

    private fun readFully(s: InputStream, buf: ByteArray, len: Int): Int {
        var total = 0
        while (total < len) {
            val r = s.read(buf, total, len - total)
            if (r < 0) break
            total += r
        }
        return total
    }

    private fun readBytes(s: InputStream, len: Int): ByteArray? {
        if (len < 0 || len > MAX_TAG) return null
        val b = ByteArray(len)
        return if (readFully(s, b, len) == len) b else null
    }

    private fun synchsafe(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0x7F) shl 21) or ((b[off + 1].toInt() and 0x7F) shl 14) or
            ((b[off + 2].toInt() and 0x7F) shl 7) or (b[off + 3].toInt() and 0x7F)

    private fun be32(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 24) or ((b[off + 1].toInt() and 0xFF) shl 16) or
            ((b[off + 2].toInt() and 0xFF) shl 8) or (b[off + 3].toInt() and 0xFF)

    private fun le32(b: ByteArray, off: Int): Int =
        ((b[off + 3].toInt() and 0xFF) shl 24) or ((b[off + 2].toInt() and 0xFF) shl 16) or
            ((b[off + 1].toInt() and 0xFF) shl 8) or (b[off].toInt() and 0xFF)

    // ----- ID3v2 -----

    private fun id3(stream: InputStream): String? {
        val header = readBytes(stream, 10) ?: return null
        val version = header[3].toInt()
        val tagSize = synchsafe(header, 6)
        val tag = readBytes(stream, tagSize.coerceAtMost(MAX_TAG)) ?: return null
        var pos = 0
        if (header[5].toInt() and 0x40 != 0 && version >= 3 && tag.size >= 4) {
            // extended header
            val ext = if (version == 4) synchsafe(tag, 0) else be32(tag, 0) + 4
            pos = ext.coerceIn(0, tag.size)
        }
        val idLen = if (version == 2) 3 else 4
        val headLen = if (version == 2) 6 else 10
        while (pos + headLen <= tag.size) {
            if (tag[pos].toInt() == 0) break
            val id = String(tag, pos, idLen, Charsets.ISO_8859_1)
            val size = when {
                version == 2 -> ((tag[pos + 3].toInt() and 0xFF) shl 16) or
                    ((tag[pos + 4].toInt() and 0xFF) shl 8) or (tag[pos + 5].toInt() and 0xFF)
                version == 4 -> synchsafe(tag, pos + 4)
                else -> be32(tag, pos + 4)
            }
            val start = pos + headLen
            if (size < 0 || start + size > tag.size) break
            if (id == "USLT" || id == "ULT") {
                val text = decodeUslt(tag, start, size)
                if (!text.isNullOrBlank()) return text
            }
            pos = start + size
        }
        return null
    }

    private fun decodeUslt(b: ByteArray, start: Int, size: Int): String? {
        if (size < 5) return null
        val enc = b[start].toInt()
        var p = start + 4 // encoding + language
        val end = start + size
        val wide = enc == 1 || enc == 2
        // skip the content descriptor
        if (wide) {
            while (p + 1 < end && !(b[p].toInt() == 0 && b[p + 1].toInt() == 0)) p += 2
            p += 2
        } else {
            while (p < end && b[p].toInt() != 0) p++
            p += 1
        }
        if (p >= end) return null
        val charset: Charset = when (enc) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
        return String(b, p, end - p, charset).trim('\u0000', ' ', '﻿')
    }

    // ----- FLAC / Ogg Vorbis -----

    private fun flac(stream: InputStream): String? {
        readBytes(stream, 4) ?: return null
        while (true) {
            val h = readBytes(stream, 4) ?: return null
            val last = h[0].toInt() and 0x80 != 0
            val type = h[0].toInt() and 0x7F
            val len = ((h[1].toInt() and 0xFF) shl 16) or ((h[2].toInt() and 0xFF) shl 8) or
                (h[3].toInt() and 0xFF)
            if (type == 4) {
                val block = readBytes(stream, len) ?: return null
                return vorbisComments(block, 0)
            }
            if (!skipFully(stream, len.toLong())) return null
            if (last) return null
        }
    }

    private fun skipFully(s: InputStream, count: Long): Boolean {
        var left = count
        while (left > 0) {
            val skipped = s.skip(left)
            if (skipped <= 0) {
                if (s.read() < 0) return false
                left -= 1
            } else {
                left -= skipped
            }
        }
        return true
    }

    private fun vorbisComments(b: ByteArray, offset: Int): String? {
        var p = offset
        if (p + 4 > b.size) return null
        p += 4 + le32(b, p)
        if (p + 4 > b.size) return null
        val count = le32(b, p)
        p += 4
        for (i in 0 until count) {
            if (p + 4 > b.size) return null
            val len = le32(b, p)
            p += 4
            if (len < 0 || p + len > b.size) return null
            val entry = String(b, p, len, Charsets.UTF_8)
            p += len
            val eq = entry.indexOf('=')
            if (eq <= 0) continue
            val key = entry.substring(0, eq).uppercase()
            if (key == "LYRICS" || key == "UNSYNCEDLYRICS" || key == "SYNCEDLYRICS") {
                val v = entry.substring(eq + 1)
                if (v.isNotBlank()) return v
            }
        }
        return null
    }

    private fun scanVorbis(stream: InputStream): String? {
        val data = ByteArray(512 * 1024)
        val n = readFully(stream, data, data.size)
        val text = String(data, 0, n, Charsets.ISO_8859_1)
        for (marker in listOf("LYRICS=", "UNSYNCEDLYRICS=")) {
            val idx = text.indexOf(marker, ignoreCase = true)
            if (idx >= 4) {
                val len = le32(data, idx - 4)
                if (len in marker.length..(n - idx + marker.length)) {
                    val value = String(data, idx + marker.length, (len - marker.length).coerceAtMost(n - idx - marker.length), Charsets.UTF_8)
                    if (value.isNotBlank()) return value
                }
            }
        }
        return null
    }

    // ----- MP4 / M4A -----

    private fun mp4(stream: InputStream): String? {
        val data = ByteArray(4 * 1024 * 1024)
        val n = readFully(stream, data, data.size)
        val marker = byteArrayOf(0xA9.toByte(), 'l'.code.toByte(), 'y'.code.toByte(), 'r'.code.toByte())
        var i = 0
        while (i + 4 <= n) {
            if (data[i] == marker[0] && data[i + 1] == marker[1] && data[i + 2] == marker[2] &&
                data[i + 3] == marker[3]
            ) {
                // layout: [size]['©lyr'][size]['data'][type 4][locale 4][text]
                val dataAtom = i + 4
                if (dataAtom + 16 <= n) {
                    val size = be32(data, dataAtom)
                    val textStart = dataAtom + 16
                    val textEnd = (dataAtom + size).coerceAtMost(n)
                    if (size > 16 && textEnd > textStart) {
                        return String(data, textStart, textEnd - textStart, Charsets.UTF_8)
                    }
                }
            }
            i++
        }
        return null
    }
}

/**
 * In-memory index of lyrics text per song so the search can look inside lyrics.
 * Built lazily in the background; text read from files is cached on disk.
 */
object LyricsIndex {

    private val index = java.util.concurrent.ConcurrentHashMap<Long, String>()

    @Volatile
    private var indexing = false

    @Volatile
    var done = false
        private set

    private fun cacheFile(context: Context, id: Long): File =
        File(File(context.cacheDir, "lyrics_embedded"), "$id.txt")

    private fun plain(raw: String): String =
        LrcParser.parse(raw).joinToString("\n") { it.text }.lowercase()

    /** Lyrics text (original case) for a song: attached lyrics first, then the file itself. */
    fun fullText(context: Context, song: Song): String? {
        val own = LyricsStore.raw(context, song.id)
        if (!own.isNullOrBlank()) return own
        val cache = cacheFile(context, song.id)
        if (cache.exists()) {
            return try { cache.readText().ifBlank { null } } catch (_: Exception) { null }
        }
        val embedded = EmbeddedLyrics.read(context, song)
        try {
            cache.parentFile?.mkdirs()
            cache.writeText(embedded.orEmpty())
        } catch (_: Exception) {
            // cache is optional
        }
        return embedded?.ifBlank { null }
    }

    fun invalidate(id: Long) {
        index.remove(id)
    }

    /** Makes freshly downloaded lyrics searchable right away. */
    fun update(id: Long, raw: String) {
        index[id] = plain(raw)
    }

    fun matches(id: Long, query: String): Boolean = index[id]?.contains(query) == true

    /** First line of the lyrics containing [query], for showing under the song title. */
    fun snippet(id: Long, query: String): String? {
        val text = index[id] ?: return null
        val at = text.indexOf(query)
        if (at < 0) return null
        val start = text.lastIndexOf('\n', at).let { if (it < 0) 0 else it + 1 }
        val end = text.indexOf('\n', at).let { if (it < 0) text.length else it }
        return text.substring(start, end).trim()
    }

    /** Indexes every song once. [onProgress] is called on the calling (IO) thread. */
    fun build(context: Context, songs: List<Song>, onProgress: () -> Unit) {
        if (indexing) return
        indexing = true
        try {
            val app = context.applicationContext
            for ((n, song) in songs.withIndex()) {
                if (!index.containsKey(song.id)) {
                    val raw = try { fullText(app, song) } catch (_: Exception) { null }
                    index[song.id] = if (raw.isNullOrBlank()) "" else plain(raw)
                }
                if (n % 40 == 0) onProgress()
            }
            done = true
        } finally {
            indexing = false
            onProgress()
        }
    }
}
