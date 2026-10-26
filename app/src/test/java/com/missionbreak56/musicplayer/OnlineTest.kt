package com.missionbreak56.musicplayer

import com.missionbreak56.musicplayer.online.Candidate
import com.missionbreak56.musicplayer.online.CoverArtClient
import com.missionbreak56.musicplayer.online.HttpFetcher
import com.missionbreak56.musicplayer.online.ItunesCoverClient
import com.missionbreak56.musicplayer.tags.LyricsText
import com.missionbreak56.musicplayer.online.LocalInfo
import com.missionbreak56.musicplayer.online.MatchQuality
import com.missionbreak56.musicplayer.online.MatchScorer
import com.missionbreak56.musicplayer.online.MusicBrainzClient
import com.missionbreak56.musicplayer.online.MusicBrainzParser
import com.missionbreak56.musicplayer.online.OnlineError
import com.missionbreak56.musicplayer.online.OnlineException
import com.missionbreak56.musicplayer.tags.TagField
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The JSON below is hand-written following the documented MusicBrainz / Cover Art Archive formats
 * (these are fixtures, not recordings of live answers).
 */
class OnlineTest {

    private fun rec(id: String, title: String, artist: String, length: Long, release: String, date: String, dis: String = ""): String =
        """{"id":"$id","score":100,"title":"$title","length":$length,"disambiguation":"$dis","first-release-date":"$date",
        "artist-credit":[{"name":"$artist","joinphrase":"","artist":{"id":"a1","name":"$artist"}}],
        "releases":[{"id":"r-$id","title":"$release","status":"Official","date":"$date","country":"XW",
        "release-group":{"id":"g-$id","primary-type":"Album","secondary-types":[]}}]}"""

    private fun search(vararg recs: String) = """{"count":${recs.size},"offset":0,"recordings":[${recs.joinToString(",")}]}"""

    private val local = LocalInfo("Blue Sky", "The Band", "First Album", 215_000, 2001)

    private fun scored(json: String): List<Candidate> =
        MusicBrainzParser.parseSearch(json) { MatchScorer.score(local, it) }.sortedByDescending { it.score }

    @Test fun clearMatchIsTrusted() {
        val rows = scored(search(rec("1", "Blue Sky", "The Band", 214_500, "First Album", "2001-03-01"),
            rec("2", "Green Sea", "Other Act", 180_000, "Other", "1999")))
        assertEquals("1", rows[0].recordingId)
        assertEquals(MatchQuality.CLEAR, MatchScorer.classify(rows))
    }

    @Test fun noResultsIsNone() {
        assertEquals(MatchQuality.NONE, MatchScorer.classify(scored(search())))
    }

    @Test fun unrelatedResultIsWeak() {
        val rows = scored(search(rec("9", "Completely Different", "Nobody", 90_000, "X", "1980")))
        assertEquals(MatchQuality.WEAK, MatchScorer.classify(rows))
    }

    @Test fun twoSimilarRecordingsAreAmbiguous() {
        val rows = scored(search(rec("1", "Blue Sky", "The Band", 215_000, "First Album", "2001"),
            rec("2", "Blue Sky", "The Band", 215_500, "First Album", "2001")))
        assertEquals(MatchQuality.AMBIGUOUS, MatchScorer.classify(rows))
    }

    @Test fun liveVersionLosesToStudioVersion() {
        val rows = scored(search(
            rec("live", "Blue Sky (Live at Wembley)", "The Band", 262_000, "Live 2005", "2005", "live"),
            rec("studio", "Blue Sky", "The Band", 215_000, "First Album", "2001")))
        assertEquals("studio", rows[0].recordingId)
    }

    @Test fun localLiveFileMatchesLiveRecording() {
        val liveLocal = LocalInfo("Blue Sky (Live)", "The Band", "", 262_000, null)
        val rows = MusicBrainzParser.parseSearch(search(
            rec("live", "Blue Sky (Live at Wembley)", "The Band", 262_000, "Live 2005", "2005", "live"),
            rec("studio", "Blue Sky", "The Band", 215_000, "First Album", "2001"))) { MatchScorer.score(liveLocal, it) }
            .sortedByDescending { it.score }
        assertEquals("live", rows[0].recordingId)
    }

