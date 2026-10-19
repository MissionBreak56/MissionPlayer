package com.missionbreak56.musicplayer.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.missionbreak56.musicplayer.data.LyricsWorker
import com.missionbreak56.musicplayer.data.Prefs

/**
 * Settings screen: everything that used to live in the floating menu of the main screen.
 */
class SettingsActivity : BaseActivity() {

    private lateinit var body: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(MaterialColors.getColor(this, android.R.attr.colorBackground, 0))
        root.addView(screenHeader(this, tr("Configurações", "Settings")) { finish() })

        val scroll = ScrollView(this)
        body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(16), dp(4), dp(16), dp(32))
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        build()
    }

    private fun build() {
        body.removeAllViews()

        header(tr("Aparência", "Appearance"))
        entry(
            tr("Personalização", "Customization"),
            tr("Modo claro/escuro, ícone, cor de destaque, fonte e fundo", "Light/dark mode, icon, accent color, font and background")
        ) { open(CustomizeActivity::class.java) }

        header(tr("Biblioteca", "Library"))
        entry(
            tr("Organizar abas", "Arrange tabs"),
            tr("Muda a ordem das abas da tela inicial", "Changes the order of the home screen tabs")
        ) {
            Dialogs.reorderTabs(this, Prefs.tabOrder()) { order -> Prefs.setTabOrder(order) }
        }
        entry(
            tr("Itens ocultos", "Hidden items"),
            tr("Mostra de novo músicas e pastas que você ocultou", "Show songs and folders you hid again")
        ) { Dialogs.manageHidden(this) {} }

        header(tr("Áudio e letras", "Audio and lyrics"))
        entry(
            tr("Equalizador", "Equalizer"),
            tr("Presets, bandas, graves, surround e aumento de volume", "Presets, bands, bass, surround and volume boost")
        ) { open(EqualizerActivity::class.java) }
        switchEntry(
            tr("Letras automáticas", "Automatic lyrics"),
            tr("Baixa em segundo plano as letras que faltam (só com internet)", "Downloads missing lyrics in the background (online only)"),
            Prefs.autoLyrics
        ) { on ->
            Prefs.autoLyrics = on
            if (on) LyricsWorker.schedule(this) else LyricsWorker.cancel(this)
            Toast.makeText(
                this,
                if (on) tr("Letras automáticas ligadas", "Auto lyrics on")
                else tr("Letras automáticas desligadas", "Auto lyrics off"),
                Toast.LENGTH_SHORT
            ).show()
        }

        header(tr("Mais", "More"))
        entry(
            tr("Estatísticas", "Statistics"),
            tr("Reproduções, tempo ouvido e mais ouvidos", "Plays, listening time and top items")
        ) { open(StatsActivity::class.java) }
        entry(
            tr("Sobre", "About"),
            tr("Versão, desenvolvedor e repositório", "Version, developer and repository")
        ) { open(AboutActivity::class.java) }
    }

    private fun open(cls: Class<*>) = startActivity(Intent(this, cls))

    private fun header(title: String) {
        val t = TextView(this)
        t.text = title
        t.textSize = 14f
        t.setTypeface(t.typeface, Typeface.BOLD)
        t.setTextColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0))
        body.addView(t, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(22)
            bottomMargin = dp(4)
            marginStart = dp(4)
        })
    }

    private fun texts(title: String, subtitle: String): LinearLayout {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        val t = TextView(this)
        t.text = title
        t.textSize = 16f
        col.addView(t, LinearLayout.LayoutParams(-1, -2))
        val s = TextView(this)
        s.text = subtitle
        s.textSize = 13f
        s.alpha = 0.7f
        col.addView(s, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })
        return col
    }

    private fun rowContainer(): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(8), dp(12), dp(8), dp(12))
        val attrs = intArrayOf(android.R.attr.selectableItemBackground)
        val ta = obtainStyledAttributes(attrs)
        row.foreground = ta.getDrawable(0)
        ta.recycle()
        row.isClickable = true
        row.isFocusable = true
        return row
    }

    private fun entry(title: String, subtitle: String, onClick: () -> Unit) {
        val row = rowContainer()
        row.addView(texts(title, subtitle), LinearLayout.LayoutParams(0, -2, 1f))
        val chevron = TextView(this)
        chevron.text = "›"
        chevron.textSize = 24f
        chevron.alpha = 0.5f
        row.addView(chevron, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        row.setOnClickListener { onClick() }
        body.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private fun switchEntry(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val row = rowContainer()
        row.addView(texts(title, subtitle), LinearLayout.LayoutParams(0, -2, 1f))
        val sw = MaterialSwitch(this)
        sw.isChecked = checked
        sw.setOnCheckedChangeListener { _, on -> onChange(on) }
        row.addView(sw, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        row.setOnClickListener { sw.toggle() }
        body.addView(row, LinearLayout.LayoutParams(-1, -2))
    }
}
