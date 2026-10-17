package com.missionbreak56.musicplayer.playback

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Process-wide sleep timer. The playback service registers [onFinish]
 * (pause the player); the UI only starts, cancels and reads the remaining time.
 */
object SleepTimer {

    private val handler = Handler(Looper.getMainLooper())
    private var endAt = 0L

    @Volatile
    var onFinish: (() -> Unit)? = null

    private val task = Runnable {
        endAt = 0L
        onFinish?.invoke()
    }

    val isActive: Boolean get() = endAt > SystemClock.uptimeMillis()

    fun start(minutes: Int) {
        handler.removeCallbacks(task)
        val delay = minutes * 60_000L
        endAt = SystemClock.uptimeMillis() + delay
        handler.postDelayed(task, delay)
    }

    fun cancel() {
        handler.removeCallbacks(task)
        endAt = 0L
    }

    fun remainingMs(): Long = (endAt - SystemClock.uptimeMillis()).coerceAtLeast(0L)
}
