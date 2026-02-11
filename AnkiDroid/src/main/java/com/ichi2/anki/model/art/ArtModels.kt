package com.ichi2.anki.model.art

import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * JSON serialization models for asset catalog
 */
@Serializable
data class ArtCatalogEntry(
    val id: String,
    val filename: String,
    val title: String,
    val artist: String,
    val description: String,
)

/**
 * UI state models
 */
data class ArtProgressWithPiece(
    val progress: UserArtProgress,
    val artPiece: ArtPiece,
)

data class PuzzleRevealState(
    val artPiece: ArtPiece,
    val piecesRevealed: Int,
    val piecesTotal: Int,
    val revealedIndices: List<Int>,
    val isCompleted: Boolean,
    val imageFilePath: String? = null,
)

/**
 * Extension functions for UserArtProgress
 */
fun UserArtProgress.getRevealedIndicesList(): List<Int> =
    if (revealedPieces.isEmpty()) {
        emptyList()
    } else {
        revealedPieces.split(",").mapNotNull { it.toIntOrNull() }
    }

fun UserArtProgress.addRevealedPiece(pieceIndex: Int): UserArtProgress {
    val currentIndices = getRevealedIndicesList().toMutableList()
    if (!currentIndices.contains(pieceIndex)) {
        currentIndices.add(pieceIndex)
    }
    return copy(
        revealedPieces = currentIndices.joinToString(","),
        piecesRevealed = currentIndices.size,
        updatedAt = System.currentTimeMillis(),
    )
}

/**
 * Helper to calculate next piece index using center-outward spiral pattern
 */
object PuzzleRevealHelper {
    /**
     * Get next piece to reveal using center-outward algorithm with slight randomness
     *
     * @param revealedIndices List of already revealed piece indices
     * @param totalPieces Total number of puzzle pieces (e.g., 300)
     * @param gridCols Number of columns in grid (e.g., 20)
     * @param gridRows Number of rows in grid (e.g., 15)
     * @return Index of next piece to reveal (0-based)
     */
    fun getNextPieceIndex(
        revealedIndices: List<Int>,
        totalPieces: Int,
        gridCols: Int,
        gridRows: Int,
    ): Int {
        val centerRow = gridRows / 2
        val centerCol = gridCols / 2

        // Get all unrevealed pieces with distance from center
        val candidates =
            (0 until totalPieces)
                .filter { it !in revealedIndices }
                .map { index ->
                    val row = index / gridCols
                    val col = index % gridCols
                    val distance =
                        sqrt(
                            (
                                (row - centerRow).toDouble().pow(2.0) +
                                    (col - centerCol).toDouble().pow(2.0)
                            ),
                        )
                    index to distance
                }.sortedBy { it.second }
                .take(10) // Top 10 closest pieces

        // Pick random from closest 10 for variety
        return candidates.randomOrNull()?.first ?: 0
    }

    /**
     * Calculate completion percentage
     */
    fun getCompletionPercentage(
        piecesRevealed: Int,
        piecesTotal: Int,
    ): Int =
        if (piecesTotal > 0) {
            (piecesRevealed * 100) / piecesTotal
        } else {
            0
        }
}
