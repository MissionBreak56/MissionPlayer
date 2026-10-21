package com.missionbreak56.musicplayer.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
    private lateinit var backCallback: OnBackPressedCallback

    /** Sort chosen for this kind of list: -1 keeps its own order, 0.. matches R.array.sort_options. */
    private var sortOrder = -1

    private val pickFolderArt = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val artKey = artKey()
        if (uri != null && artKey != null) {
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) { FolderArt.save(applicationContext, artKey, uri) }
                if (ok) refresh() else Toast.makeText(this@DetailActivity, R.string.folder_photo_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Where this list's custom ("Personalizada") order is saved. */
    private fun orderScope(): String = "${kind.name}:$key"

    /** Key of the custom picture of this folder or playlist (null for other kinds of list). */
    private fun artKey(): String? = when (kind) {
        Kind.FOLDER -> key
        Kind.PLAYLIST -> FolderArt.playlistKey(key)
        else -> null
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

        // Select several songs of a folder and send them to a playlist
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = adapter.stopSelecting()
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
        adapter.onSelectionChanged = { updateSelectionUi() }
        SongReorder(
            adapter,
            active = { sortOrder == Library.SORT_CUSTOM && !adapter.selecting },
            onSaved = { list -> Prefs.setCustomOrder(orderScope(), list.map { it.id }) }
        ).attach(binding.list)
        adapter.onLongPress = { song -> if (kind == Kind.FOLDER) adapter.startSelecting(song) }
        binding.btnSelClose.setOnClickListener { adapter.stopSelecting() }
        binding.btnSelAll.setOnClickListener { adapter.toggleSelectAll() }
        binding.btnSelAdd.setOnClickListener {
            actions.addManyToPlaylist(adapter.selectedSongs()) { adapter.stopSelecting() }
        }

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
        val songs = if (sortOrder >= 0) Library.sortedFor(group.songs, sortOrder, orderScope()) else group.songs
        adapter.reorderMode = sortOrder == Library.SORT_CUSTOM
        adapter.submit(songs)
        setupIndexBar(songs)
        binding.textEmpty.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
        binding.btnPlay.isEnabled = songs.isNotEmpty()
        binding.btnShuffle.isEnabled = songs.isNotEmpty()
        val customKey = artKey()
        if (customKey != null && FolderArt.has(this, customKey)) {
            ArtLoader.loadFolder(binding.imageArt, customKey, 400)
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
        popup.menu.add(0, 4, 0, R.string.select_songs)
        popup.menu.add(0, 1, 1, R.string.folder_change_photo)
        popup.menu.add(0, 2, 2, R.string.folder_pick_from_songs)
        if (FolderArt.has(this, key)) popup.menu.add(0, 3, 3, R.string.folder_remove_photo)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                4 -> adapter.startSelecting()
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
        val artKey = FolderArt.playlistKey(key)
        popup.menu.add(0, 3, 0, R.string.add_songs)
        popup.menu.add(0, 4, 1, R.string.playlist_change_photo)
        popup.menu.add(0, 5, 2, R.string.folder_pick_from_songs)
        if (FolderArt.has(this, artKey)) popup.menu.add(0, 6, 3, R.string.playlist_remove_photo)
        popup.menu.add(0, 1, 4, R.string.rename)
        popup.menu.add(0, 2, 5, R.string.delete)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                4 -> pickFolderArt.launch(arrayOf("image/*"))
                5 -> FolderArtPicker.pickFromSongs(this, artKey, adapter.current()) { refresh() }
                6 -> {
                    FolderArt.remove(this, artKey)
                    refresh()
                }
                3 -> startActivity(
                    Intent(this, SongPickerActivity::class.java)
                        .putExtra(SongPickerActivity.EXTRA_PLAYLIST, key)
                )
                1 -> Dialogs.promptText(this, R.string.rename, key, R.string.save) { name ->
                    val renamed = Prefs.renamePlaylist(key, name)
                    if (renamed != null) {
                        FolderArt.move(this, FolderArt.playlistKey(key), FolderArt.playlistKey(renamed))
                        Prefs.moveCustomOrder("${Kind.PLAYLIST.name}:$key", "${Kind.PLAYLIST.name}:$renamed")
                        key = renamed
                        refresh()
                    }
                }
                2 -> MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.delete_playlist_confirm, key))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        Prefs.deletePlaylist(key)
                        FolderArt.remove(this, FolderArt.playlistKey(key))
                        Prefs.removeCustomOrder("${Kind.PLAYLIST.name}:$key")
                        finish()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            true
        }
        popup.show()
    }

    private fun updateSelectionUi() {
        val on = adapter.selecting
        binding.selectBar.visibility = if (on) View.VISIBLE else View.GONE
        backCallback.isEnabled = on
        val n = adapter.selectedCount
        binding.textSelCount.text = resources.getQuantityString(R.plurals.n_selected, n, n)
        binding.btnSelAdd.isEnabled = n > 0
        binding.btnSelAdd.alpha = if (n > 0) 1f else 0.4f
        binding.btnSelAll.setText(if (adapter.allSelected()) R.string.select_none else R.string.select_all)
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
