package com.missionbreak56.musicplayer.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlin.random.Random
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.ui.MainActivity

/**
 * Background audio playback. Exposes a MediaSession so the system media
 * notification / lock screen controls work, and hosts the audio effects,
 * the sleep timer action and the play statistics. No network data source.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exo: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    /** While playing, saves the position every few seconds so "last song" is accurate. */
    private val positionTick = object : Runnable {
        override fun run() {
            exo?.let { if (it.isPlaying) PlaybackState.savePosition(it) }
            handler.postDelayed(this, 5000L)
        }
    }

    /** Counts the exact time each song is heard (for the statistics screen). */
    private val listenTick = object : Runnable {
        override fun run() {
            exo?.let { ListenTracker.tick(it) }
            handler.postDelayed(this, 1000L)
        }
    }

    /**
     * Saves shuffle / repeat / queue, and keeps the shuffle order fair: every song plays once
     * (current one first) before any song is repeated.
     */
    private val stateListener = object : Player.Listener {
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            Prefs.shuffleMode = shuffleModeEnabled
            if (shuffleModeEnabled) {
                handler.post {
                    exo?.let {
                        if (it.repeatMode == Player.REPEAT_MODE_OFF) it.repeatMode = Player.REPEAT_MODE_ALL
                        reshuffleFromCurrent(it)
                    }
                }
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Prefs.repeatMode = repeatMode
            // Shuffle never ends: with "repeat off" the queue would stop (and "next" would stop
            // working) after the last song, so shuffle keeps the cycle going instead.
            val p = exo
            if (p != null && p.shuffleModeEnabled && repeatMode == Player.REPEAT_MODE_OFF) {
                handler.post { p.repeatMode = Player.REPEAT_MODE_ALL }
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                exo?.let { PlaybackState.saveQueue(it) }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val p = exo ?: return
            PlaybackState.savePosition(p)
            if (p.shuffleModeEnabled && p.mediaItemCount > 1) {
                val order = currentOrder(p)
                val cur = p.currentMediaItemIndex
                if (order.size == p.mediaItemCount) {
                    if (lastIndex == order.last() && cur == order.first() && lastIndex != cur) {
                        // The cycle just wrapped (automatically or with "next"): new random round.
                        handler.post { reshuffleFromCurrent(p) }
                    } else if (cur == order.last()) {
                        // Last song of the round: pick a different, long-unplayed song to start the next.
                        rotateStart(p, order)
                    }
                }
            }
            lastIndex = p.currentMediaItemIndex
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            exo?.let { PlaybackState.savePosition(it) }
        }
    }

    /** Queue index that was playing before the latest transition. */
    private var lastIndex = -1

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(PlaybackQueue.RESHUFFLE)
                .add(PlaybackQueue.FIX_ADDED)
                .add(PlaybackQueue.REFRESH_ART)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == PlaybackQueue.REFRESH_ART.customAction) {
                mediaSession?.let { onUpdateNotification(it, false) }
            }
            val p = exo
            if (p != null) {
                when (customCommand.customAction) {
                    PlaybackQueue.RESHUFFLE.customAction -> reshuffleFromCurrent(p)
                    PlaybackQueue.FIX_ADDED.customAction ->
                        placeAddedSong(p, args.getInt("index", -1), args.getBoolean("next", false))
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun applyOrder(p: ExoPlayer, order: List<Int>) {
        if (order.size != p.mediaItemCount) return
        p.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), System.nanoTime()))
    }

    /** New random order: the current song first, then all the others once, in random order. */
    private fun reshuffleFromCurrent(p: ExoPlayer) {
        val n = p.mediaItemCount
        if (!p.shuffleModeEnabled || n < 2) return
        val current = p.currentMediaItemIndex.coerceIn(0, n - 1)
        val rest = (0 until n).filter { it != current }.shuffled()
        applyOrder(p, listOf(current) + rest)
    }

    /**
     * Called when the last song of a shuffle round starts: swaps a random, early-played song into
     * the first slot so the round after this one begins with a different song, not a repeat.
     */
    private fun rotateStart(p: ExoPlayer, order: List<Int>) {
        if (order.size < 2) return
        val pool = order.subList(0, maxOf(1, (order.size - 1) / 2))
        val pick = pool.random()
        val at = order.indexOf(pick)
        if (at <= 0) return
        val m = order.toMutableList()
        java.util.Collections.swap(m, 0, at)
        applyOrder(p, m)
    }

    /** Current shuffle order as a list of queue indexes (first to last). */
    private fun currentOrder(p: ExoPlayer): MutableList<Int> {
        val out = ArrayList<Int>()
        val t = p.currentTimeline
        var i = t.getFirstWindowIndex(true)
        while (i != C.INDEX_UNSET && out.size <= t.windowCount) {
            out += i
            i = t.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, true)
        }
        return out
    }

    /**
     * A song added to the queue while shuffling must come after the current one: right after it
     * for "play next", at a random spot among the songs not played yet for "add to queue".
     */
    private fun placeAddedSong(p: ExoPlayer, added: Int, next: Boolean) {
        if (!p.shuffleModeEnabled || added !in 0 until p.mediaItemCount) return
        val order = currentOrder(p)
        val pos = order.indexOf(p.currentMediaItemIndex)
        if (pos < 0 || order.size != p.mediaItemCount) return
        val played = order.subList(0, pos + 1).filter { it != added }
        val unplayed = order.subList(pos + 1, order.size).filter { it != added }.toMutableList()
        val at = if (next) 0 else Random.nextInt(unplayed.size + 1)
        unplayed.add(at, added)
        applyOrder(p, played + unplayed)
    }

    private val statsListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.toLongOrNull()?.let { Prefs.recordPlay(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exo = player

        // Restore the last chosen shuffle / repeat mode (before the listener starts saving).
        player.shuffleModeEnabled = Prefs.shuffleMode
        player.repeatMode = Prefs.repeatMode.coerceIn(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL)

        if (player.shuffleModeEnabled && player.repeatMode == Player.REPEAT_MODE_OFF) {
            player.repeatMode = Player.REPEAT_MODE_ALL
        }

        player.addListener(statsListener)
        player.addListener(stateListener)
        handler.postDelayed(positionTick, 5000L)
        handler.postDelayed(listenTick, 1000L)
        AudioFx.attach(player.audioSessionId)
        SleepTimer.onFinish = { player.pause() }

        val provider = DefaultMediaNotificationProvider.Builder(this).build()
        provider.setSmallIcon(R.drawable.ic_stat_music)
        setMediaNotificationProvider(provider)

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(sessionCallback)
            .setBitmapLoader(SongBitmapLoader(this))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.let { PlaybackState.saveQueue(it) }
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(positionTick)
        handler.removeCallbacks(listenTick)
        ListenTracker.stop()
        mediaSession?.player?.let { PlaybackState.saveQueue(it) }
        exo = null
        SleepTimer.cancel()
        SleepTimer.onFinish = null
        AudioFx.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
