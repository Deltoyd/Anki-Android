package com.ichi2.anki.ui.widget

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.tan

/**
 * A drawable that paints a diagonal semi-transparent white band
 * sweeping across its bounds, producing a shimmer/shine effect.
 *
 * @param cornerRadiusPx corner radius in pixels for clipping to a rounded rect
 */
class ShimmerDrawable(
    private val cornerRadiusPx: Float,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipPath = Path()
    private val clipRect = RectF()

    /** Normalized progress of the band across the drawable (0f..1f). */
    private var progress = 0f

    private var animator: ValueAnimator? = null

    companion object {
        private const val SWEEP_DURATION_MS = 2000L
        private const val PAUSE_BETWEEN_MS = 3000L
        private const val BAND_WIDTH_FRACTION = 0.35f
        private const val HIGHLIGHT_ALPHA = 51 // ~20% of 255
        private val ANGLE_RAD = Math.toRadians(20.0).toFloat()
    }

    fun start() {
        if (animator != null) return
        animator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = SWEEP_DURATION_MS
                startDelay = PAUSE_BETWEEN_MS
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                // The pause between sweeps comes from startDelay on first run
                // and we use a listener to add delay on subsequent repeats
                addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidateSelf()
                }
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationRepeat(animation: android.animation.Animator) {
                            // Re-apply pause between sweeps by resetting with delay
                            animation.startDelay = PAUSE_BETWEEN_MS
                        }
                    },
                )
                start()
            }
    }

    fun stop() {
        animator?.cancel()
        animator = null
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return

        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()

        // Clip to rounded rect matching the button shape
        clipRect.set(bounds)
        clipPath.reset()
        clipPath.addRoundRect(clipRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)

        // The band travels from left-offscreen to right-offscreen.
        // Extra travel accounts for the diagonal offset.
        val diagonalExtra = h * tan(ANGLE_RAD)
        val totalTravel = w + diagonalExtra + w * BAND_WIDTH_FRACTION
        val bandWidth = w * BAND_WIDTH_FRACTION

        // Current left edge of the band
        val bandLeft = -diagonalExtra - bandWidth + progress * totalTravel

        // Gradient runs diagonally: bottom-left to top-right of the band
        val x0 = bounds.left + bandLeft
        val y0 = bounds.bottom.toFloat()
        val x1 = x0 + bandWidth + diagonalExtra
        val y1 = bounds.top.toFloat()

        val transparent = Color.TRANSPARENT
        val highlight = Color.argb(HIGHLIGHT_ALPHA, 255, 255, 255)

        paint.shader =
            LinearGradient(
                x0,
                y0,
                x1,
                y1,
                intArrayOf(transparent, highlight, highlight, transparent),
                floatArrayOf(0f, 0.35f, 0.65f, 1f),
                Shader.TileMode.CLAMP,
            )

        canvas.drawRect(clipRect, paint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
