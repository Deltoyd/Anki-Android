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
    private val onChangeClick: (() -> Unit)? = null,
) : RecyclerView.Adapter<GalleryPagerAdapter.GalleryViewHolder>() {
    private var items: List<GalleryArtItem> = emptyList()
    private val bitmapCache = mutableMapOf<String, Bitmap?>()

    companion object {
        private const val MULTIPLIER = 1000
    }

    fun submitList(newItems: List<GalleryArtItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateActivePainting(painting: Bitmap?) {
        // Active painting is always at real index 0 after filtering
        if (items.isNotEmpty() && painting != null) {
            val artPiece = items[0].artPiece
            bitmapCache[artPiece.id] = painting
            notifyDataSetChanged()
        }
    }

    fun getRealPosition(virtualPosition: Int): Int = if (items.isEmpty()) 0 else virtualPosition % items.size

    fun getStartPosition(): Int = if (items.isEmpty()) 0 else (MULTIPLIER / 2) * items.size

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
        holder.bind(items[position % items.size])
    }

    override fun getItemCount() = if (items.isEmpty()) 0 else items.size * MULTIPLIER

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
            binding.changeMasterpieceButton.visibility = View.VISIBLE

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

            binding.changeMasterpieceButton.setOnClickListener {
                onChangeClick?.invoke()
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
