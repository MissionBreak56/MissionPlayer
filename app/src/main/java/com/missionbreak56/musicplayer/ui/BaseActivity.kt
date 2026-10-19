package com.missionbreak56.musicplayer.ui

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.missionbreak56.musicplayer.data.Prefs

/**
 * Applies the user's accent theme before the window is created and lets every screen
 * draw edge to edge (behind the status bar, navigation bar and display cutout).
 */
abstract class BaseActivity : AppCompatActivity() {

    private var appliedTheme = -1
    private var appliedLook = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedTheme = Prefs.theme
        appliedLook = Prefs.customSignature()
        setTheme(Themes.style(appliedTheme))
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }
    }

    /** Puts the accent / font / corner / background choices over whatever theme was just applied. */
    override fun onApplyThemeResource(theme: Resources.Theme, resid: Int, first: Boolean) {
        super.onApplyThemeResource(theme, resid, first)
        Themes.applyOverlays(theme, resources, packageName)
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applyWindowInsets()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyWindowInsets()
    }

    /** Screens with their own full-screen artwork (the player) turn the gallery background off. */
    protected open val allowImageBackground: Boolean get() = true

    /** Called when the system bar / cutout sizes are known. */
    protected open fun onWindowInsets(bars: Insets) {}

    /**
     * Keeps the screen's own background filling the whole display while its content stays
     * clear of the status bar, the notch, the navigation bar and the keyboard.
     */
    private fun applyWindowInsets() {
        val content = findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = content.getChildAt(0) ?: return
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + maxOf(bars.bottom, ime.bottom))
            onWindowInsets(bars)
            WindowInsetsCompat.CONSUMED
        }
        if (allowImageBackground) BgImage.drawable(this)?.let { root.background = it }
        ViewCompat.requestApplyInsets(root)
    }

    override fun onResume() {
        super.onResume()
        if (appliedTheme != Prefs.theme || appliedLook != Prefs.customSignature()) recreate()
    }
}
