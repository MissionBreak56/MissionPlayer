package com.missionbreak56.musicplayer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.missionbreak56.musicplayer.data.Labels
import com.missionbreak56.musicplayer.data.Prefs

/** Application entry point. Offline only: there is no network layer at all. */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Labels.init(this)
        AppCompatDelegate.setDefaultNightMode(Prefs.nightMode)
    }
}
