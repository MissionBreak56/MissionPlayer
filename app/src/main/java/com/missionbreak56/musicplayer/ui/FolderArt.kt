package com.missionbreak56.musicplayer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.security.MessageDigest

/** Custom pictures the user picked for folders, kept in the app's private storage. */
object FolderArt {

    private fun dir(context: Context) = File(context.filesDir, "folder_art")

    fun file(context: Context, path: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(path.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir(context), "$hash.jpg")
    }

    fun has(context: Context, path: String): Boolean = file(context, path).exists()

    /** Playlists reuse this storage; their key can never clash with a folder path (those start with "/"). */
    fun playlistKey(name: String): String = "playlist:$name"

    /** Moves the picture to a new key (a renamed playlist). An existing picture of the target wins. */
    fun move(context: Context, from: String, to: String) {
        if (from == to) return
        val src = file(context, from)
        if (!src.exists()) return
        val dst = file(context, to)
        if (dst.exists() || !src.renameTo(dst)) src.delete()
    }

    fun remove(context: Context, path: String) {
        file(context, path).delete()
    }

    /** Scales the picked image down and stores it. Returns false when it can't be read. */
    fun save(context: Context, path: String, uri: Uri): Boolean {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 768 && bounds.outHeight / (sample * 2) >= 768) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return false
            val target = file(context, path)
            target.parentFile?.mkdirs()
            target.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** Stores an already decoded bitmap (e.g. the cover of a song) as the folder picture. */
    fun saveBitmap(context: Context, path: String, bmp: Bitmap): Boolean {
        return try {
            val target = file(context, path)
            target.parentFile?.mkdirs()
            target.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        } catch (_: Throwable) {
            false
        }
    }
}
