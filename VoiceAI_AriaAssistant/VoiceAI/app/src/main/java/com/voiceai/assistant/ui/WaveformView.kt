package com.voiceai.assistant.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

/**
 * Custom animated waveform shown while the mic is active.
 * Animates bars based on RMS amplitude from the speech recognizer.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BB86FC")
        strokeCap = Paint.Cap.ROUND
    }

    private var amplitudes = FloatArray(32) { 0.1f }
    private var animOffset = 0f
    private var isAnimating = false
    private val runnable = Runnable { tick() }

    fun startAnimation() {
        isAnimating = true
        invalidate()
        postDelayed(runnable, 50)
    }

    fun stopAnimation() {
        isAnimating = false
        amplitudes = FloatArray(32) { 0.1f }
        removeCallbacks(runnable)
        invalidate()
    }

    fun updateAmplitude(rms: Float) {
        val norm = ((rms + 2f) / 12f).coerceIn(0.05f, 1f)
        // Shift left
        for (i in 0 until amplitudes.size - 1) amplitudes[i] = amplitudes[i + 1]
        amplitudes[amplitudes.size - 1] = norm
        invalidate()
    }

    private fun tick() {
        if (!isAnimating) return
        animOffset += 0.15f
        // Idle animation when no rms update
        for (i in amplitudes.indices) {
            amplitudes[i] = (0.2f + 0.15f * sin(animOffset + i * 0.4f).toFloat()).coerceIn(0.05f, 1f)
        }
        invalidate()
        postDelayed(runnable, 50)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val barCount = amplitudes.size
        val barWidth = w / barCount * 0.6f
        val gap = w / barCount
        paint.strokeWidth = barWidth

        for (i in 0 until barCount) {
            val x = gap * i + gap / 2f
            val barH = amplitudes[i] * h * 0.9f
            val top = (h - barH) / 2f
            val bot = (h + barH) / 2f
            val alpha = (155 + (100 * amplitudes[i]).toInt()).coerceIn(0, 255)
            paint.alpha = alpha
            canvas.drawLine(x, top, x, bot, paint)
        }
    }
}
