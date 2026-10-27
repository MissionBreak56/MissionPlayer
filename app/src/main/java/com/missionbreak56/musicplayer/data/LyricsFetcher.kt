package com.missionbreak56.musicplayer.data

import android.content.Context
import android.util.Base64
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject

// ----- Intervals (all at their practical minimum) -----
private const val RETRY_MISS_AFTER_MS = 15L * 60 * 1000 // same cadence as the background job
private const val COOLDOWN_MS = 15_000L                 // pause for a source that answered 429
private const val FAILS_BEFORE_COOLDOWN = 3
private const val CONNECT_TIMEOUT_MS = 5_000
private const val READ_TIMEOUT_MS = 8_000
private const val DURATION_TOLERANCE_MS = 3_500L

private const val UA = "MissionPlayer/2.7 (https://github.com/MissionBreak56/MissionPlayer)"
private const val PREF = "lyrics_fetch"

private val syncedTag = Regex("""\[\d{1,3}:\d{2}""")


private class Http(val code: Int, val body: String)
private class RateLimited : Exception()

private abstract class Provider(val name: String) {
    /** Lyrics text, or null when this source does not have the song. May throw. */
    abstract fun find(song: Song): String?
}


private val coolUntil = ConcurrentHashMap<String, Long>()
private val fails = ConcurrentHashMap<String, Int>()


// ----- Shared helpers -----

private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

/** "Song (feat. X) [Remastered]" -> "Song" */
private fun cleanTitle(title: String): String =
    title.replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "")
        .replace(Regex("""\s+-\s+(remaster(ed)?|live|version|mono|stereo).*""", RegexOption.IGNORE_CASE), "")
        .trim()
        .ifBlank { title }

private fun norm(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), "")

private fun similar(a: String, b: String): Boolean {
    val x = norm(a); val y = norm(b)
    if (x.isEmpty() || y.isEmpty()) return false
    return x == y || x.contains(y) || y.contains(x)
}

private fun get(url: String, headers: Map<String, String> = emptyMap()): Http {
    val c = URL(url).openConnection() as HttpURLConnection
    try {
        c.connectTimeout = CONNECT_TIMEOUT_MS
        c.readTimeout = READ_TIMEOUT_MS
        c.requestMethod = "GET"
        c.setRequestProperty("User-Agent", UA)
        c.setRequestProperty("Accept", "application/json")
        for ((k, v) in headers) c.setRequestProperty(k, v)
        val code = c.responseCode
        if (code == 429) throw RateLimited()
        if (code >= 500 || code == 403) throw IOException("HTTP $code")
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        return Http(code, body)
    } finally {
        c.disconnect()
    }
}

private fun String.orNullIfBlank(): String? =
    if (isBlank() || this == "null") null else this

private val timeTag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

/** False when synced lyrics run much longer than the song, which means they belong to another one. */
private fun fitsDuration(lrc: String, song: Song): Boolean {
    if (song.durationMs <= 0) return true
    var last = 0L
    for (m in timeTag.findAll(lrc)) {
        val ms = m.groupValues[1].toLong() * 60_000 + m.groupValues[2].toLong() * 1000
        if (ms > last) last = ms
    }
    return last <= song.durationMs + 20_000L
}

private fun lrcTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L)
    return "[%02d:%02d.%02d]".format(total / 60_000, (total / 1000) % 60, (total % 1000) / 10)
}

/** &#58; and friends -> the real characters (QQ Music sends its lyrics HTML-escaped). */
private fun unescapeHtml(s: String): String =
    s.replace(Regex("&#(\\d+);")) { m -> m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value }
        .replace("&apos;", "'").replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&amp;", "&")

/** Some endpoints wrap their JSON in a callback, e.g. MusicJsonCallback({...}). */
private fun jsonText(body: String): String {
    val t = body.trim()
    return if (t.startsWith("{") || t.startsWith("[")) t else t.substringAfter('(').substringBeforeLast(')')
}


// ----- 1. LRCLIB -----

private object LrcLib : Provider("lrclib") {
    private const val BASE = "https://lrclib.net/api"

