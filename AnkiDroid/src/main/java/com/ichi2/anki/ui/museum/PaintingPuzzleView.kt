package com.ichi2.anki.ui.museum

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


/**
 * Custom View that displays a painting divided into a grid of puzzle pieces.
 * Unlocked pieces reveal the actual painting; locked pieces show a uniform warm grey.
 * This is the hero element of the IKASI museum home screen.
 */
class PaintingPuzzleView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    companion object {
        const val COLS = 5
        const val ROWS = 4
        const val TOTAL_PIECES = COLS * ROWS // 20
    }

    private var painting: Bitmap? = null
    private var unlockedPieces: Set<Int> = emptySet()
    private var scaledBitmap: Bitmap? = null

    private val lockedPaint = Paint().apply {
        color = 0xFFC4B8A8.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = 0xFFB0A090.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        isAntiAlias = true
    }

    private val shadowPaint = Paint().apply {
        color = 0x30000000
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var pieceWidth = 0f
    private var pieceHeight = 0f

    fun setPainting(bitmap: Bitmap) {
        painting = bitmap
        scaledBitmap = null
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, width)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (painting != null && w > 0 && h > 0) {
            scaledBitmap = Bitmap.createScaledBitmap(painting!!, w, h, true)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        pieceWidth = w / COLS
        pieceHeight = h / ROWS

        if (scaledBitmap == null && painting != null && width > 0 && height > 0) {
            scaledBitmap = Bitmap.createScaledBitmap(painting!!, width, height, true)
        }

        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                val index = row * COLS + col
                val left = col * pieceWidth
                val top = row * pieceHeight
                val right = left + pieceWidth
                val bottom = top + pieceHeight

                if (index in unlockedPieces) {
                    canvas.save()
                    canvas.clipRect(left, top, right, bottom)
                    scaledBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
                    canvas.restore()
                } else {
                    // Subtle shadow offset
                    canvas.drawRect(left + 1.5f, top + 1.5f, right + 1.5f, bottom + 1.5f, shadowPaint)
                    // Locked fill
                    canvas.drawRect(left, top, right, bottom, lockedPaint)
                }

                // Border for every piece
                canvas.drawRect(
                    left + 0.75f, top + 0.75f,
                    right - 0.75f, bottom - 0.75f,
                    borderPaint,
                )
            }
        }
    }
}
