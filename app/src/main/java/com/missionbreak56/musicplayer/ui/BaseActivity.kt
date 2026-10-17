package com.missionbreak56.musicplayer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.missionbreak56.musicplayer.data.Prefs

/** Applies the user's accent theme before the window is created. */
abstract class BaseActivity : AppCompatActivity() {

    private var appliedTheme = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedTheme = Prefs.theme
        setTheme(Themes.style(appliedTheme))
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (appliedTheme != Prefs.theme) recreate()
    }
}
