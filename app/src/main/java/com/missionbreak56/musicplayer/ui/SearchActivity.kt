package com.missionbreak56.musicplayer.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.databinding.ActivitySearchBinding
import com.missionbreak56.musicplayer.playback.PlaybackQueue
import kotlinx.coroutines.launch

class SearchActivity : PlayerBoundActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: SongAdapter
    private lateinit var actions: SongActions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        actions = SongActions(this) { filter(binding.editSearch.text?.toString().orEmpty()) }
        adapter = SongAdapter(
            onClick = { list, index -> controller?.let { PlaybackQueue.play(it, list, index) } },
            onMore = { view, song -> actions.showMenu(view, song) }
        )
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.editSearch.requestFocus()

        lifecycleScope.launch {
            Library.load(this@SearchActivity, force = false)
            filter(binding.editSearch.text?.toString().orEmpty())
        }
    }

    private fun filter(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            adapter.submit(emptyList())
            binding.textEmpty.visibility = View.GONE
            return
        }
        val results = Library.songs.filter {
            it.displayTitle.lowercase().contains(q) ||
                it.displayArtist.lowercase().contains(q) ||
                it.displayAlbum.lowercase().contains(q)
        }
        adapter.submit(results)
        binding.textEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onPlayerChanged() {
        super.onPlayerChanged()
        adapter.playingId = controller?.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
    }
}
