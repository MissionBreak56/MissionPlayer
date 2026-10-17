package com.missionbreak56.musicplayer

import android.app.Application
import com.missionbreak56.musicplayer.data.Prefs

/**
 * Application entry point.
 *
 * This app has no remote license check and no network layer at all. Premium is
 * unlocked locally and persisted on first launch.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Prefs.init(this)
        // Persist premium = true on the very first run.
        if (!Prefs.premiumInitialised) {
            Prefs.premium = true
            Prefs.markPremiumInitialised()
        }
    }
}
