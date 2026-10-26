package com.missionbreak56.musicplayer.online

import com.missionbreak56.musicplayer.tags.TagField
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

/** One edition (release) a recording appears on. */
class ReleaseRef(
    val id: String,
    val title: String,
    val status: String?,
    val date: String,
    val country: String?,
    val groupId: String?,
    val primaryType: String?,
    val secondaryTypes: List<String>
)

/** One search result: a recording, seen on one particular release when it has any. */
class Candidate(
    val recordingId: String,
    val title: String,
    val artist: String,
    val artistNames: List<String>,
    val lengthMs: Long?,
    val disambiguation: String,
    val firstReleaseDate: String,
    val release: ReleaseRef?,
    val score: Double
)

/** The search outcome: the candidates (best first) and whether the best one can be trusted. */
class SearchOutcome(val quality: MatchQuality, val candidates: List<Candidate>)

/** What the chosen result offers. Only values the source really returned are present. */
class ImportedMetadata(
    val fields: Map<TagField, String>,
    val releaseId: String?,
    val releaseGroupId: String?,
    val releaseTitle: String?,
    val hasFrontCover: Boolean
)

/** Pure JSON -> model parsing for the MusicBrainz web service (kept separate so it can be tested with sample answers). */
object MusicBrainzParser {

    private fun JSONObject.str(name: String): String = if (isNull(name)) "" else optString(name, "")

    private fun JSONObject.longOrNull(name: String): Long? = if (has(name) && !isNull(name)) optLong(name, -1L).takeIf { it >= 0 } else null

