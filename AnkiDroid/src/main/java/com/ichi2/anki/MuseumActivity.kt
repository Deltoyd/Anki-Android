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
import com.ichi2.anki.ui.museum.StreakBottomSheet
import com.ichi2.anki.ui.onboarding.TopicSelectionActivity
import kotlinx.coroutines.launch

class MuseumActivity : AnkiActivity() {
    private lateinit var binding: ActivityMuseumBinding
    private val viewModel: MuseumViewModel by viewModels()
    private lateinit var galleryAdapter: GalleryPagerAdapter

    /** Ensures the cinematic break animation plays only once per activity creation. */
    private var hasPlayedBreakAnimation = false

    /** Flag to ensure initial page is set only once. */
    private var hasSetInitialPage = false

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

        viewModel.loadMuseumData(this)
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            viewModel.refreshData(this)
        }
    }

    private fun setupGallery() {
        val artService = ArtAssetService(this)
        galleryAdapter =
            GalleryPagerAdapter(artService)
        binding.galleryPager.adapter = galleryAdapter
        binding.galleryPager.offscreenPageLimit = 1

        binding.galleryPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val realPos = galleryAdapter.getRealPosition(position)
                    val items = viewModel.uiState.value.galleryItems
                    if (realPos in items.indices) {
                        val item = items[realPos]
                        binding.captionText.text = item.artPiece.title
                        binding.artistText.text = item.artPiece.artist ?: ""
                        PageIndicatorHelper.updateCurrentPage(
                            binding.pageIndicator,
                            items,
                            realPos,
                        )
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

                    // Set initial page once (at center of virtual range, on active painting)
                    if (!hasSetInitialPage && state.galleryItems.isNotEmpty()) {
                        hasSetInitialPage = true
                        val startPos = galleryAdapter.getStartPosition()
                        binding.galleryPager.setCurrentItem(startPos, false)
                    }

                    // Update indicators using real position
                    val currentRealPos = galleryAdapter.getRealPosition(binding.galleryPager.currentItem)
                    PageIndicatorHelper.setupIndicators(
                        binding.pageIndicator,
                        state.galleryItems,
                        currentRealPos,
                    )

                    // Update caption and artist using real position
                    if (currentRealPos in state.galleryItems.indices) {
                        binding.captionText.text = state.galleryItems[currentRealPos].artPiece.title
                        binding.artistText.text = state.galleryItems[currentRealPos].artPiece.artist ?: ""
                    }
                }

                // Update deck selector button
                binding.languageSelector.text = state.currentDeckName

                // Update streak pill text
                binding.streakPill.text = getString(R.string.streak_pill_format, state.streakDays)
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

    private fun setupButtons() {
        binding.streakPill.setOnClickListener {
            StreakBottomSheet().show(supportFragmentManager, StreakBottomSheet.TAG)
        }

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
}
