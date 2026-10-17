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
    val uri: Uri,
    val dateAdded: Long,
    val track: Int,
    val path: String
) {
    val displayTitle: String get() = title.ifBlank { Labels.unknownTitle }
    val displayArtist: String get() = artist.ifBlank { Labels.unknownArtist }
    val displayAlbum: String get() = album.ifBlank { Labels.unknownAlbum }

    val folderPath: String get() = path.substringBeforeLast('/', "")
    val folderName: String get() = folderPath.substringAfterLast('/').ifBlank { "/" }
}
