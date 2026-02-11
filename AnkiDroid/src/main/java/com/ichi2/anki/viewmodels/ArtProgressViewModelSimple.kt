package com.ichi2.anki.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ichi2.anki.model.art.ArtPiece
import com.ichi2.anki.model.art.PuzzleRevealHelper
import com.ichi2.anki.model.art.PuzzleRevealState
import com.ichi2.anki.model.art.UserArtGallery
import com.ichi2.anki.model.art.UserArtProgress
import com.ichi2.anki.model.art.addRevealedPiece
import com.ichi2.anki.model.art.getRevealedIndicesList
import com.ichi2.anki.services.ArtAssetService
import com.ichi2.anki.services.ArtProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

/**
 * Simplified ViewModel using SharedPreferences instead of Room
 */
class ArtProgressViewModelSimple(
    private val repository: ArtProgressRepository,
    private val artAssetService: ArtAssetService,
) : ViewModel() {
    private val _currentProgress = MutableStateFlow<PuzzleRevealState?>(null)
    val currentProgress: StateFlow<PuzzleRevealState?> = _currentProgress.asStateFlow()

    private val _completedGallery = MutableStateFlow<List<UserArtGallery>>(emptyList())
    val completedGallery: StateFlow<List<UserArtGallery>> = _completedGallery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    init {
        viewModelScope.launch {
            loadCurrentProgress()
            loadCompletedGallery()
        }
    }

    private suspend fun loadCurrentProgress() {
        try {
            val progress = repository.getCurrentProgress()
            if (progress != null) {
                val artPiece = repository.getArtPieceById(progress.artPieceId)

                if (artPiece != null) {
                    val filename = artAssetService.extractFilenameFromUri(artPiece.imageUrlFull)

                    _currentProgress.value =
                        PuzzleRevealState(
                            artPiece = artPiece,
                            piecesRevealed = progress.piecesRevealed,
                            piecesTotal = progress.piecesTotal,
                            revealedIndices = progress.getRevealedIndicesList(),
                            isCompleted = progress.isCompleted,
                            imageFilePath = filename,
                        )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load current progress")
            _error.emit("Failed to load progress: ${e.message}")
        }
    }

    private suspend fun loadCompletedGallery() {
        try {
            val gallery = repository.getCompletedGallery()
            _completedGallery.value = gallery
        } catch (e: Exception) {
            Timber.e(e, "Failed to load gallery")
        }
    }

    suspend fun revealNextPiece() {
        val currentState = _currentProgress.value ?: return
        val progress = repository.getCurrentProgress() ?: return

        val nextPieceIndex =
            PuzzleRevealHelper.getNextPieceIndex(
                revealedIndices = progress.getRevealedIndicesList(),
                totalPieces = progress.piecesTotal,
                gridCols = currentState.artPiece.puzzleGridCols,
                gridRows = currentState.artPiece.puzzleGridRows,
            )

        val updatedProgress =
            progress
                .addRevealedPiece(nextPieceIndex)
                .copy(piecesRevealed = progress.piecesRevealed + 1)

        val isCompleted = updatedProgress.piecesRevealed >= updatedProgress.piecesTotal
        val finalProgress =
            if (isCompleted) {
                updatedProgress.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                )
            } else {
                updatedProgress
            }

        repository.saveProgress(finalProgress)

        _currentProgress.value =
            currentState.copy(
                piecesRevealed = finalProgress.piecesRevealed,
                revealedIndices = finalProgress.getRevealedIndicesList(),
                isCompleted = isCompleted,
            )

        if (isCompleted) {
            handleArtPieceCompletion(finalProgress, currentState.artPiece)
        }
    }

    private suspend fun handleArtPieceCompletion(
        progress: UserArtProgress,
        artPiece: ArtPiece,
    ) {
        val completionTime = (System.currentTimeMillis() - progress.startedAt) / 1000 / 60
        val galleryEntry =
            UserArtGallery(
                artPieceId = artPiece.id,
                totalCardsReviewed = progress.piecesRevealed,
                completionTimeMinutes = completionTime.toInt(),
            )
        repository.addToGallery(galleryEntry)
        repository.deleteProgress()
        loadCompletedGallery()
        _error.emit("COMPLETION:${artPiece.title}")
    }

    suspend fun startNewArtPiece() {
        _isLoading.value = true

        try {
            var availablePieces = repository.getArtCatalog()

            if (availablePieces.isEmpty()) {
                Timber.d("No art pieces in storage, loading from assets")
                val catalogPieces = artAssetService.loadArtCatalog()
                if (catalogPieces.isNotEmpty()) {
                    repository.saveArtCatalog(catalogPieces)
                    availablePieces = catalogPieces
                } else {
                    throw IllegalStateException("No art pieces in catalog")
                }
            }

            val completedIds = _completedGallery.value.map { it.artPieceId }
            val uncompletedPieces = availablePieces.filter { it.id !in completedIds }

            val selectedPiece =
                uncompletedPieces.randomOrNull()
                    ?: availablePieces.randomOrNull()
                    ?: throw IllegalStateException("No art pieces available")

            val filename = artAssetService.extractFilenameFromUri(selectedPiece.imageUrlFull)

            if (!artAssetService.assetExists(filename)) {
                throw IOException("Asset not found: $filename")
            }

            val newProgress =
                UserArtProgress(
                    artPieceId = selectedPiece.id,
                    piecesTotal = selectedPiece.puzzlePiecesTotal,
                )
            repository.saveProgress(newProgress)

            _currentProgress.value =
                PuzzleRevealState(
                    artPiece = selectedPiece,
                    piecesRevealed = 0,
                    piecesTotal = selectedPiece.puzzlePiecesTotal,
                    revealedIndices = emptyList(),
                    isCompleted = false,
                    imageFilePath = filename,
                )

            Timber.d("Started new art piece: ${selectedPiece.title}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start new art piece")
            _error.emit("Failed to start new art piece: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun getAvailableArtPieces(): List<ArtPiece> =
        withContext(Dispatchers.IO) {
            repository.getAvailableArtPieces().ifEmpty {
                val catalogPieces = artAssetService.loadArtCatalog()
                if (catalogPieces.isNotEmpty()) {
                    repository.saveArtCatalog(catalogPieces)
                }
                catalogPieces
            }
        }

    suspend fun selectArtPiece(artPiece: ArtPiece) {
        _isLoading.value = true

        try {
            val filename = artAssetService.extractFilenameFromUri(artPiece.imageUrlFull)

            if (!artAssetService.assetExists(filename)) {
                throw IOException("Asset not found: $filename")
            }

            val newProgress =
                UserArtProgress(
                    artPieceId = artPiece.id,
                    piecesTotal = artPiece.puzzlePiecesTotal,
                )
            repository.saveProgress(newProgress)

            _currentProgress.value =
                PuzzleRevealState(
                    artPiece = artPiece,
                    piecesRevealed = 0,
                    piecesTotal = artPiece.puzzlePiecesTotal,
                    revealedIndices = emptyList(),
                    isCompleted = false,
                    imageFilePath = filename,
                )

            Timber.d("Selected art piece: ${artPiece.title}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to select art piece")
            _error.emit("Failed to select art piece: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun getTotalCompletedCount(): Int = repository.getTotalCompletedCount()
}

class ArtProgressViewModelSimpleFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArtProgressViewModelSimple::class.java)) {
            val repository = ArtProgressRepository(context)
            val artAssetService = ArtAssetService(context)

            return ArtProgressViewModelSimple(repository, artAssetService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