    private fun JSONArray?.objects(): List<JSONObject> {
        if (this == null) return emptyList()
        val out = ArrayList<JSONObject>(length())
        for (i in 0 until length()) optJSONObject(i)?.let { out += it }
        return out
    }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>(length())
        for (i in 0 until length()) optString(i, "").takeIf { it.isNotBlank() }?.let { out += it }
        return out
    }

    /** "A feat. B" style text and the individual names of an artist-credit array. */
    fun artistCredit(credit: JSONArray?): Pair<String, List<String>> {
        val text = StringBuilder()
        val names = ArrayList<String>()
        for (c in credit.objects()) {
            val name = c.str("name").ifEmpty { c.optJSONObject("artist")?.str("name").orEmpty() }
            text.append(name).append(c.str("joinphrase"))
            if (name.isNotEmpty()) names += name
            c.optJSONObject("artist")?.str("name")?.takeIf { it.isNotEmpty() && it != name }?.let { names += it }
        }
        return text.toString().trim() to names
    }

    private fun release(o: JSONObject): ReleaseRef {
        val group = o.optJSONObject("release-group")
        return ReleaseRef(
            id = o.str("id"),
            title = o.str("title"),
            status = o.str("status").ifEmpty { null },
            date = o.str("date"),
            country = o.str("country").ifEmpty { null },
            groupId = group?.str("id")?.ifEmpty { null },
            primaryType = group?.str("primary-type")?.ifEmpty { null },
            secondaryTypes = group?.optJSONArray("secondary-types").strings()
        )
    }

    /** Raw search answer -> candidate rows (unscored; the client scores them). Throws OnlineException on a bad answer. */
    fun parseSearch(json: String, score: (Candidate) -> Double): List<Candidate> {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw OnlineException(OnlineError.INVALID_RESPONSE, "The answer is not valid JSON", cause = e)
        }
        if (!root.has("recordings")) {
            if (root.has("error")) throw OnlineException(OnlineError.HTTP, root.str("error"))
            throw OnlineException(OnlineError.INVALID_RESPONSE, "The answer has no recordings list")
        }
        val out = ArrayList<Candidate>()
        for (rec in root.optJSONArray("recordings").objects()) {
            val id = rec.str("id")
            val title = rec.str("title")
            if (id.isEmpty() || title.isEmpty()) continue
            val (artist, names) = artistCredit(rec.optJSONArray("artist-credit"))
            val releases = rec.optJSONArray("releases").objects().map { release(it) }.filter { it.id.isNotEmpty() }
            val base = Candidate(
                recordingId = id, title = title, artist = artist, artistNames = names,
                lengthMs = rec.longOrNull("length"), disambiguation = rec.str("disambiguation"),
                firstReleaseDate = rec.str("first-release-date"), release = null, score = 0.0
            )
            val rows = if (releases.isEmpty()) listOf(base) else releases.map {
                Candidate(
                    base.recordingId, base.title, base.artist, base.artistNames, base.lengthMs, base.disambiguation,
                    base.firstReleaseDate, it, 0.0
                )
            }
            // Keep the few best editions of each recording, not every re-release.
            rows.map { Candidate(it.recordingId, it.title, it.artist, it.artistNames, it.lengthMs, it.disambiguation, it.firstReleaseDate, it.release, score(it)) }
                .sortedByDescending { it.score }
                .take(3)
                .forEach { out += it }
        }
        return out
    }

    /** Release lookup answer -> the fields to import for [recordingId]. */
    fun parseRelease(json: String, recordingId: String): ImportedMetadata {
        val o = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw OnlineException(OnlineError.INVALID_RESPONSE, "The answer is not valid JSON", cause = e)
        }
        if (o.str("id").isEmpty()) throw OnlineException(OnlineError.INVALID_RESPONSE, "The answer has no release")
        val fields = LinkedHashMap<TagField, String>()
        fun put(f: TagField, v: String?) {
            val t = v?.trim().orEmpty()
            if (t.isNotEmpty()) fields[f] = t
        }
        put(TagField.ALBUM, o.str("title"))
        put(TagField.ALBUM_ARTIST, artistCredit(o.optJSONArray("artist-credit")).first)
        put(TagField.DATE, o.str("date"))
        put(TagField.LABEL, o.optJSONArray("label-info").objects().firstNotNullOfOrNull { it.optJSONObject("label")?.str("name")?.ifEmpty { null } })

        val media = o.optJSONArray("media").objects()
        if (media.isNotEmpty()) put(TagField.DISC_TOTAL, media.size.toString())
        loop@ for (m in media) {
            val tracks = (m.optJSONArray("tracks").objects() + m.optJSONArray("track").objects())
            for (t in tracks) {
                val rec = t.optJSONObject("recording")
                if (rec != null && rec.str("id") == recordingId) {
                    put(TagField.DISC, m.optInt("position", 0).takeIf { it > 0 }?.toString())
                    val number = t.str("number")
                    val position = t.optInt("position", 0)
                    put(TagField.TRACK, if (number.isNotEmpty() && number.all { it.isDigit() }) number.trimStart('0').ifEmpty { "0" } else position.takeIf { it > 0 }?.toString())
                    put(TagField.TRACK_TOTAL, m.optInt("track-count", 0).takeIf { it > 0 }?.toString())
                    break@loop
                }
            }
        }

        val group = o.optJSONObject("release-group")
        val genres = (o.optJSONArray("genres").objects() + group?.optJSONArray("genres").objects())
            .sortedByDescending { it.optInt("count", 0) }
            .map { it.str("name") }
            .filter { it.isNotBlank() }
        genres.firstOrNull()?.let { put(TagField.GENRE, titleCase(it)) }

        val caa = o.optJSONObject("cover-art-archive")
        val front = caa != null && caa.optBoolean("front", false)
        return ImportedMetadata(fields, o.str("id"), group?.str("id")?.ifEmpty { null }, o.str("title").ifEmpty { null }, front)
    }

    /** Recording lookup answer (with work relationships) -> composers and lyricists, plus genres as a fallback. */
    fun parseRecording(json: String): Map<TagField, String> {
        val o = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw OnlineException(OnlineError.INVALID_RESPONSE, "The answer is not valid JSON", cause = e)
        }
        val composers = LinkedHashSet<String>()
        val lyricists = LinkedHashSet<String>()
        for (rel in o.optJSONArray("relations").objects()) {
            val work = rel.optJSONObject("work") ?: continue
            for (wr in work.optJSONArray("relations").objects()) {
                val name = wr.optJSONObject("artist")?.str("name").orEmpty()
                if (name.isEmpty()) continue
                when (wr.str("type")) {
                    "composer", "writer" -> composers += name
                    "lyricist" -> lyricists += name
                }
            }
        }
        val out = LinkedHashMap<TagField, String>()
        if (composers.isNotEmpty()) out[TagField.COMPOSER] = composers.joinToString("; ")
        if (lyricists.isNotEmpty()) out[TagField.LYRICIST] = lyricists.joinToString("; ")
        o.optJSONArray("genres").objects().maxByOrNull { it.optInt("count", 0) }?.str("name")
            ?.takeIf { it.isNotBlank() }?.let { out[TagField.GENRE] = titleCase(it) }
        return out
    }

    private fun titleCase(s: String): String =
        s.split(' ').joinToString(" ") { w -> w.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } }
}

