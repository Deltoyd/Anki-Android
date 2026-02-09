package com.ichi2.anki.ui.museum

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ichi2.anki.R
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

/**
 * ViewModel for the Museum home screen.
 *
 * Manages UI state and handles business logic for:
 * - Loading and scaling the Mona Lisa painting
 * - Tracking unlocked puzzle pieces
 * - Managing study streak and extra lives
 * - Emitting events for animations and celebrations
 *
 * Uses MVVM pattern with StateFlow following ReviewerViewModel.kt pattern.
 */
class MuseumViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MuseumUiState())
    val uiState: StateFlow<MuseumUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MuseumEvent>()
    val events: SharedFlow<MuseumEvent> = _events.asSharedFlow()

    /**
     * Loads all museum data from persistence and resources.
     * Should be called once during Activity/Fragment initialization.
     */
    fun loadMuseumData(context: Context) {
        viewModelScope.launch {
            // Load painting from resources on background thread
            val painting =
                withContext(Dispatchers.IO) {
                    BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.basque_countryside,
                    )
                }

            // Load persisted data
            val unlockedPieces = MuseumPersistence.getUnlockedPieces(context)
            val streakDays = MuseumPersistence.getStreakDays(context)
            val extraLives = MuseumPersistence.getExtraLives(context)

            // Update UI state
            _uiState.update {
                it.copy(
                    painting = painting,
                    unlockedPieces = unlockedPieces,
                    streakDays = streakDays,
                    extraLives = extraLives,
                    progressText = "${unlockedPieces.size} / 500 pieces",
                )
            }
        }
    }

    /**
     * Unlocks a specific puzzle piece and emits an event for animation.
     * @param context Android context for persistence
     * @param pieceIndex The index of the piece to unlock (0-499)
     */
    fun unlockPiece(
        context: Context,
        pieceIndex: Int,
    ) {
        viewModelScope.launch {
            val wasNewlyUnlocked = MuseumPersistence.addUnlockedPiece(context, pieceIndex)

            if (wasNewlyUnlocked) {
                val updatedPieces = MuseumPersistence.getUnlockedPieces(context)

                // Update state
                _uiState.update {
                    it.copy(
                        unlockedPieces = updatedPieces,
                        progressText = "${updatedPieces.size} / 500 pieces",
                    )
                }

                // Emit animation event
                _events.emit(MuseumEvent.PieceUnlocked(pieceIndex))

                // Check for completion
                if (updatedPieces.size == 500) {
                    _events.emit(MuseumEvent.PuzzleCompleted)
                }
            }
        }
    }

    /**
     * Refreshes the UI state from persistence.
     * Useful when returning to the Museum after studying.
     */
    fun refreshData(context: Context) {
        viewModelScope.launch {
            val unlockedPieces = MuseumPersistence.getUnlockedPieces(context)
            val streakDays = MuseumPersistence.getStreakDays(context)
            val extraLives = MuseumPersistence.getExtraLives(context)

            _uiState.update {
                it.copy(
                    unlockedPieces = unlockedPieces,
                    streakDays = streakDays,
                    extraLives = extraLives,
                    progressText = "${unlockedPieces.size} / 500 pieces",
                )
            }
        }
    }
}

/**
 * UI state for the Museum screen.
 */
data class MuseumUiState(
    val painting: Bitmap? = null,
    val unlockedPieces: Set<Int> = emptySet(),
    val streakDays: Int = 0,
    val extraLives: Int = 3,
    val progressText: String = "0 / 500 pieces",
)

/**
 * One-time events for animations and UI effects.
 */
sealed class MuseumEvent {
    /**
     * Emitted when a puzzle piece should be animated as unlocked.
     */
    data class PieceUnlocked(
        val pieceIndex: Int,
    ) : MuseumEvent()

    /**
     * Emitted when all 500 pieces are unlocked.
     */
    object PuzzleCompleted : MuseumEvent()
}
