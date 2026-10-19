package com.missionbreak56.musicplayer.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.missionbreak56.musicplayer.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Searches album covers online (iTunes Search, no account needed) and applies one to a song. */
object CoverSearch {

    private class Hit(val thumbUrl: String, val bigUrl: String, val label: String)

    private fun get(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 12000
            conn.readTimeout = 15000
            if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private fun search(query: String): List<Hit> {
        val q = URLEncoder.encode(query, "UTF-8")
        val json = JSONObject(
            String(get("https://itunes.apple.com/search?term=$q&media=music&entity=song&limit=40"))
        )
        val arr = json.optJSONArray("results") ?: return emptyList()
        val seen = HashSet<String>()
        val out = ArrayList<Hit>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val thumb = o.optString("artworkUrl100")
            if (thumb.isBlank() || !seen.add(thumb)) continue
            out.add(
                Hit(
                    thumb,
                    thumb.replace("100x100bb", "1000x1000bb"),
                    listOf(o.optString("collectionName"), o.optString("artistName"))
                        .filter { it.isNotBlank() }.joinToString(" — ")
                )
            )
        }
        return out
    }

    fun show(activity: AppCompatActivity, song: Song, onApplied: () -> Unit) {
        val pad = activity.dp(16)
        val box = LinearLayout(activity)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(pad, activity.dp(8), pad, 0)

        val input = EditText(activity)
        input.setSingleLine()
        input.imeOptions = EditorInfo.IME_ACTION_SEARCH
        input.setText("${song.displayTitle} ${song.displayArtist}".trim())
        input.setSelection(input.text.length)
        box.addView(input, LinearLayout.LayoutParams(-1, -2))

        val status = TextView(activity)
        status.textSize = 13f
        status.alpha = 0.7f
        status.setPadding(0, activity.dp(6), 0, activity.dp(6))
        box.addView(status)

        val grid = RecyclerView(activity)
        grid.layoutManager = GridLayoutManager(activity, 3)
        box.addView(grid, LinearLayout.LayoutParams(-1, activity.dp(360)))

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(tr("Buscar capa", "Find cover"))
            .setView(box)
            .setNegativeButton(tr("Fechar", "Close"), null)
            .create()

        val thumbs = HashMap<String, Bitmap>()
        var hits: List<Hit> = emptyList()
        var job: Job? = null

        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount(): Int = hits.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val iv = ImageView(activity)
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                iv.roundedBackground(0x22888888, 8)
                iv.clipToOutline = true
                val size = maxOf(parent.width, activity.dp(300)) / 3 - activity.dp(6)
                iv.layoutParams = RecyclerView.LayoutParams(size, size).apply {
                    setMargins(activity.dp(3), activity.dp(3), activity.dp(3), activity.dp(3))
                }
                return object : RecyclerView.ViewHolder(iv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val hit = hits[position]
                val iv = holder.itemView as ImageView
                iv.setImageDrawable(null)
                iv.tag = hit.thumbUrl
                val cached = thumbs[hit.thumbUrl]
                if (cached != null) {
                    iv.setImageBitmap(cached)
                } else {
                    activity.lifecycleScope.launch {
                        val bmp = withContext(Dispatchers.IO) {
                            try {
                                val bytes = get(hit.thumbUrl)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (_: Throwable) {
                                null
                            }
                        }
                        if (bmp != null) {
                            thumbs[hit.thumbUrl] = bmp
                            if (iv.tag == hit.thumbUrl) iv.setImageBitmap(bmp)
                        }
                    }
                }
                iv.setOnClickListener {
                    status.text = tr("Baixando capa…", "Downloading cover…")
                    activity.lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            try {
                                val bytes = get(hit.bigUrl)
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                bmp != null && SongArt.saveBitmap(activity, song.id, bmp)
                            } catch (_: Throwable) {
                                false
                            }
                        }
                        if (ok) {
                            dialog.dismiss()
                            Toast.makeText(activity, tr("Capa aplicada", "Cover applied"), Toast.LENGTH_SHORT).show()
                            onApplied()
                        } else {
                            status.text = tr("Não foi possível baixar essa capa.", "Could not download this cover.")
                        }
                    }
                }
            }
        }
        grid.adapter = adapter

        fun run() {
            val q = input.text.toString().trim()
            if (q.isEmpty()) return
            status.text = tr("Pesquisando…", "Searching…")
            job?.cancel()
            job = activity.lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) { search(q) }
                    hits = result
                    adapter.notifyDataSetChanged()
                    status.text = if (result.isEmpty()) {
                        tr("Nenhuma capa encontrada.", "No covers found.")
                    } else {
                        tr("Toque em uma capa para usar.", "Tap a cover to use it.")
                    }
                } catch (_: Throwable) {
                    hits = emptyList()
                    adapter.notifyDataSetChanged()
                    status.text = tr("Falha na busca. Verifique a internet.", "Search failed. Check your connection.")
                }
            }
        }
        input.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                run()
                true
            } else {
                false
            }
        }
        dialog.show()
        run()
    }
}
