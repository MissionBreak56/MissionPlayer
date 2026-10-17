package com.missionbreak56.musicplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.MusicRepository
import com.missionbreak56.musicplayer.data.Song
import com.missionbreak56.musicplayer.databinding.ActivityMainBinding
import com.missionbreak56.musicplayer.playback.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SongAdapter
    private lateinit var connection: PlayerConnection
    private var controller: MediaController? = null
    private var allSongs: List<Song> = emptyList()

    private val playerListener = object : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = updateMiniPlayer()
        override fun onPlaybackStateChanged(playbackState: Int) = updateMiniPlayer()
        override fun onIsPlayingChanged(isPlaying: Boolean) = updateMiniPlayer()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateMiniPlayer()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) loadSongs() else showEmpty(R.string.permission_required)
    }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = SongAdapter { position -> onSongClicked(position) }
        binding.recyclerSongs.adapter = adapter

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.miniPlayer.setOnClickListener {
            currentSongId()?.let { openPlayer(it) }
        }
        binding.btnMiniPlayPause.setOnClickListener { togglePlayPause() }

        connection = PlayerConnection(this) { c ->
            controller = c
            c.addListener(playerListener)
            updateMiniPlayer()
        }
        connection.connect()

        ensurePermissionAndLoad()
        maybeRequestNotificationPermission()
        setTitle(R.string.app_name)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java)); true
            }
            R.id.action_favorites -> {
                showFavorites(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        controller?.removeListener(playerListener)
        connection.release()
        controller = null
        super.onDestroy()
    }

    // ----- Permissions & loading -----

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun ensurePermissionAndLoad() {
        if (hasAudioPermission()) {
            loadSongs()
        } else {
            val perms = buildList {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
            permissionLauncher.launch(perms)
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun loadSongs() {
        lifecycleScope.launch {
            val songs = withContext(Dispatchers.IO) { MusicRepository.loadSongs(this@MainActivity) }
            allSongs = songs
            binding.editSearch.setText("")
            if (songs.isEmpty()) showEmpty(R.string.no_songs) else showList(songs)
        }
    }

    private fun filter(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) allSongs else allSongs.filter {
            it.displayTitle.lowercase().contains(q) || it.displayArtist.lowercase().contains(q)
        }
        if (filtered.isEmpty()) showEmpty(R.string.no_results) else showList(filtered)
    }

    private fun showFavorites() {
        val favs = allSongs.filter { com.missionbreak56.musicplayer.data.Prefs.isFavorite(it.id) }
        if (favs.isEmpty()) showEmpty(R.string.no_favorites) else showList(favs)
    }

    private fun showList(list: List<Song>) {
        adapter.submit(list)
        binding.recyclerSongs.visibility = View.VISIBLE
        binding.textEmpty.visibility = View.GONE
    }

    private fun showEmpty(messageRes: Int) {
        adapter.submit(emptyList())
        binding.recyclerSongs.visibility = View.GONE
        binding.textEmpty.visibility = View.VISIBLE
        binding.textEmpty.setText(messageRes)
    }

    // ----- Interaction -----

    private fun onSongClicked(position: Int) {
        val song = adapter.songAt(position) ?: return
        openPlayer(song.id)
    }

    private fun openPlayer(songId: Long) {
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra(PlayerActivity.EXTRA_SONG_ID, songId)
        )
    }

    private fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.mediaItemCount == 0) {
                Toast.makeText(this, R.string.pick_a_song, Toast.LENGTH_SHORT).show()
                return
            }
            c.play()
        }
    }

    private fun currentSongId(): Long? =
        controller?.currentMediaItem?.mediaId?.toLongOrNull()

    private fun updateMiniPlayer() {
        val c = controller
        val item = c?.currentMediaItem
        if (c == null || item == null) {
            binding.miniPlayer.visibility = View.GONE
            return
        }
        binding.miniPlayer.visibility = View.VISIBLE
        binding.miniTitle.text = item.mediaMetadata.title ?: getString(R.string.unknown_title)
        binding.miniSubtitle.text = item.mediaMetadata.artist ?: getString(R.string.unknown_artist)
        binding.btnMiniPlayPause.setImageResource(
            if (c.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }
}
