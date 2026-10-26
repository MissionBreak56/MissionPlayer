package com.missionbreak56.musicplayer

import com.missionbreak56.musicplayer.tags.AudioTagFormat
import com.missionbreak56.musicplayer.tags.TagCodec
import com.missionbreak56.musicplayer.tags.TagField
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagCodecTest {

    private fun props(vararg p: Pair<String, List<String>>): HashMap<String, Array<String>> {
        val m = HashMap<String, Array<String>>()
        for ((k, v) in p) m[k] = v.toTypedArray()
        return m
    }

    private val mp3 = props(
        "TITLE" to listOf("Old"), "ARTIST" to listOf("A1", "A2"), "ALBUM" to listOf("Alb"),
        "TRACKNUMBER" to listOf("3/12"), "DISCNUMBER" to listOf("1/2"), "DATE" to listOf("2001-05-06"),
        "LYRICS" to listOf("la la"), "MY_CUSTOM" to listOf("keep me"), "COMMENT" to listOf("c")
    )

    @Test fun readsFieldsAndSplitsTrackAndDisc() {
        val v = TagCodec.read(AudioTagFormat.MP3, mp3)
        assertEquals("Old", v[TagField.TITLE])
        assertEquals("A1; A2", v[TagField.ARTIST])
        assertEquals("3", v[TagField.TRACK]); assertEquals("12", v[TagField.TRACK_TOTAL])
        assertEquals("1", v[TagField.DISC]); assertEquals("2", v[TagField.DISC_TOTAL])
        assertEquals("la la", v[TagField.LYRICS])
    }

    @Test fun readsVorbisStyleTotals() {
        val v = TagCodec.read(AudioTagFormat.FLAC, props("TRACKNUMBER" to listOf("3"), "TOTALTRACKS" to listOf("12")))
        assertEquals("3", v[TagField.TRACK]); assertEquals("12", v[TagField.TRACK_TOTAL])
    }

    @Test fun readsLyricsFromUsltFallback() {
        assertEquals("x", TagCodec.read(AudioTagFormat.MP3, props("USLT" to listOf("x")))[TagField.LYRICS])
    }

    @Test fun unchangedFieldsKeepOriginalArraysAndUnknownTags() {
        val orig = TagCodec.read(AudioTagFormat.MP3, mp3)
        val edited = LinkedHashMap(orig); edited[TagField.TITLE] = "New"
        val out = TagCodec.apply(AudioTagFormat.MP3, mp3, orig, edited)
        assertArrayEquals(arrayOf("New"), out["TITLE"])
        assertArrayEquals(arrayOf("A1", "A2"), out["ARTIST"])   // multi-value untouched
        assertArrayEquals(arrayOf("keep me"), out["MY_CUSTOM"]) // unknown tag preserved
        assertArrayEquals(arrayOf("3/12"), out["TRACKNUMBER"])
    }

    @Test fun noChangeMeansIdenticalMap() {
        val orig = TagCodec.read(AudioTagFormat.MP3, mp3)
        assertTrue(TagCodec.sameProperties(mp3, TagCodec.apply(AudioTagFormat.MP3, mp3, orig, orig)))
    }

    @Test fun id3JoinsTrackAndTotal() {
        val orig = TagCodec.read(AudioTagFormat.MP3, mp3)
        val e = LinkedHashMap(orig); e[TagField.TRACK] = "5"; e[TagField.TRACK_TOTAL] = "9"
        assertArrayEquals(arrayOf("5/9"), TagCodec.apply(AudioTagFormat.MP3, mp3, orig, e)["TRACKNUMBER"])
    }

    @Test fun vorbisWritesSeparateTotal() {
        val flac = props("TRACKNUMBER" to listOf("3"), "TRACKTOTAL" to listOf("12"))
        val orig = TagCodec.read(AudioTagFormat.FLAC, flac)
        val e = LinkedHashMap(orig); e[TagField.TRACK] = "5"; e[TagField.TRACK_TOTAL] = "9"
        val out = TagCodec.apply(AudioTagFormat.FLAC, flac, orig, e)
        assertArrayEquals(arrayOf("5"), out["TRACKNUMBER"]); assertArrayEquals(arrayOf("9"), out["TRACKTOTAL"])
    }

    @Test fun clearingAFieldRemovesTheProperty() {
        val orig = TagCodec.read(AudioTagFormat.MP3, mp3)
        val e = LinkedHashMap(orig); e[TagField.COMMENT] = "  "
        assertFalse(TagCodec.apply(AudioTagFormat.MP3, mp3, orig, e).containsKey("COMMENT"))
    }

    @Test fun m4aDoesNotWriteUnsupportedFields() {
        val m4a = props("TITLE" to listOf("t"))
        val orig = TagCodec.read(AudioTagFormat.M4A, m4a)
        val e = LinkedHashMap(orig); e[TagField.LYRICIST] = "X"; e[TagField.ORIGINAL_ALBUM] = "Y"
        val out = TagCodec.apply(AudioTagFormat.M4A, m4a, orig, e)
        assertNull(out["LYRICIST"]); assertNull(out["ORIGINALALBUM"])
        assertFalse(AudioTagFormat.M4A.supports(TagField.LYRICIST))
    }

    @Test fun lyricsKeepTheKeyTheFileUses() {
        val p = props("USLT" to listOf("old"))
        val orig = TagCodec.read(AudioTagFormat.MP3, p)
        val e = LinkedHashMap(orig); e[TagField.LYRICS] = "new"
        assertArrayEquals(arrayOf("new"), TagCodec.apply(AudioTagFormat.MP3, p, orig, e)["USLT"])
    }

    @Test fun detectsFormats() {
        assertEquals(AudioTagFormat.MP3, AudioTagFormat.detect("a.MP3", null))
        assertEquals(AudioTagFormat.FLAC, AudioTagFormat.detect("a.flac", null))
        assertEquals(AudioTagFormat.OGG, AudioTagFormat.detect("a.opus", null))
        assertEquals(AudioTagFormat.M4A, AudioTagFormat.detect("a.m4a", null))
        assertEquals(AudioTagFormat.M4A, AudioTagFormat.detect(null, "audio/mp4"))
        assertEquals(AudioTagFormat.OTHER, AudioTagFormat.detect("a.wav", "audio/x-wav"))
        assertFalse(AudioTagFormat.OTHER.writable)
    }
}
