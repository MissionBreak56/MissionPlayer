package com.missionbreak56.musicplayer.ui

import android.content.ClipData
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.data.Song
import com.missionbreak56.musicplayer.playback.PlaybackQueue

/** The "three dots" menu of a song and the actions behind it. */
class SongActions(
    private val activity: PlayerBoundActivity,
    private val onChanged: () -> Unit
) {

    private fun toast(res: Int) = Toast.makeText(activity, res, Toast.LENGTH_SHORT).show()

    fun showMenu(anchor: View, song: Song, playlistKey: String? = null) {
        val popup = PopupMenu(activity, anchor)
        val menu = popup.menu
        menu.add(0, 1, 0, R.string.play_next)
        menu.add(0, 2, 1, R.string.add_to_queue)
        menu.add(0, 3, 2, R.string.add_to_playlist)
        menu.add(
            0, 4, 3,
            if (Prefs.isFavorite(song.id)) R.string.remove_favorite else R.string.add_favorite
        )
        menu.add(0, 5, 4, R.string.set_ringtone)
        menu.add(0, 6, 5, R.string.share)
        menu.add(0, 8, 6, R.string.hide_song)
        if (playlistKey != null) menu.add(0, 7, 7, R.string.remove_from_playlist)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> controllerAction { PlaybackQueue.playNext(it, song); toast(R.string.queue_next) }
                2 -> controllerAction { PlaybackQueue.enqueue(it, song); toast(R.string.queue_added) }
                3 -> addToPlaylist(song)
                4 -> toggleFavorite(song)
                5 -> setRingtone(song)
                6 -> share(song)
                8 -> {
                    Library.hideSong(song.id)
                    toast(R.string.song_hidden)
                    onChanged()
                }
                7 -> {
                    if (playlistKey != null) Prefs.removeFromPlaylist(playlistKey, song.id)
                    onChanged()
                }
            }
            true
        }
        popup.show()
    }

    private fun controllerAction(block: (androidx.media3.session.MediaController) -> Unit) {
        activity.controller?.let(block)
    }

    fun toggleFavorite(song: Song) {
        val fav = Prefs.toggleFavorite(song.id)
        toast(if (fav) R.string.added_to_favorites else R.string.removed_from_favorites)
        onChanged()
    }

    fun addToPlaylist(song: Song) {
        val names = Prefs.playlists().keys.toList()
        val items = (listOf(activity.getString(R.string.new_playlist)) + names).toTypedArray()
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.add_to_playlist)
            .setItems(items) { _, which ->
                if (which == 0) {
                    Dialogs.promptText(activity, R.string.new_playlist, "", R.string.create) { name ->
                        Prefs.addToPlaylist(name, song.id)
                        Toast.makeText(
                            activity, activity.getString(R.string.added_to_playlist, name), Toast.LENGTH_SHORT
                        ).show()
                        onChanged()
                    }
                } else {
                    val name = names[which - 1]
                    Prefs.addToPlaylist(name, song.id)
                    Toast.makeText(
                        activity, activity.getString(R.string.added_to_playlist, name), Toast.LENGTH_SHORT
                    ).show()
                    onChanged()
                }
            }
            .show()
    }

    /** Sends several songs to a playlist chosen (or created) in a dialog. */
    fun addManyToPlaylist(songs: List<Song>, onDone: () -> Unit = {}) {
        if (songs.isEmpty()) return
        val names = Prefs.playlists().keys.toList()
        val items = (listOf(activity.getString(R.string.new_playlist)) + names).toTypedArray()
        fun finish(name: String) {
            val added = Prefs.addAllToPlaylist(name, songs.map { it.id })
            if (added < 0) return
            val safeName = name.trim()
            val message = if (added == 0) {
                activity.getString(R.string.already_in_playlist, safeName)
            } else {
                activity.resources.getQuantityString(R.plurals.n_added_to_playlist, added, added, safeName)
            }
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            onChanged()
            onDone()
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.add_to_playlist)
            .setItems(items) { _, which ->
                if (which == 0) {
                    Dialogs.promptText(activity, R.string.new_playlist, "", R.string.create) { name -> finish(name) }
                } else {
                    finish(names[which - 1])
                }
            }
            .show()
    }

    fun setRingtone(song: Song) {
        if (!Settings.System.canWrite(activity)) {
            toast(R.string.ringtone_permission)
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    .setData(Uri.parse("package:${activity.packageName}"))
            )
            return
        }
        try {
            RingtoneManager.setActualDefaultRingtoneUri(activity, RingtoneManager.TYPE_RINGTONE, song.uri)
            toast(R.string.ringtone_set)
        } catch (_: Exception) {
            toast(R.string.ringtone_failed)
        }
    }

    fun share(song: Song) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, song.uri)
            clipData = ClipData.newRawUri(null, song.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(send, null))
    }
}
