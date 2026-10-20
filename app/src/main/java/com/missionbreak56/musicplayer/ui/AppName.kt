package com.missionbreak56.musicplayer.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.data.Prefs

/**
 * The name the user chose for the app (set only in the Customization screen).
 *
 * Android does not let an app rename its own entry in the app drawer, so the chosen name is used
 * inside the app (About screen) and on a home screen shortcut that the user can create. Once that
 * shortcut exists, changing the name or the icon updates it.
 */
object AppName {

    const val MAX_LENGTH = 24

    private const val SHORTCUT_ID = "named_launcher"

    /** The chosen name, or the app's own name when none was set. */
    fun get(context: Context): String =
        Prefs.appName.ifBlank { context.getString(R.string.app_name) }

    private fun shortcut(app: Context): ShortcutInfoCompat {
        val name = get(app)
        val icon = IconManager.icons.getOrElse(Prefs.iconVariant) { R.mipmap.ic_launcher }
        return ShortcutInfoCompat.Builder(app, SHORTCUT_ID)
            .setShortLabel(name)
            .setLongLabel(name)
            .setIcon(IconCompat.createWithResource(app, icon))
            .setIntent(Intent(app, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
            .build()
    }

    /** Asks the launcher to add a home screen shortcut with the chosen name and icon. */
    fun requestShortcut(context: Context): Boolean {
        val app = context.applicationContext
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(app)) return false
        return ShortcutManagerCompat.requestPinShortcut(app, shortcut(app), null)
    }

    /** Updates the home screen shortcut, if the user created one, with the current name and icon. */
    fun refreshShortcut(context: Context) {
        try {
            val app = context.applicationContext
            ShortcutManagerCompat.updateShortcuts(app, listOf(shortcut(app)))
        } catch (_: Exception) {
            // no shortcut yet, or the launcher does not support it: nothing to update
        }
    }
}
