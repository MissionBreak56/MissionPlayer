package com.missionbreak56.musicplayer.ui

import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.missionbreak56.musicplayer.data.Prefs

/**
 * Advanced customization: app icon, accent color, background, font and corner style.
 * Look settings are layered over the chosen theme (see [Themes.applyOverlays]).
 */
class CustomizeActivity : BaseActivity() {

    private lateinit var scroll: ScrollView
    private lateinit var body: LinearLayout

    private val primary: Int
        get() = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(MaterialColors.getColor(this, android.R.attr.colorBackground, 0))
        root.addView(screenHeader(this, tr("Personalização avançada", "Advanced customization")) { finish() })

        scroll = ScrollView(this)
        body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(16), dp(4), dp(16), dp(32))
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        build()

        val savedY = savedInstanceState?.getInt(STATE_SCROLL, 0) ?: 0
        if (savedY > 0) {
            body.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    body.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    scroll.scrollTo(0, savedY)
                }
            })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SCROLL, scroll.scrollY)
    }

    private fun build() {
        body.removeAllViews()
        iconSection()
        accentSection()
        backgroundSection()
        fontSection()
        cornerSection()
        resetButton()
    }

    // ----- Small builders -----

    private fun section(title: String, hint: String = "") {
        val t = TextView(this)
        t.text = title
        t.textSize = 17f
        t.setTypeface(t.typeface, Typeface.BOLD)
        body.addView(t, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(24)
            bottomMargin = if (hint.isBlank()) dp(8) else dp(2)
        })
        if (hint.isNotBlank()) {
            val h = TextView(this)
            h.text = hint
            h.textSize = 13f
            h.alpha = 0.7f
            body.addView(h, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun ring(selected: Boolean, radiusDp: Int, fill: Int = Color.TRANSPARENT): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = dp(radiusDp).toFloat()
        d.setColor(fill)
        d.setStroke(dp(if (selected) 3 else 1), if (selected) primary else 0x33888888)
        return d
    }

    private fun tint(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)

    private fun roundClip(view: View, radiusDp: Int) {
        val r = dp(radiusDp).toFloat()
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, r)
            }
        }
        view.clipToOutline = true
    }

    private fun caption(text: String, selected: Boolean): TextView {
        val l = TextView(this)
        l.text = text
        l.textSize = 12f
        l.gravity = Gravity.CENTER
        l.maxLines = 1
        l.alpha = if (selected) 1f else 0.7f
        if (selected) l.setTypeface(l.typeface, Typeface.BOLD)
        return l
    }

    private fun newRow(): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        body.addView(row, LinearLayout.LayoutParams(-1, -2))
        return row
    }

    // ----- Icon -----

    private fun iconSection() {
        section(
            tr("Ícone do app", "App icon"),
            tr(
                "Muda o ícone na tela inicial. Pode levar alguns segundos para atualizar.",
                "Changes the home screen icon. It may take a few seconds to update."
            )
        )
        val names = arrayOf(
            tr("Original", "Original"), tr("Verde", "Green"), tr("Azul", "Blue"),
            tr("Violeta", "Violet"), tr("Dourado", "Gold"), tr("Rosa", "Pink"),
            tr("Vinil", "Vinyl"), tr("Ondas", "Waves"), tr("Fones", "Headphones")
        )
        val perRow = 3
        var row = newRow()
        for (i in 0 until IconManager.count) {
            if (i > 0 && i % perRow == 0) row = newRow()
            val selected = i == Prefs.iconVariant
            val cell = LinearLayout(this)
            cell.orientation = LinearLayout.VERTICAL
            cell.gravity = Gravity.CENTER_HORIZONTAL
            cell.setPadding(dp(4), dp(6), dp(4), dp(6))

            val frame = FrameLayout(this)
            frame.background = ring(selected, 20)
            frame.setPadding(dp(4), dp(4), dp(4), dp(4))
            val img = ImageView(this)
            img.setImageResource(IconManager.icons[i])
            roundClip(img, 16)
            frame.addView(img, FrameLayout.LayoutParams(dp(64), dp(64)))
            cell.addView(frame, LinearLayout.LayoutParams(-2, -2))
            cell.addView(caption(names[i], selected), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })

            cell.setOnClickListener {
                if (Prefs.iconVariant != i) {
                    Prefs.iconVariant = i
                    IconManager.apply(this, i)
                    Toast.makeText(
                        this,
                        tr("Ícone alterado: ${names[i]}", "Icon changed: ${names[i]}"),
                        Toast.LENGTH_SHORT
                    ).show()
                    build()
                }
            }
            row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f))
        }
        // keep the last row's cells the same width as the others
        val missing = (perRow - IconManager.count % perRow) % perRow
        repeat(missing) { row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f)) }
    }

    // ----- Accent color -----

    private fun accentSection() {
        val names = arrayOf(
            tr("Vermelho", "Red"), tr("Laranja", "Orange"), tr("Âmbar", "Amber"), tr("Lima", "Lime"),
            tr("Verde", "Green"), tr("Menta", "Mint"), tr("Turquesa", "Teal"), tr("Céu", "Sky"),
            tr("Azul", "Blue"), tr("Índigo", "Indigo"), tr("Violeta", "Violet"), tr("Roxo", "Purple"),
            tr("Rosa", "Pink"), tr("Vinho", "Wine"), tr("Marrom", "Brown"), tr("Grafite", "Graphite")
        )
        val current = Prefs.accent
        val currentName = if (current in names.indices) names[current] else tr("Do tema", "From theme")
        section(
            tr("Cor de destaque", "Accent color"),
            tr("Escolhida: $currentName", "Selected: $currentName")
        )

        val themeChip = TextView(this)
        themeChip.text = tr("Usar a cor do tema", "Use the theme's color")
        themeChip.textSize = 14f
        themeChip.setPadding(dp(16), dp(10), dp(16), dp(10))
        themeChip.background = ring(current < 0, 20, if (current < 0) tint(primary, 0x26) else Color.TRANSPARENT)
        themeChip.setOnClickListener {
            if (Prefs.accent != -1) { Prefs.accent = -1; recreate() }
        }
        body.addView(themeChip, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(8) })

        val perRow = 8
        var row = newRow()
        for (i in Themes.accentColors.indices) {
            if (i > 0 && i % perRow == 0) row = newRow()
            val selected = i == current
            val frame = FrameLayout(this)
            frame.setPadding(dp(3), dp(3), dp(3), dp(3))
            val ringBg = GradientDrawable()
            ringBg.shape = GradientDrawable.OVAL
            ringBg.setColor(Color.TRANSPARENT)
            ringBg.setStroke(dp(if (selected) 3 else 0), if (selected) MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0) else Color.TRANSPARENT)
            frame.background = ringBg
            val dot = View(this)
            val dotBg = GradientDrawable()
            dotBg.shape = GradientDrawable.OVAL
            dotBg.setColor(Themes.accentColors[i])
            dot.background = dotBg
            frame.addView(dot, FrameLayout.LayoutParams(-1, -1))
            frame.contentDescription = names[i]
            frame.setOnClickListener {
                if (Prefs.accent != i) { Prefs.accent = i; recreate() }
            }
            row.addView(frame, LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(1), dp(2), dp(1), dp(2)) })
        }
    }

    // ----- Background -----

    private fun backgroundSection() {
        val names = arrayOf(
            tr("Padrão", "Default"), tr("Puro", "Pure"), tr("Grafite", "Graphite"),
            tr("Azul-noite", "Midnight"), tr("Sépia", "Sepia"), tr("Musgo", "Moss")
        )
        section(
            tr("Cor do fundo", "Background color"),
            tr("Cada opção tem versão clara e escura (esquerda: claro, direita: escuro).", "Each option has a light and a dark version (left: light, right: dark).")
        )
        val perRow = 3
        var row = newRow()
        for (i in 0 until Themes.backgroundCount) {
            if (i > 0 && i % perRow == 0) row = newRow()
            val selected = i == Prefs.backgroundStyle
            val cell = LinearLayout(this)
            cell.orientation = LinearLayout.VERTICAL
            cell.gravity = Gravity.CENTER_HORIZONTAL
            cell.setPadding(dp(4), dp(6), dp(4), dp(6))

            val frame = FrameLayout(this)
            frame.background = ring(selected, 14)
            frame.setPadding(dp(4), dp(4), dp(4), dp(4))
            val halves = LinearLayout(this)
            halves.orientation = LinearLayout.HORIZONTAL
            val light = View(this)
            light.setBackgroundColor(Themes.backgroundPreviews[i][0])
            val dark = View(this)
            dark.setBackgroundColor(Themes.backgroundPreviews[i][1])
            halves.addView(light, LinearLayout.LayoutParams(0, -1, 1f))
            halves.addView(dark, LinearLayout.LayoutParams(0, -1, 1f))
            roundClip(halves, 10)
            frame.addView(halves, FrameLayout.LayoutParams(dp(72), dp(44)))
            cell.addView(frame, LinearLayout.LayoutParams(-2, -2))
            cell.addView(caption(names[i], selected), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
            cell.setOnClickListener {
                if (Prefs.backgroundStyle != i) { Prefs.backgroundStyle = i; recreate() }
            }
            row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f))
        }
    }

    // ----- Font -----

    private fun fontSection() {
        val names = arrayOf(
            tr("Padrão", "Default"), tr("Serifada", "Serif"), tr("Monoespaçada", "Monospace"),
            tr("Condensada", "Condensed"), tr("Leve", "Light"), tr("Média", "Medium"),
            tr("Casual", "Casual"), tr("Cursiva", "Cursive"), tr("Pesada", "Black")
        )
        section(tr("Fonte", "Font"), tr("Vale para todo o app.", "Applies to the whole app."))
        for (i in 0 until Themes.fontCount) {
            val selected = i == Prefs.fontStyle
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(dp(14), dp(10), dp(14), dp(10))
            row.background = ring(selected, 14, if (selected) tint(primary, 0x26) else Color.TRANSPARENT)

            val sample = TextView(this)
            sample.text = "${names[i]} — " + tr("Minha música favorita", "My favorite song")
            sample.textSize = 16f
            sample.typeface = Typeface.create(Themes.fontFamilies[i], Typeface.NORMAL)
            row.addView(sample, LinearLayout.LayoutParams(0, -2, 1f))
            if (selected) {
                val mark = TextView(this)
                mark.text = "✓"
                mark.textSize = 18f
                mark.setTextColor(primary)
                row.addView(mark)
            }
            row.setOnClickListener {
                if (Prefs.fontStyle != i) { Prefs.fontStyle = i; recreate() }
            }
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }
    }

    // ----- Corners -----

    private fun cornerSection() {
        val names = arrayOf(tr("Padrão", "Default"), tr("Retos", "Square"), tr("Suaves", "Soft"), tr("Redondos", "Round"))
        val preview = intArrayOf(14, 0, 8, 22)
        section(tr("Cantos", "Corners"), tr("Formato de botões, cartões e janelas.", "Shape of buttons, cards and dialogs."))
        val row = newRow()
        for (i in 0 until Themes.cornerCount) {
            val selected = i == Prefs.cornerStyle
            val chip = TextView(this)
            chip.text = names[i]
            chip.textSize = 13f
            chip.gravity = Gravity.CENTER
            chip.maxLines = 1
            chip.setPadding(dp(4), dp(12), dp(4), dp(12))
            chip.background = ring(selected, preview[i], if (selected) tint(primary, 0x26) else Color.TRANSPARENT)
            if (selected) chip.setTypeface(chip.typeface, Typeface.BOLD)
            chip.setOnClickListener {
                if (Prefs.cornerStyle != i) { Prefs.cornerStyle = i; recreate() }
            }
            row.addView(chip, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
    }

    // ----- Reset -----

    private fun resetButton() {
        val b = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        b.text = tr("Restaurar padrões", "Restore defaults")
        b.setOnClickListener {
            Prefs.resetCustomization()
            if (Prefs.iconVariant != 0) {
                Prefs.iconVariant = 0
                IconManager.apply(this, 0)
            }
            recreate()
        }
        body.addView(b, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(28) })
    }

    companion object {
        private const val STATE_SCROLL = "scroll_y"
    }
}
