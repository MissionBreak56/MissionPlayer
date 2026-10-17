package com.missionbreak56.musicplayer.data

import android.net.Uri

/** A single local audio track discovered in MediaStore. */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri
) {
    val displayTitle: String get() = title.ifBlank { "Unknown title" }
    val displayArtist: String get() = artist.ifBlank { "Unknown artist" }
    val displayAlbum: String get() = album.ifBlank { "Unknown album" }
}