    override fun find(song: Song): String? {
        val seconds = Math.round(song.durationMs / 1000.0).toInt()
        // Exact match (needs the album).
        if (song.artist.isNotBlank() && song.album.isNotBlank() && seconds > 0) {
            val r = get(
                "$BASE/get?track_name=${enc(song.title)}&artist_name=${enc(song.artist)}" +
                    "&album_name=${enc(song.album)}&duration=$seconds"
            )
            if (r.code == 200) extract(JSONObject(r.body))?.let { return it }
        }
        val title = cleanTitle(song.title)
        val queries = ArrayList<String>()
        if (song.artist.isNotBlank()) {
            queries += "$BASE/search?track_name=${enc(song.title)}&artist_name=${enc(song.artist)}"
            if (title != song.title) {
                queries += "$BASE/search?track_name=${enc(title)}&artist_name=${enc(song.artist)}"
            }
            queries += "$BASE/search?q=${enc(song.artist + " " + title)}"
        } else {
            queries += "$BASE/search?track_name=${enc(title)}"
        }
        for (q in queries) {
            val r = get(q)
            if (r.code != 200) continue
            pick(JSONArray(r.body), song)?.let { return it }
        }
        return null
    }

    private fun pick(results: JSONArray, song: Song): String? {
        var best: JSONObject? = null
        var bestScore = Long.MAX_VALUE
        for (i in 0 until results.length()) {
            val o = results.optJSONObject(i) ?: continue
            if (extract(o) == null) continue
            var score = 0L
            if (song.durationMs > 0 && o.has("duration") && !o.isNull("duration")) {
                val diff = Math.abs((o.optDouble("duration") * 1000).toLong() - song.durationMs)
                if (diff > DURATION_TOLERANCE_MS) continue
                score += diff
            } else if (song.artist.isBlank()) {
                continue // no artist and no duration to compare: too risky
            }
            if (o.optString("syncedLyrics").isBlank()) score += 100_000 // prefer synced
            if (score < bestScore) { bestScore = score; best = o }
        }
        return best?.let { extract(it) }
    }

    private fun extract(o: JSONObject): String? {
        if (o.optBoolean("instrumental", false)) return null
        o.optString("syncedLyrics", "").orNullIfBlank()?.let { return it }
        return o.optString("plainLyrics", "").orNullIfBlank()
    }
}

// ----- 2. NetEase Cloud Music -----

private object NetEase : Provider("netease") {
    private val ref = mapOf("Referer" to "https://music.163.com/")
    private val credits = Regex("""^\[[^\]]*]\s*(作词|作曲|编曲|制作人?|混音|母带|词|曲|Lyrics by|Composed by)\s*[:：].*$""", RegexOption.IGNORE_CASE)

    override fun find(song: Song): String? {
        val title = cleanTitle(song.title)
        val keyword = "${song.artist} $title".trim()
        val r = get("https://music.163.com/api/search/get?s=${enc(keyword)}&type=1&limit=5", ref)
        if (r.code != 200) return null
        val list = JSONObject(r.body).optJSONObject("result")?.optJSONArray("songs") ?: return null

        var chosen = -1L
        var bestDiff = Long.MAX_VALUE
        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            val titleOk = similar(o.optString("name"), title)
            val artists = o.optJSONArray("artists")
            var artistOk = song.artist.isBlank()
            if (artists != null) for (k in 0 until artists.length()) {
                if (similar(artists.optJSONObject(k)?.optString("name").orEmpty(), song.artist)) artistOk = true
            }
            val dur = o.optLong("duration", o.optLong("dt", 0))
            if (song.durationMs > 0 && dur > 0) {
                val diff = Math.abs(dur - song.durationMs)
                if (diff <= DURATION_TOLERANCE_MS && (titleOk || artistOk) && diff < bestDiff) {
                    bestDiff = diff; chosen = o.optLong("id", -1)
                }
            } else if (titleOk && artistOk && chosen < 0) {
                chosen = o.optLong("id", -1)
            }
        }
        if (chosen <= 0) return null

        val l = get("https://music.163.com/api/song/lyric?id=$chosen&lv=1&kv=1&tv=-1", ref)
        if (l.code != 200) return null
        val text = JSONObject(l.body).optJSONObject("lrc")?.optString("lyric", "").orEmpty()
        if (text.isBlank()) return null
        return text.lineSequence().filterNot { credits.matches(it.trim()) }.joinToString("\n")
            .orNullIfBlank()
    }
}

// ----- 3. Kugou -----

