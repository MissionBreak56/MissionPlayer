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
import android.view.Choreographer
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.ItemTouchHelper
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
import com.missionbreak56.musicplayer.playback.PlaybackState
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
    private var restoreTried = false

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
        restoreTried = false
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
        stopMiniSpin()
        uiHandler.removeCallbacks(tick)
        controller?.removeListener(listener)
        connection?.release()
        connection = null
        controller = null
        super.onStop()
    }

    protected open fun onControllerReady(c: MediaController) {}

    protected open fun onPlayerChanged() {
        restoreQueueIfEmpty()
        updateMiniPlayer()
    }

    protected open fun onTick() {
        restoreQueueIfEmpty()
        updateMiniProgress()
    }

    /**
     * The system may stop the music service while the song is paused and the app is in the
     * background. When the screen comes back with an empty player, put the last queue back
     * (paused, same position) so the mini player does not disappear.
     */
    private fun restoreQueueIfEmpty() {
        val c = controller ?: return
        if (restoreTried || c.mediaItemCount > 0 || !Library.loaded) return
        restoreTried = true
        PlaybackState.restoreIfEmpty(c)
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
        // The full player slides up from the bottom (like expanding the mini player).
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_up_in, R.anim.stay)
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    // ----- Cover spinning slowly like a vinyl record while the music plays -----
    // Driven by frame callbacks instead of an Animator: Android turns Animators off in Battery
    // Saver and when the system animation scale is 0, which left the cover completely still.

    private var miniSpinOn = false
    private var miniSpinLastNanos = 0L

    private val miniSpinFrame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!miniSpinOn) return
            val art = findViewById<View>(R.id.miniArt)
            if (art != null) {
                val dt = if (miniSpinLastNanos == 0L) 0L else frameTimeNanos - miniSpinLastNanos
                // ~30 fps is plenty for a slow turn and saves battery
                if (miniSpinLastNanos == 0L || dt >= 33_000_000L) {
                    miniSpinAngle = (miniSpinAngle + dt / 1_000_000f * 360f / MINI_SPIN_MS) % 360f
                    art.rotation = miniSpinAngle
                    miniSpinLastNanos = frameTimeNanos
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun updateMiniSpin(playing: Boolean) {
        if (playing) {
            findViewById<View>(R.id.miniArt)?.rotation = miniSpinAngle
            if (!miniSpinOn) {
                miniSpinOn = true
                miniSpinLastNanos = 0L
                Choreographer.getInstance().postFrameCallback(miniSpinFrame)
            }
        } else {
            stopMiniSpin() // keeps the angle, so it continues from there
        }
    }

    private fun stopMiniSpin() {
        miniSpinOn = false
        Choreographer.getInstance().removeFrameCallback(miniSpinFrame)
    }

    private companion object {
        /** One full turn every 20 seconds: slow, like a record on a turntable. */
        const val MINI_SPIN_MS = 20_000f

        /** Shared by all screens so the cover keeps its angle when you change screens. */
        var miniSpinAngle = 0f
    }

    private fun updateMiniPlayer() {
        val card = findViewById<View>(R.id.miniPlayer) ?: return
        val c = controller
        val item = c?.currentMediaItem
        if (c == null || item == null) {
            card.visibility = View.GONE
            stopMiniSpin()
            return
        }
        card.visibility = View.VISIBLE
        updateMiniSpin(c.isPlaying)
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
                ColorUtils.blendARGB(container, Color.BLACK, 0.25f),
                ColorUtils.blendARGB(container, primary, 0.60f)
            )
            val bitmap = if (song != null) ArtLoader.bitmap(applicationContext, song, 96) else null
            if (bitmap != null) {
                val picked = withContext(Dispatchers.Default) {
                    try {
                        val raw = GlassColors.pick(bitmap, container)
                        intArrayOf(
                            ColorUtils.blendARGB(raw[0], Color.BLACK, 0.20f),
                            ColorUtils.blendARGB(raw[1], Color.BLACK, 0.14f),
                            ColorUtils.blendARGB(raw[2], Color.BLACK, 0.26f),
                            ColorUtils.blendARGB(raw[3], Color.BLACK, 0.20f)
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
        if (from == null || from.size != target.size) {
            styleMini(target)
            return
        }
        val eval = ArgbEvaluator()
        miniAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450L
            addUpdateListener { a ->
                val f = a.animatedFraction
                styleMini(IntArray(target.size) { i -> eval.evaluate(f, from[i], target[i]) as Int })
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

        val average = ColorUtils.blendARGB(colors[0], colors[colors.size - 1], 0.5f)
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
        }.toMutableList()
        val current = c.currentMediaItemIndex
        val adapter = QueueAdapter(entries, current) { index ->
            c.seekTo(index, 0L)
            c.play()
            dialog.dismiss()
        }
        sheet.list.layoutManager = LinearLayoutManager(this)
        sheet.list.adapter = adapter

        // With shuffle off the queue can be reordered by dragging. With shuffle on the play order
        // is random, so reordering would have no visible effect and is not offered.
        val canReorder = !c.shuffleModeEnabled && c.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)
        sheet.textQueueHint.text = if (canReorder) {
            tr("Arraste ≡ (ou segure uma música) para reorganizar a fila", "Drag ≡ (or press and hold a song) to reorder the queue")
        } else if (c.shuffleModeEnabled) {
            tr("Desligue o aleatório para reorganizar a fila", "Turn shuffle off to reorder the queue")
        } else {
            ""
        }
        sheet.textQueueHint.visibility = if (sheet.textQueueHint.text.isEmpty()) View.GONE else View.VISIBLE
        if (canReorder) {
            var dragFrom = -1
            var dragTo = -1
            val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition
                    if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                    if (dragFrom < 0) dragFrom = from
                    dragTo = to
                    adapter.move(from, to)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    // the player is updated once, when the finger is lifted
                    if (dragFrom >= 0 && dragTo >= 0 && dragFrom != dragTo) c.moveMediaItem(dragFrom, dragTo)
                    dragFrom = -1
                    dragTo = -1
                }
            })
            helper.attachToRecyclerView(sheet.list)
            adapter.onStartDrag = { helper.startDrag(it) }
        }
        sheet.list.scrollToPosition((current - 2).coerceAtLeast(0))
        dialog.setContentView(sheet.root)
        dialog.show()
    }
}
