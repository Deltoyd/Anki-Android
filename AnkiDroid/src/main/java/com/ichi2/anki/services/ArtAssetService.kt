package com.ichi2.anki.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ichi2.anki.model.art.ArtCatalogEntry
import com.ichi2.anki.model.art.ArtPiece
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Service for loading art images and metadata from bundled assets
 * 100% offline - no network required
 */
class ArtAssetService(
    private val context: Context,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        }

    /**
     * Load art catalog from assets/art/catalog.json
     * Converts JSON entries to ArtPiece entities
     */
    suspend fun loadArtCatalog(): List<ArtPiece> =
        withContext(Dispatchers.IO) {
            try {
                // Read catalog.json from assets
                val catalogJson =
                    context.assets.open("art/catalog.json").bufferedReader().use {
                        it.readText()
                    }

                val catalogEntries = json.decodeFromString<List<ArtCatalogEntry>>(catalogJson)

                // Convert to ArtPiece entities
                catalogEntries.map { entry ->
                    ArtPiece(
                        id = entry.id,
                        museumSource = "bundled",
                        museumObjectId = entry.id,
                        title = entry.title,
                        artist = entry.artist,
                        dateCreated = null,
                        culture = null,
                        department = "Proof of Concept Collection",
                        imageUrlFull = "asset://art/${entry.filename}", // Custom URI scheme
                        imageUrlPreview = "asset://art/${entry.filename}",
                        imageUrlOriginal = null,
                        isPublicDomain = true,
                        description = entry.description,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load art catalog from assets")
                emptyList()
            }
        }

    /**
     * Load bitmap from assets/art/ folder
     * @param filename Just the filename (e.g., "art_001.png")
     * @return Bitmap or null if loading fails
     */
    suspend fun loadArtBitmap(filename: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val options =
                    BitmapFactory.Options().apply {
                        // Use RGB_565 for lower memory usage (vs ARGB_8888)
                        inPreferredConfig = Bitmap.Config.RGB_565
                        // Can add inSampleSize for further optimization if needed
                        inSampleSize = 1
                    }
                context.assets.open("art/$filename").use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load bitmap from assets: $filename")
                null
            }
        }

    /**
     * Check if asset file exists
     */
    fun assetExists(filename: String): Boolean =
        try {
            context.assets.open("art/$filename").close()
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Get all available art filenames in assets/art/ folder
     */
    suspend fun getAvailableArtFiles(): List<String> =
        withContext(Dispatchers.IO) {
            try {
                context.assets.list("art")?.filter {
                    it.endsWith(".png", ignoreCase = true) ||
                        it.endsWith(".jpg", ignoreCase = true)
                } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "Failed to list art assets")
                emptyList()
            }
        }

    /**
     * Extract filename from asset URI
     * Converts "asset://art/filename.png" to "filename.png"
     */
    fun extractFilenameFromUri(assetUri: String): String = assetUri.substringAfter("asset://art/")
}
