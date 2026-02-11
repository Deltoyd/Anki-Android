package com.ichi2.anki.model.art

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Data class tracking user's current puzzle progress
 * One active progress record per user
 */
@Serializable
data class UserArtProgress(
    val id: String = UUID.randomUUID().toString(),
    val artPieceId: String,
    val deckId: Long? = null, // Link to Anki deck (optional)
    val piecesRevealed: Int = 0,
    val piecesTotal: Int = 100,
    val isCompleted: Boolean = false,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val revealedPieces: String = "", // Comma-separated list of piece indices
    val updatedAt: Long = System.currentTimeMillis(),
)
