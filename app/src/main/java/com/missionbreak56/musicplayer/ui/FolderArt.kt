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
}
