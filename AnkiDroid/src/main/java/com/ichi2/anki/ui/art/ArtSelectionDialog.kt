package com.ichi2.anki.ui.art

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ichi2.anki.R
import com.ichi2.anki.model.art.ArtPiece

/**
 * Bottom sheet dialog for selecting which art piece to work on
 */
class ArtSelectionDialog(
    context: Context,
    private val availableArtPieces: List<ArtPiece>,
    private val onArtSelected: (ArtPiece) -> Unit,
) : BottomSheetDialog(context) {
    init {
        setContentView(R.layout.dialog_art_selection)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.art_selection_recycler)
        recyclerView?.layoutManager = GridLayoutManager(context, 2)
        recyclerView?.adapter =
            ArtSelectionAdapter(availableArtPieces) { artPiece ->
                onArtSelected(artPiece)
                dismiss()
            }
    }
}

/**
 * RecyclerView adapter for art piece selection grid
 */
class ArtSelectionAdapter(
    private val artPieces: List<ArtPiece>,
    private val onItemClick: (ArtPiece) -> Unit,
) : RecyclerView.Adapter<ArtSelectionAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_art_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(artPieces[position], onItemClick)
    }

    override fun getItemCount() = artPieces.size

    class ViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.art_thumbnail)
        private val titleText: TextView = itemView.findViewById(R.id.art_title)

        fun bind(
            artPiece: ArtPiece,
            onItemClick: (ArtPiece) -> Unit,
        ) {
            titleText.text = artPiece.title

            // Load thumbnail from assets
            try {
                val filename = artPiece.imageUrlFull.substringAfter("asset://art/")
                itemView.context.assets.open("art/$filename").use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // Handle error - could set placeholder image
            }

            itemView.setOnClickListener { onItemClick(artPiece) }
        }
    }
}
