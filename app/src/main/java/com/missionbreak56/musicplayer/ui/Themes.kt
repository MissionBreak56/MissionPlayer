package com.missionbreak56.musicplayer.ui

import android.content.res.Resources
import android.view.View
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
    /** Index 0 is "as the theme"; the rest are overlays. */
    private val fontOverlays = intArrayOf(
        0, R.style.Overlay_Font1, R.style.Overlay_Font2, R.style.Overlay_Font3, R.style.Overlay_Font4,
        R.style.Overlay_Font5, R.style.Overlay_Font6, R.style.Overlay_Font7, R.style.Overlay_Font8
    )
    val fontFamilies = arrayOf(
        "sans-serif", "serif", "monospace", "sans-serif-condensed", "sans-serif-light",
        "sans-serif-medium", "casual", "cursive", "sans-serif-black"
    )
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
    val fontCount = fontOverlays.size
    val backgroundCount = backgroundOverlays.size

    /** Layers the user's look settings over the chosen theme. */
    fun applyOverlays(theme: Resources.Theme) {
        backgroundOverlays.getOrNull(Prefs.backgroundStyle)?.takeIf { it != 0 }?.let { theme.applyStyle(it, true) }
        Prefs.accent.takeIf { it in accentOverlays.indices }?.let { theme.applyStyle(accentOverlays[it], true) }
        cornerOverlays.getOrNull(Prefs.cornerStyle)?.takeIf { it != 0 }?.let { theme.applyStyle(it, true) }
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
