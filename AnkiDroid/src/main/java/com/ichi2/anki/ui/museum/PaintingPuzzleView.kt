package com.ichi2.anki.ui.museum

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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

        // The body size in every puzzle piece PNG (the square area excluding tabs).
        // All 14 PNGs share this same body dimension; tabs extend beyond it.
        private const val PNG_BODY_SIZE = 77f

        /**
         * Body offset (bodyX, bodyY) within each piece PNG.
         * The body is the 77x77 square cell area; tabs extend beyond it.
         * bodyX/bodyY indicate where the top-left of the body sits in PNG pixel coords.
         */
        private val PIECE_BODY_OFFSETS: Map<String, Pair<Float, Float>> =
            mapOf(
                // Corners: one tab each
                "corner_tl" to Pair(0f, 0f), // 77x100, tab extends down
                "corner_tr" to Pair(22f, 0f), // 99x77, tab extends left
                "corner_bl" to Pair(0f, 0f), // 99x77, tab extends right
                "corner_br" to Pair(0f, 22f), // 77x99, tab extends up
                // Top border: variant 1 has horizontal tabs, variant 2 has vertical tab
                "border_top_1" to Pair(22.5f, 0f), // 122x77, tabs left+right
                "border_top_2" to Pair(0f, 0f), // 77x99, tab extends down
                // Bottom border
                "border_bottom_1" to Pair(22.5f, 0f), // 122x77, tabs left+right
                "border_bottom_2" to Pair(0f, 22f), // 77x99, tab extends up
                // Left border
                "border_left_1" to Pair(0f, 22.5f), // 77x122, tabs up+down
                "border_left_2" to Pair(0f, 0f), // 99x77, tab extends right
                // Right border
                "border_right_1" to Pair(0f, 22.5f), // 77x122, tabs up+down
                "border_right_2" to Pair(22f, 0f), // 99x77, tab extends left
                // Middle pieces
                "middle_1" to Pair(22.5f, 0f), // 122x77, tabs left+right
                "middle_2" to Pair(0f, 22.5f), // 77x122, tabs up+down
            )
    }

    private var painting: Bitmap? = null
    private var unlockedPieces: Set<Int> = emptySet()
    private var scaledBitmap: Bitmap? = null

    // Animation state: maps piece index to alpha value (0-255)
    private val animatingPieces = mutableMapOf<Int, Int>()
    private var currentAnimator: ValueAnimator? = null

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

    // Paint for alpha masking (PorterDuff DST_IN compositing)
    private val alphaMaskPaint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

    // Paint for peek mode piece outlines
    private val peekPiecePaint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = 40
        }

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
     * Draws the full painting in peek mode with overlay and faint piece outlines.
     */
    private fun drawPeekMode(canvas: Canvas) {
        scaledBitmap?.let { bitmap ->
            // Draw full painting
            canvas.drawBitmap(bitmap, puzzleRect.left, puzzleRect.top, null)

            // Draw semi-transparent overlay
            canvas.drawRect(puzzleRect, peekOverlayPaint)

            // Draw faint puzzle piece PNG outlines
            for (row in 0 until ROWS) {
                for (col in 0 until COLS) {
                    val left = puzzleRect.left + (col * pieceWidth)
                    val top = puzzleRect.top + (row * pieceHeight)

                    // Draw piece PNG at low alpha to show jigsaw outlines
                    val pieceType = getPieceType(row, col)
                    val bmp = pieceBitmaps[pieceType] ?: continue

                    val bmpWidth = bmp.width.toFloat()
                    val bmpHeight = bmp.height.toFloat()
                    val scaleX = pieceWidth / PNG_BODY_SIZE
                    val scaleY = pieceHeight / PNG_BODY_SIZE
                    val (bodyX, bodyY) = PIECE_BODY_OFFSETS[pieceType] ?: Pair(0f, 0f)
                    val destLeft = left - bodyX * scaleX
                    val destTop = top - bodyY * scaleY
                    val destRight = destLeft + bmpWidth * scaleX
                    val destBottom = destTop + bmpHeight * scaleY

                    tmpPieceRect.set(destLeft, destTop, destRight, destBottom)
                    canvas.drawBitmap(bmp, null, tmpPieceRect, peekPiecePaint)
                }
            }
        }
    }

    /**
     * Draws a single puzzle piece. Locked pieces use custom PNG images;
     * unlocked pieces use PNG alpha masks to clip the painting to the same jigsaw shape.
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
            // Unlocked: use PNG alpha mask to clip painting to jigsaw shape
            val pieceType = getPieceType(row, col)
            val maskBmp = pieceBitmaps[pieceType] ?: return

            // Compute the same destination rect as drawLockedPiecePng
            val bmpWidth = maskBmp.width.toFloat()
            val bmpHeight = maskBmp.height.toFloat()
            val scaleX = pieceWidth / PNG_BODY_SIZE
            val scaleY = pieceHeight / PNG_BODY_SIZE
            val (bodyX, bodyY) = PIECE_BODY_OFFSETS[pieceType] ?: Pair(0f, 0f)
            val destLeft = left - bodyX * scaleX
            val destTop = top - bodyY * scaleY
            val destRight = destLeft + bmpWidth * scaleX
            val destBottom = destTop + bmpHeight * scaleY

            // Use saveLayer to create an offscreen buffer for compositing
            val layerRect = RectF(destLeft, destTop, destRight, destBottom)
            canvas.saveLayer(layerRect, null)

            // Draw painting (DST) — only the area under the piece
            val paint =
                if (index in animatingPieces) {
                    Paint().apply { alpha = animatingPieces[index] ?: 255 }
                } else {
                    null
                }
            scaledBitmap?.let {
                canvas.drawBitmap(it, puzzleRect.left, puzzleRect.top, paint)
            }

            // Apply PNG alpha mask (SRC) — DST_IN keeps only painting pixels where mask is opaque
            tmpPieceRect.set(destLeft, destTop, destRight, destBottom)
            canvas.drawBitmap(maskBmp, null, tmpPieceRect, alphaMaskPaint)

            canvas.restore()
        } else {
            // Locked: draw the custom PNG piece image
            drawLockedPiecePng(canvas, row, col, left, top)
        }
    }

    /**
     * Draws a locked piece using the appropriate custom PNG image.
     *
     * Each PNG contains a 77x77 "body" area (the main cell square) plus tab
     * extensions that protrude beyond it. To render correctly:
     * - The body must map exactly onto the grid cell (pieceWidth x pieceHeight)
     * - Tabs must extend beyond the cell into neighboring cells
     * - Adjacent pieces' tabs fill each other's holes, creating seamless interlocking
     *
     * We use per-piece body-offset data ([PIECE_BODY_OFFSETS]) to know where the
     * body sits within each PNG, then scale so the body fills the cell exactly.
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

        val bmpWidth = bmp.width.toFloat()
        val bmpHeight = bmp.height.toFloat()

        // Scale factors: map the 77x77 body in the PNG to the cell dimensions.
        // These may differ (non-uniform) when the cell is not square.
        val scaleX = pieceWidth / PNG_BODY_SIZE
        val scaleY = pieceHeight / PNG_BODY_SIZE

        // Where the body's top-left sits within the PNG (in PNG pixels)
        val (bodyX, bodyY) = PIECE_BODY_OFFSETS[pieceType] ?: Pair(0f, 0f)

        // Position the entire bitmap so the body aligns with the grid cell.
        // The bitmap's top-left in view coords = cell top-left minus the
        // scaled body offset (so tabs before the body extend to the left/above).
        val destLeft = left - bodyX * scaleX
        val destTop = top - bodyY * scaleY
        val destRight = destLeft + bmpWidth * scaleX
        val destBottom = destTop + bmpHeight * scaleY

        tmpPieceRect.set(destLeft, destTop, destRight, destBottom)
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
                val variant = if ((row + col) % 2 == 0) 2 else 1
                "border_top_$variant"
            }
            row == ROWS - 1 -> {
                val variant = if ((row + col) % 2 == 0) 2 else 1
                "border_bottom_$variant"
            }
            col == 0 -> {
                // Left border: variant 1 has vertical tabs (up+down), variant 2 has horizontal tab (right)
                // Swapped relative to top/bottom borders so tabs meet holes at corners and interiors
                val variant = if ((row + col) % 2 == 0) 1 else 2
                "border_left_$variant"
            }
            col == COLS - 1 -> {
                // Right border: same swap as left border for proper interlocking
                val variant = if ((row + col) % 2 == 0) 1 else 2
                "border_right_$variant"
            }
            else -> {
                val variant = if ((row + col) % 2 == 0) 2 else 1
                "middle_$variant"
            }
        }
}
