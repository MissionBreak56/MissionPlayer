package com.missionbreak56.musicplayer.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.databinding.ActivityEqualizerBinding
import com.missionbreak56.musicplayer.playback.AudioFx

class EqualizerActivity : BaseActivity() {

    private lateinit var binding: ActivityEqualizerBinding
    private var info: AudioFx.Info? = null
    private var levels = IntArray(0)
    private val bandSliders = ArrayList<Slider>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEqualizerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        setupBoost()

        val eqInfo = AudioFx.info()
        info = eqInfo
        if (eqInfo == null || eqInfo.bands == 0 || eqInfo.minLevel >= eqInfo.maxLevel) {
            binding.content.visibility = View.GONE
            binding.btnReset.visibility = View.GONE
            binding.textUnavailable.visibility = View.VISIBLE
            return
        }
        build(eqInfo)
    }

    /** Volume boost above the normal maximum (0-100 % of [AudioFx.MAX_GAIN_MB]). */
    private fun setupBoost() {
        val perPercent = AudioFx.MAX_GAIN_MB / 100f
        binding.sliderBoost.value = (Prefs.fxGain / perPercent).coerceIn(0f, 100f)
        updateBoostLabel()
        binding.sliderBoost.addOnChangeListener { _, v, fromUser ->
            if (fromUser) {
                Prefs.fxGain = (v * perPercent).toInt()
                updateBoostLabel()
            }
        }
    }

    private fun updateBoostLabel() {
        val percent = Math.round(Prefs.fxGain * 100f / AudioFx.MAX_GAIN_MB)
        binding.textBoostValue.text = if (percent == 0) getString(R.string.volume_boost_off) else "+$percent%"
    }

    private fun build(eq: AudioFx.Info) {
        val saved = Prefs.fxLevels()
        levels = IntArray(eq.bands) { saved.getOrNull(it) ?: 0 }

        binding.switchEnable.isChecked = Prefs.fxEnabled
        binding.switchEnable.setOnCheckedChangeListener { _, checked -> Prefs.fxEnabled = checked }

        val density = resources.displayMetrics.density
        for (i in 0 until eq.bands) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val label = TextView(this).apply {
                text = formatHz(eq.centerHz[i])
                textSize = 13f
                setTextColor(binding.root.onSurfaceVariantColor())
            }
            val slider = Slider(this).apply {
                valueFrom = eq.minLevel.toFloat()
                valueTo = eq.maxLevel.toFloat()
                value = levels[i].toFloat().coerceIn(eq.minLevel.toFloat(), eq.maxLevel.toFloat())
                addOnChangeListener { _, v, fromUser ->
                    if (fromUser) {
                        levels[i] = v.toInt()
                        Prefs.setFxLevels(levels)
                        Prefs.fxPreset = -1
                        updatePresetLabel()
                    }
                }
            }
            row.addView(label, LinearLayout.LayoutParams((64 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT))
            row.addView(slider, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            binding.bandsContainer.addView(row)
            bandSliders += slider
        }

        binding.sliderBass.value = Prefs.fxBass.toFloat().coerceIn(0f, 1000f)
        binding.sliderBass.addOnChangeListener { _, v, fromUser ->
            if (fromUser) Prefs.fxBass = v.toInt()
        }
        binding.sliderSurround.value = Prefs.fxVirtualizer.toFloat().coerceIn(0f, 1000f)
        binding.sliderSurround.addOnChangeListener { _, v, fromUser ->
            if (fromUser) Prefs.fxVirtualizer = v.toInt()
        }

        binding.btnPreset.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.eq_preset)
                .setItems(eq.presets.toTypedArray()) { _, which -> applyPreset(which) }
                .show()
        }
        binding.btnReset.setOnClickListener { reset(eq) }
        updatePresetLabel()
    }

    private fun applyPreset(index: Int) {
        val preset = AudioFx.presetLevels(index) ?: return
        for (i in levels.indices) {
            levels[i] = preset.getOrNull(i) ?: 0
            bandSliders.getOrNull(i)?.let { s ->
                s.value = levels[i].toFloat().coerceIn(s.valueFrom, s.valueTo)
            }
        }
        Prefs.setFxLevels(levels)
        Prefs.fxPreset = index
        updatePresetLabel()
    }

    private fun reset(eq: AudioFx.Info) {
        for (i in levels.indices) {
            levels[i] = 0
            bandSliders.getOrNull(i)?.let { s ->
                s.value = 0f.coerceIn(s.valueFrom, s.valueTo)
            }
        }
        Prefs.setFxLevels(levels)
        Prefs.fxPreset = -1
        Prefs.fxBass = 0
        Prefs.fxVirtualizer = 0
        Prefs.fxGain = 0
        binding.sliderBoost.value = 0f
        updateBoostLabel()
        binding.sliderBass.value = 0f
        binding.sliderSurround.value = 0f
        updatePresetLabel()
    }

    private fun updatePresetLabel() {
        val presets = info?.presets.orEmpty()
        val preset = Prefs.fxPreset
        val name = presets.getOrNull(preset) ?: getString(R.string.eq_custom)
        binding.btnPreset.text = getString(R.string.eq_preset) + ": " + name
    }

    private fun formatHz(hz: Int): String =
        if (hz >= 1000) {
            val k = hz / 1000.0
            if (hz % 1000 == 0) "${hz / 1000} kHz" else String.format(java.util.Locale.US, "%.1f kHz", k)
        } else {
            "$hz Hz"
        }
}
