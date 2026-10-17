package com.missionbreak56.musicplayer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.missionbreak56.musicplayer.data.Song
import com.missionbreak56.musicplayer.databinding.ItemSongBinding

class SongAdapter(
    private val onSongClick: (Int) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private val items = ArrayList<Song>()

    fun submit(list: List<Song>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun songAt(position: Int): Song? = items.getOrNull(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SongViewHolder(private val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song) {
            binding.textTitle.text = song.displayTitle
            binding.textSubtitle.text = "${song.displayArtist} • ${song.displayAlbum}"
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onSongClick(pos)
            }
        }
    }
}
