package com.missionbreak56.musicplayer.ui

import android.graphics.Bitmap
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlin.math.abs

/** Picks the main colours of a cover for the "liquid glass" backgrounds. */
object GlassColors {

    const val COUNT = 4

    private fun hue(color: Int): Float {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        return hsl[0]
    }

    private fun hueDistance(a: Int, b: Int): Float {
        val d = abs(hue(a) - hue(b))
        return if (d > 180f) 360f - d else d
    }

    private fun lively(color: Int): Boolean {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        return hsl[1] >= 0.15f && hsl[2] in 0.10f..0.90f
    }

    /**
     * Returns [COUNT] raw colours of the cover, most present first. Different hues are preferred
     * (e.g. green trees, blue sea and a red title instead of four reds), so the glass mixes
     * everything that is really in the picture.
     */
    fun pick(bitmap: Bitmap, fallback: Int): IntArray {
        val palette = Palette.from(bitmap).maximumColorCount(32).generate()
        val swatches = palette.swatches.sortedByDescending { it.population }
        val chosen = ArrayList<Int>()
        for (s in swatches) {
            if (chosen.size >= COUNT) break
            if (!lively(s.rgb)) continue
            if (chosen.all { hueDistance(it, s.rgb) >= 30f }) chosen.add(s.rgb)
        }
        // not enough distinct hues: complete with the remaining most common colours
        for (s in swatches) {
            if (chosen.size >= COUNT) break
            if (!chosen.contains(s.rgb)) chosen.add(s.rgb)
        }
        if (chosen.isEmpty()) chosen.add(fallback)
        var i = 0
        while (chosen.size < COUNT) {
            chosen.add(chosen[i % chosen.size])
            i++
        }
        return chosen.toIntArray()
    }
}
