package com.ichi2.anki.ui.museum

import android.graphics.Path
import android.graphics.RectF

/**
 * Generates jigsaw puzzle piece clip paths for a 10x10 grid.
 *
 * Each piece has four edges (top, right, bottom, left). Border edges are flat;
 * internal edges have either a tab (outward bump) or a hole (inward indent).
 * Adjacent pieces interlock: a tab on one piece meets a hole on its neighbor.
 *
 * Tab/hole assignment uses checkerboard alternation via (row+col) % 2,
 * consistent with the PNG variant system in [PaintingPuzzleView].
 *
 * Paths are cached by (row, col) and invalidated when the puzzle size changes.
 */
class PuzzlePiecePathGenerator {
    private val cache = mutableMapOf<Long, Path>()

    fun invalidateCache() {
        cache.clear()
    }

    /**
     * Returns a [Path] describing the jigsaw outline for the piece at [row], [col].
     *
     * @param row        Grid row (0-based)
     * @param col        Grid column (0-based)
     * @param left       X coordinate of the piece's body top-left corner
     * @param top        Y coordinate of the piece's body top-left corner
     * @param pieceWidth  Width of one grid cell
     * @param pieceHeight Height of one grid cell
     */
    fun getPiecePath(
        row: Int,
        col: Int,
        left: Float,
        top: Float,
        pieceWidth: Float,
        pieceHeight: Float,
    ): Path {
        val key = cacheKey(row, col, left, top, pieceWidth, pieceHeight)
        cache[key]?.let { return it }

        val path = Path()
        val right = left + pieceWidth
        val bottom = top + pieceHeight

        // Tab size as fraction of piece dimension
        val tabW = pieceWidth * TAB_FRACTION
        val tabH = pieceHeight * TAB_FRACTION

        // Determine edge types
        val topEdge = if (row == 0) EdgeType.FLAT else edgeType(row, col, Edge.TOP)
        val rightEdge = if (col == COLS - 1) EdgeType.FLAT else edgeType(row, col, Edge.RIGHT)
        val bottomEdge = if (row == ROWS - 1) EdgeType.FLAT else edgeType(row, col, Edge.BOTTOM)
        val leftEdge = if (col == 0) EdgeType.FLAT else edgeType(row, col, Edge.LEFT)

        // Start at top-left corner
        path.moveTo(left, top)

        // Top edge (left to right)
        drawHorizontalEdge(path, left, right, top, tabH, topEdge, isTop = true)

        // Right edge (top to bottom)
        drawVerticalEdge(path, top, bottom, right, tabW, rightEdge, isRight = true)

        // Bottom edge (right to left)
        drawHorizontalEdge(path, right, left, bottom, tabH, bottomEdge, isTop = false)

        // Left edge (bottom to top)
        drawVerticalEdge(path, bottom, top, left, tabW, leftEdge, isRight = false)

        path.close()
        cache[key] = path
        return path
    }

    /**
     * Draws a horizontal edge (top or bottom) with optional tab/hole.
     * For top edges: tab goes up (negative Y), hole goes down (positive Y).
     * For bottom edges: tab goes down (positive Y), hole goes up (negative Y).
     */
    private fun drawHorizontalEdge(
        path: Path,
        startX: Float,
        endX: Float,
        y: Float,
        tabSize: Float,
        edgeType: EdgeType,
        isTop: Boolean,
    ) {
        if (edgeType == EdgeType.FLAT) {
            path.lineTo(endX, y)
            return
        }

        val dir =
            if (isTop) {
                if (edgeType == EdgeType.TAB) -1f else 1f
            } else {
                if (edgeType == EdgeType.TAB) 1f else -1f
            }

        val midX = (startX + endX) / 2f
        val segLen = kotlin.math.abs(endX - startX)
        val sign = if (endX > startX) 1f else -1f
        val notchStart = midX - sign * segLen * NOTCH_WIDTH_FRACTION / 2f
        val notchEnd = midX + sign * segLen * NOTCH_WIDTH_FRACTION / 2f

        path.lineTo(notchStart, y)

        // Cubic bezier for smooth tab/hole shape
        val bump = tabSize * dir
        val cpOffset = segLen * NOTCH_WIDTH_FRACTION * 0.3f * sign
        path.cubicTo(
            notchStart + cpOffset,
            y,
            notchStart + cpOffset,
            y + bump,
            midX,
            y + bump,
        )
        path.cubicTo(
            notchEnd - cpOffset,
            y + bump,
            notchEnd - cpOffset,
            y,
            notchEnd,
            y,
        )

        path.lineTo(endX, y)
    }

