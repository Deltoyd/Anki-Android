package com.ichi2.anki.ui.museum

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Overlay view that displays a full painting for a few seconds, then animates it
 * breaking into puzzle pieces that scatter outward — a cinematic "quest start" effect.
 *
 * The animation runs in three phases:
 *   1. **Display** — Full image visible for [DISPLAY_DURATION_MS].
 *   2. **Crack** — Pieces separate slightly with a white flash.
 *   3. **Scatter** — Pieces fly outward, rotate, shrink, and fade (edge-first stagger).
 *
 * Usage:
 *   1. Add this view in the layout, overlaying the gallery area.
 *   2. Call [startAnimation] with the active painting bitmap.
 *   3. The view handles all timing and hides itself when done.
 */
class PuzzleBreakAnimationView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        companion object {
            /** Duration the full image is displayed before breaking (ms). */
            const val DISPLAY_DURATION_MS = 3000L

            /** Duration of the break + scatter animation (ms). */
            const val BREAK_DURATION_MS = 2500L

            /** Grid columns for puzzle pieces. */
            const val GRID_COLS = 8

            /** Grid rows for puzzle pieces. */
            const val GRID_ROWS = 6

            /** Fraction of break animation spent on the crack phase (0..1). */
            private const val CRACK_FRACTION = 0.15f

            /** Maximum stagger delay as fraction of total break (0..1).
             *  Center pieces start this much later than edge pieces. */
            private const val MAX_STAGGER = 0.35f

            /** Base scatter distance in dp. */
            private const val SCATTER_DIST_DP = 200f

            /** Extra scatter distance for edge pieces in dp. */
            private const val SCATTER_EXTRA_DP = 120f

            /** Crack separation gap in dp. */
            private const val CRACK_GAP_DP = 4f

