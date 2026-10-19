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
import android.widget.SeekBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.missionbreak56.musicplayer.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Advanced customization: app icon, accent color, background, font and corner style.
 * Look settings are layered over the chosen theme (see [Themes.applyOverlays]).
 */
class CustomizeActivity : BaseActivity() {

    private lateinit var scroll: ScrollView
    private lateinit var body: LinearLayout

    private val primary: Int
        get() = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0)

    /** Gallery picker for the background image. */
    private val pickBackground = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) { BgImage.save(applicationContext, uri) }
                if (ok) {
                    Prefs.bgImageStamp = System.currentTimeMillis()
                    recreate()
                } else {
                    Toast.makeText(
                        this@CustomizeActivity,
                        tr("Não foi possível usar essa imagem", "Couldn't use that image"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

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
        val currentName = when {
            current in names.indices -> names[current]
            Themes.isWheel(current) -> tr("Roda de cores", "Color wheel")
            else -> tr("Do tema", "From theme")
        }
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
            // fixed square size inside a weighted cell, so the dots are real circles
            val cell = FrameLayout(this)
            cell.addView(frame, FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER))
            row.addView(cell, LinearLayout.LayoutParams(0, dp(44), 1f))
        }
        wheelBlock(current)
    }

    /**
     * Free accent color: a hue wheel, or (compact mode) a hue bar, plus a bar for the tone
     * (pastel .. deep). Both modes save the same value.
     */
    private fun wheelBlock(current: Int) {
        val onWheel = Themes.isWheel(current)
        var hue = if (onWheel) Themes.wheelHue(current) else 0
        var tone = if (onWheel) Themes.wheelTone(current) else 2
        val compact = Prefs.accentCompact

        val title = TextView(this)
        title.text = tr("Cor livre", "Free color")
        title.textSize = 15f
        title.setTypeface(title.typeface, Typeface.BOLD)
        body.addView(title, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        val hint = TextView(this)
        hint.text = if (compact) {
            tr("Use as barras para escolher o tom e a intensidade.", "Use the bars to pick the hue and the intensity.")
        } else {
            tr("Arraste no círculo para escolher o tom e use a barra para a intensidade.", "Drag on the circle to pick the hue and use the bar for the intensity.")
        }
        hint.textSize = 13f
        hint.alpha = 0.7f
        body.addView(hint, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        // wheel / bars switch
        val modeRow = newRow()
        val modeNames = arrayOf(tr("Roda", "Wheel"), tr("Barras (compacto)", "Bars (compact)"))
        for (m in 0..1) {
            val selected = (m == 1) == compact
            val chip = TextView(this)
            chip.text = modeNames[m]
            chip.textSize = 13f
            chip.gravity = Gravity.CENTER
            chip.maxLines = 1
            chip.setPadding(dp(4), dp(10), dp(4), dp(10))
            chip.background = ring(selected, 20, if (selected) tint(primary, 0x26) else Color.TRANSPARENT)
            if (selected) chip.setTypeface(chip.typeface, Typeface.BOLD)
            chip.setOnClickListener {
                if (Prefs.accentCompact != (m == 1)) {
                    Prefs.accentCompact = (m == 1)
                    build()
                }
            }
            modeRow.addView(chip, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }

        fun commit() {
            Prefs.accent = Themes.wheelCode(hue, tone)
            recreate()
        }

        val toneNames = arrayOf(
            tr("Pastel", "Pastel"), tr("Suave", "Soft"), tr("Vivo", "Vivid"),
            tr("Forte", "Strong"), tr("Escuro", "Deep")
        )
        var wheel: ColorWheelView? = null
        var previewBg: GradientDrawable? = null
        val toneLabel = TextView(this)
        fun toneText() = tr("Intensidade: ${toneNames[tone]}", "Intensity: ${toneNames[tone]}")

        if (!compact) {
            val w = ColorWheelView(this)
            w.hue = hue
            w.tone = tone
            w.showThumb = onWheel
            w.onPick = { h, final ->
                hue = h
                if (final) commit()
            }
            wheel = w
            body.addView(w, LinearLayout.LayoutParams(-1, -2).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(8)
            })
        } else {
            // live preview of the chosen color
            val preview = View(this)
            val pb = GradientDrawable()
            pb.shape = GradientDrawable.RECTANGLE
            pb.cornerRadius = dp(12).toFloat()
            pb.setColor(Themes.wheelColor(hue, tone))
            preview.background = pb
            previewBg = pb
            body.addView(preview, LinearLayout.LayoutParams(-1, dp(36)).apply { topMargin = dp(10) })

            val hueLabel = TextView(this)
            hueLabel.text = tr("Tom", "Hue")
            hueLabel.textSize = 13f
            body.addView(hueLabel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
            val hueBar = SeekBar(this)
            hueBar.max = Themes.WHEEL_HUES - 1
            hueBar.progress = hue
            val rainbow = IntArray(Themes.WHEEL_HUES + 1) {
                Color.HSVToColor(floatArrayOf(it * 5f, 0.85f, 0.95f))
            }
            val track = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, rainbow)
            track.cornerRadius = dp(8).toFloat()
            hueBar.background = android.graphics.drawable.InsetDrawable(track, dp(14), dp(11), dp(14), dp(11))
            hueBar.progressDrawable = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            hueBar.thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            hueBar.setPadding(dp(14), 0, dp(14), 0)
            hueBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    hue = progress
                    previewBg?.setColor(Themes.wheelColor(hue, tone))
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) { commit() }
            })
            body.addView(hueBar, LinearLayout.LayoutParams(-1, dp(40)))
        }

        // tone bar (both modes)
        toneLabel.text = toneText()
        toneLabel.textSize = 13f
        body.addView(toneLabel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        val toneBar = SeekBar(this)
        toneBar.max = Themes.WHEEL_TONES - 1
        toneBar.progress = tone
        toneBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                tone = progress
                toneLabel.text = toneText()
                wheel?.tone = tone
                previewBg?.setColor(Themes.wheelColor(hue, tone))
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) { commit() }
        })
        body.addView(toneBar, LinearLayout.LayoutParams(-1, -2))
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
        imageBlock()
    }

    /** Picture from the gallery as the background, with an adjustable amount of visibility. */
    private fun imageBlock() {
        val title = TextView(this)
        title.text = tr("Imagem da galeria", "Gallery image")
        title.textSize = 15f
        title.setTypeface(title.typeface, Typeface.BOLD)
        body.addView(title, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })

        val hint = TextView(this)
        hint.text = if (BgImage.active) {
            tr("Imagem de fundo ativa.", "Background image is on.")
        } else {
            tr("Use uma foto da galeria como fundo do app.", "Use a photo from your gallery as the app background.")
        }
        hint.textSize = 13f
        hint.alpha = 0.7f
        body.addView(hint, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        val pick = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        pick.text = if (BgImage.active) tr("Trocar imagem", "Change image") else tr("Escolher imagem da galeria", "Choose image from gallery")
        pick.setOnClickListener { pickBackground.launch("image/*") }
        body.addView(pick, LinearLayout.LayoutParams(-1, -2))

        if (!BgImage.active) return

        val label = TextView(this)
        label.textSize = 13f
        label.text = tr("Visibilidade da imagem: ${Prefs.bgImageVisible}%", "Image visibility: ${Prefs.bgImageVisible}%")
        body.addView(label, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        val seek = SeekBar(this)
        seek.max = 90
        seek.progress = Prefs.bgImageVisible - 10
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                label.text = tr("Visibilidade da imagem: ${progress + 10}%", "Image visibility: ${progress + 10}%")
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {
                Prefs.bgImageVisible = (bar?.progress ?: 45) + 10
                recreate()
            }
        })
        body.addView(seek, LinearLayout.LayoutParams(-1, -2))

        val remove = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        remove.text = tr("Remover imagem", "Remove image")
        remove.setOnClickListener {
            BgImage.clear(applicationContext)
            recreate()
        }
        body.addView(remove, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
    }

    // ----- Font -----

    private fun fontSection() {
        section(
            tr("Fonte", "Font"),
            tr("Vale para todo o app. \"Usar a fonte do tema\" mantém a fonte própria de cada tema.", "Applies to the whole app. \"Use the theme's font\" keeps each theme's own font.")
        )
        val index = Prefs.fontStyle.coerceIn(0, Themes.fontCount - 1)
        val button = LinearLayout(this)
        button.orientation = LinearLayout.HORIZONTAL
        button.gravity = Gravity.CENTER_VERTICAL
        button.setPadding(dp(14), dp(12), dp(14), dp(12))
        button.background = ring(false, 14)
        val sample = TextView(this)
        sample.text = Themes.fonts[index].label
        sample.textSize = 16f
        Themes.typeface(this, index)?.let { sample.typeface = it }
        button.addView(sample, LinearLayout.LayoutParams(0, -2, 1f))
        val arrow = TextView(this)
        arrow.text = "▾"
        arrow.textSize = 18f
        button.addView(arrow)
        button.setOnClickListener { showFontMenu(button) }
        body.addView(button, LinearLayout.LayoutParams(-1, -2))
    }

    /** Floating list of fonts, each name drawn in its own font. */
    private fun showFontMenu(anchor: View) {
        val labels = Themes.fonts.map { it.label }
        val selected = Prefs.fontStyle.coerceIn(0, labels.lastIndex)
        val popup = android.widget.ListPopupWindow(this)
        popup.anchorView = anchor
        popup.width = anchor.width
        popup.height = (resources.displayMetrics.heightPixels * 0.6f).toInt()
        popup.isModal = true
        val bg = GradientDrawable()
        bg.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0))
        bg.cornerRadius = dp(14).toFloat()
        popup.setBackgroundDrawable(bg)
        popup.setAdapter(object : android.widget.ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1, labels
        ) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.text = labels[position]
                v.textSize = 16f
                v.typeface = Themes.typeface(context, position) ?: Typeface.DEFAULT
                v.setTextColor(
                    if (position == selected) primary
                    else MaterialColors.getColor(v, com.google.android.material.R.attr.colorOnSurface)
                )
                v.setPadding(dp(16), dp(12), dp(16), dp(12))
                return v
            }
        })
        popup.setOnItemClickListener { _, _, position, _ ->
            popup.dismiss()
            if (Prefs.fontStyle != position) { Prefs.fontStyle = position; recreate() }
        }
        popup.show()
        popup.listView?.setSelection(selected)
    }

    // ----- Reset -----

    private fun resetButton() {
        val b = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        b.text = tr("Restaurar padrões", "Restore defaults")
        b.setOnClickListener {
            BgImage.clear(applicationContext)
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
