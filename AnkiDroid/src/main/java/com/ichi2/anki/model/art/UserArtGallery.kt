package com.ichi2.anki.model.art

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Data class for permanent record of completed puzzles
 * Stores user's art gallery achievements
 */
@Serializable
data class UserArtGallery(
    val id: String = UUID.randomUUID().toString(),
    val artPieceId: String,
    val completedAt: Long = System.currentTimeMillis(),
    val totalCardsReviewed: Int,
    val completionTimeMinutes: Int?,
)
