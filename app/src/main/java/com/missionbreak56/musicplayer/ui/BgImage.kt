package com.missionbreak56.musicplayer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.media.ExifInterface
import android.net.Uri
import com.google.android.material.color.MaterialColors
import com.missionbreak56.musicplayer.data.Prefs
import java.io.File

/**
 * Gallery image used as the app background. The picked picture is scaled down and kept in the
 * app's private storage (no permission needed later), and drawn "center crop" under a layer of
 * the theme's background color so text stays readable.
 */
object BgImage {

    private const val MAX_SIDE = 1600

    private fun file(context: Context) = File(context.filesDir, "bg_image.jpg")

    @Volatile private var cached: Bitmap? = null
    @Volatile private var cachedStamp = 0L

    val active: Boolean get() = Prefs.bgImageStamp != 0L

    /** Scales the picked image down, fixes its rotation and stores it. Returns false when unreadable. */
    fun save(context: Context, uri: Uri): Boolean {
        return try {
            val tmp = File(context.cacheDir, "bg_pick.tmp")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { out -> input.copyTo(out) }
            } ?: return false

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tmp.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = BitmapFactory.decodeFile(tmp.path, opts) ?: return false

            val orientation = try {
                ExifInterface(tmp.path).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            } catch (_: Throwable) {
                ExifInterface.ORIENTATION_NORMAL
            }
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }
            if (!matrix.isIdentity) {
                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                if (rotated !== bmp) bmp.recycle()
                bmp = rotated
            }
            file(context).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            tmp.delete()
            cached = null
            cachedStamp = 0L
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun clear(context: Context) {
        file(context).delete()
        cached = null
        cachedStamp = 0L
        Prefs.bgImageStamp = 0L
    }

    private fun bitmap(context: Context): Bitmap? {
        val stamp = Prefs.bgImageStamp
        if (stamp == 0L) return null
        if (cached != null && cachedStamp == stamp) return cached
        val f = file(context)
        if (!f.exists()) return null
        val bmp = try { BitmapFactory.decodeFile(f.path) } catch (_: Throwable) { null }
        cached = bmp
        cachedStamp = stamp
        return bmp
    }

    /** The background drawable for the current settings, or null when no image is set. */
    fun drawable(context: Context): Drawable? {
        val bmp = bitmap(context) ?: return null
        val base = MaterialColors.getColor(context, android.R.attr.colorBackground, 0)
        val coverAlpha = (255 * (100 - Prefs.bgImageVisible) / 100).coerceIn(0, 245)
        val cover = object : Drawable() {
            private val paint = Paint().apply { color = (base and 0x00FFFFFF) or (coverAlpha shl 24) }
            override fun draw(canvas: Canvas) { canvas.drawRect(bounds, paint) }
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: ColorFilter?) {}
            @Suppress("OVERRIDE_DEPRECATION")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }
        return LayerDrawable(arrayOf(CenterCropDrawable(bmp, base), cover))
    }

    /** Draws a bitmap scaled to fill the bounds, cropping the overflow. */
    private class CenterCropDrawable(private val bmp: Bitmap, private val fill: Int) : Drawable() {
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private val matrix = Matrix()

        override fun onBoundsChange(b: android.graphics.Rect) {
            matrix.reset()
            if (b.width() == 0 || b.height() == 0) return
            val scale = maxOf(b.width() / bmp.width.toFloat(), b.height() / bmp.height.toFloat())
            val dx = (b.width() - bmp.width * scale) / 2f
            val dy = (b.height() - bmp.height * scale) / 2f
            matrix.setScale(scale, scale)
            matrix.postTranslate(b.left + dx, b.top + dy)
        }

        override fun draw(canvas: Canvas) {
            canvas.drawColor(fill)
            canvas.save()
            canvas.clipRect(bounds)
            canvas.drawBitmap(bmp, matrix, paint)
            canvas.restore()
        }

        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
        @Suppress("OVERRIDE_DEPRECATION")
        override fun getOpacity(): Int = PixelFormat.OPAQUE
    }
}
