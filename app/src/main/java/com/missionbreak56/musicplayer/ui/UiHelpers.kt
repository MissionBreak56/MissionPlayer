package com.missionbreak56.musicplayer.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.missionbreak56.musicplayer.R
import java.util.Locale

/** Small helpers shared by the programmatic screens. */
fun tr(pt: String, en: String): String =
    if (Locale.getDefault().language == "pt") pt else en

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

fun View.roundedBackground(color: Int, radiusDp: Int) {
    val d = GradientDrawable()
    d.setColor(color)
    d.cornerRadius = context.dp(radiusDp).toFloat()
    background = d
}

/** Top bar with a back button and a title, matching the other screens. */
fun screenHeader(context: Context, title: String, onBack: () -> Unit): LinearLayout {
    val row = LinearLayout(context)
    row.orientation = LinearLayout.HORIZONTAL
    row.gravity = android.view.Gravity.CENTER_VERTICAL
    row.setPadding(context.dp(4), 0, context.dp(4), 0)
    val back = ImageButton(context)
    back.setImageResource(R.drawable.ic_arrow_back)
    back.setBackgroundResource(android.R.color.transparent)
    back.setPadding(context.dp(12), context.dp(12), context.dp(12), context.dp(12))
    back.imageTintList = android.content.res.ColorStateList.valueOf(
        com.google.android.material.color.MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorOnSurface, 0
        )
    )
    back.contentDescription = context.getString(R.string.cd_back)
    back.setOnClickListener { onBack() }
    row.addView(back, LinearLayout.LayoutParams(context.dp(48), context.dp(48)))
    val text = TextView(context)
    text.text = title
    text.textSize = 20f
    text.setTypeface(text.typeface, android.graphics.Typeface.BOLD)
    row.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    return row
}
