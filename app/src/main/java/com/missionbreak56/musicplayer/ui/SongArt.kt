package com.missionbreak56.musicplayer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Custom covers the user imported or found online for single songs (private app storage). */
object SongArt {

    private val stamps = ConcurrentHashMap<Long, Long>()
    @Volatile
    private var ready = false

    private fun dir(context: Context) = File(context.filesDir, "song_art")

    fun file(context: Context, songId: Long) = File(dir(context), "$songId.jpg")

    private fun ensure(context: Context) {
        if (ready) return
        synchronized(this) {
            if (ready) return
            dir(context).listFiles()?.forEach { f ->
                val id = f.nameWithoutExtension.toLongOrNull()
                if (id != null) stamps[id] = f.lastModified()
            }
            ready = true
        }
    }

    /** 0 when the song has no custom cover, otherwise a value that changes whenever it changes. */
    fun stamp(context: Context, songId: Long): Long {
        ensure(context)
        return stamps[songId] ?: 0L
    }

    fun has(context: Context, songId: Long): Boolean = stamp(context, songId) > 0L

    fun remove(context: Context, songId: Long) {
        ensure(context)
        file(context, songId).delete()
        stamps.remove(songId)
    }

    fun saveBitmap(context: Context, songId: Long, bmp: Bitmap): Boolean {
        return try {
            ensure(context)
            val target = file(context, songId)
            target.parentFile?.mkdirs()
            target.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            stamps[songId] = System.currentTimeMillis()
            target.setLastModified(stamps[songId] ?: System.currentTimeMillis())
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** Scales a picked image down (max ~1000px) and stores it. */
    fun saveUri(context: Context, songId: Long, uri: Uri): Boolean {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 1000 && bounds.outHeight / (sample * 2) >= 1000) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return false
            saveBitmap(context, songId, bmp)
        } catch (_: Throwable) {
            false
        }
    }

    /** Decodes the stored cover scaled to about [size] px. */
    fun decode(context: Context, songId: Long, size: Int): Bitmap? {
        return try {
            val f = file(context, songId)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= size && bounds.outHeight / (sample * 2) >= size) sample *= 2
            BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (_: Throwable) {
            null
        }
    }
}
