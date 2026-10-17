package com.missionbreak56.musicplayer.ui

import android.view.View
import com.google.android.material.color.MaterialColors
import com.missionbreak56.musicplayer.R

/** The eight accent themes the user can pick from. */
object Themes {
    val styles = intArrayOf(
        R.style.Theme_MissionPlayer_T0, R.style.Theme_MissionPlayer_T1,
        R.style.Theme_MissionPlayer_T2, R.style.Theme_MissionPlayer_T3,
        R.style.Theme_MissionPlayer_T4, R.style.Theme_MissionPlayer_T5,
        R.style.Theme_MissionPlayer_T6, R.style.Theme_MissionPlayer_T7
    )
    val colors = intArrayOf(
        R.color.t0_primary, R.color.t1_primary, R.color.t2_primary, R.color.t3_primary,
        R.color.t4_primary, R.color.t5_primary, R.color.t6_primary, R.color.t7_primary
    )

    fun style(index: Int): Int = styles[index.coerceIn(0, styles.lastIndex)]
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
