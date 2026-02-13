package com.ichi2.anki.ui.museum

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ichi2.anki.databinding.ItemGalleryPageBinding
import com.ichi2.anki.services.ArtAssetService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class GalleryPagerAdapter(
    private val artService: ArtAssetService,
    private val onPeekClick: (Int) -> Unit,
) : RecyclerView.Adapter<GalleryPagerAdapter.GalleryViewHolder>() {
    private var items: List<GalleryArtItem> = emptyList()
    private val bitmapCache = mutableMapOf<String, Bitmap?>()

    fun submitList(newItems: List<GalleryArtItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateActivePainting(painting: Bitmap?) {
        val activeIndex = items.indexOfFirst { it.state == ArtPieceState.ACTIVE }
        if (activeIndex >= 0 && painting != null) {
            val artPiece = items[activeIndex].artPiece
            bitmapCache[artPiece.id] = painting
            notifyItemChanged(activeIndex)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): GalleryViewHolder {
        val binding =
            ItemGalleryPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return GalleryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: GalleryViewHolder,
        position: Int,
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class GalleryViewHolder(
        private val binding: ItemGalleryPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GalleryArtItem) {
            val filename = artService.extractFilenameFromUri(item.artPiece.imageUrlFull)

            // Reset visibility
            binding.paintingView.visibility = View.GONE
            binding.completedImage.visibility = View.GONE
            binding.lockedOverlay.visibility = View.GONE
            binding.progressContainer.visibility = View.GONE
            binding.peekButton.visibility = View.GONE
            binding.completedBadge.visibility = View.GONE
            binding.lockedText.visibility = View.GONE

            when (item.state) {
                ArtPieceState.ACTIVE -> bindActive(item, filename)
                ArtPieceState.COMPLETED -> bindCompleted(item, filename)
                ArtPieceState.LOCKED -> bindLocked(item, filename)
            }
        }

        private fun bindActive(
            item: GalleryArtItem,
            filename: String,
        ) {
            binding.paintingView.visibility = View.VISIBLE
            binding.progressContainer.visibility = View.VISIBLE
            binding.peekButton.visibility = View.VISIBLE

            val cached = bitmapCache[item.artPiece.id]
            if (cached != null) {
                binding.paintingView.setPainting(cached)
            } else {
                MainScope().launch {
                    val bitmap = artService.loadArtBitmap(filename)
                    if (bitmap != null) {
                        bitmapCache[item.artPiece.id] = bitmap
                        binding.paintingView.setPainting(bitmap)
                    }
                }
            }

            binding.paintingView.setUnlockedPieces(item.unlockedPieces)
            binding.progressBar.progress = item.progressPercent
            binding.progressText.text = "${item.unlockedPieces.size}/${item.artPiece.puzzlePiecesTotal}"

            binding.peekButton.setOnClickListener {
                onPeekClick(bindingAdapterPosition)
            }
        }

        private fun bindCompleted(
            item: GalleryArtItem,
            filename: String,
        ) {
            binding.completedImage.visibility = View.VISIBLE
            binding.completedBadge.visibility = View.VISIBLE

            MainScope().launch {
                val bitmap =
                    bitmapCache[item.artPiece.id]
                        ?: artService.loadArtBitmap(filename)?.also { bitmapCache[item.artPiece.id] = it }
                bitmap?.let { binding.completedImage.setImageBitmap(it) }
            }
        }

        private fun bindLocked(
            item: GalleryArtItem,
            filename: String,
        ) {
            binding.lockedOverlay.visibility = View.VISIBLE
            binding.lockedText.visibility = View.VISIBLE

            MainScope().launch {
                val bitmap =
                    bitmapCache[item.artPiece.id]
                        ?: artService.loadArtBitmap(filename)?.also { bitmapCache[item.artPiece.id] = it }
                bitmap?.let {
                    val blurred = blurBitmap(it)
                    binding.lockedImage.setImageBitmap(blurred)
                }
            }
        }

        private fun blurBitmap(
            src: Bitmap,
            scaleFactor: Float = 0.125f,
        ): Bitmap {
            val width = (src.width * scaleFactor).toInt().coerceAtLeast(1)
            val height = (src.height * scaleFactor).toInt().coerceAtLeast(1)
            val small = Bitmap.createScaledBitmap(src, width, height, true)
            return Bitmap.createScaledBitmap(small, src.width, src.height, true)
        }
    }
}
