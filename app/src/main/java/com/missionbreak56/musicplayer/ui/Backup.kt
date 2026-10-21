package com.missionbreak56.musicplayer.ui

import android.content.Context
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup and restore of everything the user changed, as a single .zip file the user saves wherever
 * they want (the system file picker lets them choose the folder).
 *
 * The zip holds `backup.json` (all settings, plus a table describing every song the data refers to)
 * and the pictures / lyrics the user added. Songs are saved with their file path, title, artist and
 * length, because Android's internal song ids are different on another device or after the library
 * is rescanned. When restoring, each song is found again by path, or by title + artist + length.
 */
object Backup {

    private const val FORMAT = 1
    private const val JSON_NAME = "backup.json"
    private const val BG_IMAGE = "bg_image.jpg"
    private val ART_NAME = Regex("[0-9a-f]{40}\\.jpg")

    /** Playback state, not a user change: kept as it is on this device. */
    private val EXCLUDED = setOf("last_queue", "last_index", "last_position")

    private val ID_SET_KEYS = listOf("favorites", "hidden_songs")
    private const val K_PLAYLISTS = "playlists"
    private const val K_RECENT = "recent"
    private val K_PAIRS = listOf("play_counts", "listen_ms")

    class Summary(val favorites: Int, val playlists: Int, val files: Int)

    class RestoreResult(val matched: Int, val missing: Int)

    // ----- Backup -----

    suspend fun create(context: Context, out: OutputStream, appVersion: String): Summary =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            Library.load(app, force = false)
            val prefs = Prefs.allEntries().filterKeys { it !in EXCLUDED }
            val filesDir = app.filesDir

            val folderArt = File(filesDir, "folder_art").listFiles { f -> ART_NAME.matches(f.name) }.orEmpty()
            val songArt = File(filesDir, "song_art").listFiles { f -> f.name.endsWith(".jpg") }.orEmpty()
            val lyrics = File(filesDir, "lyrics").listFiles { f -> f.name.endsWith(".lrc") }.orEmpty()
            val bgImage = File(filesDir, BG_IMAGE).takeIf { it.exists() }

            // every song the saved data refers to, so it can be found again on restore
            val ids = referencedIds(prefs).toMutableSet()
            songArt.forEach { f -> f.nameWithoutExtension.toLongOrNull()?.let { ids.add(it) } }
            lyrics.forEach { f -> f.nameWithoutExtension.toLongOrNull()?.let { ids.add(it) } }
            val songs = JSONArray()
            val known = HashSet<Long>()
            ids.forEach { id ->
                val song = Library.song(id) ?: return@forEach
                known.add(id)
                songs.put(
                    JSONObject().put("id", id).put("path", song.path).put("title", song.title)
                        .put("artist", song.artist).put("dur", song.durationMs)
                )
            }

            val json = JSONObject()
                .put("app", "MissionPlayer")
                .put("format", FORMAT)
                .put("appVersion", appVersion)
                .put("createdAt", System.currentTimeMillis())
                .put("prefs", encodePrefs(prefs))
                .put("songs", songs)

