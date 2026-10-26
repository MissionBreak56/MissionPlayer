package com.missionbreak56.musicplayer.tags

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** An image ready to be embedded: validated, scaled when needed, with the right MIME type. */
class PreparedCover(val bytes: ByteArray, val mime: String, val width: Int, val height: Int)

class InvalidImageException(message: String) : Exception(message)

/**
 * Turns a picked or downloaded image into bytes that are safe to embed. JPEG and PNG pictures that are
 * already small enough are kept byte for byte (no quality loss); anything else is decoded with sub-sampling
 * (so a huge photo is never fully loaded), scaled to at most [MAX_SIDE] pixels keeping its proportions
 * and stored as JPEG.
 */
object CoverImage {

    const val MAX_SIDE = 1200
    private const val KEEP_BYTES = 3 * 1024 * 1024
    private const val MAX_PIXELS = 100_000_000L
    private const val JPEG_QUALITY = 90

    /** Reads and prepares the image behind [uri] (blocking: call from a background thread). */
    fun fromUri(context: Context, uri: Uri): PreparedCover {
        val resolver = context.contentResolver
        return prepare { resolver.openInputStream(uri) }
    }

    /** Prepares an image that is already in memory (e.g. a downloaded cover). */
    fun fromBytes(raw: ByteArray): PreparedCover = prepare { raw.inputStream() }

    private fun prepare(open: () -> InputStream?): PreparedCover {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            (open() ?: throw InvalidImageException("Cannot open the image")).use { BitmapFactory.decodeStream(it, null, bounds) }
        } catch (e: IOException) {
            throw InvalidImageException("Cannot read the image")
        }
        val w = bounds.outWidth
        val h = bounds.outHeight
        val type = bounds.outMimeType
        if (w <= 0 || h <= 0 || type == null) throw InvalidImageException("This file is not a valid image")
        if (w.toLong() * h > MAX_PIXELS) throw InvalidImageException("The image is too large")

        val keepable = (type == "image/jpeg" || type == "image/png") && max(w, h) <= MAX_SIDE
        if (keepable) {
            val raw = try {
                (open() ?: throw InvalidImageException("Cannot open the image")).use { readUpTo(it, KEEP_BYTES + 1) }
            } catch (e: IOException) {
                throw InvalidImageException("Cannot read the image")
            }
            if (raw.size <= KEEP_BYTES) return PreparedCover(raw, type, w, h)
        }

        var sample = 1
        while (max(w, h) / (sample * 2) >= MAX_SIDE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = try {
            (open() ?: throw InvalidImageException("Cannot open the image")).use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: IOException) {
            throw InvalidImageException("Cannot read the image")
        } catch (e: OutOfMemoryError) {
            throw InvalidImageException("The image is too large")
        } ?: throw InvalidImageException("This file is not a valid image")

        var work = decoded
        val longest = max(work.width, work.height)
        if (longest > MAX_SIDE) {
            val ratio = MAX_SIDE.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(
                work, max(1, (work.width * ratio).roundToInt()), max(1, (work.height * ratio).roundToInt()), true
            )
            if (scaled !== work) work.recycle()
            work = scaled
        }
        if (work.hasAlpha()) {
            // JPEG has no transparency: flatten over white instead of getting a black background.
            val flat = Bitmap.createBitmap(work.width, work.height, Bitmap.Config.ARGB_8888)
            Canvas(flat).apply {
                drawColor(Color.WHITE)
                drawBitmap(work, 0f, 0f, null)
            }
            work.recycle()
            work = flat
        }
        val out = ByteArrayOutputStream()
        if (!work.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
            work.recycle()
            throw InvalidImageException("Could not encode the image")
        }
        val result = PreparedCover(out.toByteArray(), "image/jpeg", work.width, work.height)
        work.recycle()
        return result
    }

    private fun readUpTo(input: InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream(minOf(limit, 256 * 1024))
        val buffer = ByteArray(64 * 1024)
        var total = 0
        while (total < limit) {
            val n = input.read(buffer, 0, minOf(buffer.size, limit - total))
            if (n < 0) break
            out.write(buffer, 0, n)
            total += n
        }
        return out.toByteArray()
    }
}
