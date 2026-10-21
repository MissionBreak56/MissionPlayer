package com.missionbreak56.musicplayer.ui

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.missionbreak56.musicplayer.data.Song

/**
 * Drag and drop for a list that uses the "Personalizada" sort. Rows are dragged by their handle
 * (a long press keeps its other meanings). [active] is asked on every touch, so the sort can change
 * at any time; [onSaved] receives the whole list in its new order when the finger is lifted.
 */
class SongReorder(
    private val adapter: SongAdapter,
    private val active: () -> Boolean,
    private val onSaved: (List<Song>) -> Unit
) {
    private var moved = false

    private val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
    ) {
        override fun isLongPressDragEnabled(): Boolean = false

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            if (!active()) return false
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
            adapter.move(from, to)
            moved = true
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            if (moved) {
                moved = false
                onSaved(adapter.current())
            }
        }
    })

    fun attach(list: RecyclerView) {
        helper.attachToRecyclerView(list)
        adapter.onStartDrag = { holder -> if (active()) helper.startDrag(holder) }
    }
}
