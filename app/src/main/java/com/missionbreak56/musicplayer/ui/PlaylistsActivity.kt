package com.missionbreak56.musicplayer.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Group
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.databinding.ActivityPlaylistsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** All playlists: the automatic ones (favorites, recent...) and the ones the user created. */
class PlaylistsActivity : PlayerBoundActivity() {

    private lateinit var binding: ActivityPlaylistsBinding
    private lateinit var adapter: GroupAdapter

    private var pendingPlaylist: String? = null

    private val pickPlaylistArt = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val name = pendingPlaylist
        pendingPlaylist = null
        if (uri != null && name != null) {
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    FolderArt.save(applicationContext, FolderArt.playlistKey(name), uri)
                }
                if (ok) refresh() else Toast.makeText(this@PlaylistsActivity, R.string.folder_photo_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** ⋮ on a playlist row: choose the picture that replaces its small cover. */
    private fun showPhotoMenu(anchor: View, group: Group) {
        val artKey = FolderArt.playlistKey(group.key)
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, R.string.playlist_change_photo)
        popup.menu.add(0, 2, 1, R.string.folder_pick_from_songs)
        if (FolderArt.has(this, artKey)) popup.menu.add(0, 3, 2, R.string.playlist_remove_photo)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    pendingPlaylist = group.key
                    pickPlaylistArt.launch(arrayOf("image/*"))
                }
                2 -> FolderArtPicker.pickFromSongs(this, artKey, group.songs) { refresh() }
                3 -> {
                    FolderArt.remove(this, artKey)
                    refresh()
                }
            }
            true
        }
        popup.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = GroupAdapter(false) { group ->
            startActivity(
                Intent(this, DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_KIND, group.kind.name)
                    .putExtra(DetailActivity.EXTRA_KEY, group.key)
            )
        }
        adapter.onMore = { view, group -> showPhotoMenu(view, group) }
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnNew.setOnClickListener {
            Dialogs.promptText(this, R.string.new_playlist, "", R.string.create) { name ->
                Prefs.createPlaylist(name)
                refresh()
            }
        }

        lifecycleScope.launch {
            Library.load(this@PlaylistsActivity, force = false)
            refresh()
        }
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    private fun refresh() {
        if (!Library.loaded) return
        val groups = Library.playlists(this)
        adapter.submit(groups)
        binding.textEmpty.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
    }
}
