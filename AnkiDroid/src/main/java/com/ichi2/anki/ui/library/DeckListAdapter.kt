package com.ichi2.anki.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ichi2.anki.R

class DeckListAdapter(
    private val onDeckClick: (DeckItem) -> Unit,
) : ListAdapter<DeckItem, DeckListAdapter.DeckViewHolder>(DeckDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DeckViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_deck_row, parent, false)
        return DeckViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: DeckViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position), onDeckClick)
    }

    class DeckViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val deckName: TextView = itemView.findViewById(R.id.deckName)
        private val deckDueCount: TextView = itemView.findViewById(R.id.deckDueCount)

        fun bind(
            item: DeckItem,
            onDeckClick: (DeckItem) -> Unit,
        ) {
            deckName.text = item.name
            deckDueCount.text = itemView.context.getString(R.string.library_cards_due, item.dueCount)

            itemView.setOnClickListener { onDeckClick(item) }
        }
    }

    class DeckDiffCallback : DiffUtil.ItemCallback<DeckItem>() {
        override fun areItemsTheSame(
            oldItem: DeckItem,
            newItem: DeckItem,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: DeckItem,
            newItem: DeckItem,
        ): Boolean = oldItem == newItem
    }
}
