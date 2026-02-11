package com.ichi2.anki.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.R
import com.ichi2.anki.databinding.ActivityTopicSelectionBinding
import com.ichi2.anki.databinding.ItemTopicBinding
import com.ichi2.anki.libanki.DeckNameId
import com.ichi2.anki.ui.museum.MuseumPersistence
import kotlinx.coroutines.launch

class TopicSelectionActivity : AnkiActivity() {
    private lateinit var binding: ActivityTopicSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topicList.layoutManager = LinearLayoutManager(this)

        loadDecks()
    }

    private fun loadDecks() {
        lifecycleScope.launch {
            val decks = withCol { decks.allNamesAndIds(includeFiltered = false) }
            binding.topicList.adapter =
                TopicAdapter(decks) { deck ->
                    onTopicSelected(deck)
                }
        }
    }

    private fun onTopicSelected(deck: DeckNameId) {
        lifecycleScope.launch {
            MuseumPersistence.setSelectedTopicId(this@TopicSelectionActivity, deck.id)
            MuseumPersistence.setSelectedDeckId(this@TopicSelectionActivity, deck.id)
            withCol { decks.select(deck.id) }
            startActivity(Intent(this@TopicSelectionActivity, ArtSelectionActivity::class.java))
        }
    }
}

private class TopicAdapter(
    private val decks: List<DeckNameId>,
    private val onSelect: (DeckNameId) -> Unit,
) : RecyclerView.Adapter<TopicAdapter.ViewHolder>() {
    class ViewHolder(
        val binding: ItemTopicBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding = ItemTopicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val deck = decks[position]
        holder.binding.topicName.text = deck.name
        holder.binding.root.setOnClickListener { onSelect(deck) }
    }

    override fun getItemCount() = decks.size
}
