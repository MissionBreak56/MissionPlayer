package com.missionbreak56.musicplayer.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.data.Song
import com.missionbreak56.musicplayer.databinding.ActivitySongPickerBinding
import kotlinx.coroutines.launch

/** Lets the user tick songs from the whole library and add them to one playlist. */
class SongPickerActivity : BaseActivity() {

    private lateinit var binding: ActivitySongPickerBinding
    private lateinit var adapter: SongAdapter
    private var playlist = ""

    /** Songs that are not in the playlist yet, sorted by title. */
    private var candidates: List<Song> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySongPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        playlist = intent.getStringExtra(EXTRA_PLAYLIST).orEmpty()

        binding.textTitle.text = getString(R.string.add_songs_to, playlist)
        adapter = SongAdapter(onClick = { _, _ -> }, onMore = { _, _ -> })
        adapter.keepSelectionOnFilter = true
        adapter.startSelecting()
        adapter.onSelectionChanged = { updateButton() }
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { addSelected() }
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        updateButton()

        lifecycleScope.launch {
            Library.load(this@SongPickerActivity, force = false)
            val inPlaylist = Prefs.playlists()[playlist].orEmpty().toHashSet()
            candidates = Library.sorted(Library.songs.filter { it.id !in inPlaylist }, 0)
            filter(binding.editSearch.text?.toString().orEmpty())
        }
    }

    private fun filter(query: String) {
        val q = Library.sortKey(query.trim())
        val shown = if (q.isEmpty()) {
            candidates
        } else {
            candidates.filter {
                Library.sortKey(it.displayTitle).contains(q) ||
                    Library.sortKey(it.displayArtist).contains(q) ||
                    Library.sortKey(it.displayAlbum).contains(q)
            }
        }
        adapter.submit(shown)
        binding.textEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateButton() {
        val n = adapter.selectedCount
        binding.btnAdd.text = getString(R.string.add_count, n)
        binding.btnAdd.isEnabled = n > 0
    }

    private fun addSelected() {
        val ids = adapter.selectedIds()
        // keep the order of the list (by title) instead of the order of the taps
        val ordered = candidates.filter { it.id in ids }.map { it.id }
        val added = Prefs.addAllToPlaylist(playlist, ordered)
        if (added > 0) {
            Toast.makeText(
                this, resources.getQuantityString(R.plurals.n_added_to_playlist, added, added, playlist),
                Toast.LENGTH_SHORT
            ).show()
        }
        finish()
    }

    companion object {
        const val EXTRA_PLAYLIST = "extra_playlist"
    }
}