    @Test fun remixMarkerIsRecognised() {
        assertTrue(MatchScorer.versionMarkers("Song - Club Remix").contains("remix"))
        assertTrue(MatchScorer.versionMarkers("Song (2011 Remastered)").contains("remaster"))
        assertFalse(MatchScorer.versionMarkers("Song").isNotEmpty())
        assertEquals("song", MatchScorer.coreTitle("Song (feat. Someone) - 2011 Remaster"))
    }

    @Test fun invalidJsonIsReported() {
        try { MusicBrainzParser.parseSearch("<html>oops</html>") { 0.0 }; fail() } catch (e: OnlineException) {
            assertEquals(OnlineError.INVALID_RESPONSE, e.error)
        }
        try { MusicBrainzParser.parseSearch("""{"foo":1}""") { 0.0 }; fail() } catch (e: OnlineException) {
            assertEquals(OnlineError.INVALID_RESPONSE, e.error)
        }
    }

    @Test fun releaseJsonGivesTrackDiscLabelAndGenre() {
        val json = """{"id":"r1","title":"First Album","date":"2001-03-01","status":"Official",
          "artist-credit":[{"name":"The Band","joinphrase":""}],
          "label-info":[{"label":{"name":"Big Label"}}],
          "genres":[{"name":"indie rock","count":3},{"name":"rock","count":1}],
          "cover-art-archive":{"front":true,"count":2},
          "release-group":{"id":"g1","primary-type":"Album"},
          "media":[{"position":1,"track-count":2,"tracks":[{"number":"1","position":1,"recording":{"id":"x"}}]},
                   {"position":2,"track-count":10,"tracks":[{"number":"04","position":4,"recording":{"id":"rec1"}}]}]}"""
        val m = MusicBrainzParser.parseRelease(json, "rec1")
        assertEquals("First Album", m.fields[TagField.ALBUM]); assertEquals("The Band", m.fields[TagField.ALBUM_ARTIST])
        assertEquals("4", m.fields[TagField.TRACK]); assertEquals("10", m.fields[TagField.TRACK_TOTAL])
        assertEquals("2", m.fields[TagField.DISC]); assertEquals("2", m.fields[TagField.DISC_TOTAL])
        assertEquals("Big Label", m.fields[TagField.LABEL]); assertEquals("Indie Rock", m.fields[TagField.GENRE])
        assertTrue(m.hasFrontCover); assertEquals("g1", m.releaseGroupId)
    }

    @Test fun workRelationsGiveComposerAndLyricist() {
        val json = """{"relations":[{"type":"performance","work":{"relations":[
            {"type":"composer","artist":{"name":"Ann"}},{"type":"lyricist","artist":{"name":"Bob"}}]}}]}"""
        val m = MusicBrainzParser.parseRecording(json)
        assertEquals("Ann", m[TagField.COMPOSER]); assertEquals("Bob", m[TagField.LYRICIST])
    }

    // ----- client with a fake network -----

    private class Fake(val answers: MutableList<() -> ByteArray>) : HttpFetcher {
        val urls = ArrayList<String>()
        override suspend fun get(url: String, accept: String, maxBytes: Int): ByteArray {
            urls += url
            return answers.removeAt(0)()
        }
    }

    private fun client(f: HttpFetcher) = MusicBrainzClient(f, minGapMs = 0, retryDelayMs = 0)

    @Test fun clientSearchSuccess() = runBlocking {
        val body = search(rec("1", "Blue Sky", "The Band", 215_000, "First Album", "2001")).toByteArray()
        val net = Fake(mutableListOf({ body }))
        val out = client(net).search(local)
        assertEquals(MatchQuality.CLEAR, out.quality)
        assertTrue(net.urls[0].startsWith("https://musicbrainz.org/ws/2/recording?query="))
    }

