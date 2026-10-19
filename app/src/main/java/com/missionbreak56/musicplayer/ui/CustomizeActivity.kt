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
 * Customization: theme, light/dark mode, app icon, accent color, background, font and corner style.
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
        root.addView(screenHeader(this, tr("Personalização", "Customization")) { finish() })

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
        themeSection()
        modeSection()
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

    // ----- Theme and light / dark mode (floating menus) -----

    /** A button that looks like the font / icon pickers: current value and a small arrow. */
    private fun dropdownButton(text: String, onClick: (View) -> Unit) {
        val button = LinearLayout(this)
        button.orientation = LinearLayout.HORIZONTAL
        button.gravity = Gravity.CENTER_VERTICAL
        button.setPadding(dp(14), dp(12), dp(14), dp(12))
        button.background = ring(false, 14)
        val label = TextView(this)
        label.text = text
        label.textSize = 16f
        button.addView(label, LinearLayout.LayoutParams(0, -2, 1f))
        val arrow = TextView(this)
        arrow.text = "▾"
        arrow.textSize = 18f
        button.addView(arrow)
        button.setOnClickListener { onClick(button) }
        body.addView(button, LinearLayout.LayoutParams(-1, -2))
    }

    /** Floating list with the current choice highlighted. */
    private fun showListMenu(anchor: View, labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
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
            if (position != selected) onPick(position)
        }
        popup.show()
        popup.listView?.setSelection(selected)
    }

    private fun themeSection() {
        section(
            tr("Tema", "Theme"),
            tr("Cores e visual completo do app. Ao trocar, a cor de destaque e a fonte seguem o tema.", "Colors and full look of the app. When you change it, the accent color and font follow the theme.")
        )
        val names = resources.getStringArray(com.missionbreak56.musicplayer.R.array.theme_names).toList()
        val current = Prefs.theme.coerceIn(0, names.lastIndex)
        dropdownButton(names[current]) { anchor ->
            showListMenu(anchor, names, current) { index ->
                // A new theme starts with its own accent color and font.
                Prefs.accent = -1
                Prefs.fontStyle = 0
                Prefs.theme = index
                recreate()
            }
        }
    }

    private fun modeSection() {
        section(tr("Modo", "Mode"))
        val labels = listOf(tr("Do sistema", "System"), tr("Claro", "Light"), tr("Escuro", "Dark"))
        val modes = intArrayOf(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        )
        val current = modes.indexOf(Prefs.nightMode).coerceAtLeast(0)
        dropdownButton(labels[current]) { anchor ->
            showListMenu(anchor, labels, current) { index ->
                Prefs.nightMode = modes[index]
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(modes[index])
                build()
            }
        }
    }

    // ----- Icon -----

    private fun iconNames() = arrayOf(
        tr("Original", "Original"), tr("Verde", "Green"), tr("Azul", "Blue"),
        tr("Violeta", "Violet"), tr("Dourado", "Gold"), tr("Rosa", "Pink"),
        tr("Vinil", "Vinyl"), tr("Ondas", "Waves"), tr("Fones", "Headphones")
    )

    private fun iconSection() {
        section(
            tr("Ícone do app", "App icon"),
            tr(
                "Muda o ícone na tela inicial. Pode levar alguns segundos para atualizar.",
                "Changes the home screen icon. It may take a few seconds to update."
            )
        )
        val names = iconNames()
        val index = Prefs.iconVariant.coerceIn(0, IconManager.count - 1)
        val button = LinearLayout(this)
        button.orientation = LinearLayout.HORIZONTAL
        button.gravity = Gravity.CENTER_VERTICAL
        button.setPadding(dp(14), dp(10), dp(14), dp(10))
        button.background = ring(false, 14)
        val img = ImageView(this)
        img.setImageResource(IconManager.icons[index])
        roundClip(img, 10)
        button.addView(img, LinearLayout.LayoutParams(dp(40), dp(40)))
        val label = TextView(this)
        label.text = names[index]
        label.textSize = 16f
        button.addView(label, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) })
        val arrow = TextView(this)
        arrow.text = "▾"
        arrow.textSize = 18f
        button.addView(arrow)
        button.setOnClickListener { showIconMenu(button) }
        body.addView(button, LinearLayout.LayoutParams(-1, -2))
    }

    /** Floating list of app icons, each with its picture. */
    private fun showIconMenu(anchor: View) {
        val names = iconNames()
        val selected = Prefs.iconVariant.coerceIn(0, IconManager.count - 1)
        val popup = android.widget.ListPopupWindow(this)
        popup.anchorView = anchor
        popup.width = anchor.width
        popup.height = (resources.displayMetrics.heightPixels * 0.6f).toInt()
        popup.isModal = true
        val bg = GradientDrawable()
        bg.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0))
        bg.cornerRadius = dp(14).toFloat()
        popup.setBackgroundDrawable(bg)
        popup.setAdapter(object : android.widget.BaseAdapter() {
            override fun getCount(): Int = IconManager.count
            override fun getItem(position: Int): Any = names[position]
            override fun getItemId(position: Int): Long = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val row = LinearLayout(this@CustomizeActivity)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.setPadding(dp(16), dp(8), dp(16), dp(8))
                val pic = ImageView(this@CustomizeActivity)
                pic.setImageResource(IconManager.icons[position])
                roundClip(pic, 10)
                row.addView(pic, LinearLayout.LayoutParams(dp(40), dp(40)))
                val t = TextView(this@CustomizeActivity)
                t.text = names[position]
                t.textSize = 16f
                t.setTextColor(
                    if (position == selected) primary
                    else MaterialColors.getColor(t, com.google.android.material.R.attr.colorOnSurface)
                )
                row.addView(t, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) })
                if (position == selected) {
                    val mark = TextView(this@CustomizeActivity)
                    mark.text = "✓"
                    mark.textSize = 18f
                    mark.setTextColor(primary)
                    row.addView(mark)
                }
                return row
            }
        })
        popup.setOnItemClickListener { _, _, position, _ ->
            popup.dismiss()
            if (Prefs.iconVariant != position) {
                Prefs.iconVariant = position
                IconManager.apply(this, position)
                Toast.makeText(
                    this,
                    tr("Ícone alterado: ${names[position]}", "Icon changed: ${names[position]}"),
                    Toast.LENGTH_SHORT
                ).show()
                build()
            }
        }
        popup.show()
        popup.listView?.setSelection(selected)
    }

    // ----- Accent color -----

    /** Accent color: the theme's own, or any hue (bar) at five intensities (bar). */
    private fun accentSection() {
        val current = Prefs.accent
        val onCustom = Themes.isWheel(current)
        var hue = if (onCustom) Themes.wheelHue(current) else 0
        var tone = if (onCustom) Themes.wheelTone(current) else 2

        section(
            tr("Cor de Destaque", "Accent color"),
            if (onCustom) tr("Cor personalizada", "Custom color")
            else tr("Usando a cor do tema", "Using the theme's color")
        )

        val themeChip = TextView(this)
        themeChip.text = tr("Usar a cor do tema", "Use the theme's color")
        themeChip.textSize = 14f
        themeChip.setPadding(dp(16), dp(10), dp(16), dp(10))
        themeChip.background = ring(!onCustom, 20, if (!onCustom) tint(primary, 0x26) else Color.TRANSPARENT)
        themeChip.setOnClickListener {
            if (Prefs.accent != -1) { Prefs.accent = -1; recreate() }
        }
        body.addView(themeChip, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(4) })

        fun commit() {
            Prefs.accent = Themes.wheelCode(hue, tone)
            recreate()
        }

        val toneNames = arrayOf(
            tr("Pastel", "Pastel"), tr("Suave", "Soft"), tr("Vivo", "Vivid"),
            tr("Forte", "Strong"), tr("Escuro", "Deep")
        )

        // live preview of the chosen color
        val preview = View(this)
        val previewBg = GradientDrawable()
        previewBg.shape = GradientDrawable.RECTANGLE
        previewBg.cornerRadius = dp(12).toFloat()
        previewBg.setColor(Themes.wheelColor(hue, tone))
        preview.background = previewBg
        preview.alpha = if (onCustom) 1f else 0.5f
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
                previewBg.setColor(Themes.wheelColor(hue, tone))
                preview.alpha = 1f
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) { commit() }
        })
        body.addView(hueBar, LinearLayout.LayoutParams(-1, dp(40)))

        val toneLabel = TextView(this)
        fun toneText() = tr("Intensidade: ${toneNames[tone]}", "Intensity: ${toneNames[tone]}")
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
                previewBg.setColor(Themes.wheelColor(hue, tone))
                preview.alpha = 1f
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
        var shown = 0
        for (i in 1 until Themes.backgroundCount) {
            if (shown > 0 && shown % perRow == 0) row = newRow()
            shown++
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
        // keep the last row aligned with the others
        while (shown % perRow != 0) {
            row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            shown++
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
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(tr("Restaurar padrões?", "Restore defaults?"))
                .setMessage(
                    tr(
                        "Tema, modo, ícone, cor de destaque, fundo (inclusive a imagem) e fonte voltarão ao padrão do app.",
                        "Theme, mode, icon, accent color, background (including the image) and font will go back to the app defaults."
                    )
                )
                .setPositiveButton(tr("Restaurar", "Restore")) { _, _ ->
                    BgImage.clear(applicationContext)
                    Prefs.resetCustomization()
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(Prefs.nightMode)
                    if (Prefs.iconVariant != 0) {
                        Prefs.iconVariant = 0
                        IconManager.apply(this, 0)
                    }
                    recreate()
                }
                .setNegativeButton(tr("Cancelar", "Cancel"), null)
                .show()
        }
        body.addView(b, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(28) })
    }

    companion object {
        private const val STATE_SCROLL = "scroll_y"
    }
}
