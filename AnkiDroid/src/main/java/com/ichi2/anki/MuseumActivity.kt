package com.ichi2.anki

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ichi2.anki.databinding.ActivityMuseumBinding
import com.ichi2.anki.ui.museum.MuseumEvent
import com.ichi2.anki.ui.museum.MuseumPersistence
import com.ichi2.anki.ui.museum.MuseumViewModel
import com.ichi2.anki.ui.museum.toDateKey
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Random

/**
 * IKASI museum home screen - the main entry point of the app.
 *
 * Displays:
 *   • The Mona Lisa as a 500-piece puzzle that progressively reveals with study
 *   • Study streak and extra lives
 *   • Year heatmap of study activity
 *   • Floating "Ikasi" study button
 *   • Quick action buttons (Decks, Gallery)
 *
 * Design philosophy: Minimalistic and elegant (Tesla/Apple inspired),
 * making users excited to press the study button.
 */
class MuseumActivity : AnkiActivity() {
    private lateinit var binding: ActivityMuseumBinding
    private val viewModel: MuseumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMuseumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupHeatmap()
        setupButtons()

        // Load initial data
        viewModel.loadMuseumData(this)
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from Reviewer
        viewModel.refreshData(this)
    }

    /**
     * Setup lifecycle observers for StateFlow and SharedFlow.
     */
    private fun setupObservers() {
        // Observe UI state
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update painting
                state.painting?.let { painting ->
                    binding.paintingView.setPainting(painting)
                }

                // Update unlocked pieces
                binding.paintingView.setUnlockedPieces(state.unlockedPieces)

                // Update progress text
                binding.progressText.text = state.progressText

                // Update streak
                binding.streakText.text = "🔥 ${state.streakDays} days"

                // Update lives
                val maxLives = 3
                binding.livesText.text = "❤️".repeat(state.extraLives) +
                    "☐".repeat(maxLives - state.extraLives)
            }
        }

        // Observe events
        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is MuseumEvent.PieceUnlocked -> {
                        // Trigger piece unlock animation
                        binding.paintingView.animateUnlock(event.pieceIndex)
                    }
                    is MuseumEvent.PuzzleCompleted -> {
                        // Show completion celebration dialog
                        showCompletionDialog()
                    }
                }
            }
        }
    }

    /**
     * Setup heatmap with activity data.
     */
    private fun setupHeatmap() {
        // Use existing mock data generation for now
        // TODO: Replace with actual study data from collection
        binding.heatmapView.setActivityData(generateMockActivityData())

        binding.heatmapView.onDayTapped = { dateKey ->
            val cards = generateMockActivityData()[dateKey] ?: 0
            showThemedToast(this, "$dateKey — $cards cards reviewed", false)
        }
    }

    /**
     * Setup button click listeners.
     */
    private fun setupButtons() {
        // Study button → Launch Reviewer
        binding.studyButton.setOnClickListener {
            startActivity(Intent(this, Reviewer::class.java))
        }

        // Decks button → Launch DeckPicker
        binding.decksButton.setOnClickListener {
            startActivity(Intent(this, DeckPicker::class.java))
        }

        // Gallery button → Coming soon toast
        binding.galleryButton.setOnClickListener {
            showThemedToast(this, "Gallery coming soon!", false)
        }
    }

    /**
     * Shows a celebration dialog when the puzzle is completed.
     */
    private fun showCompletionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Masterpiece Complete!")
            .setMessage("You've revealed the entire Mona Lisa! Your dedication to learning is inspiring.")
            .setPositiveButton("Continue Learning") { dialog, _ ->
                dialog.dismiss()
            }.setNeutralButton("View Gallery") { dialog, _ ->
                showThemedToast(this, "Gallery coming soon!", false)
                dialog.dismiss()
            }.setCancelable(false)
            .show()
    }

    /**
     * Generates 120 days of plausible mock study data.
     * Uses a fixed seed so the heatmap looks consistent across runs.
     *
     * TODO: Replace with actual study data from collection
     */
    private fun generateMockActivityData(): Map<String, Int> {
        val data = mutableMapOf<String, Int>()
        val random = Random(42)
        val streakDays = MuseumPersistence.getStreakDays(this)

        for (i in 0..120) {
            val date =
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -i)
                }
            // Recent streak days always have data
            if (i < streakDays || random.nextFloat() < 0.6f) {
                val cards =
                    if (i < streakDays) {
                        random.nextInt(15) + 10
                    } else {
                        random.nextInt(25) + 5
                    }
                data[date.toDateKey()] = cards
            }
        }
        return data
    }
}
