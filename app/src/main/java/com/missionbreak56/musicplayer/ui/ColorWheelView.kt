package com.missionbreak56.musicplayer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Hue wheel for picking the accent color. The hue snaps to one of [Themes.WHEEL_HUES] steps
 * (5 degrees each); the center shows the color with the currently chosen tone.
 */
class ColorWheelView(context: Context) : View(context) {

    var hue: Int = 0
        set(value) { field = value.coerceIn(0, Themes.WHEEL_HUES - 1); invalidate() }
    var tone: Int = 2
        set(value) { field = value.coerceIn(0, Themes.WHEEL_TONES - 1); invalidate() }
    /** False while nothing from the wheel is the active accent (the thumb is then hidden). */
    var showThumb: Boolean = true
        set(value) { field = value; invalidate() }

    /** Called while dragging (final = false) and on release (final = true). */
    var onPick: ((hue: Int, final: Boolean) -> Unit)? = null

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.WHITE }
    private val thumbShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0x66000000 }
    private val ringRect = RectF()
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f
    private var ringWidth = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f
        cy = h / 2f
        ringWidth = context.dp(34).toFloat()
        radius = min(w, h) / 2f - context.dp(6) - ringWidth / 2f
        ringRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        val colors = IntArray(37) { Color.HSVToColor(floatArrayOf(it * 10f, 0.85f, 0.95f)) }
        ringPaint.strokeWidth = ringWidth
        ringPaint.shader = SweepGradient(cx, cy, colors, null)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawOval(ringRect, ringPaint)
        centerPaint.color = Themes.wheelColor(hue, tone)
        canvas.drawCircle(cx, cy, radius - ringWidth / 2f - context.dp(14), centerPaint)
        if (showThumb) {
            val angle = Math.toRadians(hue * 5.0)
            val tx = cx + radius * cos(angle).toFloat()
            val ty = cy + radius * sin(angle).toFloat()
            val r = ringWidth / 2f + context.dp(2)
            thumbFill.color = Color.HSVToColor(floatArrayOf(hue * 5f, 0.85f, 0.95f))
            thumbShadow.strokeWidth = context.dp(5).toFloat()
            thumbStroke.strokeWidth = context.dp(3).toFloat()
            canvas.drawCircle(tx, ty, r, thumbFill)
            canvas.drawCircle(tx, ty, r, thumbShadow)
            canvas.drawCircle(tx, ty, r, thumbStroke)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val size = min(w, context.dp(260))
        setMeasuredDimension(size, size)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val dx = event.x - cx
                val dy = event.y - cy
                // Only start on the ring itself (with some slack), not in the middle.
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < radius - ringWidth || dist > radius + ringWidth) return false
                parent?.requestDisallowInterceptTouchEvent(true)
                pick(event, false)
                return true
            }
            MotionEvent.ACTION_MOVE -> { pick(event, false); return true }
            MotionEvent.ACTION_UP -> { pick(event, true); return true }
            MotionEvent.ACTION_CANCEL -> { onPick?.invoke(hue, true); return true }
        }
        return super.onTouchEvent(event)
    }

    private fun pick(event: MotionEvent, final: Boolean) {
        var deg = Math.toDegrees(atan2((event.y - cy).toDouble(), (event.x - cx).toDouble()))
        if (deg < 0) deg += 360.0
        showThumb = true
        hue = Math.round(deg / 5.0).toInt() % Themes.WHEEL_HUES
        onPick?.invoke(hue, final)
    }
}
