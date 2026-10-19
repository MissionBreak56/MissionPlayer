package com.missionbreak56.musicplayer.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.color.MaterialColors
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Prefs

/** The eight accent themes the user can pick from. */
object Themes {
    val styles = intArrayOf(
        R.style.Theme_MissionPlayer_T0, R.style.Theme_MissionPlayer_T1,
        R.style.Theme_MissionPlayer_T2, R.style.Theme_MissionPlayer_T3,
        R.style.Theme_MissionPlayer_T4, R.style.Theme_MissionPlayer_T5,
        R.style.Theme_MissionPlayer_T6, R.style.Theme_MissionPlayer_T7,
        R.style.Theme_MissionPlayer_T8, R.style.Theme_MissionPlayer_T9, R.style.Theme_MissionPlayer_T10, R.style.Theme_MissionPlayer_T11, R.style.Theme_MissionPlayer_T12, R.style.Theme_MissionPlayer_T13, R.style.Theme_MissionPlayer_T14
    )
    val colors = intArrayOf(
        R.color.t0_primary, R.color.t1_primary, R.color.t2_primary, R.color.t3_primary,
        R.color.t4_primary, R.color.t5_primary, R.color.t6_primary, R.color.t7_primary,
        R.color.x8_primary, R.color.x9_primary, R.color.x10_primary, R.color.x11_primary, R.color.x12_primary, R.color.x13_primary, R.color.x14_primary
    )

    fun style(index: Int): Int = styles[index.coerceIn(0, styles.lastIndex)]

    // ----- Advanced customization -----

    /** Accent palette (same order as the Overlay.AccentN styles). */
    val accentColors = intArrayOf(
        0xFFE53935.toInt(), 0xFFFB8C00.toInt(), 0xFFFFB300.toInt(), 0xFFC0CA33.toInt(),
        0xFF43A047.toInt(), 0xFF26C6A2.toInt(), 0xFF00ACC1.toInt(), 0xFF29B6F6.toInt(),
        0xFF1E88E5.toInt(), 0xFF3F51B5.toInt(), 0xFF7E57C2.toInt(), 0xFFAB47BC.toInt(),
        0xFFEC407A.toInt(), 0xFF8E2442.toInt(), 0xFF8D6E63.toInt(), 0xFF78909A.toInt()
    )
    private val accentOverlays = intArrayOf(
        R.style.Overlay_Accent0, R.style.Overlay_Accent1, R.style.Overlay_Accent2, R.style.Overlay_Accent3,
        R.style.Overlay_Accent4, R.style.Overlay_Accent5, R.style.Overlay_Accent6, R.style.Overlay_Accent7,
        R.style.Overlay_Accent8, R.style.Overlay_Accent9, R.style.Overlay_Accent10, R.style.Overlay_Accent11,
        R.style.Overlay_Accent12, R.style.Overlay_Accent13, R.style.Overlay_Accent14, R.style.Overlay_Accent15
    )
    /**
     * Font options. Index 0 follows the theme; the rest are overlays that also replace the theme's
     * text view style (several complete themes set their own font there and would win otherwise).
     * Indexes are saved in the settings, so new fonts are only ever added at the end.
     */
    class FontOption(
        val pt: String,
        val en: String,
        /** Android system family, or null when the font is bundled in [fontRes]. */
        val family: String?,
        val fontRes: Int = 0
    ) {
        val label: String get() = tr(pt, en)
    }

    val fonts: List<FontOption> = listOf(
        FontOption("Usar a fonte do tema", "Use the theme's font", null),
        FontOption("Serifada", "Serif", "serif"),
        FontOption("Monoespaçada", "Monospace", "monospace"),
        FontOption("Condensada", "Condensed", "sans-serif-condensed"),
        FontOption("Leve", "Light", "sans-serif-light"),
        FontOption("Média", "Medium", "sans-serif-medium"),
        FontOption("Casual", "Casual", "casual"),
        FontOption("Cursiva", "Cursive", "cursive"),
        FontOption("Pesada", "Black", "sans-serif-black"),
        FontOption("Padrão do sistema", "System default", "sans-serif"),
        FontOption("Fina", "Thin", "sans-serif-thin"),
        FontOption("Versalete", "Small caps", "sans-serif-smallcaps"),
        FontOption("Condensada leve", "Condensed light", "sans-serif-condensed-light"),
        FontOption("Condensada média", "Condensed medium", "sans-serif-condensed-medium"),
        FontOption("Serifada mono", "Serif mono", "serif-monospace"),
        FontOption("Arredondada (Fredoka)", "Rounded (Fredoka)", null, R.font.fredoka_one),
        FontOption("Manuscrita (Amatic)", "Handwritten (Amatic)", null, R.font.amatic_sc),
        FontOption("Rabisco (Intuitive)", "Scribble (Intuitive)", null, R.font.intuitive),
        FontOption("Serifa elegante (Source Serif)", "Elegant serif (Source Serif)", null, R.font.source_serif),
        FontOption("Moderna (Hanken Grotesk)", "Modern (Hanken Grotesk)", null, R.font.hanken_grotesk)
    )
    private val fontOverlays = intArrayOf(
        0, R.style.Overlay_Font1, R.style.Overlay_Font2, R.style.Overlay_Font3, R.style.Overlay_Font4,
        R.style.Overlay_Font5, R.style.Overlay_Font6, R.style.Overlay_Font7, R.style.Overlay_Font8,
        R.style.Overlay_Font9, R.style.Overlay_Font10, R.style.Overlay_Font11, R.style.Overlay_Font12,
        R.style.Overlay_Font13, R.style.Overlay_Font14, R.style.Overlay_Font15, R.style.Overlay_Font16,
        R.style.Overlay_Font17, R.style.Overlay_Font18, R.style.Overlay_Font19
    )

