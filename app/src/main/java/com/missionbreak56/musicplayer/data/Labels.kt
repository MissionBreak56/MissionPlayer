package com.missionbreak56.musicplayer.data

import android.content.Context
import com.missionbreak56.musicplayer.R

/** Localised fallbacks for missing tags. Initialised once from [com.missionbreak56.musicplayer.App]. */
object Labels {
    var unknownTitle = "Unknown title"
        private set
    var unknownArtist = "Unknown artist"
        private set
    var unknownAlbum = "Unknown album"
        private set

    fun init(context: Context) {
        unknownTitle = context.getString(R.string.unknown_title)
        unknownArtist = context.getString(R.string.unknown_artist)
        unknownAlbum = context.getString(R.string.unknown_album)
    }
}
