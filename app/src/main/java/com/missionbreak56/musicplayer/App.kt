package com.missionbreak56.musicplayer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.missionbreak56.musicplayer.data.Labels
import com.missionbreak56.musicplayer.data.LyricsWorker
import com.missionbreak56.musicplayer.data.Prefs

/** Application entry point. Playback is offline; the internet is only used to download lyrics. */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        com.missionbreak56.musicplayer.ui.Themes.migrateLegacyAccent()
        Labels.init(this)
        AppCompatDelegate.setDefaultNightMode(Prefs.nightMode)
        if (Prefs.autoLyrics) LyricsWorker.schedule(this)
    }
}
