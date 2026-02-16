package com.ichi2.anki.ui.museum

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ichi2.anki.databinding.ItemGalleryCompletedPageBinding
import com.ichi2.anki.model.art.ArtPiece
import com.ichi2.anki.model.art.UserArtGallery
import com.ichi2.anki.services.ArtAssetService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

data class CompletedGalleryItem(
    val artPiece: ArtPiece,
    val galleryEntry: UserArtGallery,
)

class CompletedGalleryPagerAdapter(
    private val artService: ArtAssetService,
) : RecyclerView.Adapter<CompletedGalleryPagerAdapter.CompletedViewHolder>() {
    private var items: List<CompletedGalleryItem> = emptyList()
    private val bitmapCache = mutableMapOf<String, Bitmap?>()

    fun submitList(newItems: List<CompletedGalleryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getItem(position: Int): CompletedGalleryItem? = items.getOrNull(position)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CompletedViewHolder {
        val binding =
            ItemGalleryCompletedPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return CompletedViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CompletedViewHolder,
        position: Int,
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class CompletedViewHolder(
        private val binding: ItemGalleryCompletedPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CompletedGalleryItem) {
            val filename = artService.extractFilenameFromUri(item.artPiece.imageUrlFull)

            val cached = bitmapCache[item.artPiece.id]
            if (cached != null) {
                binding.completedImage.setImageBitmap(cached)
            } else {
                MainScope().launch {
                    val bitmap = artService.loadArtBitmap(filename)
                    if (bitmap != null) {
                        bitmapCache[item.artPiece.id] = bitmap
                        binding.completedImage.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }
}
