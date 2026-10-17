package com.missionbreak56.musicplayer.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Labels
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Song
import com.missionbreak56.musicplayer.databinding.SheetQueueBinding
import com.missionbreak56.musicplayer.playback.PlayerConnection
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base for screens that talk to the playback service. Connects on start,
 * disconnects on stop, and drives the mini player if the layout includes one.
 */
abstract class PlayerBoundActivity : BaseActivity() {

    var controller: MediaController? = null
        private set

    protected val uiHandler = Handler(Looper.getMainLooper())
    private var connection: PlayerConnection? = null
    private var miniShownId = -1L

    private val tick = object : Runnable {
        override fun run() {
            onTick()
            uiHandler.postDelayed(this, 500L)
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            onPlayerChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        bindMiniPlayer()
        val conn = PlayerConnection(this) { c ->
            controller = c
            c.addListener(listener)
            onControllerReady(c)
            onPlayerChanged()
        }
        connection = conn
        conn.connect()
        uiHandler.post(tick)
    }

    override fun onStop() {
        uiHandler.removeCallbacks(tick)
        controller?.removeListener(listener)
        connection?.release()
        connection = null
        controller = null
        super.onStop()
    }

    protected open fun onControllerReady(c: MediaController) {}

    protected open fun onPlayerChanged() {
        updateMiniPlayer()
    }

    protected open fun onTick() {
        updateMiniProgress()
    }

    // ----- Mini player -----

    private fun bindMiniPlayer() {
        (findViewById<View>(R.id.miniPlayer) as? MaterialCardView)?.apply {
            setCardBackgroundColor(Color.TRANSPARENT)
            strokeColor = Color.argb(96, 255, 255, 255)
            strokeWidth = (1 * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        }
        findViewById<View>(R.id.miniPlayer)?.setOnClickListener { openPlayer() }
        findViewById<View>(R.id.miniPlay)?.setOnClickListener { togglePlay() }
        findViewById<View>(R.id.miniNext)?.setOnClickListener { controller?.seekToNextMediaItem() }
        findViewById<View>(R.id.miniQueue)?.setOnClickListener { showQueue() }
    }

    fun openPlayer() {
        startActivity(Intent(this, PlayerActivity::class.java))
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    private fun updateMiniPlayer() {
        val card = findViewById<View>(R.id.miniPlayer) ?: return
        val c = controller
        val item = c?.currentMediaItem
        if (c == null || item == null) {
            card.visibility = View.GONE
            return
        }
        card.visibility = View.VISIBLE
        findViewById<TextView>(R.id.miniTitle).text = item.mediaMetadata.title ?: Labels.unknownTitle
        findViewById<TextView>(R.id.miniSubtitle).text = item.mediaMetadata.artist ?: Labels.unknownArtist
        findViewById<ImageButton>(R.id.miniPlay).setImageResource(
            if (c.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        val id = item.mediaId.toLongOrNull() ?: -1L
        if (id != miniShownId) {
            miniShownId = id
            val song = Library.song(id)
            ArtLoader.load(findViewById(R.id.miniArt), song, 160)
            updateMiniColors(song)
        }
    }

    // ----- Mini player "liquid glass" colours taken from the cover art -----

    private var miniColors: IntArray? = null
    private var miniColorJob: Job? = null
    private var miniAnimator: ValueAnimator? = null

    private fun updateMiniColors(song: Song?) {
        miniColorJob?.cancel()
        miniColorJob = lifecycleScope.launch {
            val card = findViewById<View>(R.id.miniPlayer) ?: return@launch
            val container = MaterialColors.getColor(card, com.google.android.material.R.attr.colorPrimaryContainer)
            val primary = MaterialColors.getColor(card, androidx.appcompat.R.attr.colorPrimary)
            var target = intArrayOf(
                container,
                ColorUtils.blendARGB(container, primary, 0.35f),
                ColorUtils.blendARGB(container, Color.BLACK, 0.25f)
            )
            val bitmap = if (song != null) ArtLoader.bitmap(applicationContext, song, 96) else null
            if (bitmap != null) {
                val picked = withContext(Dispatchers.Default) {
                    try {
                        val p = Palette.from(bitmap).maximumColorCount(16).generate()
                        val dominant = p.getDominantColor(container)
                        val vibrant = p.getVibrantColor(p.getLightVibrantColor(dominant))
                        val deep = p.getDarkVibrantColor(p.getDarkMutedColor(dominant))
                        intArrayOf(
                            ColorUtils.blendARGB(dominant, Color.BLACK, 0.18f),
                            ColorUtils.blendARGB(vibrant, Color.BLACK, 0.12f),
                            ColorUtils.blendARGB(deep, Color.BLACK, 0.30f)
                        )
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (picked != null) target = picked
            }
            animateMiniColors(target)
        }
    }

    private fun animateMiniColors(target: IntArray) {
        val from = miniColors
        miniAnimator?.cancel()
        if (from == null) {
            styleMini(target)
            return
        }
        val eval = ArgbEvaluator()
        miniAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450L
            addUpdateListener { a ->
                val f = a.animatedFraction
                styleMini(IntArray(3) { i -> eval.evaluate(f, from[i], target[i]) as Int })
            }
            start()
        }
    }

    private fun styleMini(colors: IntArray) {
        miniColors = colors
        val content = findViewById<View>(R.id.miniContent) ?: return
        val density = resources.displayMetrics.density
        val base = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
        // glass sheen: bright top edge fading out, a soft shade at the bottom
        val sheen = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb(80, 255, 255, 255), Color.argb(18, 255, 255, 255),
                Color.argb(0, 255, 255, 255), Color.argb(40, 0, 0, 0)
            )
        )
        content.background = LayerDrawable(arrayOf(base, sheen))

        val average = ColorUtils.blendARGB(colors[0], colors[2], 0.5f)
        val fg = if (ColorUtils.calculateLuminance(average) > 0.5) Color.rgb(20, 20, 26) else Color.WHITE
        findViewById<TextView>(R.id.miniTitle)?.setTextColor(fg)
        findViewById<TextView>(R.id.miniSubtitle)?.setTextColor(ColorUtils.setAlphaComponent(fg, 190))
        val tint = ColorStateList.valueOf(fg)
        findViewById<ImageButton>(R.id.miniNext)?.imageTintList = tint
        findViewById<ImageButton>(R.id.miniQueue)?.imageTintList = tint
        findViewById<ImageButton>(R.id.miniPlay)?.let { play ->
            play.imageTintList = tint
            play.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ColorUtils.setAlphaComponent(fg, 36))
                setStroke((2 * density).toInt().coerceAtLeast(1), fg)
            }
        }
        findViewById<LinearProgressIndicator>(R.id.miniProgress)?.setIndicatorColor(fg)
    }

    private fun updateMiniProgress() {
        val bar = findViewById<LinearProgressIndicator>(R.id.miniProgress) ?: return
        val c = controller ?: return
        val duration = c.duration
        bar.max = if (duration > 0) duration.toInt() else 1
        bar.setProgressCompat(c.currentPosition.toInt().coerceAtLeast(0), false)
    }

    // ----- Queue sheet -----

    fun showQueue() {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return
        val dialog = BottomSheetDialog(this)
        val sheet = SheetQueueBinding.inflate(layoutInflater)
        val entries = (0 until c.mediaItemCount).map { i ->
            val item = c.getMediaItemAt(i)
            QueueEntry(
                item.mediaId,
                item.mediaMetadata.title?.toString().orEmpty(),
                item.mediaMetadata.artist?.toString().orEmpty()
            )
        }
        val current = c.currentMediaItemIndex
        sheet.list.layoutManager = LinearLayoutManager(this)
        sheet.list.adapter = QueueAdapter(entries, current) { index ->
            c.seekTo(index, 0L)
            c.play()
            dialog.dismiss()
        }
        sheet.list.scrollToPosition((current - 2).coerceAtLeast(0))
        dialog.setContentView(sheet.root)
        dialog.show()
    }
}
