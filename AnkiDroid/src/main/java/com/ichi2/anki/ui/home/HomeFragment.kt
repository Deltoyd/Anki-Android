package com.ichi2.anki.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.ichi2.anki.MainContainerActivity
import com.ichi2.anki.R
import com.ichi2.anki.Reviewer
import com.ichi2.anki.databinding.FragmentHomeBinding
import com.ichi2.anki.services.ArtAssetService
import com.ichi2.anki.ui.museum.GalleryActivity
import com.ichi2.anki.ui.museum.GalleryPagerAdapter
import com.ichi2.anki.ui.museum.MuseumEvent
import com.ichi2.anki.ui.museum.MuseumViewModel
import com.ichi2.anki.ui.museum.StreakBottomSheet
import com.ichi2.anki.ui.onboarding.ArtSelectionActivity
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val viewModel: MuseumViewModel by activityViewModels()
    private lateinit var galleryAdapter: GalleryPagerAdapter

    private var hasSetInitialPage = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupGallery()
        setupObservers()
        setupButtons()

        viewModel.loadMuseumData(requireContext())
    }

    override fun onResume() {
        super.onResume()
        if (::galleryAdapter.isInitialized) {
            viewModel.refreshData(requireContext())
        }
    }

    private fun setupToolbar() {
        binding.homeToolbar.inflateMenu(R.menu.home_toolbar)
        binding.homeToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sync -> {
                    (activity as? MainContainerActivity)?.sync()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupGallery() {
        val artService = ArtAssetService(requireContext())
        galleryAdapter = GalleryPagerAdapter(artService)
        binding.galleryPager.adapter = galleryAdapter
        binding.galleryPager.offscreenPageLimit = 1
        binding.galleryPager.isUserInputEnabled = false
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.galleryItems.isNotEmpty()) {
                    galleryAdapter.submitList(state.galleryItems)

                    state.painting?.let { painting ->
                        galleryAdapter.updateActivePainting(painting)
                    }

                    if (!hasSetInitialPage && state.galleryItems.isNotEmpty()) {
                        hasSetInitialPage = true
                        val startPos = galleryAdapter.getStartPosition()
                        binding.galleryPager.setCurrentItem(startPos, false)
                    }

                    val currentRealPos = galleryAdapter.getRealPosition(binding.galleryPager.currentItem)
                    if (currentRealPos in state.galleryItems.indices) {
                        binding.captionText.text = state.galleryItems[currentRealPos].artPiece.title
                        binding.artistText.text = state.galleryItems[currentRealPos].artPiece.artist ?: ""
                    }
                }

                binding.streakPill.text = getString(R.string.streak_pill_format, state.streakDays)

                if (state.completedCount > 0) {
                    binding.galleryPill.visibility = View.VISIBLE
                    binding.galleryPill.text = "🖼️ ${state.completedCount}"
                } else {
                    binding.galleryPill.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is MuseumEvent.PieceUnlocked -> {}
                    is MuseumEvent.PuzzleCompleted -> showCompletionDialog()
                }
            }
        }
    }

    private fun setupButtons() {
        binding.streakPill.setOnClickListener {
            StreakBottomSheet().show(parentFragmentManager, StreakBottomSheet.TAG)
        }

        binding.reviewButton.setOnClickListener {
            startActivity(Intent(requireContext(), Reviewer::class.java))
        }

        binding.peekButton.setOnClickListener {
            showPeekPreview(binding.galleryPager.currentItem)
        }

        binding.changeMasterpieceButton.setOnClickListener {
            startActivity(Intent(requireContext(), ArtSelectionActivity::class.java))
        }

        binding.galleryPill.setOnClickListener {
            startActivity(Intent(requireContext(), GalleryActivity::class.java))
        }
    }

    private fun showPeekPreview(position: Int) {
        val recyclerView = binding.galleryPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
        val paintingView = viewHolder?.itemView?.findViewById<com.ichi2.anki.ui.museum.PaintingPuzzleView>(R.id.paintingView)

        paintingView?.let {
            it.enablePeekMode()
            it.postDelayed({ it.disablePeekMode() }, 3000)
        }
    }

    private fun showCompletionDialog() {
        com.google.android.material.dialog
            .MaterialAlertDialogBuilder(requireContext())
            .setTitle("Masterpiece Complete!")
            .setMessage("You've revealed the entire painting! Your dedication to learning is inspiring.")
            .setPositiveButton("Continue Learning") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }
}
