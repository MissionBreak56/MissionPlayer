package com.missionbreak56.musicplayer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils

/** Vertical "# A B C ... Z" strip: touch or drag on it to jump to a letter. */
class AlphaIndexBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val letters: List<String> = listOf("#") + ('A'..'Z').map { it.toString() }

    /** Letters that actually have items; the others are drawn dimmer. */
    var available: Set<String> = emptySet()
        set(value) {
            field = value
            invalidate()
        }

    /** Called with the touched letter, and with null when the finger is lifted. */
    var onLetter: ((String?) -> Unit)? = null

    private var active = -1
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val pill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val cell = height.toFloat() / letters.size
        val primary = primaryColor()
        val variant = onSurfaceVariantColor()

        pill.color = ColorUtils.setAlphaComponent(onSurfaceColor(), if (active >= 0) 40 else 18)
        rect.set(w * 0.1f, 0f, w * 0.9f, height.toFloat())
        canvas.drawRoundRect(rect, w / 2f, w / 2f, pill)

        paint.textSize = minOf(cell * 0.68f, 11f * resources.displayMetrics.scaledDensity)
        for (i in letters.indices) {
            val letter = letters[i]
            paint.color = when {
                i == active -> primary
                available.contains(letter) -> variant
                else -> ColorUtils.setAlphaComponent(variant, 80)
            }
            val y = cell * i + cell / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(letter, w / 2f, y, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val index = (event.y / height * letters.size).toInt().coerceIn(0, letters.size - 1)
                if (index != active) {
                    active = index
                    invalidate()
                    onLetter?.invoke(letters[index])
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                active = -1
                invalidate()
                onLetter?.invoke(null)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