private object Kugou : Provider("kugou") {
    override fun find(song: Song): String? {
        val title = cleanTitle(song.title)
        val keyword = if (song.artist.isBlank()) title else "${song.artist} - $title"
        val r = get(
            "https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=${enc(keyword)}" +
                "&duration=${song.durationMs}&hash="
        )
        if (r.code != 200) return null
        val list = JSONObject(r.body).optJSONArray("candidates") ?: return null

        var id = ""
        var key = ""
        var bestDiff = Long.MAX_VALUE
        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            val dur = o.optLong("duration", 0)
            val diff = if (song.durationMs > 0 && dur > 0) Math.abs(dur - song.durationMs) else 0L
            if (song.durationMs > 0 && dur > 0 && diff > DURATION_TOLERANCE_MS) continue
            if (!similar(o.optString("song"), title) && !similar(o.optString("singer"), song.artist)) continue
            if (diff < bestDiff) {
                bestDiff = diff
                id = o.optString("id"); key = o.optString("accesskey")
            }
        }
        if (id.isBlank() || key.isBlank()) return null

        val d = get("https://lyrics.kugou.com/download?ver=1&client=pc&id=$id&accesskey=$key&fmt=lrc&charset=utf8")
        if (d.code != 200) return null
        val content = JSONObject(d.body).optString("content", "")
        if (content.isBlank()) return null
        val text = String(Base64.decode(content, Base64.DEFAULT), Charsets.UTF_8).trimStart('\uFEFF')
        return text.orNullIfBlank()
    }
}

// ----- 4. lyrics.ovh (plain text only) -----

private object LyricsOvh : Provider("lyricsovh") {
    override fun find(song: Song): String? {
        if (song.artist.isBlank()) return null
        val titles = linkedSetOf(song.title, cleanTitle(song.title))
        for (t in titles) {
            val r = get("https://api.lyrics.ovh/v1/${enc(song.artist)}/${enc(t)}")
            if (r.code != 200) continue
            var text = JSONObject(r.body).optString("lyrics", "").replace("\r\n", "\n").trim()
            if (text.startsWith("Paroles de la chanson", ignoreCase = true)) {
                text = text.substringAfter('\n', "").trim()
            }
            if (text.isNotBlank()) return text
        }
        return null
    }
}


// ----- 5. QQ Music (synced) -----

private object QQMusic : Provider("qqmusic") {
    private val ref = mapOf("Referer" to "https://y.qq.com/")

    override fun find(song: Song): String? {
        val title = cleanTitle(song.title)
        val keyword = "${song.artist} $title".trim()
        val r = get(
            "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?ct=24&qqmusic_ver=1298&new_json=1" +
                "&remoteplace=txt.yqq.center&t=0&aggr=1&cr=1&catZhida=1&lossless=0&flag_qc=0&p=1&n=5" +
                "&w=${enc(keyword)}&g_tk=5381&loginUin=0&hostUin=0&format=json&inCharset=utf8" +
                "&outCharset=utf-8&notice=0&platform=yqq.json&needNewCode=0",
            ref
        )
        if (r.code != 200) return null
        val list = JSONObject(jsonText(r.body)).optJSONObject("data")?.optJSONObject("song")
            ?.optJSONArray("list") ?: return null

        var mid = ""
        var bestDiff = Long.MAX_VALUE
        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            val titleOk = similar(o.optString("name"), title) || similar(o.optString("title"), title)
            val singers = o.optJSONArray("singer")
            var artistOk = song.artist.isBlank()
            if (singers != null) for (k in 0 until singers.length()) {
                if (similar(singers.optJSONObject(k)?.optString("name").orEmpty(), song.artist)) artistOk = true
            }
            val dur = o.optLong("interval", 0) * 1000
            val diff = if (song.durationMs > 0 && dur > 0) Math.abs(dur - song.durationMs) else 0L
            if (song.durationMs > 0 && dur > 0 && diff > DURATION_TOLERANCE_MS) continue
            if (!titleOk || !artistOk) continue
            if (diff < bestDiff) {
                bestDiff = diff
                mid = o.optString("mid")
            }
        }
        if (mid.isBlank()) return null

