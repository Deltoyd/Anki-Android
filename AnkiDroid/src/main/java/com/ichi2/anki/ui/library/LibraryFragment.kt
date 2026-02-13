package com.ichi2.anki.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ichi2.anki.R
import com.ichi2.anki.Reviewer
import com.ichi2.anki.StudyOptionsActivity
import com.ichi2.anki.databinding.FragmentLibraryBinding
import com.ichi2.anki.dialogs.CreateDeckDialog
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

class LibraryFragment : Fragment(R.layout.fragment_library) {
    private val binding by viewBinding(FragmentLibraryBinding::bind)
    private val viewModel: LibraryViewModel by viewModels()
    private lateinit var adapter: DeckListAdapter

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        setupObservers()

        viewModel.loadDecks()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDecks()
    }

    private fun setupRecyclerView() {
        adapter =
            DeckListAdapter { deck ->
                viewModel.selectDeck(deck.id)
                startActivity(Intent(requireContext(), Reviewer::class.java))
            }

        binding.deckList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LibraryFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupButtons() {
        binding.editButton.setOnClickListener {
            showEditOptions()
        }

        binding.fabAdd.setOnClickListener {
            showAddOptions()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submitList(state.decks)

                binding.deckList.isVisible = state.decks.isNotEmpty()
                binding.emptyState.isVisible = state.decks.isEmpty() && !state.isLoading
            }
        }
    }

    private fun showEditOptions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Library")
            .setItems(arrayOf("Manage Decks", "Check Database", "Check Media")) { _, which ->
                when (which) {
                    0 -> {
                        // Navigate to deck management
                        startActivity(Intent(requireContext(), com.ichi2.anki.DeckPicker::class.java))
                    }
                    1 -> {
                        // Check database
                    }
                    2 -> {
                        // Check media
                    }
                }
            }.show()
    }

    private fun showAddOptions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add")
            .setItems(
                arrayOf(
                    getString(R.string.library_create_deck),
                    getString(R.string.library_import_deck),
                ),
            ) { _, which ->
                when (which) {
                    0 -> showCreateDeckDialog()
                    1 -> {
                        // Import deck - navigate to existing import flow
                        startActivity(Intent(requireContext(), com.ichi2.anki.SharedDecksActivity::class.java))
                    }
                }
            }.show()
    }

    private fun showCreateDeckDialog() {
        val dialog =
            CreateDeckDialog(
                requireContext(),
                R.string.library_create_deck,
                CreateDeckDialog.DeckDialogType.DECK,
                null,
            )
        dialog.onNewDeckCreated = {
            viewModel.loadDecks()
        }
        dialog.showDialog()
    }
}