    /** Typeface used to preview a font option (null = whatever the theme uses). */
    fun typeface(context: Context, index: Int): Typeface? {
        val f = fonts.getOrNull(index) ?: return null
        return when {
            f.family != null -> Typeface.create(f.family, Typeface.NORMAL)
            f.fontRes != 0 -> try { ResourcesCompat.getFont(context, f.fontRes) } catch (_: Throwable) { null }
            else -> null
        }
    }

    // ----- Accent color wheel -----

    const val WHEEL_BASE = 100
    const val WHEEL_HUES = 72
    const val WHEEL_TONES = 5
    private val wheelTones = arrayOf(
        floatArrayOf(0.42f, 0.98f), floatArrayOf(0.62f, 0.95f), floatArrayOf(0.80f, 0.90f),
        floatArrayOf(0.92f, 0.74f), floatArrayOf(0.95f, 0.55f)
    )

    /** Accent code (as saved in [Prefs.accent]) for a wheel hue / tone. */
    fun wheelCode(hue: Int, tone: Int): Int =
        WHEEL_BASE + hue.coerceIn(0, WHEEL_HUES - 1) * 10 + tone.coerceIn(0, WHEEL_TONES - 1)

    fun isWheel(code: Int): Boolean = code >= WHEEL_BASE
    fun wheelHue(code: Int): Int = ((code - WHEEL_BASE) / 10).coerceIn(0, WHEEL_HUES - 1)
    fun wheelTone(code: Int): Int = ((code - WHEEL_BASE) % 10).coerceIn(0, WHEEL_TONES - 1)

    /** Color of a wheel hue (0..71, 5 degrees each) at a given tone (0 = pastel .. 4 = deep). */
    fun wheelColor(hue: Int, tone: Int): Int {
        val t = wheelTones[tone.coerceIn(0, WHEEL_TONES - 1)]
        return Color.HSVToColor(floatArrayOf(hue.coerceIn(0, WHEEL_HUES - 1) * 5f, t[0], t[1]))
    }

    private val cornerOverlays = intArrayOf(
        0, R.style.Overlay_Corner1, R.style.Overlay_Corner2, R.style.Overlay_Corner3
    )
    private val backgroundOverlays = intArrayOf(
        0, R.style.Overlay_Bg1, R.style.Overlay_Bg2, R.style.Overlay_Bg3, R.style.Overlay_Bg4, R.style.Overlay_Bg5
    )
    /** Light / night preview colors of each background option (index 0 = the default look). */
    val backgroundPreviews = arrayOf(
        intArrayOf(0xFFF6F6FA.toInt(), 0xFF000000.toInt()),
        intArrayOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt()),
        intArrayOf(0xFFF1F2F4.toInt(), 0xFF121316.toInt()),
        intArrayOf(0xFFEDF2FA.toInt(), 0xFF0A1020.toInt()),
        intArrayOf(0xFFF5EEE1.toInt(), 0xFF17120D.toInt()),
        intArrayOf(0xFFEBF3EC.toInt(), 0xFF0B140D.toInt())
    )
    val cornerCount = cornerOverlays.size
    val fontCount = fonts.size
    val backgroundCount = backgroundOverlays.size

    /** Layers the user's look settings over the chosen theme. */
    fun applyOverlays(theme: Resources.Theme, resources: Resources, packageName: String) {
        backgroundOverlays.getOrNull(Prefs.backgroundStyle)?.takeIf { it != 0 }?.let { theme.applyStyle(it, true) }
        val accent = Prefs.accent
        if (isWheel(accent)) {
            val id = resources.getIdentifier(
                "Overlay_Wheel_${wheelHue(accent)}_${wheelTone(accent)}", "style", packageName
            )
            if (id != 0) theme.applyStyle(id, true)
        } else if (accent in accentOverlays.indices) {
            theme.applyStyle(accentOverlays[accent], true)
        }
        fontOverlays.getOrNull(Prefs.fontStyle)?.takeIf { it != 0 }?.let { theme.applyStyle(it, true) }
    }
}

fun View.primaryColor(): Int = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary)

fun View.onSurfaceColor(): Int = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)

fun View.onSurfaceVariantColor(): Int =
    MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant)

object Format {
    fun time(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val minutes = total / 60
        val seconds = total % 60
        return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
    }
}
