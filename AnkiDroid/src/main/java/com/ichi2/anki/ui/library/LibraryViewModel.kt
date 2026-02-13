package com.ichi2.anki.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ichi2.anki.CollectionManager.withCol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun loadDecks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val decks =
                    withCol {
                        val tree = sched.deckDueTree()
                        tree.filter { !it.filtered }.map { node ->
                            DeckItem(
                                id = node.did,
                                name = node.fullDeckName,
                                dueCount = node.revCount + node.lrnCount + node.newCount,
                            )
                        }
                    }

                _uiState.update {
                    it.copy(
                        decks = decks,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectDeck(deckId: Long) {
        viewModelScope.launch {
            withCol {
                decks.select(deckId)
            }
        }
    }
}

data class LibraryUiState(
    val decks: List<DeckItem> = emptyList(),
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
)

data class DeckItem(
    val id: Long,
    val name: String,
    val dueCount: Int,
)
