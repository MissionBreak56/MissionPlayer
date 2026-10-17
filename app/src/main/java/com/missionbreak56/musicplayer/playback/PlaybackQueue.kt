package com.missionbreak56.musicplayer.playback

import android.content.ContentUris
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
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

    /** Replaces the queue with [songs] and starts at [index]. [shuffle] null keeps the current mode. */
    fun play(controller: MediaController, songs: List<Song>, index: Int, shuffle: Boolean? = null) {
        if (songs.isEmpty()) return
        controller.setMediaItems(songs.map { it.toMediaItem() }, index.coerceIn(0, songs.lastIndex), 0L)
        if (shuffle != null) controller.shuffleModeEnabled = shuffle
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
            controller.addMediaItem(controller.currentMediaItemIndex + 1, song.toMediaItem())
        }
    }

    fun enqueue(controller: MediaController, song: Song) {
        if (controller.mediaItemCount == 0) {
            play(controller, listOf(song), 0)
        } else {
            controller.addMediaItem(song.toMediaItem())
        }
    }
}
