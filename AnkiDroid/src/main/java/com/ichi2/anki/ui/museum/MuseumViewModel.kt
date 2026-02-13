package com.ichi2.anki.ui.museum

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.libanki.DeckNameId
import com.ichi2.anki.model.art.ArtPiece
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MuseumViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MuseumUiState())
    val uiState: StateFlow<MuseumUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MuseumEvent>()
    val events: SharedFlow<MuseumEvent> = _events.asSharedFlow()

    fun loadMuseumData(context: Context) {
        viewModelScope.launch {
            val streakDays = MuseumPersistence.getStreakDays(context)
            val extraLives = MuseumPersistence.getExtraLives(context)

            val currentDeckName =
                try {
                    val selectedDeckId = MuseumPersistence.getSelectedDeckId(context)
                    withCol {
                        if (selectedDeckId != null) {
                            decks.name(selectedDeckId, default = true)
                        } else {
                            decks.current().name
                        }
                    }
                } catch (e: Exception) {
                    "Default"
                }

            _uiState.update {
                it.copy(
                    streakDays = streakDays,
                    extraLives = extraLives,
                    currentDeckName = currentDeckName,
                )
            }

            // Load gallery data
            loadGalleryData(context)
        }
    }

    fun loadGalleryData(context: Context) {
        viewModelScope.launch {
            val artService = ArtAssetService(context)
            val repo = ArtProgressRepository(context)

            val catalog = withContext(Dispatchers.IO) { artService.loadArtCatalog() }
            if (catalog.isEmpty()) return@launch

            val currentProgress = withContext(Dispatchers.IO) { repo.getCurrentProgress() }
            val completedGallery = withContext(Dispatchers.IO) { repo.getCompletedGallery() }
            val completedIds = completedGallery.map { it.artPieceId }.toSet()
            val activeArtPieceId = MuseumPersistence.getActiveArtPieceId(context)

            var activePageIndex = 0
            var activePainting: Bitmap? = null
            var activeTitle = ""

            val galleryItems =
                catalog
                    .map { artPiece ->
                        val state =
                            when {
                                artPiece.id == activeArtPieceId && currentProgress != null &&
                                    currentProgress.artPieceId == artPiece.id -> {
                                    activeTitle = artPiece.title
                                    ArtPieceState.ACTIVE
                                }
                                artPiece.id in completedIds -> ArtPieceState.COMPLETED
                                else -> ArtPieceState.LOCKED
                            }

                        val unlockedPieces =
                            if (state == ArtPieceState.ACTIVE && currentProgress != null) {
                                currentProgress.getRevealedIndicesList().toSet()
                            } else {
                                emptySet()
                            }

                        val progressPercent =
                            if (state == ArtPieceState.ACTIVE && currentProgress != null) {
                                if (currentProgress.piecesTotal > 0) {
                                    (currentProgress.piecesRevealed * 100) / currentProgress.piecesTotal
                                } else {
                                    0
                                }
                            } else if (state == ArtPieceState.COMPLETED) {
                                100
                            } else {
                                0
                            }

                        GalleryArtItem(
                            artPiece = artPiece,
                            state = state,
                            unlockedPieces = unlockedPieces,
                            progressPercent = progressPercent,
                        )
                    }.filter { it.state != ArtPieceState.COMPLETED }
                    .sortedBy {
                        when (it.state) {
                            ArtPieceState.ACTIVE -> 0
                            ArtPieceState.LOCKED -> 2
                            ArtPieceState.COMPLETED -> 1 // Dead code after filter
                        }
                    }

            // Active painting is always sorted to index 0
            activePageIndex = 0

            // Load active piece painting bitmap
            val activeItem = galleryItems.find { it.state == ArtPieceState.ACTIVE }
            if (activeItem != null) {
                val filename = artService.extractFilenameFromUri(activeItem.artPiece.imageUrlFull)
                activePainting = withContext(Dispatchers.IO) { artService.loadArtBitmap(filename) }
            }

            // Always start on active painting (index 0)
            val initialPage = 0

            _uiState.update {
                it.copy(
                    painting = activePainting,
                    galleryItems = galleryItems,
                    activePageIndex = initialPage,
                    currentArtTitle = activeTitle,
                    unlockedPieces = activeItem?.unlockedPieces ?: emptySet(),
                    progressText =
                        if (activeItem != null) {
                            "${activeItem.unlockedPieces.size} / ${activeItem.artPiece.puzzlePiecesTotal} pieces"
                        } else {
                            "0 / 100 pieces"
                        },
                )
            }
        }
    }

    suspend fun loadAllDecks(): List<DeckNameId> =
        withCol {
            decks.allNamesAndIds(includeFiltered = false)
        }

    fun setCurrentDeck(
        context: Context,
        deckId: Long,
        deckName: String,
    ) {
        viewModelScope.launch {
            MuseumPersistence.setSelectedDeckId(context, deckId)
            withCol {
                decks.select(deckId)
            }
            _uiState.update {
                it.copy(currentDeckName = deckName)
            }
        }
    }

    fun unlockPiece(
        context: Context,
        pieceIndex: Int,
    ) {
        viewModelScope.launch {
            val wasNewlyUnlocked = MuseumPersistence.addUnlockedPiece(context, pieceIndex)

            if (wasNewlyUnlocked) {
                val updatedPieces = MuseumPersistence.getUnlockedPieces(context)

                _uiState.update {
                    it.copy(
                        unlockedPieces = updatedPieces,
                        progressText = "${updatedPieces.size} / 100 pieces",
                    )
                }

                _events.emit(MuseumEvent.PieceUnlocked(pieceIndex))

                if (updatedPieces.size == 100) {
                    _events.emit(MuseumEvent.PuzzleCompleted)
                }
            }
        }
    }

    fun refreshData(context: Context) {
        viewModelScope.launch {
            val streakDays = MuseumPersistence.getStreakDays(context)
            val extraLives = MuseumPersistence.getExtraLives(context)

            _uiState.update {
                it.copy(
                    streakDays = streakDays,
                    extraLives = extraLives,
                )
            }

            // Reload gallery items to reflect progress changes
            loadGalleryData(context)
        }
    }
}

data class MuseumUiState(
    val painting: Bitmap? = null,
    val unlockedPieces: Set<Int> = emptySet(),
    val streakDays: Int = 0,
    val extraLives: Int = 3,
    val progressText: String = "0 / 100 pieces",
    val currentDeckName: String = "Default",
    val galleryItems: List<GalleryArtItem> = emptyList(),
    val activePageIndex: Int = 0,
    val currentArtTitle: String = "",
)

sealed class MuseumEvent {
    data class PieceUnlocked(
        val pieceIndex: Int,
    ) : MuseumEvent()

    object PuzzleCompleted : MuseumEvent()
}

data class GalleryArtItem(
    val artPiece: ArtPiece,
    val state: ArtPieceState,
    val unlockedPieces: Set<Int> = emptySet(),
    val progressPercent: Int = 0,
)

enum class ArtPieceState { ACTIVE, COMPLETED, LOCKED }
