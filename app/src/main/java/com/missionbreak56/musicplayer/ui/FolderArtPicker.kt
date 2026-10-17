package com.missionbreak56.musicplayer.ui

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lets the user pick the picture of a folder from the covers of the songs inside it. */
object FolderArtPicker {

    private const val THUMB = 192
    private const val FULL = 768
    private const val MAX_CANDIDATES = 100

    private class Candidate(val song: Song, val thumb: Bitmap)

    /**
     * Shows a grid with the different covers found in [songs] (one per album / embedded picture).
     * Choosing one stores it as the picture of the folder [path] and calls [onChanged].
     */
    fun pickFromSongs(activity: AppCompatActivity, path: String, songs: List<Song>, onChanged: () -> Unit) {
        val app = activity.applicationContext
        activity.lifecycleScope.launch {
            val candidates = withContext(Dispatchers.IO) {
                songs
                    .distinctBy { if (it.albumId > 0 && it.album.isNotBlank()) "a${it.albumId}" else "s${it.id}" }
                    .take(MAX_CANDIDATES)
                    .mapNotNull { song ->
                        ArtLoader.decodeSync(app, song, THUMB)?.let { Candidate(song, it) }
                    }
            }
            if (activity.isFinishing || activity.isDestroyed) return@launch
            if (candidates.isEmpty()) {
                Toast.makeText(activity, R.string.folder_no_song_art, Toast.LENGTH_SHORT).show()
                return@launch
            }

            var dialog: AlertDialog? = null
            val gridAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemCount(): Int = candidates.size

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pick_art, parent, false)
                    return object : RecyclerView.ViewHolder(v) {}
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val c = candidates[position]
                    holder.itemView.findViewById<ImageView>(R.id.imageArt).setImageBitmap(c.thumb)
                    holder.itemView.setOnClickListener {
                        dialog?.dismiss()
                        activity.lifecycleScope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                val full = ArtLoader.decodeSync(app, c.song, FULL)
                                full != null && FolderArt.saveBitmap(app, path, full)
                            }
                            if (ok) onChanged()
                            else Toast.makeText(app, R.string.folder_photo_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            val list = RecyclerView(activity).apply {
                layoutManager = GridLayoutManager(activity, 3)
                adapter = gridAdapter
                val pad = (12 * activity.resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, 0)
                clipToPadding = false
            }
            dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.folder_pick_from_songs)
                .setView(list)
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}
