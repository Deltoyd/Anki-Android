package com.ichi2.anki.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.MainContainerActivity
import com.ichi2.anki.databinding.ActivityArtSelectionBinding
import com.ichi2.anki.databinding.ItemArtMasonryBinding
import com.ichi2.anki.model.art.ArtPiece
import com.ichi2.anki.model.art.UserArtProgress
import com.ichi2.anki.services.ArtAssetService
import com.ichi2.anki.services.ArtProgressRepository
import com.ichi2.anki.ui.museum.MuseumPersistence
import kotlinx.coroutines.launch

class ArtSelectionActivity : AnkiActivity() {
    private lateinit var binding: ActivityArtSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.artGrid.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        loadArt()
    }

    private fun loadArt() {
        lifecycleScope.launch {
            val artService = ArtAssetService(this@ArtSelectionActivity)
            val catalog = artService.loadArtCatalog()
            binding.artGrid.adapter =
                ArtMasonryAdapter(artService, catalog) { artPiece ->
                    onArtSelected(artPiece)
                }
        }
    }

    private fun onArtSelected(artPiece: ArtPiece) {
        lifecycleScope.launch {
            val repo = ArtProgressRepository(this@ArtSelectionActivity)

            // Save catalog so gallery can use it
            val artService = ArtAssetService(this@ArtSelectionActivity)
            repo.saveArtCatalog(artService.loadArtCatalog())

            // Initialize progress for this art piece
            val progress =
                UserArtProgress(
                    artPieceId = artPiece.id,
                    deckId = MuseumPersistence.getSelectedDeckId(this@ArtSelectionActivity),
                    piecesTotal = artPiece.puzzlePiecesTotal,
                )
            repo.saveProgress(progress)

            // Mark onboarding complete
            MuseumPersistence.setActiveArtPieceId(this@ArtSelectionActivity, artPiece.id)
            MuseumPersistence.setOnboardingComplete(this@ArtSelectionActivity, true)

            // Launch museum with clear top
            val intent = Intent(this@ArtSelectionActivity, MainContainerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}

private class ArtMasonryAdapter(
    private val artService: ArtAssetService,
    private val artPieces: List<ArtPiece>,
    private val onSelect: (ArtPiece) -> Unit,
) : RecyclerView.Adapter<ArtMasonryAdapter.ViewHolder>() {
    class ViewHolder(
        val binding: ItemArtMasonryBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding =
            ItemArtMasonryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val artPiece = artPieces[position]
        val filename = artService.extractFilenameFromUri(artPiece.imageUrlFull)

        // Load bitmap asynchronously
        kotlinx.coroutines.MainScope().launch {
            val bitmap = artService.loadArtBitmap(filename)
            bitmap?.let { holder.binding.artImage.setImageBitmap(it) }
        }

        holder.binding.root.setOnClickListener { onSelect(artPiece) }
    }

    override fun getItemCount() = artPieces.size
}
