package com.ichi2.anki.ui.museum

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.ichi2.anki.R

/**
 * Custom View that displays a painting as a jigsaw puzzle with interlocking pieces.
 * Unlocked pieces reveal the actual painting; locked pieces show a beautiful gradient of grays.
 * Features a decorative museum-style frame around the artwork.
 * Supports "peek mode" to temporarily reveal the full painting.
 */
class PaintingPuzzleView(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    companion object {
        const val COLS = 10
        const val ROWS = 10
        const val TOTAL_PIECES = COLS * ROWS // 100

        // How far tabs extend beyond the cell, as a fraction of piece size
        private const val TAB_OVERFLOW = 0.25f
    }

    private var painting: Bitmap? = null
    private var unlockedPieces: Set<Int> = emptySet()
    private var scaledBitmap: Bitmap? = null

    // Animation state: maps piece index to alpha value (0-255)
    private val animatingPieces = mutableMapOf<Int, Int>()
    private var currentAnimator: ValueAnimator? = null

    // Shared path generator for consistent jigsaw shapes across views
    private val pathGenerator = PuzzlePiecePathGenerator()

    // Custom PNG bitmaps for locked piece types
    private val pieceBitmaps by lazy {
        mapOf(
            "corner_tl" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_corner_tl),
            "corner_tr" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_corner_tr),
            "corner_bl" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_corner_bl),
            "corner_br" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_corner_br),
            "border_top_1" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_top_1),
            "border_top_2" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_top_2),
            "border_bottom_1" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_bottom_1),
            "border_bottom_2" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_bottom_2),
            "border_left_1" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_left_1),
            "border_left_2" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_left_2),
            "border_right_1" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_right_1),
            "border_right_2" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_right_2),
            "middle_1" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_middle_1),
            "middle_2" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_middle_2),
        )
    }

    // Reusable rect for drawing piece PNGs
    private val tmpPieceRect = RectF()

    // Paint for drawing piece bitmaps with filtering
    private val pieceBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Peek mode state
    private var isPeekMode = false

    private val pieceBorderPaint =
        Paint().apply {
            color = 0xFF757575.toInt() // museolingo_puzzle_border
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

    private val shadowPaint =
        Paint().apply {
            color = 0x20000000
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    // Peek mode overlay paint (semi-transparent white with blur)
    private val peekOverlayPaint =
        Paint().apply {
            color = 0x30FFFFFF
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    // Peek mode outline paint
    private val peekOutlinePaint =
        Paint().apply {
            color = 0x80000000.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

    // Frame paints
    private val frameOuterPaint =
        Paint().apply {
            color = 0xFF8B6914.toInt() // Dark gold
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    private val frameInnerPaint =
        Paint().apply {
            color = 0xFFD4A017.toInt() // Bright gold
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    private val frameShadowPaint =
        Paint().apply {
            color = 0xFF5C4A0F.toInt() // Very dark gold
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    private var pieceWidth = 0f
    private var pieceHeight = 0f
    private var frameWidth = 0f
    private var puzzleRect = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Required for blur effects
    }

    fun setPainting(bitmap: Bitmap) {
        painting = bitmap
        scaledBitmap = null
        pathGenerator.invalidateCache()
        requestLayout()
        invalidate()
    }

    fun setUnlockedPieces(pieces: Set<Int>) {
        unlockedPieces = pieces
        invalidate()
    }

    fun unlockPiece(index: Int) {
        unlockedPieces = unlockedPieces + index
        invalidate()
    }

    /**
     * Enable peek mode to temporarily show the full painting.
     */
    fun enablePeekMode() {
        isPeekMode = true
        invalidate()
    }

    /**
     * Disable peek mode to return to normal puzzle view.
     */
    fun disablePeekMode() {
        isPeekMode = false
        invalidate()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val bmp = painting
        val height =
            if (bmp != null && bmp.width > 0) {
                (width * bmp.height.toFloat() / bmp.width).toInt()
            } else {
                (width * 2f / 3f).toInt() // fallback 3:2 until bitmap loads
            }
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)

        // No frame in new design - puzzle fills entire view
        frameWidth = 0f
        puzzleRect = RectF(0f, 0f, w.toFloat(), h.toFloat())

        val puzzleWidth = w
        val puzzleHeight = h

        if (painting != null && puzzleWidth > 0 && puzzleHeight > 0) {
            scaledBitmap =
                Bitmap.createScaledBitmap(
                    painting!!,
                    puzzleWidth,
                    puzzleHeight,
                    true,
                )
        }

        // Clear path cache when size changes
        pathGenerator.invalidateCache()
    }

    /**
     * Animates the unlocking of a puzzle piece with a smooth fade-in effect.
     */
    fun animateUnlock(index: Int) {
        currentAnimator?.cancel()
        unlockedPieces = unlockedPieces + index

        currentAnimator =
            ValueAnimator.ofInt(0, 255).apply {
                duration = 400
                interpolator = DecelerateInterpolator()

                addUpdateListener { animator ->
                    val alpha = animator.animatedValue as Int
                    animatingPieces[index] = alpha
                    invalidate()
                }

                addListener(
                    object : android.animation.Animator.AnimatorListener {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            animatingPieces.remove(index)
                            invalidate()
                        }

                        override fun onAnimationCancel(animation: android.animation.Animator) {
                            animatingPieces.remove(index)
                        }

                        override fun onAnimationStart(animation: android.animation.Animator) {}

                        override fun onAnimationRepeat(animation: android.animation.Animator) {}
                    },
                )

                start()
            }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // No frame in new design

        // Calculate piece dimensions
        val puzzleWidth = puzzleRect.width()
        val puzzleHeight = puzzleRect.height()
        pieceWidth = puzzleWidth / COLS
        pieceHeight = puzzleHeight / ROWS

        // Create scaled bitmap if needed
        if (scaledBitmap == null && painting != null && puzzleWidth > 0 && puzzleHeight > 0) {
            scaledBitmap =
                Bitmap.createScaledBitmap(
                    painting!!,
                    puzzleWidth.toInt(),
                    puzzleHeight.toInt(),
                    true,
                )
        }

        if (isPeekMode) {
            // In peek mode, show full painting with overlay and outlines
            drawPeekMode(canvas)
        } else {
            // Normal mode: draw puzzle pieces
            drawPuzzlePieces(canvas)
        }
    }

    /**
     * Draws the decorative museum-style frame around the puzzle.
     */
    private fun drawFrame(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Outer frame (darkest)
        canvas.drawRect(0f, 0f, w, h, frameOuterPaint)

        // Inner bevel (lighter gold)
        val bevelWidth = frameWidth * 0.3f
        canvas.drawRect(
            bevelWidth,
            bevelWidth,
            w - bevelWidth,
            h - bevelWidth,
            frameInnerPaint,
        )

        // Inner shadow (creates depth)
        val shadowWidth = frameWidth * 0.15f
        val shadowRect =
            RectF(
                frameWidth - shadowWidth,
                frameWidth - shadowWidth,
                w - frameWidth + shadowWidth,
                h - frameWidth + shadowWidth,
            )
        canvas.drawRect(shadowRect, frameShadowPaint)
    }

    /**
     * Draws puzzle pieces in normal mode.
     */
    private fun drawPuzzlePieces(canvas: Canvas) {
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                val index = row * COLS + col
                drawPuzzlePiece(canvas, row, col, index)
            }
        }
    }

    /**
     * Draws the full painting in peek mode with overlay and outlines.
     */
    private fun drawPeekMode(canvas: Canvas) {
        scaledBitmap?.let { bitmap ->
            // Draw full painting
            canvas.drawBitmap(bitmap, puzzleRect.left, puzzleRect.top, null)

            // Draw semi-transparent overlay
            canvas.drawRect(puzzleRect, peekOverlayPaint)

            // Draw puzzle piece outlines
            for (row in 0 until ROWS) {
                for (col in 0 until COLS) {
                    val index = row * COLS + col
                    val left = puzzleRect.left + (col * pieceWidth)
                    val top = puzzleRect.top + (row * pieceHeight)
                    val piecePath = pathGenerator.getPiecePath(row, col, left, top, pieceWidth, pieceHeight)

                    // Draw piece outline
                    canvas.drawPath(piecePath, peekOutlinePaint)
                }
            }
        }
    }

    /**
     * Draws a single puzzle piece. Locked pieces use custom PNG images;
     * unlocked pieces clip to the computed jigsaw path and reveal the painting.
     */
    private fun drawPuzzlePiece(
        canvas: Canvas,
        row: Int,
        col: Int,
        index: Int,
    ) {
        val left = puzzleRect.left + (col * pieceWidth)
        val top = puzzleRect.top + (row * pieceHeight)

        if (index in unlockedPieces) {
            // Unlocked: clip to jigsaw path and draw painting
            val piecePath = pathGenerator.getPiecePath(row, col, left, top, pieceWidth, pieceHeight)
            canvas.save()
            canvas.clipPath(piecePath)

            val paint =
                if (index in animatingPieces) {
                    Paint().apply {
                        alpha = animatingPieces[index] ?: 255
                    }
                } else {
                    null
                }

            scaledBitmap?.let {
                canvas.drawBitmap(it, puzzleRect.left, puzzleRect.top, paint)
            }
            canvas.restore()

            // Draw border for unlocked pieces
            canvas.drawPath(piecePath, pieceBorderPaint)
        } else {
            // Locked: draw the custom PNG piece image
            drawLockedPiecePng(canvas, row, col, left, top)
        }
    }

    /**
     * Draws a locked piece using the appropriate custom PNG image.
     * The dest rect accounts for tab protrusions extending beyond the cell.
     */
    private fun drawLockedPiecePng(
        canvas: Canvas,
        row: Int,
        col: Int,
        left: Float,
        top: Float,
    ) {
        val pieceType = getPieceType(row, col)
        val bmp = pieceBitmaps[pieceType] ?: return

        val tabW = pieceWidth * TAB_OVERFLOW
        val tabH = pieceHeight * TAB_OVERFLOW

        tmpPieceRect.set(
            left - if (col > 0) tabW else 0f,
            top - if (row > 0) tabH else 0f,
            left + pieceWidth + if (col < COLS - 1) tabW else 0f,
            top + pieceHeight + if (row < ROWS - 1) tabH else 0f,
        )

        canvas.drawBitmap(bmp, null, tmpPieceRect, pieceBitmapPaint)
    }

    /**
     * Returns the piece type key for the given grid position.
     */
    private fun getPieceType(
        row: Int,
        col: Int,
    ): String =
        when {
            row == 0 && col == 0 -> "corner_tl"
            row == 0 && col == COLS - 1 -> "corner_tr"
            row == ROWS - 1 && col == 0 -> "corner_bl"
            row == ROWS - 1 && col == COLS - 1 -> "corner_br"
            row == 0 -> {
                val variant = if ((row + col) % 2 == 0) 1 else 2
                "border_top_$variant"
            }
            row == ROWS - 1 -> {
                val variant = if ((row + col) % 2 == 0) 1 else 2
                "border_bottom_$variant"
            }
            col == 0 -> {
                val variant = if ((row + col) % 2 == 0) 1 else 2
                "border_left_$variant"
            }
            col == COLS - 1 -> {
                val variant = if ((row + col) % 2 == 0) 1 else 2
                "border_right_$variant"
            }
            else -> {
                val variant = if ((row + col) % 2 == 0) 1 else 2
                "middle_$variant"
            }
        }
}
