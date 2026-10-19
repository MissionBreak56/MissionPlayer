package com.missionbreak56.musicplayer.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Group
import com.missionbreak56.musicplayer.data.Kind
import com.missionbreak56.musicplayer.data.LyricLine
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Song
import com.missionbreak56.musicplayer.databinding.ItemLyricBinding
import com.missionbreak56.musicplayer.databinding.ItemSongBinding
import com.missionbreak56.musicplayer.databinding.ItemThemeBinding

class SongAdapter(
    private val onClick: (List<Song>, Int) -> Unit,
    private val onMore: (View, Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.VH>() {

    private var items: List<Song> = emptyList()

    /** Optional per-song line shown instead of "artist | album" (e.g. a lyrics match). */
    var snippets: Map<Long, String> = emptyMap()

    var playingId: Long = -1L
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    fun submit(list: List<Song>) {
        if (list == items) return
        items = list
        notifyDataSetChanged()
    }

    fun current(): List<Song> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class VH(private val b: ItemSongBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(song: Song) {
            b.textTitle.text = song.displayTitle
            b.textTitle.setTextColor(if (song.id == playingId) b.root.primaryColor() else b.root.onSurfaceColor())
            val snippet = snippets[song.id]
            b.textSubtitle.text = if (snippet != null) {
                "\u266A $snippet"
            } else {
                "${song.displayArtist} | ${song.displayAlbum}"
            }
            b.textDuration.text = Format.time(song.durationMs)
            ArtLoader.load(b.imageArt, song, 160)
            b.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onClick(items, pos)
            }
            b.btnMore.setOnClickListener { onMore(it, song) }
        }
    }
}

class GroupAdapter(
    private val grid: Boolean,
    private val onClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.VH>() {

    private var items: List<Group> = emptyList()

    fun current(): List<Group> = items

    /** Overflow button shown on folder rows (hide folder). */
    var onMore: ((View, Group) -> Unit)? = null

    fun submit(list: List<Group>) {
        if (list == items) return
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = if (grid) R.layout.item_album_grid else R.layout.item_group_list
        return VH(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val art: ImageView = view.findViewById(R.id.imageArt)
        private val title: TextView = view.findViewById(R.id.textTitle)
        private val subtitle: TextView = view.findViewById(R.id.textSubtitle)

        fun bind(group: Group) {
            val res = itemView.resources
            title.text = group.title
            val count = res.getQuantityString(R.plurals.n_songs, group.songs.size, group.songs.size)
            subtitle.text = if (group.extra.isNotBlank()) "${group.extra} \u00B7 $count" else count

            val iconRes = when {
                group.kind == Kind.FOLDER -> R.drawable.ic_folder
                group.kind == Kind.GENRE -> R.drawable.ic_genre
                (group.kind == Kind.PLAYLIST || group.kind == Kind.AUTO) && group.songs.isEmpty() ->
                    R.drawable.ic_playlist
                else -> 0
            }
            if (iconRes != 0) {
                val pad = (res.displayMetrics.density * 6).toInt()
                art.setTag(R.id.tag_art, null)
                art.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                art.scaleType = ImageView.ScaleType.CENTER_INSIDE
                art.setPadding(pad, pad, pad, pad)
                art.imageTintList = ColorStateList.valueOf(itemView.primaryColor())
                art.setImageResource(iconRes)
            } else {
                art.scaleType = ImageView.ScaleType.CENTER_CROP
                art.setBackgroundResource(R.color.surface_high)
                art.setPadding(0, 0, 0, 0)
                art.imageTintList = null
                ArtLoader.load(art, group.songs.firstOrNull(), 320)
            }
            itemView.setOnClickListener { onClick(group) }

            val more: View? = itemView.findViewById(R.id.btnMore)
            val chevron: View? = itemView.findViewById(R.id.chevron)
            val handler = onMore
            if (more != null) {
                val show = handler != null && group.kind == Kind.FOLDER
                more.visibility = if (show) View.VISIBLE else View.GONE
                chevron?.visibility = if (show) View.GONE else View.VISIBLE
                more.setOnClickListener { v -> handler?.invoke(v, group) }
            }
            itemView.setOnLongClickListener {
                if (handler != null && group.kind == Kind.FOLDER) {
                    handler(itemView, group)
                    true
                } else {
                    false
                }
            }
        }
    }
}

class QueueEntry(val mediaId: String, val title: String, val artist: String)

class QueueAdapter(
    private val items: List<QueueEntry>,
    private val currentIndex: Int,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<QueueAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position)

    override fun getItemCount(): Int = items.size

    inner class VH(private val b: ItemSongBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(entry: QueueEntry, position: Int) {
            b.textTitle.text = entry.title
            b.textTitle.setTextColor(
                if (position == currentIndex) b.root.primaryColor() else b.root.onSurfaceColor()
            )
            b.textSubtitle.text = entry.artist
            b.textDuration.visibility = View.GONE
            b.btnMore.visibility = View.GONE
            ArtLoader.load(b.imageArt, Library.song(entry.mediaId.toLongOrNull() ?: -1L), 160)
            b.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onClick(pos)
            }
        }
    }
}

class LyricsAdapter(
    private val onLineClick: (LyricLine) -> Unit
) : RecyclerView.Adapter<LyricsAdapter.VH>() {

    private var lines: List<LyricLine> = emptyList()
    private var current = -1

    /** Optional colours (used by the full-screen player's coloured background). */
    var colorActive: Int? = null
    var colorIdle: Int? = null

    fun submit(list: List<LyricLine>) {
        lines = list
        current = -1
        notifyDataSetChanged()
    }

    fun setCurrent(index: Int) {
        val old = current
        current = index
        if (old >= 0) notifyItemChanged(old)
        if (index >= 0) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemLyricBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(lines[position], position)

    override fun getItemCount(): Int = lines.size

    inner class VH(private val b: ItemLyricBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(line: LyricLine, position: Int) {
            val tv = b.root
            tv.text = line.text.ifBlank { "\u266A" }
            val active = position == current
            tv.setTextColor(
                if (active) colorActive ?: tv.primaryColor() else colorIdle ?: tv.onSurfaceVariantColor()
            )
            tv.textSize = if (active) 24f else 20f
            tv.setOnClickListener { onLineClick(line) }
        }
    }
}

class ThemeAdapter(
    private val names: Array<String>,
    private var selected: Int,
    private val onPick: (Int) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(position)

    override fun getItemCount(): Int = Themes.colors.size

    inner class VH(private val b: ItemThemeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(position: Int) {
            val color = ContextCompat.getColor(b.root.context, Themes.colors[position])
            b.dot.backgroundTintList = ColorStateList.valueOf(color)
            b.check.visibility = if (position == selected) View.VISIBLE else View.GONE
            b.textName.text = names.getOrElse(position) { "" }
            b.root.setOnClickListener {
                selected = position
                notifyDataSetChanged()
                onPick(position)
            }
        }
    }
}
