package com.missionbreak56.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/** Reads the on-device music library. Fully offline. */
object MusicRepository {

    private fun clean(value: String?): String {
        val t = value?.trim().orEmpty()
        return if (t.equals("<unknown>", ignoreCase = true)) "" else t
    }

    fun loadSongs(context: Context): List<Song> {
        val songs = ArrayList<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val fileName = c.getString(nameCol).orEmpty().substringBeforeLast('.')
                songs += Song(
                    id = id,
                    title = clean(c.getString(titleCol)).ifBlank { fileName },
                    artist = clean(c.getString(artistCol)),
                    album = clean(c.getString(albumCol)),
                    albumId = c.getLong(albumIdCol),
                    durationMs = c.getLong(durationCol),
                    uri = ContentUris.withAppendedId(collection, id),
                    dateAdded = c.getLong(addedCol),
                    track = c.getInt(trackCol) % 1000,
                    path = c.getString(dataCol).orEmpty()
                )
            }
        }
        return songs
    }

    /** Genre name -> ids of the songs in it. Best effort: returns an empty map on any failure. */
    fun loadGenres(context: Context): Map<String, List<Long>> {
        val result = LinkedHashMap<String, List<Long>>()
        try {
            val resolver = context.contentResolver
            resolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null, null, null
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (c.moveToNext()) {
                    val genreId = c.getLong(idCol)
                    val name = c.getString(nameCol)?.trim().orEmpty()
                    if (name.isBlank()) continue
                    val ids = ArrayList<Long>()
                    resolver.query(
                        MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null, null, null
                    )?.use { m ->
                        val audioCol = m.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        while (m.moveToNext()) ids += m.getLong(audioCol)
                    }
                    if (ids.isNotEmpty()) result[name] = (result[name] ?: emptyList()) + ids
                }
            }
        } catch (_: Exception) {
            // genres are optional
        }
        return result
    }
}