            var files = 0
            ZipOutputStream(BufferedOutputStream(out)).use { zip ->
                zip.putNextEntry(ZipEntry(JSON_NAME))
                zip.write(json.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                fun add(name: String, file: File) {
                    zip.putNextEntry(ZipEntry(name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    files++
                }
                folderArt.forEach { add("folder_art/${it.name}", it) }
                songArt.filter { it.nameWithoutExtension.toLongOrNull()?.let { id -> id in known } == true }
                    .forEach { add("song_art/${it.name}", it) }
                lyrics.filter { it.nameWithoutExtension.toLongOrNull()?.let { id -> id in known } == true }
                    .forEach { add("lyrics/${it.name}", it) }
                if (bgImage != null) add(BG_IMAGE, bgImage)
            }

            Summary(
                favorites = (prefs["favorites"] as? Set<*>)?.size ?: 0,
                playlists = Prefs.playlists().size,
                files = files
            )
        }

    // ----- Restore -----

    /** Replaces the user's data with the backup. Throws [IOException] when the file is not a valid backup. */
    suspend fun restore(context: Context, input: InputStream): RestoreResult =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            Library.load(app, force = false)
            val tmp = File(app.cacheDir, "restore_tmp")
            tmp.deleteRecursively()
            tmp.mkdirs()
            try {
                var json: JSONObject? = null
                var idMap: Map<Long, Long> = emptyMap()
                val zip = ZipInputStream(BufferedInputStream(input))
                try {
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (name == JSON_NAME) {
                            val parsed = try {
                                JSONObject(String(zip.readBytes(), Charsets.UTF_8))
                            } catch (e: Exception) {
                                throw IOException("invalid backup", e)
                            }
                            if (parsed.optString("app") != "MissionPlayer" || parsed.optInt("format", 0) != FORMAT) {
                                throw IOException("not a MissionPlayer backup")
                            }
                            json = parsed
                            idMap = buildIdMap(parsed)
                        } else if (json != null && !entry.isDirectory) {
                            val target = targetFor(tmp, name, idMap)
                            if (target != null) {
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { zip.copyTo(it) }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                } finally {
                    zip.close()
                }
                val data = json ?: throw IOException("backup.json not found")

                // settings: remap song ids, keep this device's playback state
                val missing = HashSet<Long>()
                val restored = remapPrefs(decodePrefs(data.getJSONArray("prefs")), idMap, missing)
                val merged = LinkedHashMap<String, Any?>(restored)
                Prefs.allEntries().forEach { (key, value) -> if (key in EXCLUDED) merged[key] = value }
                Prefs.replaceAll(merged)

                // pictures and lyrics: swap the old ones for the restored ones
                val filesDir = app.filesDir
                for (dir in listOf("folder_art", "song_art", "lyrics")) {
                    File(filesDir, dir).deleteRecursively()
                    val src = File(tmp, dir)
                    if (src.exists()) src.copyRecursively(File(filesDir, dir), overwrite = true)
                }
                val bgDst = File(filesDir, BG_IMAGE)
                bgDst.delete()
                File(tmp, BG_IMAGE).takeIf { it.exists() }?.copyTo(bgDst, overwrite = true)
                SongArt.reset()

                RestoreResult(matched = idMap.size, missing = missing.size)
            } finally {
                tmp.deleteRecursively()
            }
        }

    /** Where an entry of the zip goes (null = ignore it). Only known names are accepted. */
    private fun targetFor(tmp: File, name: String, idMap: Map<Long, Long>): File? = when {
        name == BG_IMAGE -> File(tmp, BG_IMAGE)
        name.startsWith("folder_art/") -> {
            val file = name.removePrefix("folder_art/")
            if (ART_NAME.matches(file)) File(tmp, "folder_art/$file") else null
        }
        name.startsWith("song_art/") && name.endsWith(".jpg") -> {
            val old = name.removePrefix("song_art/").removeSuffix(".jpg").toLongOrNull()
            val new = old?.let { idMap[it] }
            if (new != null) File(tmp, "song_art/$new.jpg") else null
        }
        name.startsWith("lyrics/") && name.endsWith(".lrc") -> {
            val old = name.removePrefix("lyrics/").removeSuffix(".lrc").toLongOrNull()
            val new = old?.let { idMap[it] }
            if (new != null) File(tmp, "lyrics/$new.lrc") else null
        }
        else -> null
    }

    // ----- Finding the songs again -----

    private fun meta(title: String, artist: String, durationMs: Long) =
        "${title.trim().lowercase()}|${artist.trim().lowercase()}|${durationMs / 1000}"

    /** Old song id -> id of the same song on this device (songs that cannot be found are left out). */
    private fun buildIdMap(json: JSONObject): Map<Long, Long> {
        val all: List<Song> = Library.allSongs
        val byPath = HashMap<String, Song>(all.size * 2)
        val byMeta = HashMap<String, Song>(all.size * 2)
        all.forEach { song ->
            byPath[song.path] = song
            byMeta.putIfAbsent(meta(song.title, song.artist, song.durationMs), song)
        }
        val map = HashMap<Long, Long>()
        val songs = json.optJSONArray("songs") ?: return map
        for (i in 0 until songs.length()) {
            val o = songs.getJSONObject(i)
            val found = byPath[o.optString("path")]
                ?: byMeta[meta(o.optString("title"), o.optString("artist"), o.optLong("dur"))]
            if (found != null) map[o.getLong("id")] = found.id
        }
        return map
    }

    // ----- Settings <-> JSON -----

    private fun encodePrefs(prefs: Map<String, Any?>): JSONArray {
        val arr = JSONArray()
        prefs.forEach { (key, value) ->
            val o = JSONObject().put("k", key)
            when (value) {
                is Boolean -> o.put("t", "b").put("v", value)
                is Int -> o.put("t", "i").put("v", value)
                is Long -> o.put("t", "l").put("v", value)
                is Float -> o.put("t", "f").put("v", value.toDouble())
                is String -> o.put("t", "s").put("v", value)
                is Set<*> -> o.put("t", "ss").put("v", JSONArray(value.filterIsInstance<String>()))
                else -> return@forEach
            }
            arr.put(o)
        }
        return arr
    }

    private fun decodePrefs(arr: JSONArray): Map<String, Any?> {
        val map = LinkedHashMap<String, Any?>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val key = o.getString("k")
            map[key] = when (o.getString("t")) {
                "b" -> o.getBoolean("v")
                "i" -> o.getInt("v")
                "l" -> o.getLong("v")
                "f" -> o.getDouble("v").toFloat()
                "s" -> o.getString("v")
                "ss" -> o.getJSONArray("v").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                else -> continue
            }
        }
        return map
    }

    // ----- Song ids inside the settings -----

    private fun parsePlaylists(raw: String): List<Pair<String, List<Long>>> =
        raw.split("\n").filter { it.isNotBlank() }.map { line ->
            val parts = line.split("|")
            parts.getOrElse(0) { "" } to
                (parts.getOrNull(1)?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList())
        }

    private fun parsePairs(raw: String): Map<Long, Long> {
        val map = LinkedHashMap<Long, Long>()
        raw.split(",").forEach { entry ->
            val parts = entry.split(":")
            val id = parts.getOrNull(0)?.trim()?.toLongOrNull()
            val value = parts.getOrNull(1)?.trim()?.toLongOrNull()
            if (id != null && value != null) map[id] = value
        }
        return map
    }

    private fun parseIds(raw: String): List<Long> = raw.split(",").mapNotNull { it.trim().toLongOrNull() }

    private fun referencedIds(prefs: Map<String, Any?>): Set<Long> {
        val ids = LinkedHashSet<Long>()
        ID_SET_KEYS.forEach { key ->
            (prefs[key] as? Set<*>)?.forEach { v -> (v as? String)?.toLongOrNull()?.let { ids.add(it) } }
        }
        (prefs[K_PLAYLISTS] as? String)?.let { raw -> parsePlaylists(raw).forEach { ids.addAll(it.second) } }
        K_PAIRS.forEach { key -> (prefs[key] as? String)?.let { ids.addAll(parsePairs(it).keys) } }
        (prefs[K_RECENT] as? String)?.let { ids.addAll(parseIds(it)) }
        return ids
    }

    private fun remapPrefs(
        prefs: Map<String, Any?>,
        idMap: Map<Long, Long>,
        missing: MutableSet<Long>
    ): Map<String, Any?> {
        fun map(id: Long): Long? = idMap[id].also { if (it == null) missing.add(id) }
        val out = LinkedHashMap<String, Any?>(prefs)

        ID_SET_KEYS.forEach { key ->
            (prefs[key] as? Set<*>)?.let { set ->
                out[key] = set.mapNotNull { (it as? String)?.toLongOrNull() }
                    .mapNotNull { map(it) }.map { it.toString() }.toSet()
            }
        }
        (prefs[K_PLAYLISTS] as? String)?.let { raw ->
            out[K_PLAYLISTS] = parsePlaylists(raw).joinToString("\n") { (name, ids) ->
                name + "|" + ids.mapNotNull { map(it) }.distinct().joinToString(",")
            }
        }
        K_PAIRS.forEach { key ->
            (prefs[key] as? String)?.let { raw ->
                val merged = LinkedHashMap<Long, Long>()
                parsePairs(raw).forEach { (id, value) ->
                    map(id)?.let { newId -> merged[newId] = (merged[newId] ?: 0L) + value }
                }
                out[key] = merged.entries.joinToString(",") { "${it.key}:${it.value}" }
            }
        }
        (prefs[K_RECENT] as? String)?.let { raw ->
            out[K_RECENT] = parseIds(raw).mapNotNull { map(it) }.distinct().joinToString(",")
        }
        return out
    }
}
