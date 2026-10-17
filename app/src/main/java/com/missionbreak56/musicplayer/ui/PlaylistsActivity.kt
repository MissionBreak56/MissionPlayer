package com.missionbreak56.musicplayer.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.databinding.ActivityPlaylistsBinding
import kotlinx.coroutines.launch

/** All playlists: the automatic ones (favorites, recent...) and the ones the user created. */
class PlaylistsActivity : PlayerBoundActivity() {

    private lateinit var binding: ActivityPlaylistsBinding
    private lateinit var adapter: GroupAdapter

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
