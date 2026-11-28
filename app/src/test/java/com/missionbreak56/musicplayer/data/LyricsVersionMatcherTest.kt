package com.missionbreak56.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsVersionMatcherTest {
    private fun candidate(
        id: String,
        title: String,
        durationMs: Long = 180_000L
    ) = LyricsVersionCandidate(
        id = id,
        source = "test",
        title = title,
        artist = "Costa Gold",
        durationMs = durationMs,
        lyrics = "[00:01.00]linha",
        synced = true
    )

    @Test fun detectsCommonRecordingVariants() {
        assertEquals(LyricsVersionKind.LIVE, LyricsVersionMatcher.detectVersion("Faixa (Ao Vivo)"))
        assertEquals(LyricsVersionKind.REMIX, LyricsVersionMatcher.detectVersion("Faixa - DJ Remix"))
        assertEquals(LyricsVersionKind.ACOUSTIC, LyricsVersionMatcher.detectVersion("Faixa (Acoustic)"))
        assertEquals(LyricsVersionKind.REMASTERED, LyricsVersionMatcher.detectVersion("Faixa 2011 Remastered"))
        assertEquals(LyricsVersionKind.ORIGINAL, LyricsVersionMatcher.detectVersion("Faixa"))
    }

    @Test fun prefersLiveCandidateForLiveRequest() {
        val ranked = LyricsVersionMatcher.rank(
            requestedTitle = "Faixa (Live)",
            requestedArtist = "Costa Gold",
            requestedDurationMs = 240_000L,
            candidates = listOf(
                candidate("studio", "Faixa", 180_000L),
                candidate("live", "Faixa (Live)", 240_000L),
                candidate("remix", "Faixa (Remix)", 220_000L)
            )
        )
        assertEquals("live", ranked.first().candidate.id)
    }

    @Test fun explicitVariantIsNotSilentlyReplacedByOriginal() {
        val ranked = LyricsVersionMatcher.rank(
            requestedTitle = "Faixa (Remix)",
            requestedArtist = "Costa Gold",
            requestedDurationMs = 180_000L,
            candidates = listOf(candidate("original", "Faixa", 180_000L))
        )
        assertTrue(ranked.first().confidence != LyricsMatchConfidence.HIGH)
        assertNull(LyricsVersionMatcher.automaticChoice(ranked))
    }

    @Test fun onlyHighConfidenceResultsAreAutomaticallySelected() {
        val ranked = LyricsVersionMatcher.rank(
            requestedTitle = "Faixa",
            requestedArtist = "Costa Gold",
            requestedDurationMs = 180_000L,
            candidates = listOf(candidate("exact", "Faixa", 180_000L))
        )
        assertNotNull(LyricsVersionMatcher.automaticChoice(ranked))
    }

    @Test fun preservesOriginalLyricTextAndTimestamps() {
        val original = "[00:01.25]Primeira linha\n[00:03.50]Segunda linha"
        val option = candidate("lrc", "Faixa").copy(lyrics = original)
        assertEquals(original, option.lyrics)
    }
}