    /**
     * Draws a vertical edge (left or right) with optional tab/hole.
     * For right edges: tab goes right (positive X), hole goes left (negative X).
     * For left edges: tab goes left (negative X), hole goes right (positive X).
     */
    private fun drawVerticalEdge(
        path: Path,
        startY: Float,
        endY: Float,
        x: Float,
        tabSize: Float,
        edgeType: EdgeType,
        isRight: Boolean,
    ) {
        if (edgeType == EdgeType.FLAT) {
            path.lineTo(x, endY)
            return
        }

        val dir =
            if (isRight) {
                if (edgeType == EdgeType.TAB) 1f else -1f
            } else {
                if (edgeType == EdgeType.TAB) -1f else 1f
            }

        val midY = (startY + endY) / 2f
        val segLen = kotlin.math.abs(endY - startY)
        val sign = if (endY > startY) 1f else -1f
        val notchStart = midY - sign * segLen * NOTCH_WIDTH_FRACTION / 2f
        val notchEnd = midY + sign * segLen * NOTCH_WIDTH_FRACTION / 2f

        path.lineTo(x, notchStart)

        val bump = tabSize * dir
        val cpOffset = segLen * NOTCH_WIDTH_FRACTION * 0.3f * sign
        path.cubicTo(
            x,
            notchStart + cpOffset,
            x + bump,
            notchStart + cpOffset,
            x + bump,
            midY,
        )
        path.cubicTo(
            x + bump,
            notchEnd - cpOffset,
            x,
            notchEnd - cpOffset,
            x,
            notchEnd,
        )

        path.lineTo(x, endY)
    }

    /**
     * Determines whether a given edge of the piece at (row, col) is a tab or hole.
     * Uses checkerboard pattern: even (row+col) pieces have tabs on right/bottom,
     * holes on left/top; odd pieces are the inverse.
     */
    private fun edgeType(
        row: Int,
        col: Int,
        edge: Edge,
    ): EdgeType {
        val even = (row + col) % 2 == 0
        return when (edge) {
            Edge.TOP -> if (even) EdgeType.HOLE else EdgeType.TAB
            Edge.RIGHT -> if (even) EdgeType.TAB else EdgeType.HOLE
            Edge.BOTTOM -> if (even) EdgeType.TAB else EdgeType.HOLE
            Edge.LEFT -> if (even) EdgeType.HOLE else EdgeType.TAB
        }
    }

    private fun cacheKey(
        row: Int,
        col: Int,
        left: Float,
        top: Float,
        pieceWidth: Float,
        pieceHeight: Float,
    ): Long {
        // Combine row/col into a single long for fast lookup
        // Includes size info so cache invalidates on resize
        val posKey =
            (row.toLong() shl 48) or (col.toLong() shl 32) or
                (pieceWidth.toBits().toLong() and 0xFFFF0000L) or
                (pieceHeight.toBits().toLong() shr 16 and 0xFFFFL)
        return posKey
    }

    private enum class EdgeType { FLAT, TAB, HOLE }

    private enum class Edge { TOP, RIGHT, BOTTOM, LEFT }

    companion object {
        private const val COLS = 10
        private const val ROWS = 10
        private const val TAB_FRACTION = 0.22f
        private const val NOTCH_WIDTH_FRACTION = 0.4f
    }
}