            /** Peak alpha for the white flash (0–255). */
            private const val FLASH_PEAK_ALPHA = 70
        }

        // ─── Piece model ────────────────────────────────────────────────────

        /**
         * Holds per-piece layout info and animated properties.
         * Layout values are set once in [setupPieces]; animated values mutate each frame.
         */
        private data class AnimPiece(
            // Source rectangle in the scaled bitmap
            val srcRect: Rect,
            // Original center position on the canvas
            val centerX: Float,
            val centerY: Float,
            val halfWidth: Float,
            val halfHeight: Float,
            // Animation parameters (computed once)
            val staggerDelay: Float,
            val scatterDirX: Float,
            val scatterDirY: Float,
            val scatterDist: Float,
            val rotationTarget: Float,
            // Animated state (mutated per frame)
            var offsetX: Float = 0f,
            var offsetY: Float = 0f,
            var rotation: Float = 0f,
            var scale: Float = 1f,
            var alpha: Int = 255,
        )

        // ─── Drawing state ──────────────────────────────────────────────────

        private var scaledBitmap: Bitmap? = null
        private val pieces = mutableListOf<AnimPiece>()

        private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val flashPaint = Paint().apply { color = Color.WHITE }
        private var flashAlpha = 0

        // Bitmap placement on canvas (centered, aspect-fit)
        private var bitmapOffsetX = 0f
        private var bitmapOffsetY = 0f
        private var bitmapDrawWidth = 0f
        private var bitmapDrawHeight = 0f

        private val density = resources.displayMetrics.density

        // ─── Animation control ──────────────────────────────────────────────

        private var breakAnimator: ValueAnimator? = null
        private var flashAnimator: ValueAnimator? = null
        private var hasStarted = false
        private var isBreaking = false
        private var onCompleteCallback: (() -> Unit)? = null

        // Reusable RectF to avoid allocation during onDraw
        private val tmpDstRect = RectF()

        // ─── Public API ─────────────────────────────────────────────────────

        /**
         * Kicks off the full animation sequence: display → crack → scatter → hide.
         *
         * @param bitmap     The painting to animate.
         * @param onComplete Called after the animation finishes and the view hides itself.
         */
        fun startAnimation(
            bitmap: Bitmap,
            onComplete: () -> Unit,
        ) {
            if (hasStarted) return
            hasStarted = true
            onCompleteCallback = onComplete
            visibility = VISIBLE

            // Wait until the view is laid out so we know our dimensions
            if (width == 0 || height == 0) {
                post { prepareAndStart(bitmap) }
            } else {
                prepareAndStart(bitmap)
            }
        }

        // ─── Setup ──────────────────────────────────────────────────────────

        private fun prepareAndStart(bitmap: Bitmap) {
            createScaledBitmap(bitmap)
            invalidate()

            // Show the full image for DISPLAY_DURATION_MS, then break it apart
            postDelayed({
                setupPieces()
                startBreakAnimation()
            }, DISPLAY_DURATION_MS)
        }

        /** Creates a scaled copy of [original] that fits the view while maintaining aspect ratio. */
        private fun createScaledBitmap(original: Bitmap) {
            val viewW = width.toFloat()
            val viewH = height.toFloat()
            if (viewW <= 0 || viewH <= 0) return

            val bmpW = original.width.toFloat()
            val bmpH = original.height.toFloat()
            val scale = minOf(viewW / bmpW, viewH / bmpH)

            bitmapDrawWidth = bmpW * scale
            bitmapDrawHeight = bmpH * scale
            bitmapOffsetX = (viewW - bitmapDrawWidth) / 2f
            bitmapOffsetY = (viewH - bitmapDrawHeight) / 2f

            scaledBitmap =
                Bitmap.createScaledBitmap(
                    original,
                    bitmapDrawWidth.toInt().coerceAtLeast(1),
                    bitmapDrawHeight.toInt().coerceAtLeast(1),
                    true,
                )
        }

        /**
         * Computes every piece's position, scatter direction, stagger delay, etc.
         * Called once right before the break animation starts.
         */
        private fun setupPieces() {
            pieces.clear()
            val bmp = scaledBitmap ?: return

            val pieceW = bitmapDrawWidth / GRID_COLS
            val pieceH = bitmapDrawHeight / GRID_ROWS
            val bmpPieceW = bmp.width.toFloat() / GRID_COLS
            val bmpPieceH = bmp.height.toFloat() / GRID_ROWS

            // Image center — used for radial scatter direction
            val imgCenterX = bitmapOffsetX + bitmapDrawWidth / 2f
            val imgCenterY = bitmapOffsetY + bitmapDrawHeight / 2f
            val maxDist = hypot(bitmapDrawWidth / 2f, bitmapDrawHeight / 2f)

            val random = Random(42) // Fixed seed → same pattern every time

            for (row in 0 until GRID_ROWS) {
                for (col in 0 until GRID_COLS) {
                    // Source rect in the scaled bitmap
                    val srcLeft = (col * bmpPieceW).toInt()
                    val srcTop = (row * bmpPieceH).toInt()
                    val srcRight = ((col + 1) * bmpPieceW).toInt().coerceAtMost(bmp.width)
                    val srcBottom = ((row + 1) * bmpPieceH).toInt().coerceAtMost(bmp.height)

                    // Center of this piece on the canvas
                    val cx = bitmapOffsetX + col * pieceW + pieceW / 2f
                    val cy = bitmapOffsetY + row * pieceH + pieceH / 2f

                    // Normalized distance from image center (0 = center, 1 = corner)
                    val dist = hypot(cx - imgCenterX, cy - imgCenterY)
                    val normDist = (dist / maxDist).coerceIn(0f, 1f)

                    // Scatter direction: radially outward from center
                    val angle = atan2((cy - imgCenterY).toDouble(), (cx - imgCenterX).toDouble())
                    val dirX = cos(angle).toFloat()
                    val dirY = sin(angle).toFloat()

                    // Edge pieces start first → low stagger; center pieces later → high stagger
                    val stagger = (1f - normDist) * MAX_STAGGER

                    // Edge pieces fly further
                    val baseDist = SCATTER_DIST_DP * density
                    val extraDist = SCATTER_EXTRA_DP * density * normDist
                    val totalDist = baseDist + extraDist

                    // Random rotation per piece for organic feel
                    val rotTarget = random.nextFloat() * 120f - 60f

                    pieces.add(
                        AnimPiece(
                            srcRect = Rect(srcLeft, srcTop, srcRight, srcBottom),
                            centerX = cx,
                            centerY = cy,
                            halfWidth = pieceW / 2f,
                            halfHeight = pieceH / 2f,
                            staggerDelay = stagger,
                            scatterDirX = dirX,
                            scatterDirY = dirY,
                            scatterDist = totalDist,
                            rotationTarget = rotTarget,
                        ),
                    )
                }
            }
        }

        // ─── Animation ──────────────────────────────────────────────────────

        private fun startBreakAnimation() {
            isBreaking = true
            startFlash()

            breakAnimator =
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = BREAK_DURATION_MS
                    // Linear overall — easing is applied per-piece in updatePieces()
                    interpolator = null
                    addUpdateListener { anim ->
                        updatePieces(anim.animatedValue as Float)
                        invalidate()
                    }
                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                isBreaking = false
                                visibility = GONE
                                onCompleteCallback?.invoke()
                            }
                        },
                    )
                    start()
                }
        }

        /** Quick white flash that fires at the instant pieces start cracking. */
        private fun startFlash() {
            flashAnimator =
                ValueAnimator.ofInt(0, FLASH_PEAK_ALPHA, 0).apply {
                    duration = 400
                    addUpdateListener { flashAlpha = it.animatedValue as Int }
                    start()
                }
        }

        /**
         * Per-frame update: computes every piece's offset, rotation, scale, and alpha
         * based on the global animation progress (0 → 1).
         */
        private fun updatePieces(globalProgress: Float) {
            val accelInterp = AccelerateInterpolator(1.8f)
            val gapPx = CRACK_GAP_DP * density

            for (piece in pieces) {
                // Adjust for this piece's stagger delay
                val effectiveRange = 1f - piece.staggerDelay
                val adjusted =
                    ((globalProgress - piece.staggerDelay) / effectiveRange)
                        .coerceIn(0f, 1f)

                if (adjusted <= 0f) {
                    // This piece hasn't started animating yet
                    piece.offsetX = 0f
                    piece.offsetY = 0f
                    piece.rotation = 0f
                    piece.scale = 1f
                    piece.alpha = 255
                    continue
                }

                if (adjusted < CRACK_FRACTION) {
                    // ── Crack phase: small separation, hint of rotation ──
                    val t = adjusted / CRACK_FRACTION
                    piece.offsetX = piece.scatterDirX * gapPx * t
                    piece.offsetY = piece.scatterDirY * gapPx * t
                    piece.rotation = piece.rotationTarget * 0.05f * t
                    piece.scale = 1f
                    piece.alpha = 255
                } else {
                    // ── Scatter phase: fly out, spin, shrink, fade ──
                    val scatterLinear = (adjusted - CRACK_FRACTION) / (1f - CRACK_FRACTION)
                    val eased = accelInterp.getInterpolation(scatterLinear)

                    piece.offsetX = piece.scatterDirX * (gapPx + piece.scatterDist * eased)
                    piece.offsetY = piece.scatterDirY * (gapPx + piece.scatterDist * eased)
                    piece.rotation = piece.rotationTarget * eased
                    piece.scale = 1f - 0.5f * eased
                    // Quadratic fade-out for smoother disappearance
                    piece.alpha = (255 * (1f - eased * eased)).toInt().coerceIn(0, 255)
                }
            }
        }

        // ─── Drawing ────────────────────────────────────────────────────────

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val bmp = scaledBitmap ?: return

            if (!isBreaking) {
                // Display phase: draw the full bitmap centered in the view
                bitmapPaint.alpha = 255
                canvas.drawBitmap(
                    bmp,
                    null,
                    RectF(
                        bitmapOffsetX,
                        bitmapOffsetY,
                        bitmapOffsetX + bitmapDrawWidth,
                        bitmapOffsetY + bitmapDrawHeight,
                    ),
                    bitmapPaint,
                )
                return
            }

            // Break phase: draw each piece with its individual transform
            for (piece in pieces) {
                if (piece.alpha <= 0) continue

                canvas.save()

                // Translate to piece center + animated offset
                canvas.translate(
                    piece.centerX + piece.offsetX,
                    piece.centerY + piece.offsetY,
                )
                // Rotate around the piece's own center
                canvas.rotate(piece.rotation)
                // Scale from the piece's own center
                canvas.scale(piece.scale, piece.scale)

                // Destination rect centered at the origin
                tmpDstRect.set(
                    -piece.halfWidth,
                    -piece.halfHeight,
                    piece.halfWidth,
                    piece.halfHeight,
                )

                bitmapPaint.alpha = piece.alpha
                canvas.drawBitmap(bmp, piece.srcRect, tmpDstRect, bitmapPaint)

                canvas.restore()
            }

            // White flash overlay
            if (flashAlpha > 0) {
                flashPaint.alpha = flashAlpha
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), flashPaint)
            }
        }

        // ─── Lifecycle ──────────────────────────────────────────────────────

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            breakAnimator?.cancel()
            flashAnimator?.cancel()
        }

        /** Resets internal state so the animation can be triggered again. */
        fun reset() {
            hasStarted = false
            isBreaking = false
            pieces.clear()
            flashAlpha = 0
            breakAnimator?.cancel()
            flashAnimator?.cancel()
            scaledBitmap = null
            visibility = GONE
        }
    }
