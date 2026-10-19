package com.missionbreak56.musicplayer.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.core.graphics.Insets
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Labels
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.LrcParser
import com.missionbreak56.musicplayer.data.LyricLine
import com.missionbreak56.musicplayer.data.LyricsFetcher
import com.missionbreak56.musicplayer.data.LyricsIndex
import com.missionbreak56.musicplayer.data.LyricsStore
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.databinding.ActivityPlayerBinding
import com.missionbreak56.musicplayer.playback.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** Full "Now playing" screen: art, seek bar, play modes, lyrics, sleep timer, equalizer, queue. */
class PlayerActivity : PlayerBoundActivity() {

    override val allowImageBackground: Boolean get() = false

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var actions: SongActions
    private lateinit var lyricsAdapter: LyricsAdapter

    private var shownId = Long.MIN_VALUE
    private var showLyrics = false
    private var seeking = false
    private var lyricsLines: List<LyricLine> = emptyList()
    private var lyricIndex = -1
    private val triedOnline = HashSet<Long>()

    private var closePending = false
    private var restoreTried = false
    private val closeIfEmpty = Runnable {
        closePending = false
        val c = controller
        if (c != null && c.mediaItemCount == 0 && !isFinishing) finish()
    }

    private val pickLyrics = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importLyrics(uri)
    }

    private val pickCover = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val song = currentSong()
        if (uri != null && song != null) {
            if (SongArt.saveUri(this, song.id, uri)) {
                Toast.makeText(this, tr("Capa aplicada", "Cover applied"), Toast.LENGTH_SHORT).show()
                refreshCover()
            } else {
                Toast.makeText(this, tr("Não foi possível usar essa imagem.", "Could not use this image."), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Reloads the cover and the colours taken from it after it was changed. */
    private fun refreshCover() {
        val c = controller ?: return
        val id = c.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        showSong(c.currentMediaItem, id)
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
        binding.btnPlaylistAdd.setOnClickListener { currentSong()?.let { song -> actions.addToPlaylist(song) } }
        binding.btnEqualizer.setOnClickListener {
            startActivity(Intent(this, EqualizerActivity::class.java))
        }
        binding.btnLyrics.setOnClickListener { toggleLyrics() }
        binding.imageArt.setOnClickListener { toggleLyrics() }
        setupArtSwipe()
        binding.btnQueue.setOnClickListener { showQueue() }
        binding.btnAddLyrics.setOnClickListener { pickLyrics.launch(arrayOf("*/*")) }
        binding.btnAddLyricsPanel.setOnClickListener { pickLyrics.launch(arrayOf("*/*")) }

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

        applySkinForeground()
        animateSkin(fallbackSkin())
        lifecycleScope.launch {
            if (!Library.loaded) {
                Library.load(this@PlayerActivity, force = false)
                shownId = Long.MIN_VALUE
                onPlayerChanged()
            }
        }
    }

    /**
     * Swipe the cover left for the next song and right for the previous one.
     * A plain tap still opens the lyrics.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupArtSwipe() {
        val art = binding.imageArt
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        val density = resources.displayMetrics.density
        var downX = 0f
        var downY = 0f
        var dragging = false

        fun settle() {
            art.animate().translationX(0f).alpha(1f).setDuration(150).start()
        }

        art.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && abs(dx) > slop && abs(dx) > abs(dy)) {
                        dragging = true
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (dragging) {
                        art.animate().cancel()
                        art.translationX = dx * 0.6f
                        art.alpha = 1f - (abs(dx) / (v.width * 1.5f)).coerceIn(0f, 0.5f)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - downX
                    if (!dragging) {
                        v.performClick()
                    } else {
                        val c = controller
                        val threshold = maxOf(v.width * 0.2f, 72 * density)
                        val goNext = dx < -threshold && c?.hasNextMediaItem() == true
                        val goPrev = dx > threshold && c?.hasPreviousMediaItem() == true
                        if (goNext || goPrev) {
                            val dir = if (goNext) -1f else 1f
                            art.animate().translationX(dir * v.width * 0.4f).alpha(0f).setDuration(120)
                                .withEndAction {
                                    if (goNext) c?.seekToNextMediaItem() else c?.seekToPreviousMediaItem()
                                    art.translationX = -dir * v.width * 0.3f
                                    art.animate().translationX(0f).alpha(1f).setDuration(180).start()
                                }
                                .start()
                        } else {
                            settle()
                        }
                    }
                    dragging = false
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (dragging) settle()
                    dragging = false
                    true
                }
                else -> true
            }
        }
    }

    override fun onStop() {
        uiHandler.removeCallbacks(closeIfEmpty)
        closePending = false
        restoreTried = false
        super.onStop()
    }

    /** Lets the album tint fill the area behind the status bar and the notch too. */
    override fun onWindowInsets(bars: Insets) {
        val lp = binding.bgTint.layoutParams as ViewGroup.MarginLayoutParams
        lp.setMargins(-bars.left, -bars.top, -bars.right, -bars.bottom)
        binding.bgTint.layoutParams = lp
    }

    private fun currentSong() =
        Library.song(controller?.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L)

    // ----- Player events -----

    override fun onPlayerChanged() {
        super.onPlayerChanged()
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            // The queue can be empty for a moment (service restarted, queue not restored yet):
            // try to put the last queue back, and only close if it is still empty a few seconds later.
            if (!restoreTried && Library.loaded) {
                restoreTried = true
                PlaybackState.restoreIfEmpty(c)
            }
            if (!closePending) {
                closePending = true
                uiHandler.postDelayed(closeIfEmpty, 4000L)
            }
            return
        }
        if (closePending) {
            closePending = false
            uiHandler.removeCallbacks(closeIfEmpty)
        }
        val item = c.currentMediaItem
        val id = item?.mediaId?.toLongOrNull() ?: -1L
        if (id != shownId) {
            shownId = id
            showSong(item, id)
        }

        binding.btnPlayPause.setImageResource(if (c.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

        val active = fgActive
        val idle = fgIdle
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
            if (showLyrics) active else fgIdle
        )
    }

    private fun showSong(item: MediaItem?, id: Long) {
        binding.textTitle.text = item?.mediaMetadata?.title ?: Labels.unknownTitle
        binding.textTitle.isSelected = true
        binding.textArtist.text = item?.mediaMetadata?.artist ?: Labels.unknownArtist

        val song = Library.song(id)
        ArtLoader.load(binding.imageArt, song, 900)
        lifecycleScope.launch {
            val bitmap = if (song != null) ArtLoader.bitmap(applicationContext, song, 200) else null
            if (shownId != id) return@launch
            val picked = if (bitmap != null) {
                withContext(Dispatchers.Default) { skinFromBitmap(bitmap) }
            } else {
                null
            }
            if (shownId == id) animateSkin(picked ?: fallbackSkin())
        }
        loadLyrics(id)
    }

    // ----- "Liquid glass" background made from the cover colours -----

    private var skinColors: IntArray? = null
    private var skinAnimator: ValueAnimator? = null
    private val fgActive = Color.WHITE
    private val fgIdle = Color.argb(120, 255, 255, 255)

    private fun skinFromBitmap(bitmap: Bitmap): IntArray? = try {
        val p = Palette.from(bitmap).maximumColorCount(16).generate()
        val dominant = p.getDominantColor(0xFF303040.toInt())
        val vibrant = p.getVibrantColor(p.getLightVibrantColor(dominant))
        val deep = p.getDarkVibrantColor(p.getDarkMutedColor(dominant))
        intArrayOf(
            ColorUtils.blendARGB(dominant, Color.BLACK, 0.40f),
            ColorUtils.blendARGB(vibrant, Color.BLACK, 0.38f),
            ColorUtils.blendARGB(deep, Color.BLACK, 0.62f)
        )
    } catch (_: Throwable) {
        null
    }

    private fun fallbackSkin(): IntArray {
        val primary = binding.root.primaryColor()
        return intArrayOf(
            ColorUtils.blendARGB(primary, Color.BLACK, 0.55f),
            ColorUtils.blendARGB(primary, Color.BLACK, 0.62f),
            ColorUtils.blendARGB(primary, Color.BLACK, 0.82f)
        )
    }

    private fun animateSkin(target: IntArray) {
        val from = skinColors
        skinAnimator?.cancel()
        if (from == null) {
            styleSkin(target)
            return
        }
        val eval = ArgbEvaluator()
        skinAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600L
            addUpdateListener { a ->
                val f = a.animatedFraction
                styleSkin(IntArray(3) { i -> eval.evaluate(f, from[i], target[i]) as Int })
            }
            start()
        }
    }

    private fun styleSkin(c: IntArray) {
        skinColors = c
        val w = resources.displayMetrics.widthPixels.toFloat()
        val base = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(c[0], c[1], c[2]))
        fun blob(color: Int, cx: Float, cy: Float, radius: Float) = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = radius
            setGradientCenter(cx, cy)
            setColors(
                intArrayOf(
                    ColorUtils.setAlphaComponent(ColorUtils.blendARGB(color, Color.WHITE, 0.22f), 150),
                    Color.TRANSPARENT
                )
            )
        }
        val sheen = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb(55, 255, 255, 255), Color.argb(0, 255, 255, 255),
                Color.argb(0, 255, 255, 255), Color.argb(70, 0, 0, 0)
            )
        )
        binding.bgTint.background = LayerDrawable(
            arrayOf(base, blob(c[1], 0.12f, 0.16f, w * 1.1f), blob(c[0], 0.95f, 0.72f, w * 1.0f), sheen)
        )
    }

    /** The glass background is always dark, so every control is drawn in white. */
    private fun applySkinForeground() {
        val white = ColorStateList.valueOf(fgActive)
        listOf(
            binding.btnBack, binding.btnPlaylistAdd, binding.btnMore,
            binding.btnFavorite, binding.btnPrev, binding.btnNext, binding.btnQueue,
            binding.btnEqualizer, binding.btnPlayPause
        ).forEach { it.imageTintList = white }
        binding.textTitle.setTextColor(fgActive)
        val soft = Color.argb(190, 255, 255, 255)
        binding.textArtist.setTextColor(soft)
        binding.textPos.setTextColor(soft)
        binding.textDur.setTextColor(soft)
        binding.textLyricsHint.setTextColor(soft)
        binding.textLyricsEmpty.setTextColor(soft)
        binding.seekBar.progressTintList = white
        binding.seekBar.thumbTintList = white
        binding.seekBar.progressBackgroundTintList = ColorStateList.valueOf(Color.argb(70, 255, 255, 255))
        lyricsAdapter.colorActive = fgActive
        lyricsAdapter.colorIdle = Color.argb(150, 255, 255, 255)
        WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = false
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
        syncLyrics(position)
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

            // No lyrics yet: try to download them now (once per song per session), then reload.
            if (lines.isEmpty() && id > 0 && song != null && Prefs.autoLyrics && triedOnline.add(id)) {
                val found = withContext(Dispatchers.IO) {
                    LyricsFetcher.shouldTry(applicationContext, id) &&
                        LyricsFetcher.fetch(applicationContext, song) == LyricsFetcher.Outcome.FOUND
                }
                if (found && shownId == id) loadLyrics(id)
            }
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
        popup.menu.add(0, 2, 1, R.string.set_ringtone)
        popup.menu.add(0, 3, 2, R.string.share)
        if (LyricsStore.has(this, song.id)) popup.menu.add(0, 5, 4, R.string.remove_lyrics)
        popup.menu.add(0, 6, 5, tr("Importar capa", "Import cover"))
        popup.menu.add(0, 7, 6, tr("Buscar capa online", "Find cover online"))
        if (SongArt.has(this, song.id)) popup.menu.add(0, 8, 7, tr("Remover capa personalizada", "Remove custom cover"))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                2 -> actions.setRingtone(song)
                3 -> actions.share(song)
                5 -> {
                    LyricsStore.delete(this, song.id)
                    loadLyrics(song.id)
                }
                6 -> pickCover.launch("image/*")
                7 -> CoverSearch.show(this, song) { refreshCover() }
                8 -> {
                    SongArt.remove(this, song.id)
                    refreshCover()
                }
            }
            true
        }
        popup.show()
    }

    override fun finish() {
        super.finish()
        // ...and slides back down when closed.
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out)
    }
}
