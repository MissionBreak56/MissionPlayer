package com.missionbreak56.musicplayer.playback

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.missionbreak56.musicplayer.ui.SongArt
import java.util.concurrent.Executors

/**
 * Loads the cover shown in the playback notification and on the lock screen. Songs carry a
 * `missionart://song/<id>?album=<albumId>` address: if the user set a custom cover for the song
 * it is used, otherwise the album art from MediaStore. Nothing is cached here, so a new custom
 * cover shows up as soon as the notification is refreshed.
 */
@OptIn(UnstableApi::class)
class SongBitmapLoader(private val context: Context) : BitmapLoader {

    private val fallback = DataSourceBitmapLoader(context)
    private val executor = Executors.newSingleThreadExecutor()

    override fun supportsMimeType(mimeType: String): Boolean = fallback.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = fallback.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        if (uri.scheme != SCHEME) return fallback.loadBitmap(uri)
        val id = uri.lastPathSegment?.toLongOrNull()
        val album = uri.getQueryParameter("album")?.toLongOrNull() ?: 0L
        if (id != null && SongArt.has(context, id)) {
            val future = SettableFuture.create<Bitmap>()
            executor.execute {
                val bmp = SongArt.decode(context, id, 512)
                if (bmp != null) future.set(bmp) else future.setException(IllegalStateException("no cover"))
            }
            return future
        }
        return fallback.loadBitmap(ContentUris.withAppendedId(ART_BASE, album))
    }

    companion object {
        const val SCHEME = "missionart"
        private val ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")

        fun uriFor(songId: Long, albumId: Long): Uri = Uri.parse("$SCHEME://song/$songId?album=$albumId")
    }
}
