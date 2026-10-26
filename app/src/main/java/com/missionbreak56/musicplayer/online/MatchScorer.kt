package com.missionbreak56.musicplayer.online

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** What the file already says about the song; this is all the search is based on. */
class LocalInfo(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val year: Int?
)

enum class MatchQuality {
    /** One recording is clearly the one: the editor may fill the fields (the user still reviews them). */
    CLEAR,

    /** Several plausible results: the user must choose. */
    AMBIGUOUS,

    /** Nothing resembles the song well enough to be trusted. */
    WEAK,

    /** The service found nothing at all. */
    NONE
}

/**
 * Scores MusicBrainz results against the local file. Everything here is plain Kotlin so the rules
 * (what counts as the same song, how live/remix versions are told apart) can be unit tested.
 */
object MatchScorer {

    private val accents = Regex("\\p{M}+")
    private val brackets = Regex("[(\\[{][^)\\]}]*[)\\]}]")
    private val nonWord = Regex("[^\\p{L}\\p{N}]+")
    private val featPart = Regex("\\b(feat|ft|featuring|with|part|pt)\\b.*$")

    /** Words that make a recording a different version of the same song. */
    private val markers = mapOf(
        "live" to listOf("live", "ao vivo", "en vivo", "concert", "unplugged"),
        "remix" to listOf("remix", "rmx", "rework", "mix"),
        "remaster" to listOf("remaster", "remastered", "remasterizado", "remasterizada"),
        "acoustic" to listOf("acoustic", "acustico", "acustica"),
        "instrumental" to listOf("instrumental"),
        "karaoke" to listOf("karaoke"),
        "demo" to listOf("demo"),
        "edit" to listOf("radio edit", "single edit", "edit", "short version", "extended", "extended version"),
        "cover" to listOf("cover", "tribute"),
        "mono" to listOf("mono version", "mono mix")
    )

    fun plain(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD).replace(accents, "")
        return decomposed.lowercase(Locale.ROOT).replace(nonWord, " ").trim().replace(Regex("\\s+"), " ")
    }

    /** Title without parenthesised parts, "feat." credits and " - remaster" style suffixes. */
    fun coreTitle(title: String): String {
        var t = title.replace(brackets, " ")
        val dash = t.indexOf(" - ")
        if (dash > 0) {
            val tail = plain(t.substring(dash + 3))
            if (versionMarkers(tail).isNotEmpty() || tail.matches(Regex("\\d{4}.*"))) t = t.substring(0, dash)
        }
        val p = plain(t).replace(featPart, "").trim()
        return p.ifEmpty { plain(title) }
    }

    /** Which kinds of alternative version [text] announces ("Song (Live at X)" -> {live}). */
    fun versionMarkers(text: String): Set<String> {
        val p = " " + plain(text) + " "
        val found = HashSet<String>()
        for ((name, words) in markers) {
            if (words.any { p.contains(" $it ") }) found += name
        }
        return found
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = min(min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    /** 0..1 similarity of two already-plain strings, forgiving about word order and small typos. */
    fun similarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        val edit = 1.0 - levenshtein(a, b).toDouble() / max(a.length, b.length)
        val ta = a.split(' ').filter { it.isNotEmpty() }.toSet()
        val tb = b.split(' ').filter { it.isNotEmpty() }.toSet()
        val common = ta.intersect(tb).size
        val tokens = if (ta.isEmpty() || tb.isEmpty()) 0.0 else common.toDouble() / max(ta.size, tb.size)
        return max(edit, tokens * 0.95)
    }

    private fun artistSimilarity(local: String, candidate: Candidate): Double {
        val l = plain(local)
        if (l.isEmpty()) return -1.0
        val names = candidate.artistNames + candidate.artist
        return names.maxOf { similarity(l, plain(it)) }
    }

    private fun durationScore(localMs: Long, candidateMs: Long?): Double {
        if (localMs <= 0 || candidateMs == null || candidateMs <= 0) return -1.0
        val d = abs(localMs - candidateMs)
        return when {
            d <= 3_000 -> 1.0
            d <= 8_000 -> 0.6
            d <= 15_000 -> 0.25
            else -> 0.0
        }
    }

    private fun yearScore(localYear: Int?, date: String): Double {
        val y = date.take(4).toIntOrNull()
        if (localYear == null || localYear <= 0 || y == null) return -1.0
        return when (abs(localYear - y)) {
            0 -> 1.0
            1 -> 0.5
            else -> 0.0
        }
    }

    /** Final 0..1 score of [c] for the local song. */
    fun score(local: LocalInfo, c: Candidate): Double {
        val title = similarity(coreTitle(local.title), coreTitle(c.title))
        val artist = artistSimilarity(local.artist, c)
        val album = if (local.album.isBlank() || c.release == null) -1.0
        else similarity(coreTitle(local.album), coreTitle(c.release.title))
        val duration = durationScore(local.durationMs, c.lengthMs)
        val year = yearScore(local.year, c.release?.date?.ifEmpty { null } ?: c.firstReleaseDate)

        var sum = 0.0
        var weights = 0.0
        fun add(value: Double, weight: Double) {
            if (value < 0) return
            sum += value * weight
            weights += weight
        }
        add(title, 0.40)
        add(artist, 0.25)
        add(album, 0.15)
        add(duration, 0.15)
        add(year, 0.05)
        if (weights == 0.0) return 0.0
        var result = sum / weights

        // A title that matches only because the version words were ignored must not win over the real one.
        val localMarkers = versionMarkers(local.title) + versionMarkers(local.album)
        val candidateMarkers = versionMarkers(c.title) + versionMarkers(c.disambiguation) +
            (c.release?.let { versionMarkers(it.title) } ?: emptySet())
        val extra = (candidateMarkers - localMarkers).size
        val missing = (localMarkers - candidateMarkers).size
        result -= min(0.30, extra * 0.12) + min(0.20, missing * 0.10)

        // Tie-breakers between editions: official releases of normal albums first.
        c.release?.let { r ->
            if (r.status.equals("Official", ignoreCase = true)) result += 0.01
            if (r.primaryType.equals("Album", ignoreCase = true) && r.secondaryTypes.isEmpty()) result += 0.01
            if (r.secondaryTypes.any { it.equals("Compilation", true) } && !plain(local.album).contains("hits")) result -= 0.01
        }
        // A candidate whose title does not even resemble the local one is never a good match.
        if (title < 0.5) result = min(result, 0.45)
        return result.coerceIn(0.0, 1.0)
    }

    /** Decides whether the best result can be trusted or the user has to choose. [sorted] is best first. */
    fun classify(sorted: List<Candidate>): MatchQuality {
        if (sorted.isEmpty()) return MatchQuality.NONE
        val best = sorted[0]
        val second = sorted.getOrNull(1)?.score ?: 0.0
        val otherRecording = sorted.firstOrNull { it.recordingId != best.recordingId }?.score ?: 0.0
        return when {
            best.score >= 0.80 && best.score - second >= 0.04 && best.score - otherRecording >= 0.08 -> MatchQuality.CLEAR
            best.score >= 0.55 -> MatchQuality.AMBIGUOUS
            else -> MatchQuality.WEAK
        }
    }
}
