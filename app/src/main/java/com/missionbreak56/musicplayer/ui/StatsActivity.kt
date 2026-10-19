package com.missionbreak56.musicplayer.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import com.missionbreak56.musicplayer.data.Library
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.playback.ListenTracker

/** Listening statistics: totals, favourites and your top songs, artists and albums. */
class StatsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(MaterialColors.getColor(this, android.R.attr.colorBackground, 0))
        root.addView(screenHeader(this, tr("Estatísticas", "Statistics")) { finish() })

        val scroll = ScrollView(this)
        val body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(16), dp(8), dp(16), dp(24))
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        ListenTracker.flush()
        val counts = Prefs.playCounts()
        val listened = Prefs.listenedMs()
        val songs = Library.songs
        val totalPlays = counts.values.sum()
        val listenedMs = listened.values.sum()

        // Summary cards
        val cards = LinearLayout(this)
        cards.orientation = LinearLayout.HORIZONTAL
        cards.addView(card(songs.size.toString(), tr("músicas", "songs")), cardParams(false))
        cards.addView(card(totalPlays.toString(), tr("reproduções", "plays")), cardParams(true))
        cards.addView(card(formatListened(listenedMs), tr("ouvidas", "listened")), cardParams(true))
        body.addView(cards)

        val cards2 = LinearLayout(this)
        cards2.orientation = LinearLayout.HORIZONTAL
        val totalMs = songs.sumOf { it.durationMs }
        cards2.addView(
            card("${totalMs / 3_600_000L}h", tr("de biblioteca", "of library")), cardParams(false)
        )
        cards2.addView(card(Prefs.favorites().size.toString(), tr("favoritas", "favorites")), cardParams(true))
        cards2.addView(card(Library.albums().size.toString(), tr("álbuns", "albums")), cardParams(true))
        body.addView(cards2, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        // Top songs
        val topSongs = songs.filter { (counts[it.id] ?: 0) > 0 }
            .sortedByDescending { counts[it.id] ?: 0 }.take(10)
        section(body, tr("Músicas mais tocadas", "Top songs"))
        if (topSongs.isEmpty()) {
            empty(body)
        } else {
            val max = counts[topSongs.first().id] ?: 1
            topSongs.forEachIndexed { i, s ->
                body.addView(
                    bar(
                        "${i + 1}. ${s.displayTitle}", s.displayArtist, counts[s.id] ?: 0, max,
                        "${counts[s.id] ?: 0}× · ${formatListened(listened[s.id] ?: 0L)}"
                    )
                )
            }
        }

        // Top artists
        val byArtist = HashMap<String, Int>()
        songs.forEach { s ->
            val c = counts[s.id] ?: 0
            if (c > 0) byArtist[s.displayArtist] = (byArtist[s.displayArtist] ?: 0) + c
        }
        val topArtists = byArtist.entries.sortedByDescending { it.value }.take(5)
        section(body, tr("Artistas mais ouvidos", "Top artists"))
        if (topArtists.isEmpty()) {
            empty(body)
        } else {
            val max = topArtists.first().value
            topArtists.forEachIndexed { i, e ->
                body.addView(bar("${i + 1}. ${e.key}", "", e.value, max))
            }
        }

        // Top albums
        val byAlbum = HashMap<String, Int>()
        songs.forEach { s ->
            val c = counts[s.id] ?: 0
            if (c > 0) byAlbum[s.displayAlbum] = (byAlbum[s.displayAlbum] ?: 0) + c
        }
        val topAlbums = byAlbum.entries.sortedByDescending { it.value }.take(5)
        section(body, tr("Álbuns mais ouvidos", "Top albums"))
        if (topAlbums.isEmpty()) {
            empty(body)
        } else {
            val max = topAlbums.first().value
            topAlbums.forEachIndexed { i, e ->
                body.addView(bar("${i + 1}. ${e.key}", "", e.value, max))
            }
        }

        val reset = com.google.android.material.button.MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        )
        reset.text = tr("Resetar tudo", "Reset all")
        reset.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(tr("Resetar estatísticas?", "Reset statistics?"))
                .setMessage(
                    tr(
                        "Isso apaga todas as contagens de reproduções e o tempo ouvido. Não dá para desfazer.",
                        "This erases all play counts and listening time. This cannot be undone."
                    )
                )
                .setNegativeButton(tr("Cancelar", "Cancel"), null)
                .setPositiveButton(tr("Resetar", "Reset")) { _, _ ->
                    ListenTracker.stop()
                    Prefs.resetStats()
                    recreate()
                }
                .show()
        }
        body.addView(reset, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(28) })
    }

    private fun formatListened(ms: Long): String {
        val total = ms / 1000L
        val h = total / 3600L
        val m = (total / 60L) % 60L
        val s = total % 60L
        return when {
            h > 0L -> "${h}h ${m}m"
            m > 0L -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    private fun cardParams(margin: Boolean) =
        LinearLayout.LayoutParams(0, -2, 1f).apply { if (margin) marginStart = dp(8) }

    private fun card(value: String, label: String): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        box.setPadding(dp(8), dp(16), dp(8), dp(16))
        val primary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0)
        box.roundedBackground((primary and 0x00FFFFFF) or 0x26000000, 18)
        val v = TextView(this)
        v.text = value
        v.textSize = 22f
        v.setTextColor(primary)
        v.setTypeface(v.typeface, android.graphics.Typeface.BOLD)
        v.gravity = Gravity.CENTER
        val l = TextView(this)
        l.text = label
        l.textSize = 12f
        l.alpha = 0.7f
        l.gravity = Gravity.CENTER
        box.addView(v)
        box.addView(l)
        return box
    }

    private fun section(parent: LinearLayout, title: String) {
        val t = TextView(this)
        t.text = title
        t.textSize = 17f
        t.setTypeface(t.typeface, android.graphics.Typeface.BOLD)
        parent.addView(t, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(24)
            bottomMargin = dp(8)
        })
    }

    private fun empty(parent: LinearLayout) {
        val t = TextView(this)
        t.text = tr("Ainda sem dados. Ouça algumas músicas!", "No data yet. Play some songs!")
        t.alpha = 0.6f
        parent.addView(t)
    }

    /** One ranked row with a proportional progress bar. */
    private fun bar(title: String, subtitle: String, value: Int, max: Int, label: String = "$value×"): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(0, dp(6), 0, dp(6))

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        val name = TextView(this)
        name.text = if (subtitle.isBlank()) title else "$title — $subtitle"
        name.textSize = 14f
        name.maxLines = 1
        name.ellipsize = android.text.TextUtils.TruncateAt.END
        top.addView(name, LinearLayout.LayoutParams(0, -2, 1f))
        val count = TextView(this)
        count.text = label
        count.textSize = 13f
        count.alpha = 0.7f
        top.addView(count)
        row.addView(top)

        val track = LinearLayout(this)
        track.orientation = LinearLayout.HORIZONTAL
        track.roundedBackground(0x22888888, 4)
        val fill = View(this)
        val primary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0)
        fill.roundedBackground(primary, 4)
        val weight = (value.toFloat() / maxOf(max, 1)).coerceIn(0.03f, 1f)
        track.addView(fill, LinearLayout.LayoutParams(0, dp(6), weight))
        track.addView(View(this), LinearLayout.LayoutParams(0, dp(6), 1f - weight))
        row.addView(track, LinearLayout.LayoutParams(-1, dp(6)).apply { topMargin = dp(4) })
        return row
    }
}
