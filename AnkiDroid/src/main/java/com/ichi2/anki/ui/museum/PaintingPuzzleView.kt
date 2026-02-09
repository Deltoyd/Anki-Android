package com.ichi2.anki.ui.museum

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.random.Random

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
        const val COLS = 25
        const val ROWS = 20
        const val TOTAL_PIECES = COLS * ROWS // 500

        // Puzzle piece tab configuration
        private const val TAB_SIZE = 0.2f // Tab size relative to piece size

        // Gradient colors for locked pieces
        private val GRAY_GRADIENT_COLORS =
            intArrayOf(
                0xFFD4CFC8.toInt(), // Lightest gray
                0xFFC4B8A8.toInt(), // Medium-light
                0xFFB5A89A.toInt(), // Medium
                0xFFA6998B.toInt(), // Medium-dark
                0xFF978A7C.toInt(), // Darkest gray
            )
    }

    private var painting: Bitmap? = null
    private var unlockedPieces: Set<Int> = emptySet()
    private var scaledBitmap: Bitmap? = null

    // Animation state: maps piece index to alpha value (0-255)
    private val animatingPieces = mutableMapOf<Int, Int>()
    private var currentAnimator: ValueAnimator? = null

    // Cache for puzzle piece paths
    private val piecePathCache = mutableMapOf<Int, Path>()
    private val tabPattern = IntArray(TOTAL_PIECES) // 0=none, 1=tab out, -1=blank in

    // Peek mode state
    private var isPeekMode = false

    private val lockedPaint =
        Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    private val pieceBorderPaint =
        Paint().apply {
            color = 0xFF8B7355.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
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
        // Initialize tab pattern for consistent puzzle piece connections
        initializeTabPattern()
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Required for blur effects
    }

    /**
     * Initialize the tab pattern so pieces interlock correctly.
     * Each piece's right and bottom edges determine the pattern.
     */
    private fun initializeTabPattern() {
        val random = Random(42) // Fixed seed for consistent pattern
        for (i in 0 until TOTAL_PIECES) {
            // Randomly assign tab or blank (50/50)
            tabPattern[i] = if (random.nextBoolean()) 1 else -1
        }
    }

    fun setPainting(bitmap: Bitmap) {
        painting = bitmap
        scaledBitmap = null
        piecePathCache.clear()
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
        // Portrait aspect ratio (1:1.4) for Mona Lisa + frame
        val height = (width * 1.4f).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calculate frame dimensions (8% of width)
        frameWidth = w * 0.08f

        // Calculate puzzle area (inside frame)
        val puzzleWidth = w - (frameWidth * 2)
        val puzzleHeight = h - (frameWidth * 2)
        puzzleRect = RectF(frameWidth, frameWidth, w - frameWidth, h - frameWidth)

        if (painting != null && puzzleWidth > 0 && puzzleHeight > 0) {
            scaledBitmap =
                Bitmap.createScaledBitmap(
                    painting!!,
                    puzzleWidth.toInt(),
                    puzzleHeight.toInt(),
                    true,
                )
        }

        // Clear path cache when size changes
        piecePathCache.clear()
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

        // Draw frame first
        drawFrame(canvas)

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
                    val piecePath = getPiecePathForGrid(row, col, index, left, top)

                    // Draw piece outline
                    canvas.drawPath(piecePath, peekOutlinePaint)
                }
            }
        }
    }

    /**
     * Draws a single jigsaw puzzle piece with interlocking tabs.
     */
    private fun drawPuzzlePiece(
        canvas: Canvas,
        row: Int,
        col: Int,
        index: Int,
    ) {
        val left = puzzleRect.left + (col * pieceWidth)
        val top = puzzleRect.top + (row * pieceHeight)

        // Get or create the puzzle piece path
        val piecePath = getPiecePathForGrid(row, col, index, left, top)

        if (index in unlockedPieces) {
            // Draw unlocked piece (reveal painting)
            canvas.save()
            canvas.clipPath(piecePath)

            // Apply animation alpha if piece is animating
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
        } else {
            // Draw locked piece with gradient
            drawLockedPieceWithGradient(canvas, piecePath, row, col)
        }

        // Draw piece border for puzzle piece outline
        canvas.drawPath(piecePath, pieceBorderPaint)
    }

    /**
     * Draws a locked piece with a beautiful gradient based on position.
     */
    private fun drawLockedPieceWithGradient(
        canvas: Canvas,
        piecePath: Path,
        row: Int,
        col: Int,
    ) {
        // Calculate gradient based on piece position
        // Creates a diagonal gradient from top-left (lightest) to bottom-right (darkest)
        val normalizedX = col.toFloat() / COLS
        val normalizedY = row.toFloat() / ROWS
        val gradientPosition = (normalizedX + normalizedY) / 2f

        // Interpolate between gradient colors
        val colorIndex = (gradientPosition * (GRAY_GRADIENT_COLORS.size - 1)).toInt()
        val nextColorIndex = (colorIndex + 1).coerceAtMost(GRAY_GRADIENT_COLORS.size - 1)
        val fraction = (gradientPosition * (GRAY_GRADIENT_COLORS.size - 1)) - colorIndex

        val color1 = GRAY_GRADIENT_COLORS[colorIndex]
        val color2 = GRAY_GRADIENT_COLORS[nextColorIndex]

        // Interpolate color
        val interpolatedColor = interpolateColor(color1, color2, fraction)
        lockedPaint.color = interpolatedColor

        canvas.drawPath(piecePath, lockedPaint)
    }

    /**
     * Interpolates between two colors.
     */
    private fun interpolateColor(
        color1: Int,
        color2: Int,
        fraction: Float,
    ): Int {
        val f = fraction.coerceIn(0f, 1f)
        val invF = 1f - f

        val a1 = (color1 shr 24 and 0xFF)
        val r1 = (color1 shr 16 and 0xFF)
        val g1 = (color1 shr 8 and 0xFF)
        val b1 = (color1 and 0xFF)

        val a2 = (color2 shr 24 and 0xFF)
        val r2 = (color2 shr 16 and 0xFF)
        val g2 = (color2 shr 8 and 0xFF)
        val b2 = (color2 and 0xFF)

        val a = (a1 * invF + a2 * f).toInt()
        val r = (r1 * invF + r2 * f).toInt()
        val g = (g1 * invF + g2 * f).toInt()
        val b = (b1 * invF + b2 * f).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Creates a jigsaw puzzle piece path with tabs and blanks.
     * Caches paths for performance.
     */
    private fun getPiecePathForGrid(
        row: Int,
        col: Int,
        index: Int,
        left: Float,
        top: Float,
    ): Path {
        // Check cache first
        piecePathCache[index]?.let { return it }

        val path = Path()
        val right = left + pieceWidth
        val bottom = top + pieceHeight

        // Tab size
        val tabWidth = pieceWidth * TAB_SIZE
        val tabHeight = pieceHeight * TAB_SIZE

        // Start at top-left corner
        path.moveTo(left, top)

        // Top edge (check if piece above has a tab pointing down)
        if (row > 0) {
            val aboveIndex = (row - 1) * COLS + col
            val hasTab = tabPattern[aboveIndex] == 1
            drawEdge(path, left, top, right, top, tabWidth, hasTab, isHorizontal = true)
        } else {
            path.lineTo(right, top)
        }

        // Right edge (check if this piece has a tab pointing right)
        if (col < COLS - 1) {
            val hasTab = tabPattern[index] == 1
            drawEdge(path, right, top, right, bottom, tabHeight, hasTab, isHorizontal = false)
        } else {
            path.lineTo(right, bottom)
        }

        // Bottom edge (check if this piece has a tab pointing down - inverse for piece below)
        if (row < ROWS - 1) {
            val hasTab = tabPattern[index] == -1 // Inverse: if we have blank, neighbor has tab
            drawEdge(path, right, bottom, left, bottom, tabWidth, hasTab, isHorizontal = true, reverse = true)
        } else {
            path.lineTo(left, bottom)
        }

        // Left edge (check if piece to left has a tab pointing right)
        if (col > 0) {
            val leftIndex = row * COLS + (col - 1)
            val hasTab = tabPattern[leftIndex] == 1
            drawEdge(path, left, bottom, left, top, tabHeight, hasTab, isHorizontal = false, reverse = true)
        } else {
            path.lineTo(left, top)
        }

        path.close()
        piecePathCache[index] = path
        return path
    }

    /**
     * Draws an edge with or without a tab/blank.
     */
    private fun drawEdge(
        path: Path,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        tabSize: Float,
        hasTab: Boolean,
        isHorizontal: Boolean,
        reverse: Boolean = false,
    ) {
        if (!hasTab) {
            // No tab, just straight line
            path.lineTo(endX, endY)
            return
        }

        val direction = if (reverse) -1 else 1

        if (isHorizontal) {
            // Horizontal edge with tab
            val midX = (startX + endX) / 2
            val tabY = startY + (tabSize * direction)

            // Draw to start of tab
            path.lineTo(midX - tabSize, startY)
            // Curve into tab
            path.cubicTo(
                midX - tabSize * 0.5f,
                startY,
                midX - tabSize * 0.5f,
                tabY,
                midX,
                tabY,
            )
            // Curve out of tab
            path.cubicTo(
                midX + tabSize * 0.5f,
                tabY,
                midX + tabSize * 0.5f,
                startY,
                midX + tabSize,
                startY,
            )
            // Continue to end
            path.lineTo(endX, endY)
        } else {
            // Vertical edge with tab
            val midY = (startY + endY) / 2
            val tabX = startX + (tabSize * direction)

            // Draw to start of tab
            path.lineTo(startX, midY - tabSize)
            // Curve into tab
            path.cubicTo(
                startX,
                midY - tabSize * 0.5f,
                tabX,
                midY - tabSize * 0.5f,
                tabX,
                midY,
            )
            // Curve out of tab
            path.cubicTo(
                tabX,
                midY + tabSize * 0.5f,
                startX,
                midY + tabSize * 0.5f,
                startX,
                midY + tabSize,
            )
            // Continue to end
            path.lineTo(endX, endY)
        }
    }
}