    @Test fun clientRetriesOnceWhenRateLimited() = runBlocking {
        val body = search(rec("1", "Blue Sky", "The Band", 215_000, "First Album", "2001")).toByteArray()
        val net = Fake(mutableListOf({ throw OnlineException(OnlineError.RATE_LIMITED, "slow", 503) }, { body }))
        assertEquals(MatchQuality.CLEAR, client(net).search(local).quality)
        assertEquals(2, net.urls.size)
    }

    @Test fun clientReportsConnectionFailure() = runBlocking {
        val net = Fake(mutableListOf({ throw OnlineException(OnlineError.NETWORK, "down") }))
        try { client(net).search(local); fail() } catch (e: OnlineException) { assertEquals(OnlineError.NETWORK, e.error) }
    }

    @Test fun clientTriesSecondQueryWhenFirstIsEmpty() = runBlocking {
        val body = search(rec("1", "Blue Sky", "The Band", 215_000, "First Album", "2001")).toByteArray()
        val net = Fake(mutableListOf({ search().toByteArray() }, { body }))
        assertEquals(MatchQuality.CLEAR, client(net).search(local).quality)
        assertEquals(2, net.urls.size)
    }

    @Test fun clientEmptyEverywhereIsNone() = runBlocking {
        val net = Fake(mutableListOf({ search().toByteArray() }, { search().toByteArray() }))
        assertEquals(MatchQuality.NONE, client(net).search(local).quality)
    }

    // ----- cover art -----

    private val caaJson = """{"images":[
        {"id":"2","front":false,"back":true,"types":["Back"],"image":"http://coverartarchive.org/release/r/2.jpg","thumbnails":{"500":"http://coverartarchive.org/release/r/2-500.jpg"}},
        {"id":"1","front":true,"back":false,"types":["Front"],"image":"http://coverartarchive.org/release/r/1.jpg",
         "thumbnails":{"250":"http://x/1-250.jpg","500":"http://x/1-500.jpg","1200":"http://x/1-1200.jpg"}}]}"""

    @Test fun coverParsingPutsFrontFirstAndUsesHttps() {
        val o = CoverArtClient.parse(caaJson, false)
        assertEquals("1", o[0].id); assertTrue(o[0].isFront)
        assertEquals("https://x/1-1200.jpg", o[0].fullUrl); assertEquals("https://x/1-500.jpg", o[0].thumbUrl)
        assertFalse(o[1].isFront)
    }

    @Test fun coverArchive404MeansNoCovers() = runBlocking {
        val net = Fake(mutableListOf({ throw OnlineException(OnlineError.NOT_FOUND, "nf", 404) }))
        assertTrue(CoverArtClient(net).forRelease("r").isEmpty())
    }

    @Test fun coverInvalidJsonIsReported() {
        try { CoverArtClient.parse("nope", false); fail() } catch (e: OnlineException) { assertEquals(OnlineError.INVALID_RESPONSE, e.error) }
    }

    @Test fun itunesCoversUseBigUrlAndSkipDuplicates() {
        val json = """{"results":[{"collectionId":1,"artworkUrl100":"https://a/x/100x100bb.jpg"},
            {"collectionId":2,"artworkUrl100":"https://a/x/100x100bb.jpg"},{"collectionId":3,"artworkUrl100":""}]}"""
        val o = ItunesCoverClient.parse(json)
        assertEquals(1, o.size); assertEquals("https://a/x/1200x1200bb.jpg", o[0].fullUrl)
    }

    @Test fun syncedLyricsBecomePlainText() {
        val lrc = "[ar:The Band]\n[offset:0]\n[00:12.30]First line\n[01:02.5][01:30.00]Second <00:13.00>line\n\n\n\n[02:00.00]Last"
        assertEquals("First line\nSecond line\n\nLast", LyricsText.toPlain(lrc))
        assertEquals("já é texto", LyricsText.toPlain("já é texto"))
    }
}
