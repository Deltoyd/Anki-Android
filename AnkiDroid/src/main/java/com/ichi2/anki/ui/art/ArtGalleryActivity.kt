package com.ichi2.anki.ui.art

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ichi2.anki.R
import com.ichi2.anki.model.art.ArtPiece
import com.ichi2.anki.model.art.UserArtGallery
import com.ichi2.anki.services.ArtAssetService
import com.ichi2.anki.services.ArtProgressRepository
import com.ichi2.anki.viewmodels.ArtProgressViewModelSimple
import com.ichi2.anki.viewmodels.ArtProgressViewModelSimpleFactory
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity displaying all completed art pieces in a gallery grid
 */
class ArtGalleryActivity : AppCompatActivity() {
    private val viewModel: ArtProgressViewModelSimple by viewModels {
        ArtProgressViewModelSimpleFactory(this)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArtGalleryAdapter
    private val repository by lazy { ArtProgressRepository(this) }
    private val artAssetService by lazy { ArtAssetService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_art_gallery)

        setupToolbar()
        setupRecyclerView()
        observeData()
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.art_gallery)
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        adapter =
            ArtGalleryAdapter(
                repository = repository,
                artAssetService = artAssetService,
            ) { galleryItem ->
                // TODO: Show art detail dialog
                Timber.d("Clicked gallery item: ${galleryItem.id}")
            }

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.completedGallery.collect { gallery ->
                    adapter.submitList(gallery)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

/**
 * RecyclerView adapter for art gallery grid
 */
class ArtGalleryAdapter(
    private val repository: ArtProgressRepository,
    private val artAssetService: ArtAssetService,
    private val onItemClick: (UserArtGallery) -> Unit,
) : RecyclerView.Adapter<ArtGalleryAdapter.ViewHolder>() {
    private var items = listOf<UserArtGallery>()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun submitList(newItems: List<UserArtGallery>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_art_gallery, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(items[position], repository, artAssetService, dateFormat, onItemClick)
    }

    override fun getItemCount() = items.size

    class ViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.art_image)
        private val titleText: TextView = itemView.findViewById(R.id.art_title)
        private val dateText: TextView = itemView.findViewById(R.id.completion_date)

        fun bind(
            item: UserArtGallery,
            repository: ArtProgressRepository,
            artAssetService: ArtAssetService,
            dateFormat: SimpleDateFormat,
            onItemClick: (UserArtGallery) -> Unit,
        ) {
            // Load art piece info from repository
            itemView.post {
                kotlinx.coroutines.MainScope().launch {
                    try {
                        val artPiece = repository.getArtPieceById(item.artPieceId)
                        artPiece?.let {
                            titleText.text = it.title

                            // Format completion date
                            val completedDate = Date(item.completedAt)
                            dateText.text =
                                itemView.context.getString(
                                    R.string.completed_at,
                                    dateFormat.format(completedDate),
                                )

                            // Load image from assets
                            val filename = artAssetService.extractFilenameFromUri(it.imageUrlFull)
                            val bitmap = artAssetService.loadArtBitmap(filename)
                            bitmap?.let { bmp ->
                                imageView.setImageBitmap(bmp)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load gallery item")
                    }
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
