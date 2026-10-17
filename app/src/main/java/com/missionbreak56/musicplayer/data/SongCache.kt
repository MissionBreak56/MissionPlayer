package com.missionbreak56.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Copy of the last song list on disk, so the lists can appear instantly when the app opens
 * while the real MediaStore scan runs in the background.
 */
object SongCache {

    private const val FILE_NAME = "song_cache.bin"
    private const val VERSION = 1

    fun exists(context: Context): Boolean = File(context.filesDir, FILE_NAME).exists()

    fun read(context: Context): List<Song>? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return try {
            DataInputStream(BufferedInputStream(file.inputStream(), 64 * 1024)).use { input ->
                if (input.readInt() != VERSION) return null
                val count = input.readInt()
                if (count < 0) return null
                val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val songs = ArrayList<Song>(count)
                repeat(count) {
                    val id = input.readLong()
                    songs += Song(
                        id = id,
                        title = input.readUTF(),
                        artist = input.readUTF(),
                        album = input.readUTF(),
                        albumId = input.readLong(),
                        durationMs = input.readLong(),
                        uri = ContentUris.withAppendedId(collection, id),
                        dateAdded = input.readLong(),
                        track = input.readInt(),
                        path = input.readUTF()
                    )
                }
                songs
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun write(context: Context, songs: List<Song>) {
        val target = File(context.filesDir, FILE_NAME)
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        try {
            DataOutputStream(BufferedOutputStream(temp.outputStream(), 64 * 1024)).use { out ->
                out.writeInt(VERSION)
                out.writeInt(songs.size)
                for (s in songs) {
                    out.writeLong(s.id)
                    out.writeUTF(s.title)
                    out.writeUTF(s.artist)
                    out.writeUTF(s.album)
                    out.writeLong(s.albumId)
                    out.writeLong(s.durationMs)
                    out.writeLong(s.dateAdded)
                    out.writeInt(s.track)
                    out.writeUTF(s.path)
                }
            }
            if (!temp.renameTo(target)) {
                target.delete()
                temp.renameTo(target)
            }
        } catch (_: Throwable) {
            temp.delete()
            target.delete()
        }
    }
}