/**
 * Client of the official MusicBrainz web service (https://musicbrainz.org/doc/MusicBrainz_API).
 * Requests are spaced by [minGapMs] (the service allows about one per second) and sent with the
 * User-Agent configured in [http]. Nothing is written anywhere: it only returns data.
 */
class MusicBrainzClient(
    private val http: HttpFetcher,
    private val minGapMs: Long = 1_100L,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val retryDelayMs: Long = 2_000L
) {
    private val gate = Mutex()
    private var lastRequest = 0L

    private suspend fun fetch(path: String): String = gate.withLock {
        var attempt = 0
        var result: String? = null
        while (result == null) {
            val wait = minGapMs - (clock() - lastRequest)
            if (wait > 0) delay(wait)
            try {
                result = String(http.get("https://musicbrainz.org/ws/2/$path"), Charsets.UTF_8)
            } catch (e: OnlineException) {
                if (e.error == OnlineError.RATE_LIMITED && attempt == 0) {
                    attempt++
                    delay(retryDelayMs)
                } else {
                    throw e
                }
            } finally {
                lastRequest = clock()
            }
        }
        checkNotNull(result)
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun phrase(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    internal fun queries(local: LocalInfo): List<String> {
        val core = MatchScorer.coreTitle(local.title).ifEmpty { local.title.trim() }
        val artist = local.artist.trim()
        val first = buildString {
            append("recording:").append(phrase(core))
            if (artist.isNotEmpty()) append(" AND artist:").append(phrase(artist))
        }
        val tokens = core.split(' ').filter { it.isNotEmpty() }.joinToString(" ")
        val second = buildString {
            append("recording:(").append(tokens.replace(Regex("[^\\p{L}\\p{N} ]"), " ")).append(")")
            if (artist.isNotEmpty()) append(" AND artist:(").append(MatchScorer.plain(artist)).append(")")
        }
        return listOf(first, second)
    }

    /** Looks the song up and scores the results. Throws [OnlineException] for connection, HTTP and format problems. */
    suspend fun search(local: LocalInfo): SearchOutcome {
        if (local.title.isBlank()) return SearchOutcome(MatchQuality.NONE, emptyList())
        var rows: List<Candidate> = emptyList()
        for (q in queries(local)) {
            val body = fetch("recording?query=${enc(q)}&limit=25&fmt=json")
            rows = MusicBrainzParser.parseSearch(body) { MatchScorer.score(local, it) }
            if (rows.isNotEmpty()) break
        }
        val sorted = rows.sortedByDescending { it.score }.take(12)
        return SearchOutcome(MatchScorer.classify(sorted), sorted)
    }

    /** Downloads the data of the chosen result: its release (album, track/disc numbers, label) and the work credits. */
    suspend fun details(candidate: Candidate): ImportedMetadata {
        val fields = LinkedHashMap<TagField, String>()
        fields[TagField.TITLE] = candidate.title
        if (candidate.artist.isNotBlank()) fields[TagField.ARTIST] = candidate.artist

        var imported: ImportedMetadata? = null
        val release = candidate.release
        if (release != null) {
            val body = fetch("release/${release.id}?inc=recordings+artist-credits+labels+release-groups+genres&fmt=json")
            imported = MusicBrainzParser.parseRelease(body, candidate.recordingId)
            fields.putAll(imported.fields)
        } else if (candidate.firstReleaseDate.isNotBlank()) {
            fields[TagField.DATE] = candidate.firstReleaseDate
        }

        // Composer / lyricist come from the "work" the recording is a performance of. Optional: a failure here
        // must not throw away what the release already gave.
        try {
            val body = fetch("recording/${candidate.recordingId}?inc=work-rels+work-level-rels+genres&fmt=json")
            for ((k, v) in MusicBrainzParser.parseRecording(body)) if (!fields.containsKey(k)) fields[k] = v
        } catch (e: OnlineException) {
            if (e.error == OnlineError.RATE_LIMITED || e.error == OnlineError.NETWORK) throw e
        }
        return ImportedMetadata(
            fields = fields,
            releaseId = imported?.releaseId ?: release?.id,
            releaseGroupId = imported?.releaseGroupId ?: release?.groupId,
            releaseTitle = imported?.releaseTitle ?: release?.title,
            hasFrontCover = imported?.hasFrontCover ?: false
        )
    }
}
