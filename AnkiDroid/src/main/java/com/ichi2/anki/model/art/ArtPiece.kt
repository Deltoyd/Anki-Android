package com.ichi2.anki.model.art

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Data class representing an art piece from museum collections
 * Used for the progressive puzzle reveal system
 */
@Serializable
data class ArtPiece(
    val id: String = UUID.randomUUID().toString(),
    val museumSource: String, // "aic", "met", "paris_musees", "bundled"
    val museumObjectId: String,
    val title: String,
    val artist: String?,
    val dateCreated: String?,
    val culture: String?,
    val department: String?,
    val imageUrlFull: String,
    val imageUrlPreview: String,
    val imageUrlOriginal: String?,
    val puzzlePiecesTotal: Int = 100,
    val puzzleGridRows: Int = 10,
    val puzzleGridCols: Int = 10,
    val isActive: Boolean = true,
    val isPublicDomain: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
