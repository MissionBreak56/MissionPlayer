package com.missionbreak56.musicplayer.tags

import java.util.Locale

/**
 * Text fields of a track that the metadata editor can show and change. Each one is kept as plain text,
 * exactly as the tag stores it. [key] is the TagLib property name used to read and write it.
 */
enum class TagField(val key: String) {
    TITLE("TITLE"),
    ARTIST("ARTIST"),
    ALBUM_ARTIST("ALBUMARTIST"),
    ALBUM("ALBUM"),
    GENRE("GENRE"),
    DATE("DATE"),
    TRACK("TRACKNUMBER"),
    TRACK_TOTAL("TRACKTOTAL"),
    DISC("DISCNUMBER"),
    DISC_TOTAL("DISCTOTAL"),
    COMPOSER("COMPOSER"),
    LYRICIST("LYRICIST"),
    LABEL("LABEL"),
    ORIGINAL_ALBUM("ORIGINALALBUM"),
    COMMENT("COMMENT"),
    LYRICS("LYRICS")
}

/** The tag containers the editor knows how to write. Every other format is read-only here. */
enum class AudioTagFormat(val label: String, val writable: Boolean, val vorbisStyle: Boolean) {
    MP3("MP3 (ID3)", true, false),
    FLAC("FLAC", true, true),
    OGG("Ogg (Vorbis/Opus)", true, true),
    M4A("M4A/MP4", true, false),
    OTHER("", false, false);

    /** Whether this container can store [field] in a standard way (so it is shown as editable). */
    fun supports(field: TagField): Boolean = when (this) {
        OTHER -> false
        M4A -> field != TagField.LYRICIST && field != TagField.ORIGINAL_ALBUM
        else -> true
    }

    companion object {
        fun detect(displayName: String?, mimeType: String?): AudioTagFormat {
            val ext = displayName.orEmpty().substringAfterLast('.', "").lowercase(Locale.ROOT)
            when (ext) {
                "mp3" -> return MP3
                "flac" -> return FLAC
                "ogg", "oga", "opus" -> return OGG
                "m4a", "mp4", "m4b", "m4p" -> return M4A
            }
            return when (mimeType.orEmpty().lowercase(Locale.ROOT)) {
                "audio/mpeg", "audio/mp3" -> MP3
                "audio/flac", "audio/x-flac" -> FLAC
                "audio/ogg", "audio/opus", "application/ogg" -> OGG
                "audio/mp4", "audio/x-m4a", "audio/m4a" -> M4A
                else -> OTHER
            }
        }
    }
}

/** What the user wants to do with the embedded cover when saving. */
sealed class CoverChange {
    /** Leave the picture(s) in the file alone. */
    object Keep : CoverChange()

    /** Remove the front cover picture only. */
    object Remove : CoverChange()

    /** Put [bytes] ([mime], already validated and scaled) in place of the front cover. */
    class Replace(val bytes: ByteArray, val mime: String) : CoverChange()
}

/**
 * Pure (Android-free) rules that turn a TagLib property map into editor fields and back. Keeping them
 * here, away from file access, is what lets them be unit tested on the JVM.
 */
object TagCodec {

    private const val JOIN = "; "

    /** "3/12" -> ("3", "12"); "3" -> ("3", ""). */
    fun splitPair(value: String): Pair<String, String> {
        val v = value.trim()
        val i = v.indexOf('/')
        return if (i < 0) v to "" else v.substring(0, i).trim() to v.substring(i + 1).trim()
    }

    /** ("3", "12") -> "3/12"; ("3", "") -> "3"; ("", "12") -> "0/12" is avoided: it gives "". */
    fun joinPair(number: String, total: String): String {
        val n = number.trim()
        val t = total.trim()
        return when {
            n.isEmpty() -> ""
            t.isEmpty() -> n
            else -> "$n/$t"
        }
    }

    private fun text(props: Map<String, Array<String>>, key: String, glue: String = JOIN): String =
        props[key]?.map { it.trim() }?.filter { it.isNotEmpty() }?.joinToString(glue).orEmpty()

    private fun firstKey(props: Map<String, Array<String>>, vararg keys: String): String? =
        keys.firstOrNull { props[it]?.any { v -> v.isNotBlank() } == true }

