package com.missionbreak56.musicplayer.playback

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import com.missionbreak56.musicplayer.data.Song
import kotlin.random.Random

private val ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(displayTitle)
            .setArtist(displayArtist)
            .setAlbumTitle(displayAlbum)
            .setArtworkUri(ContentUris.withAppendedId(ART_BASE, albumId))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
    )
    .build()

/** Helpers that turn "play this list" gestures into player commands. */
object PlaybackQueue {

    /** Asks the service to rebuild the shuffle order so every song plays once before any repeats. */
    val RESHUFFLE = SessionCommand("com.missionbreak56.musicplayer.RESHUFFLE", Bundle.EMPTY)

    /** Tells the service a song was just added to the queue (extras: "index", "next"). */
    val FIX_ADDED = SessionCommand("com.missionbreak56.musicplayer.FIX_ADDED", Bundle.EMPTY)

    private fun reshuffle(controller: MediaController) {
        controller.sendCustomCommand(RESHUFFLE, Bundle.EMPTY)
    }

    private fun fixAdded(controller: MediaController, index: Int, next: Boolean) {
        val args = Bundle().apply {
            putInt("index", index)
            putBoolean("next", next)
        }
        controller.sendCustomCommand(FIX_ADDED, args)
    }

    /** Puts a saved queue back without starting playback. */
    fun restore(controller: MediaController, songs: List<Song>, index: Int, positionMs: Long) {
        if (songs.isEmpty()) return
        controller.setMediaItems(songs.map { it.toMediaItem() }, index.coerceIn(0, songs.lastIndex), positionMs)
        controller.prepare()
        reshuffle(controller)
    }

    /** Replaces the queue with [songs] and starts at [index]. [shuffle] null keeps the current mode. */
    fun play(controller: MediaController, songs: List<Song>, index: Int, shuffle: Boolean? = null) {
        if (songs.isEmpty()) return
        controller.setMediaItems(songs.map { it.toMediaItem() }, index.coerceIn(0, songs.lastIndex), 0L)
        if (shuffle != null) controller.shuffleModeEnabled = shuffle
        reshuffle(controller)
        controller.prepare()
        controller.play()
    }

    fun shuffleAll(controller: MediaController, songs: List<Song>) {
        if (songs.isEmpty()) return
        play(controller, songs, Random.nextInt(songs.size), true)
    }

    fun playNext(controller: MediaController, song: Song) {
        if (controller.mediaItemCount == 0) {
            play(controller, listOf(song), 0)
        } else {
            val at = controller.currentMediaItemIndex + 1
            controller.addMediaItem(at, song.toMediaItem())
            fixAdded(controller, at, true)
        }
    }

    fun enqueue(controller: MediaController, song: Song) {
        if (controller.mediaItemCount == 0) {
            play(controller, listOf(song), 0)
        } else {
            val at = controller.mediaItemCount
            controller.addMediaItem(song.toMediaItem())
            fixAdded(controller, at, false)
        }
    }
}
