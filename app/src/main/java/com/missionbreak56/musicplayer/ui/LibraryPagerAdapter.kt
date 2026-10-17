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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null

    /** False until the first lists have been built, so "loading" is shown instead of "empty". */
    private var ready = false

    private class Snapshot(
        val songs: List<Song>,
        val albums: List<Group>,
        val artists: List<Group>,
        val folders: List<Group>
    )

    /** Rebuilds the lists off the main thread, then shows them. */
    fun refresh() {
        val order = Prefs.sortOrder
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val snap = withContext(Dispatchers.Default) {
                Snapshot(
                    Library.sorted(Library.songs, order),
                    Library.albums(),
                    Library.artists(),
                    Library.folders()
                )
            }
            songAdapter.submit(snap.songs)
            groupAdapters[1]?.submit(snap.albums)
            groupAdapters[2]?.submit(snap.artists)
            groupAdapters[3]?.submit(snap.folders)
            ready = true
            notifyDataSetChanged()
        }
    }

    /** Stops any list rebuild still running (call from the activity's onDestroy). */
    fun release() {
        scope.cancel()
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
            b.textEmpty.setText(
                when {
                    !ready -> R.string.loading
                    page == 0 -> R.string.no_songs
                    else -> R.string.empty_list
                }
            )

            setupIndexBar(page)

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

        /** Letters of the visible list, in display order ("#" for digits/symbols). */
        private fun letters(page: Int): List<String> = when (page) {
            0 -> songAdapter.current().map { Library.letterOf(it.displayTitle) }
            else -> groupAdapters[page]?.current().orEmpty().map { Library.letterOf(it.title) }
        }

        private fun setupIndexBar(page: Int) {
            val density = b.root.resources.displayMetrics.density
            val list = letters(page)
            // The songs list only follows the alphabet when sorted A-Z.
            val alphabetical = page != 0 || Prefs.sortOrder == 0
            val show = alphabetical && list.size > 12
            b.alphaBar.visibility = if (show) View.VISIBLE else View.GONE
            b.textBubble.visibility = View.GONE
            b.list.setPadding(0, 0, if (show) (22 * density).toInt() else 0, (8 * density).toInt())
            if (!show) return
            b.alphaBar.available = list.toSet()
            b.alphaBar.onLetter = { letter ->
                if (letter == null) {
                    b.textBubble.visibility = View.GONE
                } else {
                    b.textBubble.text = letter
                    b.textBubble.visibility = View.VISIBLE
                    // first item whose letter is the chosen one, else the next letter that exists
                    var pos = list.indexOfFirst { it == letter }
                    if (pos < 0) {
                        val all = listOf("#") + ('A'..'Z').map { it.toString() }
                        val from = all.indexOf(letter)
                        pos = list.indexOfFirst { all.indexOf(it) > from }
                        if (pos < 0) pos = list.lastIndex
                    }
                    (b.list.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(pos, 0)
                }
            }
        }

        private fun showSortMenu(anchor: View) {
            val options = anchor.resources.getStringArray(R.array.sort_options)
            val popup = PopupMenu(anchor.context, anchor)
            options.forEachIndexed { index, label ->
                popup.menu.add(0, index, index, label).apply {
                    isCheckable = true
                    isChecked = index == Prefs.sortOrder
                }
            }
            popup.menu.setGroupCheckable(0, true, true)
            popup.setOnMenuItemClickListener { item ->
                Prefs.sortOrder = item.itemId
                refresh()
                true
            }
            popup.show()
        }
    }
}
