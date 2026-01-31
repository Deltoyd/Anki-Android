package com.ichi2.anki.ui.museum

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var color: Int,
    var life: Float, // 1.0 → 0.0
)

class RewardOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private var label: String? = null
    private val paintParticle = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 48f
        isFakeBoldText = true
    }
    private var animating = false

    override fun onTouchEvent(event: MotionEvent): Boolean = false

    fun playReward(tier: RewardTier) {
        if (tier == RewardTier.NONE) return
        particles.clear()
        label = null

        val cx = width / 2f
        val cy = height / 2f

        when (tier) {
            RewardTier.COMMON -> {
                label = null
                spawnBurst(cx, cy, 15, longArrayOf(GOLD))
                scheduleAnimation(600L)
            }
            RewardTier.UNCOMMON -> {
                label = "+1"
                spawnBurst(cx, cy, 30, longArrayOf(GOLD, WHITE))
                scheduleAnimation(900L)
            }
            RewardTier.RARE -> {
                label = "+1 RARE!"
                spawnBurst(cx, cy, 50, longArrayOf(GOLD, WHITE, PINK, CYAN, PURPLE))
                scheduleAnimation(1200L)
            }
            RewardTier.NONE -> {} // unreachable
        }
        invalidate()
    }

    private fun spawnBurst(cx: Float, cy: Float, count: Int, colors: LongArray) {
        repeat(count) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = Random.nextFloat() * 300f + 100f
            particles.add(
                Particle(
                    x = cx,
                    y = cy,
                    vx = Math.cos(angle.toDouble()).toFloat() * speed,
                    vy = Math.sin(angle.toDouble()).toFloat() * speed,
                    size = Random.nextFloat() * 6f + 4f,
                    color = colors[it % colors.size].toInt(),
                    life = 1.0f,
                ),
            )
        }
    }

    private fun scheduleAnimation(durationMs: Long) {
        if (animating) return
        animating = true
        val animator = ValueAnimator.ofFloat(1.0f, 0.0f).apply {
            duration = durationMs
            addUpdateListener { anim ->
                val progress = anim.animatedValue as Float
                updateParticles(progress)
                invalidate()
            }
            addListener(object : android.animation.AnimatorListener {
                    override fun onAnimationStart(animation: android.animation.Animator) {}

                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        particles.clear()
                        label = null
                        animating = false
                        invalidate()
                    }

                    override fun onAnimationCancel(animation: android.animation.Animator) {}
                    override fun onAnimationRepeat(animation: android.animation.Animator) {}
                },
            )
        }
        animator.start()
    }

    private fun updateParticles(remainingLife: Float) {
        val dt = 0.016f // approximate 60 fps frame delta
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life = remainingLife
            p.x += p.vx * dt
            p.y += p.vy * dt
            // gravity
            p.vy += 200f * dt
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in particles) {
            val alpha = (p.life * 255f).toInt().coerceIn(0, 255)
            paintParticle.color = Color.argb(alpha, Color.red(p.color), Color.green(p.color), Color.blue(p.color))
            canvas.drawCircle(p.x, p.y, p.size, paintParticle)
        }
        label?.let { text ->
            val cx = width / 2f
            val cy = height / 2f - 40f
            val alpha = if (particles.isNotEmpty()) (particles[0].life * 255f).toInt().coerceIn(0, 255) else 255
            paintText.color = Color.argb(alpha, 255, 255, 255)
            canvas.drawText(text, cx, cy, paintText)
        }
    }

    companion object {
        private const val GOLD = 0xFFFFC107L
        private const val WHITE = 0xFFFFFFFFL
        private const val PINK = 0xFFE91E63L
        private const val CYAN = 0xFF00BCDBL
        private const val PURPLE = 0xFF9C27B0L
    }
}
