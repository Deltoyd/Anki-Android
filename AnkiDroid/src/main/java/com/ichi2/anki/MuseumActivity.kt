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
 * MuseoLingo home screen - the main entry point of the app.
 *
 * Displays:
 *   • Top navigation with language selector, MuseoLingo title, and menu
 *   • Stats row showing streak and today's cards
 *   • Landscape puzzle card with Basque countryside image
 *   • Progress bar and peek button
 *   • Activity heatmap with year header
 *   • Bottom review cards button with due count
 *
 * Design: Clean, editorial learning app aesthetic with amber accents
 */
class MuseumActivity : AnkiActivity() {
    private lateinit var binding: ActivityMuseumBinding
    private val viewModel: MuseumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMuseumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupButtons()
        setupStats()
        setupHeatmap()

        // Load initial data (now uses basque_countryside.jpg)
        viewModel.loadMuseumData(this)
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from Reviewer
        viewModel.refreshData(this)
        updateStats()
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

                // Update progress (out of 500 pieces)
                val progress = (state.unlockedPieces.size * 100) / 500
                binding.progressBar.progress = progress
                binding.progressText.text = "${state.unlockedPieces.size}/500"

                // Update deck selector button
                binding.languageSelector.text = state.currentDeckName
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
     * Setup stats row (streak and today's cards).
     */
    private fun setupStats() {
        updateStats()
    }

    /**
     * Update stats display.
     */
    private fun updateStats() {
        val streakDays = MuseumPersistence.getStreakDays(this)
        binding.streakStat.text = "🔥 $streakDays"

        // TODO: Get actual cards reviewed today from collection
        val cardsToday = 0 // Placeholder
        binding.todayStat.text = "📖 $cardsToday today"
    }

    /**
     * Setup heatmap with activity data.
     */
    private fun setupHeatmap() {
        // Set current year
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        binding.heatmapYear.text = currentYear.toString()

        // Use existing mock data generation for now
        val activityData = generateMockActivityData()
        binding.heatmapView.setActivityData(activityData)

        // Calculate total reviews and active days
        val totalReviews = activityData.values.sum()
        val activeDays = activityData.size
        binding.heatmapSubheader.text = "$totalReviews reviews   $activeDays days"

        binding.heatmapView.onDayTapped = { dateKey ->
            val cards = activityData[dateKey] ?: 0
            showThemedToast(this, "$dateKey — $cards cards reviewed", false)
        }
    }

    /**
     * Setup button click listeners.
     */
    private fun setupButtons() {
        // Deck selector
        binding.languageSelector.setOnClickListener {
            showDeckSelectorDialog()
        }

        // Menu button
        binding.menuButton.setOnClickListener {
            startActivity(Intent(this, DeckPicker::class.java))
        }

        // Peek button → Show full painting for 3 seconds
        binding.peekButton.setOnClickListener {
            showPeekPreview()
        }

        // Review Cards button → Launch Reviewer
        binding.reviewButton.setOnClickListener {
            startActivity(Intent(this, Reviewer::class.java))
        }
    }

    /**
     * Shows a dialog allowing the user to select which deck to study.
     */
    private fun showDeckSelectorDialog() {
        lifecycleScope.launch {
            val decks = viewModel.loadAllDecks()
            if (decks.isEmpty()) {
                showThemedToast(this@MuseumActivity, "No decks available", false)
                return@launch
            }

            val deckNames = decks.map { it.name }.toTypedArray()
            val deckIds = decks.map { it.id }

            MaterialAlertDialogBuilder(this@MuseumActivity)
                .setTitle("Select Deck")
                .setItems(deckNames) { dialog, which ->
                    val selectedDeck = decks[which]
                    viewModel.setCurrentDeck(
                        this@MuseumActivity,
                        selectedDeck.id,
                        selectedDeck.name,
                    )
                    showThemedToast(
                        this@MuseumActivity,
                        "Switched to ${selectedDeck.name}",
                        false,
                    )
                    dialog.dismiss()
                }.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
    }

    /**
     * Shows the full painting for 3 seconds with a countdown toast.
     */
    private fun showPeekPreview() {
        // Enable peek mode
        binding.paintingView.enablePeekMode()

        // Show countdown toast
        showThemedToast(this, "Peeking for 3 seconds...", false)

        // Disable peek mode after 3 seconds
        binding.paintingView.postDelayed({
            binding.paintingView.disablePeekMode()
        }, 3000)
    }

    /**
     * Shows a celebration dialog when the puzzle is completed.
     */
    private fun showCompletionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Masterpiece Complete!")
            .setMessage("You've revealed the entire Basque countryside! Your dedication to learning is inspiring.")
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
