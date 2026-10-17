package com.missionbreak56.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/** Reads the on-device music library. Fully offline. */
object MusicRepository {

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
            MediaStore.Audio.Media.IS_MUSIC
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                songs += Song(
                    id = id,
                    title = cursor.getString(titleCol).orEmpty(),
                    artist = cursor.getString(artistCol).orEmpty(),
                    album = cursor.getString(albumCol).orEmpty(),
                    albumId = cursor.getLong(albumIdCol),
                    durationMs = cursor.getLong(durationCol),
                    uri = ContentUris.withAppendedId(collection, id)
                )
            }
        }
        return songs
    }
}
