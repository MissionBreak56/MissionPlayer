package com.missionbreak56.musicplayer.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.missionbreak56.musicplayer.R

/**
 * Switches the launcher icon by enabling one of the <activity-alias> entries in the manifest
 * (each one has its own icon) and disabling the others. Index 0 is the original icon.
 */
object IconManager {

    /** Same as the manifest namespace; aliases are declared relative to it. */
    private const val NAMESPACE = "com.missionbreak56.musicplayer"

    private val aliases = arrayOf(
        "LauncherDefault", "LauncherGreen", "LauncherBlue", "LauncherViolet", "LauncherGold",
        "LauncherPink", "LauncherVinyl", "LauncherWaves", "LauncherPhones"
    )

    /** Preview image of every icon, in the same order as [aliases]. */
    val icons = intArrayOf(
        R.mipmap.ic_launcher, R.mipmap.ic_launcher_green, R.mipmap.ic_launcher_blue,
        R.mipmap.ic_launcher_violet, R.mipmap.ic_launcher_gold, R.mipmap.ic_launcher_pink,
        R.mipmap.ic_launcher_vinyl, R.mipmap.ic_launcher_waves, R.mipmap.ic_launcher_phones
    )

    val count: Int get() = aliases.size

    fun apply(context: Context, index: Int) {
        val pm = context.packageManager
        val target = index.coerceIn(0, aliases.lastIndex)
        // Enable the new one first so there is never a moment without a launcher entry.
        pm.setComponentEnabledSetting(
            component(context, target),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        for (i in aliases.indices) {
            if (i == target) continue
            pm.setComponentEnabledSetting(
                component(context, i),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        AppName.refreshShortcut(context)
    }

    private fun component(context: Context, index: Int) =
        ComponentName(context.packageName, "$NAMESPACE.${aliases[index]}")
}