    /** Editor values read from a property map. Missing fields are empty strings. */
    fun read(format: AudioTagFormat, props: Map<String, Array<String>>): Map<TagField, String> {
        val out = LinkedHashMap<TagField, String>()
        for (f in TagField.values()) out[f] = ""
        out[TagField.TITLE] = text(props, "TITLE")
        out[TagField.ARTIST] = text(props, "ARTIST")
        out[TagField.ALBUM_ARTIST] = text(props, "ALBUMARTIST")
        out[TagField.ALBUM] = text(props, "ALBUM")
        out[TagField.GENRE] = text(props, "GENRE")
        out[TagField.DATE] = text(props, "DATE").ifEmpty { text(props, "YEAR") }
        out[TagField.COMPOSER] = text(props, "COMPOSER")
        out[TagField.LYRICIST] = text(props, "LYRICIST")
        out[TagField.LABEL] = text(props, "LABEL").ifEmpty { text(props, "PUBLISHER") }
        out[TagField.ORIGINAL_ALBUM] = text(props, "ORIGINALALBUM")
        out[TagField.COMMENT] = text(props, "COMMENT", "\n")
        val lyricsKey = firstKey(props, "LYRICS", "USLT")
        out[TagField.LYRICS] = if (lyricsKey == null) "" else text(props, lyricsKey, "\n")

        val (track, trackInline) = splitPair(text(props, "TRACKNUMBER"))
        val trackTotal = trackInline.ifEmpty { text(props, "TRACKTOTAL").ifEmpty { text(props, "TOTALTRACKS") } }
        out[TagField.TRACK] = track
        out[TagField.TRACK_TOTAL] = trackTotal
        val (disc, discInline) = splitPair(text(props, "DISCNUMBER"))
        val discTotal = discInline.ifEmpty { text(props, "DISCTOTAL").ifEmpty { text(props, "TOTALDISCS") } }
        out[TagField.DISC] = disc
        out[TagField.DISC_TOTAL] = discTotal
        return out
    }

    private fun HashMap<String, Array<String>>.setOrRemove(key: String, value: String) {
        val v = value.trim()
        if (v.isEmpty()) remove(key) else put(key, arrayOf(v))
    }

    /**
     * The property map to write: a copy of [original] where only the fields the user really changed
     * ([edited] differs from [originalValues]) are touched. Untouched properties, including ones the
     * editor does not know about and multi-valued ones, are carried over exactly as they were.
     */
    fun apply(
        format: AudioTagFormat,
        original: Map<String, Array<String>>,
        originalValues: Map<TagField, String>,
        edited: Map<TagField, String>
    ): HashMap<String, Array<String>> {
        val out = HashMap<String, Array<String>>()
        for ((k, v) in original) out[k] = v.copyOf()

        fun changed(f: TagField) = format.supports(f) && edited[f].orEmpty().trim() != originalValues[f].orEmpty().trim()
        fun now(f: TagField) = edited[f].orEmpty().trim()

        val simple = listOf(
            TagField.TITLE, TagField.ARTIST, TagField.ALBUM_ARTIST, TagField.ALBUM, TagField.GENRE,
            TagField.DATE, TagField.COMPOSER, TagField.LYRICIST, TagField.LABEL,
            TagField.ORIGINAL_ALBUM, TagField.COMMENT
        )
        for (f in simple) {
            if (!changed(f)) continue
            out.setOrRemove(f.key, now(f))
            // Older/alternative spellings of the same property would otherwise keep showing the old text.
            if (f == TagField.DATE) out.remove("YEAR")
            if (f == TagField.LABEL) out.remove("PUBLISHER")
        }

        if (changed(TagField.LYRICS)) {
            val key = if (original.containsKey("LYRICS")) "LYRICS" else if (original.containsKey("USLT")) "USLT" else "LYRICS"
            out.setOrRemove(key, now(TagField.LYRICS))
        }

        if (changed(TagField.TRACK) || changed(TagField.TRACK_TOTAL)) {
            if (format.vorbisStyle) {
                out.setOrRemove("TRACKNUMBER", now(TagField.TRACK))
                val totalKey = if (original.containsKey("TOTALTRACKS") && !original.containsKey("TRACKTOTAL")) "TOTALTRACKS" else "TRACKTOTAL"
                out.setOrRemove(totalKey, if (now(TagField.TRACK).isEmpty()) "" else now(TagField.TRACK_TOTAL))
            } else {
                out.setOrRemove("TRACKNUMBER", joinPair(now(TagField.TRACK), now(TagField.TRACK_TOTAL)))
            }
        }
        if (changed(TagField.DISC) || changed(TagField.DISC_TOTAL)) {
            if (format.vorbisStyle) {
                out.setOrRemove("DISCNUMBER", now(TagField.DISC))
                val totalKey = if (original.containsKey("TOTALDISCS") && !original.containsKey("DISCTOTAL")) "TOTALDISCS" else "DISCTOTAL"
                out.setOrRemove(totalKey, if (now(TagField.DISC).isEmpty()) "" else now(TagField.DISC_TOTAL))
            } else {
                out.setOrRemove("DISCNUMBER", joinPair(now(TagField.DISC), now(TagField.DISC_TOTAL)))
            }
        }
        return out
    }

    /** True when both maps hold the same properties with the same values. */
    fun sameProperties(a: Map<String, Array<String>>, b: Map<String, Array<String>>): Boolean {
        if (a.keys != b.keys) return false
        return a.all { (k, v) -> v.contentEquals(b[k]) }
    }
}
