package com.missionbreak56.musicplayer.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Labels
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.LrcParser
import com.missionbreak56.musicplayer.data.LyricLine
import com.missionbreak56.musicplayer.data.LyricsIndex
import com.missionbreak56.musicplayer.data.LyricsStore
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.databinding.ActivityPlayerBinding
import com.missionbreak56.musicplayer.playback.SleepTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Full "Now playing" screen: art, seek bar, play modes, lyrics, sleep timer, equalizer, queue. */
class PlayerActivity : PlayerBoundActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var actions: SongActions
    private lateinit var lyricsAdapter: LyricsAdapter

    private var shownId = Long.MIN_VALUE
    private var showLyrics = false
    private var seeking = false
    private var lyricsLines: List<LyricLine> = emptyList()
    private var lyricIndex = -1

    private val pickLyrics = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importLyrics(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actions = SongActions(this) { onPlayerChanged() }
        lyricsAdapter = LyricsAdapter { line ->
            if (line.timeMs >= 0) controller?.seekTo(line.timeMs)
        }
        binding.lyricsList.layoutManager = LinearLayoutManager(this)
        binding.lyricsList.adapter = lyricsAdapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnMore.setOnClickListener { showMoreMenu(it) }
        binding.btnPlayPause.setOnClickListener { togglePlay() }
        binding.btnNext.setOnClickListener { controller?.seekToNextMediaItem() }
        binding.btnPrev.setOnClickListener { controller?.seekToPrevious() }
        binding.btnShuffle.setOnClickListener {
            controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
        }
        binding.btnRepeat.setOnClickListener {
            controller?.let {
                it.repeatMode = when (it.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }
        }
        binding.btnFavorite.setOnClickListener {
            currentSong()?.let { song -> actions.toggleFavorite(song) }
        }
        binding.btnTimer.setOnClickListener { Dialogs.sleepTimer(this) { updateTimerLabel() } }
        binding.btnEqualizer.setOnClickListener {
            startActivity(Intent(this, EqualizerActivity::class.java))
        }
        binding.btnLyrics.setOnClickListener { toggleLyrics() }
        binding.imageArt.setOnClickListener { toggleLyrics() }
        binding.btnQueue.setOnClickListener { showQueue() }
        binding.btnAddLyrics.setOnClickListener { pickLyrics.launch(arrayOf("*/*")) }
        binding.btnAddLyricsPanel.setOnClickListener { pickLyrics.launch(arrayOf("*/*")) }
        binding.btnThemes.setOnClickListener { startActivity(Intent(this, ThemesActivity::class.java)) }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.textPos.text = Format.time(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                seeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                controller?.seekTo((seekBar?.progress ?: 0).toLong())
                seeking = false
            }
        })

        applyTint(null)
        lifecycleScope.launch {
            if (!Library.loaded) {
                Library.load(this@PlayerActivity, force = false)
                shownId = Long.MIN_VALUE
                onPlayerChanged()
            }
        }
    }

    private fun currentSong() =
        Library.song(controller?.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L)

    // ----- Player events -----

    override fun onPlayerChanged() {
        super.onPlayerChanged()
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            finish()
            return
        }
        val item = c.currentMediaItem
        val id = item?.mediaId?.toLongOrNull() ?: -1L
        if (id != shownId) {
            shownId = id
            showSong(item, id)
        }

        binding.btnPlayPause.setImageResource(if (c.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

        val active = binding.root.primaryColor()
        val idle = binding.root.onSurfaceVariantColor()
        binding.btnShuffle.imageTintList = ColorStateList.valueOf(if (c.shuffleModeEnabled) active else idle)
        binding.btnRepeat.setImageResource(
            if (c.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat
        )
        binding.btnRepeat.imageTintList = ColorStateList.valueOf(
            if (c.repeatMode == Player.REPEAT_MODE_OFF) idle else active
        )
        binding.btnFavorite.setImageResource(
            if (id > 0 && Prefs.isFavorite(id)) R.drawable.ic_star else R.drawable.ic_star_border
        )
        binding.btnLyrics.imageTintList = ColorStateList.valueOf(
            if (showLyrics) active else binding.root.onSurfaceColor()
        )
    }

    private fun showSong(item: MediaItem?, id: Long) {
        binding.textTitle.text = item?.mediaMetadata?.title ?: Labels.unknownTitle
        binding.textTitle.isSelected = true
        binding.textArtist.text = item?.mediaMetadata?.artist ?: Labels.unknownArtist

        val song = Library.song(id)
        ArtLoader.load(binding.imageArt, song, 900)
        applyTint(null)
        if (song != null) {
            lifecycleScope.launch {
                val bitmap = ArtLoader.bitmap(applicationContext, song, 900)
                if (bitmap != null && shownId == id) {
                    val color = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).generate().getDominantColor(0)
                    }
                    if (color != 0 && shownId == id) applyTint(color)
                }
            }
        }
        loadLyrics(id)
    }

    private fun applyTint(color: Int?) {
        val bg = MaterialColors.getColor(binding.root, android.R.attr.colorBackground)
        val top = if (color == null) bg else ColorUtils.blendARGB(bg, color, 0.45f)
        binding.bgTint.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bg)
        )
    }

    override fun onTick() {
        super.onTick()
        val c = controller ?: return
        val songDuration = Library.song(shownId)?.durationMs ?: 0L
        val duration = if (c.duration > 0) c.duration else songDuration
        val position = c.currentPosition.coerceAtLeast(0L)
        if (!seeking) {
            binding.seekBar.max = duration.toInt().coerceAtLeast(1)
            binding.seekBar.progress = position.toInt().coerceIn(0, binding.seekBar.max)
            binding.textPos.text = Format.time(position)
            binding.textDur.text = Format.time(duration)
        }
        updateTimerLabel()
        syncLyrics(position)
    }

    private fun updateTimerLabel() {
        binding.textTimer.text = if (SleepTimer.isActive) Format.time(SleepTimer.remainingMs()) else ""
    }

    // ----- Lyrics -----

    private fun toggleLyrics() {
        showLyrics = !showLyrics
        binding.imageArt.visibility = if (showLyrics) View.INVISIBLE else View.VISIBLE
        binding.lyricsPanel.visibility = if (showLyrics) View.VISIBLE else View.GONE
        lyricIndex = -2
        onPlayerChanged()
    }

    private fun loadLyrics(id: Long) {
        lifecycleScope.launch {
            val song = Library.song(id)
            val lines = if (id > 0 && song != null) {
                withContext(Dispatchers.IO) {
                    val text = LyricsIndex.fullText(applicationContext, song)
                    if (text.isNullOrBlank()) emptyList() else LrcParser.parse(text)
                }
            } else {
                emptyList()
            }
            if (shownId != id) return@launch
            lyricsLines = lines
            lyricIndex = -2
            lyricsAdapter.submit(lines)
            binding.lyricsList.visibility = if (lines.isEmpty()) View.GONE else View.VISIBLE
            binding.lyricsEmpty.visibility = if (lines.isEmpty()) View.VISIBLE else View.GONE
            binding.textLyricsHint.text = if (lines.isEmpty()) getString(R.string.no_lyrics_short) else ""
            binding.textLyricsHint.visibility = if (lines.isEmpty()) View.VISIBLE else View.GONE
            binding.btnAddLyrics.visibility = if (lines.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun syncLyrics(position: Long) {
        if (!showLyrics || lyricsLines.isEmpty() || lyricsLines.first().timeMs < 0) return
        var index = -1
        for (i in lyricsLines.indices) {
            if (lyricsLines[i].timeMs <= position) index = i else break
        }
        if (index != lyricIndex) {
            lyricIndex = index
            lyricsAdapter.setCurrent(index)
            if (index >= 0) {
                (binding.lyricsList.layoutManager as LinearLayoutManager)
                    .scrollToPositionWithOffset(index, binding.lyricsList.height / 3)
            }
        }
    }

    private fun importLyrics(uri: Uri) {
        val id = shownId
        if (id <= 0) return
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val text = contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                    if (text.isNullOrBlank()) {
                        false
                    } else {
                        LyricsStore.save(applicationContext, id, text)
                        true
                    }
                } catch (_: Exception) {
                    false
                }
            }
            if (ok) {
                Toast.makeText(this@PlayerActivity, R.string.lyrics_saved, Toast.LENGTH_SHORT).show()
                loadLyrics(id)
            } else {
                Toast.makeText(this@PlayerActivity, R.string.lyrics_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ----- Menu -----

    private fun showMoreMenu(anchor: View) {
        val song = currentSong() ?: return
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, R.string.add_to_playlist)
        popup.menu.add(0, 2, 1, R.string.set_ringtone)
        popup.menu.add(0, 3, 2, R.string.share)
        popup.menu.add(0, 4, 3, R.string.add_lyrics)
        if (LyricsStore.has(this, song.id)) popup.menu.add(0, 5, 4, R.string.remove_lyrics)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> actions.addToPlaylist(song)
                2 -> actions.setRingtone(song)
                3 -> actions.share(song)
                4 -> pickLyrics.launch(arrayOf("*/*"))
                5 -> {
                    LyricsStore.delete(this, song.id)
                    loadLyrics(song.id)
                }
            }
            true
        }
        popup.show()
    }
}
