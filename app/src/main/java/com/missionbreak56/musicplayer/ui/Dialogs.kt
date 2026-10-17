package com.missionbreak56.musicplayer.ui

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.playback.SleepTimer

object Dialogs {

    /** Lets the user drag the library tabs into the order they prefer. */
    fun reorderTabs(activity: Activity, order: List<Int>, onSave: (List<Int>) -> Unit) {
        val titles = activity.resources.getStringArray(R.array.tabs)
        val items = order.toMutableList()
        val density = activity.resources.displayMetrics.density

        val tabAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount(): Int = items.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (56 * density).toInt()
                    )
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding((24 * density).toInt(), 0, (24 * density).toInt(), 0)
                    textSize = 17f
                    setTextColor(parent.onSurfaceColor())
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as TextView).text = "\u2261   " + titles[items[position]]
            }
        }

        val list = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity)
            this.adapter = tabAdapter
        }
        val touch = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from < 0 || to < 0) return false
                java.util.Collections.swap(items, from, to)
                tabAdapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        touch.attachToRecyclerView(list)

        val hint = TextView(activity).apply {
            setText(R.string.tabs_order_hint)
            textSize = 14f
            setPadding((24 * density).toInt(), 0, (24 * density).toInt(), (8 * density).toInt())
        }
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(hint)
            addView(list)
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.menu_tabs_order)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ -> onSave(items.toList()) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Lists hidden folders and songs; unchecking an entry shows it again. */
    fun manageHidden(activity: Activity, onChanged: () -> Unit) {
        val folders = Prefs.hiddenFolders().sorted()
        val songs = Prefs.hiddenSongs().mapNotNull { it.toLongOrNull() }.mapNotNull { Library.song(it) }
        if (folders.isEmpty() && songs.isEmpty()) {
            Toast.makeText(activity, R.string.hidden_none, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = folders.map { activity.getString(R.string.hidden_folder_label, it.substringAfterLast('/')) } +
            songs.map { "${it.displayTitle} \u2013 ${it.displayArtist}" }
        val checked = BooleanArray(labels.size) { true }
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.menu_hidden)
            .setMultiChoiceItems(labels.toTypedArray(), checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(R.string.save) { _, _ ->
                val hiddenFolders = Prefs.hiddenFolders()
                val hiddenSongs = Prefs.hiddenSongs()
                folders.forEachIndexed { i, path -> if (!checked[i]) hiddenFolders.remove(path) }
                songs.forEachIndexed { i, song ->
                    if (!checked[folders.size + i]) hiddenSongs.remove(song.id.toString())
                }
                Prefs.setHiddenFolders(hiddenFolders)
                Prefs.setHiddenSongs(hiddenSongs)
                Library.applyHidden()
                onChanged()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

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
