package com.ichi2.anki.ui.museum

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.databinding.ActivityGalleryBinding
import com.ichi2.anki.services.ArtAssetService
import com.ichi2.anki.services.ArtProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryActivity : AnkiActivity() {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: CompletedGalleryPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.galleryToolbar.setNavigationOnClickListener { finish() }

        val artService = ArtAssetService(this)
        adapter = CompletedGalleryPagerAdapter(artService)
        binding.galleryCompletedPager.adapter = adapter

        binding.galleryCompletedPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateCaptions(position)
                }
            },
        )

        loadData(artService)
    }

    private fun loadData(artService: ArtAssetService) {
        lifecycleScope.launch {
            val repo = ArtProgressRepository(this@GalleryActivity)
            val completedGallery = withContext(Dispatchers.IO) { repo.getCompletedGallery() }
            val catalog = withContext(Dispatchers.IO) { artService.loadArtCatalog() }
            val catalogMap = catalog.associateBy { it.id }

            val items =
                completedGallery.mapNotNull { entry ->
                    val artPiece = catalogMap[entry.artPieceId] ?: return@mapNotNull null
                    CompletedGalleryItem(artPiece = artPiece, galleryEntry = entry)
                }

            if (items.isEmpty()) {
                binding.captionText.text = "No completed masterpieces yet"
                binding.captionText.visibility = View.VISIBLE
                return@launch
            }

            adapter.submitList(items)
            updateCaptions(0)
        }
    }

    private fun updateCaptions(position: Int) {
        val item = adapter.getItem(position) ?: return
        binding.captionText.text = item.artPiece.title
        binding.artistText.text = item.artPiece.artist ?: ""
        binding.descriptionText.text = item.artPiece.description ?: ""
        binding.descriptionText.visibility =
            if (item.artPiece.description.isNullOrBlank()) View.GONE else View.VISIBLE
    }
}
