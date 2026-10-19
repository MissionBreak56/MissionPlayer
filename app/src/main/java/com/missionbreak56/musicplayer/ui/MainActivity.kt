package com.missionbreak56.musicplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Group
import com.missionbreak56.musicplayer.data.Kind
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.data.Song
import com.missionbreak56.musicplayer.databinding.ActivityMainBinding
import com.missionbreak56.musicplayer.playback.PlaybackQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : PlayerBoundActivity(), LibraryHost {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pages: LibraryPagerAdapter
    private lateinit var actions: SongActions
    private var deniedCount = 0

    private var queueRestored = false

    private val libraryListener: () -> Unit = {
        pages.refresh()
        updateRecentTile()
        maybeRestoreQueue()
    }

    /** Puts the last queue back (paused, at the saved position) when the player starts empty. */
    private fun maybeRestoreQueue() {
        val c = controller ?: return
        if (queueRestored || !Library.loaded) return
        queueRestored = true
        if (c.mediaItemCount > 0) return
        val ids = Prefs.lastQueueIds()
        if (ids.isEmpty()) return
        val saved = Prefs.lastIndex.coerceIn(0, ids.lastIndex)
        val songs = ArrayList<Song>()
        var index = 0
        var alive = false
        ids.forEachIndexed { i, id ->
            val song = Library.song(id)
            if (i == saved) {
                index = songs.size
                alive = song != null
            }
            if (song != null) songs.add(song)
        }
        if (songs.isEmpty()) return
        PlaybackQueue.restore(c, songs, index.coerceAtMost(songs.lastIndex), if (alive) Prefs.lastPosition else 0L)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasAudioPermission()) {
            onPermissionGranted()
        } else {
            deniedCount++
            binding.permissionPanel.visibility = View.VISIBLE
        }
    }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actions = SongActions(this) { pages.refresh() }
        pages = LibraryPagerAdapter(this, this)
        binding.pager.adapter = pages
        binding.pager.offscreenPageLimit = 3
        attachTabs()

        binding.tileFav.setOnClickListener { openAuto("fav") }
        binding.tileRecent.setOnClickListener { openAuto("recent") }
        binding.tilePlaylists.setOnClickListener { startActivity(Intent(this, PlaylistsActivity::class.java)) }
        binding.btnSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.btnMenu.contentDescription = tr("Configurações", "Settings")
        binding.btnMenu.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.btnGrant.setOnClickListener {
            if (deniedCount >= 2) {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:$packageName"))
                )
            } else {
                requestAudioPermission()
            }
        }

        Library.addListener(libraryListener)
        // The library may already be loaded (e.g. the screen was recreated): show it right away.
        if (Library.loaded) pages.refresh()
        if (hasAudioPermission()) onPermissionGranted() else requestAudioPermission()
        maybeRequestNotificationPermission()
    }

    private var mediator: TabLayoutMediator? = null

    private fun attachTabs() {
        mediator?.detach()
        val titles = resources.getStringArray(R.array.tabs)
        mediator = TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = titles[pages.order[position]]
        }.also { it.attach() }
    }

    private fun openAuto(key: String) {
        startActivity(
            Intent(this, DetailActivity::class.java)
                .putExtra(DetailActivity.EXTRA_KIND, Kind.AUTO.name)
                .putExtra(DetailActivity.EXTRA_KEY, key)
        )
    }

    private fun updateRecentTile() {
        val last = Prefs.recent().firstNotNullOfOrNull { Library.song(it) }
        if (last == null) binding.tileRecentArt.setImageDrawable(null) else ArtLoader.load(binding.tileRecentArt, last, 320)
    }

    override fun onStart() {
        super.onStart()
        val savedOrder = Prefs.tabOrder()
        if (savedOrder != pages.order) {
            pages.order = savedOrder
            attachTabs()
        }
        if (Library.loaded) updateRecentTile()
        // Pick up files added while the app was in the background.
        if (hasAudioPermission() && Library.loaded) {
            lifecycleScope.launch { Library.load(this@MainActivity) }
        }
    }

    override fun onDestroy() {
        Library.removeListener(libraryListener)
        pages.release()
        super.onDestroy()
    }

    // ----- Permissions -----

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun requestAudioPermission() {
        permissionLauncher.launch(arrayOf(audioPermission()))
    }

    private fun onPermissionGranted() {
        binding.permissionPanel.visibility = View.GONE
        lifecycleScope.launch { Library.load(this@MainActivity) }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ----- LibraryHost -----

    override fun onPlay(list: List<Song>, index: Int) {
        controller?.let { PlaybackQueue.play(it, list, index) }
    }

    override fun onShuffle(list: List<Song>) {
        controller?.let { PlaybackQueue.shuffleAll(it, list) }
    }

    override fun onOpenGroup(group: Group) {
        startActivity(
            Intent(this, DetailActivity::class.java)
                .putExtra(DetailActivity.EXTRA_KIND, group.kind.name)
                .putExtra(DetailActivity.EXTRA_KEY, group.key)
        )
    }

    private var pendingFolder: String? = null

    private val pickFolderArt = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val path = pendingFolder
        pendingFolder = null
        if (uri != null && path != null) {
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) { FolderArt.save(applicationContext, path, uri) }
                if (ok) pages.refresh() else Toast.makeText(this@MainActivity, R.string.folder_photo_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onFolderMenu(anchor: View, group: Group) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, R.string.folder_change_photo)
        popup.menu.add(0, 4, 1, R.string.folder_pick_from_songs)
        if (FolderArt.has(this, group.key)) popup.menu.add(0, 2, 2, R.string.folder_remove_photo)
        popup.menu.add(0, 3, 3, R.string.hide_folder)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    pendingFolder = group.key
                    pickFolderArt.launch(arrayOf("image/*"))
                }
                4 -> FolderArtPicker.pickFromSongs(this, group.key, group.songs) { pages.refresh() }
                2 -> {
                    FolderArt.remove(this, group.key)
                    pages.refresh()
                }
                3 -> MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.hide_folder_confirm, group.title))
                    .setPositiveButton(R.string.hide_folder) { _, _ ->
                        Library.hideFolder(group.key)
                        Toast.makeText(this, R.string.folder_hidden, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            true
        }
        popup.show()
    }

    override fun onSongMenu(anchor: View, song: Song) {
        actions.showMenu(anchor, song)
    }

    // ----- Player events -----

    override fun onPlayerChanged() {
        super.onPlayerChanged()
        pages.setPlayingId(controller?.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L)
    }

    override fun onControllerReady(c: MediaController) {
        pages.setPlayingId(c.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L)
        maybeRestoreQueue()
    }
}
