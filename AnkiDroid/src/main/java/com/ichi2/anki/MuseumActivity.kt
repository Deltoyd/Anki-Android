package com.ichi2.anki

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ichi2.anki.databinding.ActivityMuseumBinding
import com.ichi2.anki.services.ArtAssetService
import com.ichi2.anki.ui.museum.GalleryPagerAdapter
import com.ichi2.anki.ui.museum.MuseumEvent
import com.ichi2.anki.ui.museum.MuseumPersistence
import com.ichi2.anki.ui.museum.MuseumViewModel
import com.ichi2.anki.ui.museum.PageIndicatorHelper
import com.ichi2.anki.ui.museum.toDateKey
import com.ichi2.anki.ui.onboarding.TopicSelectionActivity
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Random

class MuseumActivity : AnkiActivity() {
    private lateinit var binding: ActivityMuseumBinding
    private val viewModel: MuseumViewModel by viewModels()
    private lateinit var galleryAdapter: GalleryPagerAdapter

    /** Ensures the cinematic break animation plays only once per activity creation. */
    private var hasPlayedBreakAnimation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Onboarding check
        if (!MuseumPersistence.isOnboardingComplete(this)) {
            startActivity(Intent(this, TopicSelectionActivity::class.java))
            finish()
            return
        }

        binding = ActivityMuseumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGallery()
        setupObservers()
        setupButtons()
        setupStats()
        setupHeatmap()

        viewModel.loadMuseumData(this)
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            viewModel.refreshData(this)
            updateStats()
        }
    }

    private fun setupGallery() {
        val artService = ArtAssetService(this)
        galleryAdapter =
            GalleryPagerAdapter(artService) { position ->
                showPeekPreview(position)
            }
        binding.galleryPager.adapter = galleryAdapter
        binding.galleryPager.offscreenPageLimit = 1

        binding.galleryPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val items = viewModel.uiState.value.galleryItems
                    if (position in items.indices) {
                        val item = items[position]
                        binding.captionText.text = item.artPiece.title
                        PageIndicatorHelper.updateCurrentPage(
                            binding.pageIndicator,
                            items,
                            position,
                        )
                        MuseumPersistence.setGalleryPosition(this@MuseumActivity, position)
                    }
                }
            },
        )
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.galleryItems.isNotEmpty()) {
                    galleryAdapter.submitList(state.galleryItems)

                    // Update painting for active item
                    state.painting?.let { painting ->
                        galleryAdapter.updateActivePainting(painting)

                        // Play the cinematic puzzle break animation once
                        if (!hasPlayedBreakAnimation) {
                            hasPlayedBreakAnimation = true
                            binding.puzzleBreakOverlay.startAnimation(painting) {
                                // Animation complete — gallery is now visible underneath
                            }
                        }
                    }

                    // Set initial page
                    if (binding.galleryPager.currentItem != state.activePageIndex &&
                        state.activePageIndex in state.galleryItems.indices
                    ) {
                        binding.galleryPager.setCurrentItem(state.activePageIndex, false)
                    }

                    // Update indicators
                    PageIndicatorHelper.setupIndicators(
                        binding.pageIndicator,
                        state.galleryItems,
                        binding.galleryPager.currentItem,
                    )

                    // Update caption
                    val currentPos = binding.galleryPager.currentItem
                    if (currentPos in state.galleryItems.indices) {
                        binding.captionText.text = state.galleryItems[currentPos].artPiece.title
                    }
                }

                // Update deck selector button
                binding.languageSelector.text = state.currentDeckName
            }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is MuseumEvent.PieceUnlocked -> {
                        // The active page's PaintingPuzzleView will be updated via adapter
                    }
                    is MuseumEvent.PuzzleCompleted -> {
                        showCompletionDialog()
                    }
                }
            }
        }
    }

    private fun setupStats() {
        updateStats()
    }

    private fun updateStats() {
        val streakDays = MuseumPersistence.getStreakDays(this)
        binding.streakStat.text = "🔥 $streakDays"

        val cardsToday = 0
        binding.todayStat.text = "📖 $cardsToday today"
    }

    private fun setupHeatmap() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        binding.heatmapYear.text = currentYear.toString()

        val activityData = generateMockActivityData()
        binding.heatmapView.setActivityData(activityData)

        val totalReviews = activityData.values.sum()
        val activeDays = activityData.size
        binding.heatmapSubheader.text = "$totalReviews reviews   $activeDays days"

        binding.heatmapView.onDayTapped = { dateKey ->
            val cards = activityData[dateKey] ?: 0
            showThemedToast(this, "$dateKey — $cards cards reviewed", false)
        }
    }

    private fun setupButtons() {
        binding.languageSelector.setOnClickListener {
            showDeckSelectorDialog()
        }

        binding.menuButton.setOnClickListener {
            startActivity(Intent(this, DeckPicker::class.java))
        }

        binding.reviewButton.setOnClickListener {
            startActivity(Intent(this, Reviewer::class.java))
        }
    }

    private fun showPeekPreview(position: Int) {
        // Find the PaintingPuzzleView in the current ViewPager2 page
        val recyclerView = binding.galleryPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
        val paintingView = viewHolder?.itemView?.findViewById<com.ichi2.anki.ui.museum.PaintingPuzzleView>(R.id.paintingView)

        paintingView?.let {
            it.enablePeekMode()
            showThemedToast(this, "Peeking for 3 seconds...", false)
            it.postDelayed({ it.disablePeekMode() }, 3000)
        }
    }

    private fun showDeckSelectorDialog() {
        lifecycleScope.launch {
            val decks = viewModel.loadAllDecks()
            if (decks.isEmpty()) {
                showThemedToast(this@MuseumActivity, "No decks available", false)
                return@launch
            }

            val deckNames = decks.map { it.name }.toTypedArray()

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

    private fun showCompletionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Masterpiece Complete!")
            .setMessage("You've revealed the entire painting! Your dedication to learning is inspiring.")
            .setPositiveButton("Continue Learning") { dialog, _ ->
                dialog.dismiss()
            }.setNeutralButton("View Gallery") { dialog, _ ->
                showThemedToast(this, "Gallery coming soon!", false)
                dialog.dismiss()
            }.setCancelable(false)
            .show()
    }

    private fun generateMockActivityData(): Map<String, Int> {
        val data = mutableMapOf<String, Int>()
        val random = Random(42)
        val streakDays = MuseumPersistence.getStreakDays(this)

        for (i in 0..120) {
            val date =
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -i)
                }
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
