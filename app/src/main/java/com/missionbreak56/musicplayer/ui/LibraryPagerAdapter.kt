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
    fun onFolderMenu(anchor: View, group: Group)
}

/**
 * ViewPager2 adapter where every page is a list. Tab ids: 0 songs, 1 albums (grid),
 * 2 artists, 3 folders. [order] decides which tabs are shown and in what order.
 */
class LibraryPagerAdapter(
    private val context: Context,
    private val host: LibraryHost
) : RecyclerView.Adapter<LibraryPagerAdapter.PageHolder>() {

    var order: List<Int> = Prefs.tabOrder()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val songAdapter = SongAdapter(
        onClick = { list, index -> host.onPlay(list, index) },
        onMore = { view, song -> host.onSongMenu(view, song) }
    )

    private val groupAdapters: Array<GroupAdapter?> = arrayOf(
        null,
        GroupAdapter(true) { host.onOpenGroup(it) },
        GroupAdapter(false) { host.onOpenGroup(it) },
        GroupAdapter(false) { host.onOpenGroup(it) }
    )

    init {
        groupAdapters[3]?.onMore = { view, group -> host.onFolderMenu(view, group) }
    }

    fun setPlayingId(id: Long) {
        songAdapter.playingId = id
    }

    fun refresh() {
        songAdapter.submit(Library.sorted(Library.songs, Prefs.sortOrder))
        groupAdapters[1]?.submit(Library.albums())
        groupAdapters[2]?.submit(Library.artists())
        groupAdapters[3]?.submit(Library.folders())
        notifyDataSetChanged()
    }

    // One view type per tab so a recycled holder never carries another tab's layout manager
    // (this used to turn the songs list into the album grid).
    override fun getItemViewType(position: Int): Int = order[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val b = PageListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val ctx = parent.context
        b.list.layoutManager = if (viewType == 1) GridLayoutManager(ctx, 3) else LinearLayoutManager(ctx)
        return PageHolder(b)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) = holder.bind(order[position])

    override fun getItemCount(): Int = order.size

    inner class PageHolder(private val b: PageListBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(page: Int) {
            val ctx = b.root.context
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
