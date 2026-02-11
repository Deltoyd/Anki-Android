package com.ichi2.anki.services

import android.content.Context
import android.content.SharedPreferences
import com.ichi2.anki.model.art.ArtPiece
import com.ichi2.anki.model.art.UserArtGallery
import com.ichi2.anki.model.art.UserArtProgress
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Simple SharedPreferences-based repository for art progress
 * Perfect for POC - can migrate to Room later if needed
 */
class ArtProgressRepository(
    context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("art_progress", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        const val KEY_ART_CATALOG = "art_catalog"
        const val KEY_CURRENT_PROGRESS = "current_progress"
        const val KEY_COMPLETED_GALLERY = "completed_gallery"
    }

    // Art Catalog Management
    suspend fun saveArtCatalog(artPieces: List<ArtPiece>) {
        val catalogJson = json.encodeToString(artPieces)
        prefs.edit().putString(KEY_ART_CATALOG, catalogJson).apply()
    }

    suspend fun getArtCatalog(): List<ArtPiece> {
        val catalogJson = prefs.getString(KEY_ART_CATALOG, null) ?: return emptyList()
        return try {
            json.decodeFromString(catalogJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getArtPieceById(id: String): ArtPiece? = getArtCatalog().firstOrNull { it.id == id }

    // Current Progress Management
    suspend fun getCurrentProgress(): UserArtProgress? {
        val progressJson = prefs.getString(KEY_CURRENT_PROGRESS, null) ?: return null
        return try {
            json.decodeFromString(progressJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveProgress(progress: UserArtProgress) {
        val progressJson = json.encodeToString(progress)
        prefs.edit().putString(KEY_CURRENT_PROGRESS, progressJson).apply()
    }

    suspend fun deleteProgress() {
        prefs.edit().remove(KEY_CURRENT_PROGRESS).apply()
    }

    // Gallery Management
    suspend fun getCompletedGallery(): List<UserArtGallery> {
        val galleryJson = prefs.getString(KEY_COMPLETED_GALLERY, null) ?: return emptyList()
        return try {
            json.decodeFromString(galleryJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addToGallery(galleryEntry: UserArtGallery) {
        val current = getCompletedGallery().toMutableList()
        current.add(0, galleryEntry) // Add at beginning
        val galleryJson = json.encodeToString(current)
        prefs.edit().putString(KEY_COMPLETED_GALLERY, galleryJson).apply()
    }

    suspend fun getTotalCompletedCount(): Int = getCompletedGallery().size

    suspend fun getAvailableArtPieces(): List<ArtPiece> {
        val allPieces = getArtCatalog()
        val completedIds = getCompletedGallery().map { it.artPieceId }
        return allPieces.filter { it.id !in completedIds }
    }
}
