package com.missionbreak56.musicplayer.ui

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import android.widget.ImageView
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Loads album art from MediaStore into ImageViews, with a small in-memory cache. */
object ArtLoader {

    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val missing = HashSet<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private fun key(song: Song, size: Int) =
        if (song.albumId > 0 && song.album.isNotBlank()) "a${song.albumId}_$size" else "s${song.id}_$size"

    private fun missingKey(song: Song, size: Int) = "${song.id}_$size"

    private fun decode(context: Context, song: Song, size: Int): Bitmap? {
        val primary = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(song.uri, Size(size, size), null)
            } else {
                val artUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), song.albumId
                )
                context.contentResolver.openInputStream(artUri)?.use { BitmapFactory.decodeStream(it) }
            }
        } catch (_: Exception) {
            null
        }
        return primary ?: decodeEmbedded(context, song, size)
    }

    /** Fallback: read the picture embedded in the audio file when MediaStore has no thumbnail. */
    private fun decodeEmbedded(context: Context, song: Song, size: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, song.uri)
            val bytes = retriever.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= size && bounds.outHeight / (sample * 2) >= size) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Throwable) {
            null
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    }

    /** Suspending variant used when the raw bitmap is needed (e.g. for colour extraction). */
    suspend fun bitmap(context: Context, song: Song, size: Int): Bitmap? {
        val k = key(song, size)
        cache.get(k)?.let { return it }
        val app = context.applicationContext
        val bmp = withContext(Dispatchers.IO) { decode(app, song, size) }
        if (bmp != null) cache.put(k, bmp)
        return bmp
    }

    /** Loads the custom picture of a folder (see [FolderArt]). */
    fun loadFolder(view: ImageView, path: String, size: Int) {
        val file = FolderArt.file(view.context, path)
        val k = "f${path}_${file.lastModified()}_$size"
        view.setTag(R.id.tag_art, k)
        val cached = cache.get(k)
        if (cached != null) {
            view.setImageBitmap(cached)
            return
        }
        view.setImageResource(R.drawable.art_placeholder)
        scope.launch {
            val bmp = withContext(Dispatchers.IO) {
                try {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.path, bounds)
                    var sample = 1
                    while (bounds.outWidth / (sample * 2) >= size && bounds.outHeight / (sample * 2) >= size) sample *= 2
                    BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                } catch (_: Throwable) {
                    null
                }
            }
            if (bmp != null) {
                cache.put(k, bmp)
                if (view.getTag(R.id.tag_art) == k) view.setImageBitmap(bmp)
            }
        }
    }

    fun load(view: ImageView, song: Song?, size: Int) {
        if (song == null) {
            view.setTag(R.id.tag_art, null)
            view.setImageResource(R.drawable.art_placeholder)
            return
        }
        val k = key(song, size)
        view.setTag(R.id.tag_art, k)
        val cached = cache.get(k)
        if (cached != null) {
            view.setImageBitmap(cached)
            return
        }
        view.setImageResource(R.drawable.art_placeholder)
        val mk = missingKey(song, size)
        if (missing.contains(mk)) return
        val app = view.context.applicationContext
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { decode(app, song, size) }
            if (bmp != null) cache.put(k, bmp) else missing.add(mk)
            if (bmp != null && view.getTag(R.id.tag_art) == k) view.setImageBitmap(bmp)
        }
    }
}
