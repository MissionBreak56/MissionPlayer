package com.missionbreak56.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/**
 * Small helper that connects a UI component to the [PlaybackService] via a
 * MediaController. Call [release] when the component stops.
 */
class PlayerConnection(
    private val context: Context,
    private val onConnected: (MediaController) -> Unit
) {
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener(
            {
                if (!f.isCancelled) {
                    try {
                        val c = f.get()
                        controller = c
                        onConnected(c)
                    } catch (_: Exception) {
                        // connection failed or was released meanwhile
                    }
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun controller(): MediaController? = controller

    fun release() {
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
    }
}
