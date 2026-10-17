package com.missionbreak56.musicplayer.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Labels
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.databinding.SheetQueueBinding
import com.missionbreak56.musicplayer.playback.PlayerConnection
import androidx.media3.common.Player
import androidx.media3.session.MediaController

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
            ArtLoader.load(findViewById(R.id.miniArt), Library.song(id), 160)
        }
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
