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
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlin.random.Random
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.ui.MainActivity
import com.missionbreak56.musicplayer.ui.tr

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

    /**
     * While playing, saves the position every few seconds so "last song" is accurate.
     * The tick only runs while music is actually playing: when the player pauses it goes
     * quiet instead of waking the main thread every 5 s for nothing.
     */
    private val positionTick = object : Runnable {
        override fun run() {
            val p = exo
            if (p != null && p.isPlaying) {
                PlaybackState.savePosition(p)
                handler.postDelayed(this, 5000L)
            }
        }
    }

    private fun startPositionTick() {
        handler.removeCallbacks(positionTick)
        handler.postDelayed(positionTick, 5000L)
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
            updateCustomLayout()
            if (shuffleModeEnabled) {
                handler.post {
                    exo?.let {
                        if (it.repeatMode == Player.REPEAT_MODE_OFF) it.repeatMode = Player.REPEAT_MODE_ALL
                        syncShuffleRound(it, newRound = true)
                    }
                }
            } else {
                roundPlayed.clear()
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
            // Every song that starts, however it started (end of the previous one, next, previous,
            // a tap in the queue), is registered in the shuffle round. "Repeat one" is not a new song.
            if (p.shuffleModeEnabled && mediaItem != null &&
                reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            ) {
                syncShuffleRound(p, newRound = false)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            exo?.let { PlaybackState.savePosition(it) }
            // Resume the position saver only when it has something to save.
            if (isPlaying) startPositionTick()
        }
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(PlaybackQueue.RESHUFFLE)
                .add(PlaybackQueue.FIX_ADDED)
                .add(PlaybackQueue.REFRESH_ART)
                .add(PlaybackQueue.REFRESH_SONG)
                .add(PlaybackQueue.TOGGLE_SHUFFLE)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .setCustomLayout(ImmutableList.of(shuffleButton(exo?.shuffleModeEnabled == true)))
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
                    PlaybackQueue.RESHUFFLE.customAction -> syncShuffleRound(p, newRound = true)
                    PlaybackQueue.TOGGLE_SHUFFLE.customAction -> p.shuffleModeEnabled = !p.shuffleModeEnabled
                    PlaybackQueue.REFRESH_SONG.customAction -> refreshSong(p, args.getLong("id", -1L))
                    PlaybackQueue.FIX_ADDED.customAction ->
                        placeAddedSong(p, args.getInt("index", -1), args.getBoolean("next", false))
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    /**
     * After the metadata editor rewrote a file: queue entries get the new title/artist (an in-place update, so
     * order and shuffle are kept) and, when that song is the one playing, the file is loaded again from the
     * same position because the audio may now start at a different offset.
     */
    private fun refreshSong(p: ExoPlayer, id: Long) {
        if (id < 0) return
        val song = com.missionbreak56.musicplayer.data.Library.song(id) ?: return
        val key = id.toString()
        for (i in 0 until p.mediaItemCount) {
            if (p.getMediaItemAt(i).mediaId == key) p.replaceMediaItem(i, song.toMediaItem())
        }
        if (p.currentMediaItem?.mediaId == key) {
            val index = p.currentMediaItemIndex
            val pos = p.currentPosition
            val play = p.playWhenReady
            p.stop()
            p.prepare()
            p.seekTo(index, pos)
            p.playWhenReady = play
        }
        mediaSession?.let { onUpdateNotification(it, false) }
    }

    private fun applyOrder(p: ExoPlayer, order: List<Int>) {
        if (order.size != p.mediaItemCount) return
        p.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), System.nanoTime()))
    }

    /** Ids of the songs already played in the current shuffle round, in the order they were played. */
    private val roundPlayed = LinkedHashSet<String>()

    /**
     * Keeps the shuffle order honest. The order is always: songs already played in this round (in
     * the order they played, so "previous" goes back through them), then the songs not played yet.
     * Whatever starts a song (end of the previous one, next, previous, a tap in the queue) calls
     * this, so a song is never repeated before all the others had their turn. When the last song of
     * the round starts, a new round is prepared: random order, with the songs heard last pushed to
     * the end of it so they do not come back right away.
     *
     * [newRound] throws the history away: new queue, shuffle just turned on, saved queue restored.
     */
    private fun syncShuffleRound(p: ExoPlayer, newRound: Boolean) {
        val n = p.mediaItemCount
        if (!p.shuffleModeEnabled || n < 2) {
            roundPlayed.clear()
            return
        }
        val cur = p.currentMediaItemIndex.coerceIn(0, n - 1)
        val ids = List(n) { p.getMediaItemAt(it).mediaId }
        val byId = ids.indices.groupBy { ids[it] }
        val curId = ids[cur]

        if (newRound) roundPlayed.clear()
        roundPlayed.retainAll(ids.toSet())
        roundPlayed.add(curId) // nothing changes when stepping back into a song already heard

        var unplayed: List<Int> = if (newRound) {
            (0 until n).filter { ids[it] != curId }.shuffled()
        } else {
            currentOrder(p).filter { ids[it] !in roundPlayed }
        }

        if (!newRound && unplayed.isEmpty() && roundPlayed.last() == curId) {
            // Last song of the round: prepare the next round.
            val recent = roundPlayed.toList().dropLast(1).takeLast((n / 4).coerceIn(1, 10)).toSet()
            roundPlayed.clear()
            roundPlayed.add(curId)
            val others = (0 until n).filter { ids[it] != curId }.shuffled()
            val (heardLately, rest) = others.partition { ids[it] in recent }
            unplayed = rest + heardLately
        }

        val order = LinkedHashSet<Int>()
        for (id in roundPlayed) {
            val indexes = byId[id].orEmpty()
            if (id == curId) {
                indexes.filter { it != cur }.forEach { order.add(it) }
                order.add(cur)
            } else {
                indexes.forEach { order.add(it) }
            }
        }
        unplayed.forEach { order.add(it) }
        for (i in 0 until n) order.add(i) // safety net: nothing may be missing
        applyOrder(p, order.toList())
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

    // ----- Notification button: shuffle <-> in order -----

    private fun shuffleButton(shuffleOn: Boolean): CommandButton =
        CommandButton.Builder()
            .setDisplayName(if (shuffleOn) tr("Aleatório", "Shuffle") else tr("Em sequência", "In order"))
            .setIconResId(if (shuffleOn) R.drawable.ic_notif_shuffle else R.drawable.ic_notif_sequence)
            .setSessionCommand(PlaybackQueue.TOGGLE_SHUFFLE)
            .build()

    private fun updateCustomLayout() {
        mediaSession?.setCustomLayout(ImmutableList.of(shuffleButton(exo?.shuffleModeEnabled == true)))
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
        if (player.isPlaying) startPositionTick()
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
            .setCustomLayout(ImmutableList.of(shuffleButton(player.shuffleModeEnabled)))
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
