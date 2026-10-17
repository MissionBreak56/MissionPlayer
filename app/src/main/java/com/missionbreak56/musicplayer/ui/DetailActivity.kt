package com.missionbreak56.musicplayer.ui

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Kind
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.databinding.ActivityDetailBinding
import com.missionbreak56.musicplayer.playback.PlaybackQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Songs of one album, artist, folder, genre or playlist. */
class DetailActivity : PlayerBoundActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var adapter: SongAdapter
    private lateinit var actions: SongActions
    private var kind = Kind.ALBUM
    private var key = ""

    private val pickFolderArt = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && kind == Kind.FOLDER) {
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) { FolderArt.save(applicationContext, key, uri) }
                if (ok) refresh() else Toast.makeText(this@DetailActivity, R.string.folder_photo_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        kind = try {
            Kind.valueOf(intent.getStringExtra(EXTRA_KIND).orEmpty())
        } catch (_: IllegalArgumentException) {
            Kind.ALBUM
        }
        key = intent.getStringExtra(EXTRA_KEY).orEmpty()

        actions = SongActions(this) { refresh() }
        adapter = SongAdapter(
            onClick = { list, index -> controller?.let { PlaybackQueue.play(it, list, index) } },
            onMore = { view, song ->
                actions.showMenu(view, song, if (kind == Kind.PLAYLIST) key else null)
            }
        )
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnMore.visibility =
            if (kind == Kind.PLAYLIST || kind == Kind.FOLDER) View.VISIBLE else View.GONE
        binding.btnMore.setOnClickListener {
            if (kind == Kind.FOLDER) showFolderMenu(it) else showPlaylistMenu(it)
        }
        binding.btnPlay.setOnClickListener {
            val songs = adapter.current()
            controller?.let { PlaybackQueue.play(it, songs, 0) }
        }
        binding.btnShuffle.setOnClickListener {
            val songs = adapter.current()
            controller?.let { PlaybackQueue.shuffleAll(it, songs) }
        }

        lifecycleScope.launch {
            Library.load(this@DetailActivity, force = false)
            refresh()
        }
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    private fun refresh() {
        if (!Library.loaded) return
        val group = Library.group(this, kind, key)
        if (group == null) {
            finish()
            return
        }
        binding.textBarTitle.text = group.title
        binding.textName.text = group.title
        val count = resources.getQuantityString(R.plurals.n_songs, group.songs.size, group.songs.size)
        binding.textInfo.text = if (group.extra.isNotBlank()) "${group.extra}\n$count" else count
        adapter.submit(group.songs)
        binding.textEmpty.visibility = if (group.songs.isEmpty()) View.VISIBLE else View.GONE
        binding.btnPlay.isEnabled = group.songs.isNotEmpty()
        binding.btnShuffle.isEnabled = group.songs.isNotEmpty()
        if (kind == Kind.FOLDER && FolderArt.has(this, key)) {
            ArtLoader.loadFolder(binding.imageArt, key, 400)
        } else {
            ArtLoader.load(binding.imageArt, group.songs.firstOrNull(), 400)
        }
    }

    private fun showFolderMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, R.string.folder_change_photo)
        popup.menu.add(0, 2, 1, R.string.folder_pick_from_songs)
        if (FolderArt.has(this, key)) popup.menu.add(0, 3, 2, R.string.folder_remove_photo)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> pickFolderArt.launch(arrayOf("image/*"))
                2 -> FolderArtPicker.pickFromSongs(this, key, adapter.current()) { refresh() }
                3 -> {
                    FolderArt.remove(this, key)
                    refresh()
                }
            }
            true
        }
        popup.show()
    }

    private fun showPlaylistMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, R.string.rename)
        popup.menu.add(0, 2, 1, R.string.delete)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> Dialogs.promptText(this, R.string.rename, key, R.string.save) { name ->
                    val renamed = Prefs.renamePlaylist(key, name)
                    if (renamed != null) {
                        key = renamed
                        refresh()
                    }
                }
                2 -> MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.delete_playlist_confirm, key))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        Prefs.deletePlaylist(key)
                        finish()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            true
        }
        popup.show()
    }

    override fun onPlayerChanged() {
        super.onPlayerChanged()
        adapter.playingId = controller?.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
    }

    companion object {
        const val EXTRA_KIND = "extra_kind"
        const val EXTRA_KEY = "extra_key"
    }
}
