package com.missionbreak56.musicplayer.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.MusicRepository
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.databinding.ActivityPlayerBinding
import com.missionbreak56.musicplayer.playback.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var connection: PlayerConnection
    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refreshUi()
        override fun onIsPlayingChanged(isPlaying: Boolean) = refreshUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.now_playing)

        val songId = intent.getLongExtra(EXTRA_SONG_ID, -1L)
        if (songId <= 0L) {
            finish()
            return
        }

        binding.btnFavorite.setOnClickListener {
            val id = controller?.currentMediaItem?.mediaId?.toLongOrNull()
                ?: return@setOnClickListener
            val fav = Prefs.toggleFavorite(id)
            refreshUi()
            Toast.makeText(
                this,
                if (fav) R.string.added_to_favorites else R.string.removed_from_favorites,
                Toast.LENGTH_SHORT
            ).show()
        }

        connection = PlayerConnection(this) { c ->
            controller = c
            binding.playerView.player = c
            c.addListener(listener)
            startQueue(c, songId)
        }
        connection.connect()
    }

    private fun startQueue(c: MediaController, songId: Long) {
        lifecycleScope.launch {
            val songs = withContext(Dispatchers.IO) { MusicRepository.loadSongs(this@PlayerActivity) }
            if (songs.isEmpty()) {
                Toast.makeText(this@PlayerActivity, R.string.no_songs, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val index = songs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
            val items = songs.map { song ->
                MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(song.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.displayTitle)
                            .setArtist(song.displayArtist)
                            .setAlbumTitle(song.displayAlbum)
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .build()
                    )
                    .build()
            }
            c.setMediaItems(items, index, 0L)
            c.prepare()
            c.play()
            refreshUi()
        }
    }

    private fun refreshUi() {
        val c = controller ?: return
        val item = c.currentMediaItem
        binding.textTitle.text = item?.mediaMetadata?.title ?: getString(R.string.unknown_title)
        binding.textArtist.text = item?.mediaMetadata?.artist ?: getString(R.string.unknown_artist)
        val id = item?.mediaId?.toLongOrNull()
        binding.btnFavorite.setImageResource(
            if (id != null && Prefs.isFavorite(id)) R.drawable.ic_favorite
            else R.drawable.ic_favorite_border
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        controller?.removeListener(listener)
        connection.release()
        controller = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SONG_ID = "extra_song_id"
    }
}
