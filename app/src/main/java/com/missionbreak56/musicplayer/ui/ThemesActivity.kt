package com.missionbreak56.musicplayer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.databinding.ActivityThemesBinding

class ThemesActivity : BaseActivity() {

    private lateinit var binding: ActivityThemesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        binding.listThemes.layoutManager = GridLayoutManager(this, 4)
        binding.listThemes.adapter = ThemeAdapter(
            resources.getStringArray(R.array.theme_names), Prefs.theme
        ) { index ->
            if (index != Prefs.theme) {
                // A new theme starts with its own accent color and font.
                Prefs.accent = -1
                Prefs.fontStyle = 0
            }
            Prefs.theme = index
            recreate()
        }

        binding.radioMode.check(
            when (Prefs.nightMode) {
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.radioLight
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.radioDark
                else -> R.id.radioSystem
            }
        )
        binding.radioMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radioDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            Prefs.nightMode = mode
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}
