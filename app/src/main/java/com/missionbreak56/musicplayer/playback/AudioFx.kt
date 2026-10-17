package com.missionbreak56.musicplayer.playback

import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.missionbreak56.musicplayer.data.Prefs

/**
 * Equalizer, bass boost and virtualizer attached to the player's audio session.
 * Settings live in [Prefs]; any change to an `fx_*` key is re-applied immediately.
 */
object AudioFx {

    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var loud: LoudnessEnhancer? = null

    /** Highest volume boost offered (millibels, 1500 = +15 dB). */
    const val MAX_GAIN_MB = 1500

    // SharedPreferences keeps listeners weakly, so it must be held here.
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.startsWith("fx_")) apply()
    }

    class Info(
        val bands: Int,
        val minLevel: Int,
        val maxLevel: Int,
        val centerHz: IntArray,
        val presets: List<String>
    )

    fun attach(sessionId: Int) {
        release()
        try { eq = Equalizer(0, sessionId) } catch (_: Throwable) { eq = null }
        try { bass = BassBoost(0, sessionId) } catch (_: Throwable) { bass = null }
        try { virt = Virtualizer(0, sessionId) } catch (_: Throwable) { virt = null }
        try { loud = LoudnessEnhancer(sessionId) } catch (_: Throwable) { loud = null }
        Prefs.registerListener(prefListener)
        apply()
    }

    fun release() {
        try { eq?.release() } catch (_: Throwable) {}
        try { bass?.release() } catch (_: Throwable) {}
        try { virt?.release() } catch (_: Throwable) {}
        try { loud?.release() } catch (_: Throwable) {}
        eq = null
        bass = null
        virt = null
        loud = null
    }

    fun apply() {
        val enabled = Prefs.fxEnabled
        try {
            eq?.let { e ->
                e.setEnabled(enabled)
                if (enabled) {
                    val levels = Prefs.fxLevels()
                    val range = e.bandLevelRange
                    val bands = e.numberOfBands.toInt()
                    for (i in 0 until bands) {
                        val level = levels.getOrNull(i) ?: continue
                        e.setBandLevel(i.toShort(), level.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
                    }
                }
            }
        } catch (_: Throwable) {}
        try {
            bass?.let { b ->
                val strength = Prefs.fxBass
                b.setEnabled(enabled && strength > 0)
                if (b.strengthSupported) b.setStrength(strength.coerceIn(0, 1000).toShort())
            }
        } catch (_: Throwable) {}
        try {
            virt?.let { v ->
                val strength = Prefs.fxVirtualizer
                v.setEnabled(enabled && strength > 0)
                if (v.strengthSupported) v.setStrength(strength.coerceIn(0, 1000).toShort())
            }
        } catch (_: Throwable) {}

        // Volume boost works on its own, independent of the equalizer switch.
        try {
            loud?.let { l ->
                val gain = Prefs.fxGain.coerceIn(0, MAX_GAIN_MB)
                l.setTargetGain(gain)
                l.setEnabled(gain > 0)
            }
        } catch (_: Throwable) {}
    }

    private fun describe(e: Equalizer): Info {
        val bands = e.numberOfBands.toInt()
        val range = e.bandLevelRange
        val centers = IntArray(bands) { e.getCenterFreq(it.toShort()) / 1000 }
        val presets = (0 until e.numberOfPresets.toInt()).map { e.getPresetName(it.toShort()) }
        return Info(bands, range[0].toInt(), range[1].toInt(), centers, presets)
    }

    private fun readPreset(e: Equalizer, preset: Int): IntArray {
        e.usePreset(preset.toShort())
        return IntArray(e.numberOfBands.toInt()) { e.getBandLevel(it.toShort()).toInt() }
    }

    /** Band layout of this device's equalizer, or null when unsupported. */
    fun info(): Info? {
        return try {
            val live = eq
            if (live != null) {
                describe(live)
            } else {
                val temp = Equalizer(0, 0)
                try { describe(temp) } finally { temp.release() }
            }
        } catch (_: Throwable) {
            null
        }
    }

    /** Band levels (millibels) of a built-in preset, or null on failure. */
    fun presetLevels(preset: Int): IntArray? {
        return try {
            val live = eq
            if (live != null) {
                readPreset(live, preset)
            } else {
                val temp = Equalizer(0, 0)
                try { readPreset(temp, preset) } finally { temp.release() }
            }
        } catch (_: Throwable) {
            null
        }
    }
}
