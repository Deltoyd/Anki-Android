package com.ichi2.anki.ui.art

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.ichi2.anki.model.art.PuzzleRevealState
import timber.log.Timber

/**
 * Custom view for rendering progressive puzzle reveal
 * Displays 300-piece grid with revealed pieces from art bitmap
 */
class PuzzleCanvasView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        private var state: PuzzleRevealState? = null
        private var artBitmap: Bitmap? = null

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val grayPaint =
            Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.FILL
            }

        private val borderPaint =
            Paint().apply {
                color = Color.DKGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

        private var lastRevealedPiece: Int? = null
        private var revealAnimator: ValueAnimator? = null

        /**
         * Update puzzle state and trigger redraw
         */
        fun updateState(newState: PuzzleRevealState) {
            val previousRevealed = state?.piecesRevealed ?: 0
            state = newState

            // Load bitmap from assets if not loaded
            if (artBitmap == null && newState.imageFilePath != null) {
                loadBitmapFromAsset(newState.imageFilePath)
            }

            // Detect newly revealed piece for animation
            if (newState.piecesRevealed > previousRevealed) {
                val newIndices = newState.revealedIndices.toSet()
                val oldIndices = state?.revealedIndices?.toSet() ?: emptySet()
                val newlyRevealed = (newIndices - oldIndices).firstOrNull()

                newlyRevealed?.let {
                    lastRevealedPiece = it
                    animateReveal()
                }
            }

            invalidate()
        }

        /**
         * Load bitmap from assets folder
         */
        private fun loadBitmapFromAsset(filename: String) {
            try {
                val options =
                    BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                        inSampleSize = 1 // Can adjust for lower memory usage
                    }
                context.assets.open("art/$filename").use { inputStream ->
                    artBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load art bitmap from assets: $filename")
            }
        }

        /**
         * Animate newly revealed piece with fade-in effect
         */
        private fun animateReveal() {
            revealAnimator?.cancel()
            revealAnimator =
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 500
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { invalidate() }
                    start()
                }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val currentState = state ?: return
            val bitmap = artBitmap ?: return

            val rows = currentState.artPiece.puzzleGridRows
            val cols = currentState.artPiece.puzzleGridCols
            val totalPieces = currentState.piecesTotal

            val pieceWidth = width / cols.toFloat()
            val pieceHeight = height / rows.toFloat()

            val srcPieceWidth = bitmap.width / cols.toFloat()
            val srcPieceHeight = bitmap.height / rows.toFloat()

            // Draw each puzzle piece
            for (i in 0 until totalPieces) {
                val row = i / cols
                val col = i % cols

                val destRect =
                    RectF(
                        col * pieceWidth,
                        row * pieceHeight,
                        (col + 1) * pieceWidth,
                        (row + 1) * pieceHeight,
                    )

                if (i in currentState.revealedIndices) {
                    // Draw revealed piece from bitmap
                    val srcRect =
                        Rect(
                            (col * srcPieceWidth).toInt(),
                            (row * srcPieceHeight).toInt(),
                            ((col + 1) * srcPieceWidth).toInt(),
                            ((row + 1) * srcPieceHeight).toInt(),
                        )

                    // Apply animation alpha if this is newly revealed
                    if (i == lastRevealedPiece && revealAnimator?.isRunning == true) {
                        paint.alpha = (255 * (revealAnimator?.animatedValue as Float)).toInt()
                    } else {
                        paint.alpha = 255
                    }

                    canvas.drawBitmap(bitmap, srcRect, destRect, paint)
                } else {
                    // Draw unrevealed piece as gray rectangle
                    canvas.drawRect(destRect, grayPaint)
                }

                // Draw border around each piece
                canvas.drawRect(destRect, borderPaint)
            }

            // Draw progress text
            val progressText = "${currentState.piecesRevealed} / ${currentState.piecesTotal}"
            val textPaint =
                Paint().apply {
                    color = Color.WHITE
                    textSize = 32f
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(4f, 0f, 0f, Color.BLACK)
                }
            canvas.drawText(progressText, width / 2f, 50f, textPaint)
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
            val desiredHeight = (desiredWidth * 0.75).toInt() // 4:3 aspect ratio

            setMeasuredDimension(desiredWidth, desiredHeight)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            revealAnimator?.cancel()
            artBitmap?.recycle()
            artBitmap = null
        }
    }
