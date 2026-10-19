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
import com.missionbreak56.musicplayer.data.Song
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

    /** Sort chosen for this kind of list: -1 keeps its own order, 0.. matches R.array.sort_options. */
    private var sortOrder = -1

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
        sortOrder = Prefs.detailSort(kind.name)

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
        binding.btnSort.setOnClickListener { showSortMenu(it) }
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
        val songs = if (sortOrder >= 0) Library.sorted(group.songs, sortOrder) else group.songs
        adapter.submit(songs)
        setupIndexBar(songs)
        binding.textEmpty.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
        binding.btnPlay.isEnabled = songs.isNotEmpty()
        binding.btnShuffle.isEnabled = songs.isNotEmpty()
        if (kind == Kind.FOLDER && FolderArt.has(this, key)) {
            ArtLoader.loadFolder(binding.imageArt, key, 400)
        } else {
            ArtLoader.load(binding.imageArt, group.songs.firstOrNull(), 400)
        }
    }

    /** Letters of the list in display order, or null when it is not sorted alphabetically. */
    private fun indexLetters(songs: List<Song>): List<String>? = when (sortOrder) {
        0 -> songs.map { Library.letterOf(it.displayTitle) }
        1 -> songs.map { Library.letterOf(it.displayArtist) }
        2 -> songs.map { Library.letterOf(it.displayAlbum) }
        else -> null
    }

    /** A-Z strip on the side, shown for long lists sorted by title, artist or album. */
    private fun setupIndexBar(songs: List<Song>) {
        val density = resources.displayMetrics.density
        val letters = indexLetters(songs)
        val show = letters != null && letters.size > 12
        binding.alphaBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.textBubble.visibility = View.GONE
        binding.list.setPadding(0, 0, if (show) (22 * density).toInt() else 0, (8 * density).toInt())
        if (!show || letters == null) return
        binding.alphaBar.available = letters.toSet()
        binding.alphaBar.onLetter = { letter ->
            if (letter == null) {
                binding.textBubble.visibility = View.GONE
            } else {
                binding.textBubble.text = letter
                binding.textBubble.visibility = View.VISIBLE
                // first item with this letter, else the next letter that exists
                var pos = letters.indexOfFirst { it == letter }
                if (pos < 0) {
                    val all = listOf("#") + ('A'..'Z').map { it.toString() }
                    val from = all.indexOf(letter)
                    pos = letters.indexOfFirst { all.indexOf(it) > from }
                    if (pos < 0) pos = letters.lastIndex
                }
                (binding.list.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(pos, 0)
            }
        }
    }

    private fun showSortMenu(anchor: View) {
        val options = resources.getStringArray(R.array.sort_options)
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, DEFAULT_ORDER, 0, tr("Ordem padrão", "Default order")).apply {
            isCheckable = true
            isChecked = sortOrder < 0
        }
        options.forEachIndexed { index, label ->
            popup.menu.add(0, index, index + 1, label).apply {
                isCheckable = true
                isChecked = index == sortOrder
            }
        }
        popup.menu.setGroupCheckable(0, true, true)
        popup.setOnMenuItemClickListener { item ->
            sortOrder = if (item.itemId == DEFAULT_ORDER) -1 else item.itemId
            Prefs.setDetailSort(kind.name, sortOrder)
            refresh()
            binding.list.scrollToPosition(0)
            true
        }
        popup.show()
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
        private const val DEFAULT_ORDER = 100
    }
}
