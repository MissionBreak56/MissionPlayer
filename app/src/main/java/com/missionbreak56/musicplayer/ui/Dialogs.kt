package com.missionbreak56.musicplayer.ui

import android.app.Activity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.playback.SleepTimer

object Dialogs {

    fun promptText(
        activity: Activity,
        titleRes: Int,
        initial: String,
        positiveRes: Int,
        onOk: (String) -> Unit
    ) {
        val density = activity.resources.displayMetrics.density
        val input = EditText(activity).apply {
            setText(initial)
            setSingleLine()
            setHint(R.string.playlist_name)
            setSelection(text.length)
        }
        val container = FrameLayout(activity).apply {
            val side = (20 * density).toInt()
            setPadding(side, (8 * density).toInt(), side, 0)
            addView(
                input,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(titleRes)
            .setView(container)
            .setPositiveButton(positiveRes) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) onOk(text)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun sleepTimer(activity: Activity, onChanged: () -> Unit) {
        val minutes = intArrayOf(15, 30, 45, 60, 90, 120)
        val labels = ArrayList<String>()
        val active = SleepTimer.isActive
        if (active) labels += activity.getString(R.string.sleep_off)
        minutes.forEach { labels += activity.getString(R.string.sleep_option, it) }

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.menu_sleep_timer)
            .setItems(labels.toTypedArray()) { _, which ->
                val offset = if (active) 1 else 0
                if (active && which == 0) {
                    SleepTimer.cancel()
                    Toast.makeText(activity, R.string.sleep_cancelled, Toast.LENGTH_SHORT).show()
                } else {
                    val chosen = minutes[which - offset]
                    SleepTimer.start(chosen)
                    Toast.makeText(
                        activity, activity.getString(R.string.sleep_set, chosen), Toast.LENGTH_SHORT
                    ).show()
                }
                onChanged()
            }
            .show()
    }
}
