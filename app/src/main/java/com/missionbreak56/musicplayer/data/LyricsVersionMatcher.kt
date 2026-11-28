package com.missionbreak56.musicplayer.data

import java.text.Normalizer

/**
 * Metadata for one result returned by an online lyrics source.
 *
 * Keep provider metadata separate from the lyric body: version matching must never rewrite
 * timestamps or alter the original LRC text.
 */
data class LyricsVersionCandidate(
    val id: String,
    val source: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val lyrics: String,
    val synced: Boolean
)

enum class LyricsVersionKind {
    ORIGINAL, LIVE, REMIX, ACOUSTIC, INSTRUMENTAL, REMASTERED, EXTENDED, RADIO_EDIT, OTHER
}

enum class LyricsMatchConfidence { HIGH, MEDIUM, LOW }

data class RankedLyricsVersion(
    val candidate: LyricsVersionCandidate,
    val score: Int,
    val confidence: LyricsMatchConfidence,
    val version: LyricsVersionKind
)

/**
 * Ranks lyric results without treating a loose title match as proof that two recordings
 * are the same. In particular, a candidate explicitly marked Live/Remix is not silently
 * treated as the original recording.
 */
object LyricsVersionMatcher {
    private val versionPatterns = listOf(
        LyricsVersionKind.LIVE to Regex("""\b(live|ao vivo|en vivo|concert|acoustic live)\b"""),
        LyricsVersionKind.REMIX to Regex("""\b(remix|remixed|mix|rework|bootleg)\b"""),
        LyricsVersionKind.ACOUSTIC to Regex("""\b(acoustic|acustic[oa]|unplugged)\b"""),
        LyricsVersionKind.INSTRUMENTAL to Regex("""\b(instrumental|karaoke|backing track)\b"""),
        LyricsVersionKind.REMASTERED to Regex("""\b(remaster(ed)?|201[0-9] remaster|202[0-9] remaster)\b"""),
        LyricsVersionKind.EXTENDED to Regex("""\b(extended|long version|12 inch)\b"""),
        LyricsVersionKind.RADIO_EDIT to Regex("""\b(radio edit|radio version|single edit)\b""")
    )

    private fun normalized(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("""\b(feat\.?|ft\.?|featuring)\\s+[^,;]+"""), " ")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    private fun compact(value: String) = normalized(value).replace(" ", "")

    fun detectVersion(vararg metadata: String): LyricsVersionKind {
        val text = metadata.joinToString(" ").lowercase()
        return versionPatterns.firstOrNull { (_, pattern) -> pattern.containsMatchIn(text) }?.first
            ?: LyricsVersionKind.ORIGINAL
    }

    private fun similarity(expected: String, actual: String): Int {
        val a = compact(expected)
        val b = compact(actual)
        if (a.isEmpty() || b.isEmpty()) return 0
        if (a == b) return 100
        if (a.startsWith(b) || b.startsWith(a)) return 82
        val at = normalized(expected).split(" ").filter { it.length > 1 }.toSet()
        val bt = normalized(actual).split(" ").filter { it.length > 1 }.toSet()
        if (at.isEmpty() || bt.isEmpty()) return 0
        return ((at intersect bt).size * 100) / (at union bt).size
    }

    fun rank(
        requestedTitle: String,
        requestedArtist: String,
        requestedAlbum: String = "",
        requestedDurationMs: Long = 0L,
        candidates: List<LyricsVersionCandidate>
    ): List<RankedLyricsVersion> {
        val requestedVersion = detectVersion(requestedTitle, requestedAlbum)
        return candidates.map { candidate ->
            val candidateVersion = detectVersion(candidate.title, candidate.album)
            val titleScore = similarity(requestedTitle, candidate.title)
            val artistScore = if (requestedArtist.isBlank()) 50 else similarity(requestedArtist, candidate.artist)
            val albumScore = if (requestedAlbum.isBlank() || candidate.album.isBlank()) 50
                else similarity(requestedAlbum, candidate.album)

            val versionScore = when {
                requestedVersion == candidateVersion -> 100
                requestedVersion == LyricsVersionKind.ORIGINAL &&
                    candidateVersion != LyricsVersionKind.ORIGINAL -> 20
                else -> 0
            }
            val durationScore = when {
                requestedDurationMs <= 0L || candidate.durationMs <= 0L -> 50
                else -> {
                    val diff = kotlin.math.abs(requestedDurationMs - candidate.durationMs)
                    when {
                        diff <= 2_000L -> 100
                        diff <= 5_000L -> 80
                        diff <= 10_000L -> 55
                        diff <= 20_000L -> 25
                        else -> 0
                    }
                }
            }
            val syncScore = if (candidate.synced) 100 else 65
            val score = (titleScore * 30 + artistScore * 25 + versionScore * 25 +
                albumScore * 5 + durationScore * 10 + syncScore * 5) / 100
            val confidence = when {
                score >= 82 && titleScore >= 80 && artistScore >= 70 -> LyricsMatchConfidence.HIGH
                score >= 62 && titleScore >= 60 && artistScore >= 50 -> LyricsMatchConfidence.MEDIUM
                else -> LyricsMatchConfidence.LOW
            }
            RankedLyricsVersion(candidate, score, confidence, candidateVersion)
        }.sortedWith(
            compareByDescending<RankedLyricsVersion> { it.score }
                .thenByDescending { it.candidate.synced }
        )
    }

    /** A manual choice is always respected by callers; this helper only suggests auto-selection. */
    fun automaticChoice(ranked: List<RankedLyricsVersion>): RankedLyricsVersion? =
        ranked.firstOrNull()?.takeIf { it.confidence == LyricsMatchConfidence.HIGH }
}
