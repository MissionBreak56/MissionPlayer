package com.missionbreak56.musicplayer.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Group
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.data.Song
import com.missionbreak56.musicplayer.databinding.PageListBinding

interface LibraryHost {
    fun onPlay(list: List<Song>, index: Int)
    fun onShuffle(list: List<Song>)
    fun onOpenGroup(group: Group)
    fun onSongMenu(anchor: View, song: Song)
}

/**
 * ViewPager2 adapter where every page is a list: 0 songs, 1 playlists,
 * 2 albums (grid), 3 artists, 4 folders, 5 genres.
 */
class LibraryPagerAdapter(
    private val context: Context,
    private val host: LibraryHost
) : RecyclerView.Adapter<LibraryPagerAdapter.PageHolder>() {

    private val songAdapter = SongAdapter(
        onClick = { list, index -> host.onPlay(list, index) },
        onMore = { view, song -> host.onSongMenu(view, song) }
    )

    private val groupAdapters: Array<GroupAdapter?> = arrayOf(
        null,
        GroupAdapter(false) { host.onOpenGroup(it) },
        GroupAdapter(true) { host.onOpenGroup(it) },
        GroupAdapter(false) { host.onOpenGroup(it) },
        GroupAdapter(false) { host.onOpenGroup(it) },
        GroupAdapter(false) { host.onOpenGroup(it) }
    )

    fun setPlayingId(id: Long) {
        songAdapter.playingId = id
    }

    fun refresh() {
        songAdapter.submit(Library.sorted(Library.songs, Prefs.sortOrder))
        groupAdapters[1]?.submit(Library.playlists(context))
        groupAdapters[2]?.submit(Library.albums())
        groupAdapters[3]?.submit(Library.artists())
        groupAdapters[4]?.submit(Library.folders())
        groupAdapters[5]?.submit(Library.genres())
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder =
        PageHolder(PageListBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PageHolder, position: Int) = holder.bind(position)

    override fun getItemCount(): Int = groupAdapters.size

    inner class PageHolder(private val b: PageListBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(page: Int) {
            val ctx = b.root.context
            if (b.list.layoutManager == null) {
                b.list.layoutManager =
                    if (page == 2) GridLayoutManager(ctx, 2) else LinearLayoutManager(ctx)
            }
            val adapter: RecyclerView.Adapter<*> =
                if (page == 0) songAdapter else groupAdapters[page] ?: songAdapter
            if (b.list.adapter !== adapter) b.list.adapter = adapter

            b.textEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            b.textEmpty.setText(if (page == 0) R.string.no_songs else R.string.empty_list)

            if (page == 0) {
                b.header.visibility = View.VISIBLE
                val count = songAdapter.current().size
                b.textCount.text = ctx.resources.getQuantityString(R.plurals.n_songs, count, count)
                b.btnShuffleAll.setOnClickListener { host.onShuffle(songAdapter.current()) }
                b.btnSort.setOnClickListener { showSortMenu(it) }
            } else {
                b.header.visibility = View.GONE
            }
        }

        private fun showSortMenu(anchor: View) {
            val options = anchor.resources.getStringArray(R.array.sort_options)
            val popup = PopupMenu(anchor.context, anchor)
            options.forEachIndexed { index, label -> popup.menu.add(0, index, index, label) }
            popup.setOnMenuItemClickListener { item ->
                Prefs.sortOrder = item.itemId
                refresh()
                true
            }
            popup.show()
        }
    }
}