        val l = get(
            "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=$mid&g_tk=5381" +
                "&format=json&inCharset=utf8&outCharset=utf-8&nobase64=1",
            ref
        )
        if (l.code != 200) return null
        val text = JSONObject(jsonText(l.body)).optString("lyric", "")
        return unescapeHtml(text).replace("\r\n", "\n").trim().orNullIfBlank()
    }
}

// ----- 6. LrcApi (api.lrc.cx, synced, community service) -----

private object LrcApi : Provider("lrcapi") {
    override fun find(song: Song): String? {
        if (song.artist.isBlank()) return null // too fuzzy without the artist
        val title = cleanTitle(song.title)
        val album = if (song.album.isBlank()) "" else "&album=${enc(song.album)}"
        val r = get(
            "https://api.lrc.cx/lyrics?title=${enc(title)}&artist=${enc(song.artist)}$album",
            mapOf("Accept" to "text/plain")
        )
        if (r.code != 200) return null
        val text = r.body.trimStart('\uFEFF').trim()
        if (text.startsWith("{") || text.startsWith("<")) return null
        if (!syncedTag.containsMatchIn(text) || !fitsDuration(text, song)) return null
        return text
    }
}

// ----- 7. Textyl (synced, community service) -----

private object Textyl : Provider("textyl") {
    override fun find(song: Song): String? {
        if (song.artist.isBlank()) return null
        val r = get("https://api.textyl.co/api/lyrics?q=${enc(song.artist + " " + cleanTitle(song.title))}")
        if (r.code != 200) return null
        val arr = try {
            JSONArray(r.body)
        } catch (_: Exception) {
            return null
        }
        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val line = o.optString("lyrics", "").trim()
            sb.append(lrcTime((o.optDouble("seconds", 0.0) * 1000).toLong())).append(line).append('\n')
        }
        val text = sb.toString().trim()
        if (text.isEmpty() || !fitsDuration(text, song)) return null
        return text
    }
}

// ----- 8. Lyrist (plain text, community service backed by Genius) -----

private object Lyrist : Provider("lyrist") {
    override fun find(song: Song): String? {
        if (song.artist.isBlank()) return null
        val title = cleanTitle(song.title)
        val r = get("https://lyrist.vercel.app/api/${enc(title)}/${enc(song.artist)}")
        if (r.code != 200) return null
        val o = try {
            JSONObject(r.body)
        } catch (_: Exception) {
            return null
        }
        // It searches loosely, so only accept a result that names the song and artist we asked for.
        if (!similar(o.optString("title"), title) || !similar(o.optString("artist"), song.artist)) return null
        val text = o.optString("lyrics", "").replace("\r\n", "\n").trim()
        return if (text.length >= 40) text else null
    }
}


private val providers: List<Provider> =
    listOf(LrcLib, NetEase, Kugou, QQMusic, LrcApi, Textyl, LyricsOvh, Lyrist)

/**
 * Downloads missing lyrics and keeps them in [LyricsStore], so from then on they work offline.
 *
 * Several free sources are tried in order, and the first *synced* (.lrc) result wins. When only
 * plain text is found, it is used as a fallback after every source has been asked:
 *   1. LRCLIB (lrclib.net)       - synced + plain, open community database
 *   2. NetEase Cloud Music       - synced (unofficial public endpoint)
 *   3. Kugou                     - synced (unofficial public endpoint)
 *   4. QQ Music                  - synced (unofficial public endpoint)
 *   5. LrcApi (api.lrc.cx)       - synced (community service)
 *   6. Textyl                    - synced (community service)
 *   7. lyrics.ovh                - plain text only
 *   8. Lyrist                    - plain text only (community service)
 * Results from the loose-matching sources are checked (same title/artist, or lyric times that fit
 * the length of the song) so lyrics of another song are not saved by mistake.
 * Only title, artist, album and duration are sent. A source that is rate-limiting or failing is
 * skipped for a short while, so one bad source never blocks the others.
 */
object LyricsFetcher {

    enum class Outcome { FOUND, NOT_FOUND, ERROR, RATE_LIMITED }

    // ----- Miss bookkeeping -----

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** False when this song was searched very recently and nothing was found. */
    fun shouldTry(context: Context, songId: Long): Boolean {
        val last = prefs(context).getLong("miss_$songId", 0L)
        return last == 0L || System.currentTimeMillis() - last > RETRY_MISS_AFTER_MS
    }

    private fun markMiss(context: Context, songId: Long) {
        prefs(context).edit().putLong("miss_$songId", System.currentTimeMillis()).apply()
    }

    private fun clearMiss(context: Context, songId: Long) {
        prefs(context).edit().remove("miss_$songId").apply()
    }

    // ----- Public entry point -----

    /** What a search found (without saving anything): the best text (synced if some source had it) and how it went. */
    class Lookup(val text: String?, val outcome: Outcome)

    /** Looks the song up online and saves the lyrics. Blocking: call from a background thread. */
    fun fetch(context: Context, song: Song): Outcome {
        val app = context.applicationContext
        val found = lookup(song)
        val best = found.text
        when {
            best != null -> {
                LyricsStore.save(app, song.id, best)
                LyricsIndex.update(song.id, best)
                clearMiss(app, song.id)
            }
            found.outcome == Outcome.NOT_FOUND -> markMiss(app, song.id)
        }
        return found.outcome
    }

    /** One version of the lyrics offered by a source. [synced] means it has [mm:ss] time stamps (LRC). */
    class Option(val source: String, val text: String, val synced: Boolean)

    class Choices(val options: List<Option>, val outcome: Outcome)

    /**
     * Asks the sources and keeps every different version they return (up to [max]), synced ones first, so the
     * user can choose. Nothing is saved. Blocking: call from a background thread.
     */
    fun lookupAll(song: Song, max: Int = 6): Choices {
        if (song.title.isBlank()) return Choices(emptyList(), Outcome.NOT_FOUND)
        val found = ArrayList<Option>()
        val seen = HashSet<String>()
        var answered = false
        var failed = false
        var skipped = false
        for (p in providers) {
            if (found.size >= max) break
            val until = coolUntil[p.name] ?: 0L
            if (System.currentTimeMillis() < until) { skipped = true; continue }
            try {
                val text = p.find(song)
                answered = true
                fails[p.name] = 0
                if (!text.isNullOrBlank() && seen.add(text.trim().replace(Regex("\\s+"), " "))) {
                    found += Option(p.name, text, syncedTag.containsMatchIn(text))
                }
            } catch (_: RateLimited) {
                coolUntil[p.name] = System.currentTimeMillis() + COOLDOWN_MS
                skipped = true
            } catch (_: Exception) {
                failed = true
            }
        }
        val sorted = found.sortedByDescending { it.synced }
        val outcome = when {
            sorted.isNotEmpty() -> Outcome.FOUND
            answered -> Outcome.NOT_FOUND
            skipped && !failed -> Outcome.RATE_LIMITED
            else -> Outcome.ERROR
        }
        return Choices(sorted, outcome)
    }

    /** Asks the sources for the lyrics of [song] and returns them without saving. Blocking: call from a background thread. */
    fun lookup(song: Song): Lookup {
        if (song.title.isBlank()) return Lookup(null, Outcome.NOT_FOUND)

        var plainFallback: String? = null
        var synced: String? = null
        var answered = false   // at least one source replied properly (even "not found")
        var failed = false     // at least one source errored
        var skipped = false    // at least one source was cooling down

        for (p in providers) {
            val until = coolUntil[p.name] ?: 0L
            if (System.currentTimeMillis() < until) { skipped = true; continue }
            try {
                val text = p.find(song)
                answered = true
                fails[p.name] = 0
                if (!text.isNullOrBlank()) {
                    if (syncedTag.containsMatchIn(text)) { synced = text; break }
                    if (plainFallback == null) plainFallback = text
                }
            } catch (_: RateLimited) {
                coolUntil[p.name] = System.currentTimeMillis() + COOLDOWN_MS
                skipped = true
            } catch (_: Exception) {
                failed = true
                val n = (fails[p.name] ?: 0) + 1
                fails[p.name] = n
                if (n >= FAILS_BEFORE_COOLDOWN) {
                    coolUntil[p.name] = System.currentTimeMillis() + COOLDOWN_MS
                    fails[p.name] = 0
                }
            }
        }

        val best = synced ?: plainFallback
        return when {
            best != null -> Lookup(best, Outcome.FOUND)
            answered -> Lookup(null, Outcome.NOT_FOUND)
            skipped && !failed -> Lookup(null, Outcome.RATE_LIMITED)
            else -> Lookup(null, Outcome.ERROR)
        }
    }
}
