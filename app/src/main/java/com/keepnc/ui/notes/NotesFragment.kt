package com.keepnc.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.keepnc.R
import com.keepnc.data.settings.SettingsStorage
import com.keepnc.databinding.FragmentNotesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The main notes list screen — a staggered 2-column grid of note cards.
 *
 * Receives a filter via Bundle arguments (set by [MainActivity] when the user
 * taps a drawer item). Defaults to showing all notes.
 *
 * BEGINNER NOTE: StaggeredGridLayoutManager lets cards have different heights
 * (unlike GridLayoutManager which forces equal-height rows) — this is what gives
 * Google Keep its characteristic look.
 */
@AndroidEntryPoint
class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels()

    @Inject
    lateinit var settingsStorage: SettingsStorage

    private lateinit var adapter: NoteCardAdapter

    // Tracks search input; we debounce it to avoid querying Room on every keystroke
    private val searchQuery = MutableStateFlow("")

    companion object {
        const val ARG_FILTER_TYPE = "filter_type"
        const val ARG_FILTER_VALUE = "filter_value"

        fun newInstance(filter: NotesFilter): NotesFragment {
            val (type, value) = when (filter) {
                is NotesFilter.All -> "all" to null
                is NotesFilter.Favorites -> "favorites" to null
                is NotesFilter.ByCategory -> "category" to filter.category
                is NotesFilter.Search -> "search" to filter.query
            }
            return NotesFragment().apply {
                arguments = bundleOf(ARG_FILTER_TYPE to type, ARG_FILTER_VALUE to value)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        setupSwipeRefresh()
        applyArgumentFilter()
        observeUiState()
        observeSyncState()
        observeCardFontSize()
        setupSearchDebounce()
        setupMenu()
    }

    private fun setupRecyclerView() {
        adapter = NoteCardAdapter(
            onNoteClick = { note ->
                findNavController().navigate(
                    R.id.action_notesFragment_to_editorFragment,
                    bundleOf("note_id" to note.id)
                )
            },
            onNoteLongClick = { note ->
                showDeleteDialog(note.id, note.title)
                true
            },
            cardFontSize = settingsStorage.getCardFontSize()
        )

        binding.recyclerView.layoutManager = StaggeredGridLayoutManager(
            2, StaggeredGridLayoutManager.VERTICAL
        )
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            // Navigate to editor with no note ID (creates a new note)
            findNavController().navigate(
                R.id.action_notesFragment_to_editorFragment,
                bundleOf("note_id" to -1L)
            )
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.syncNotes()
        }
    }

    private fun applyArgumentFilter() {
        val type = arguments?.getString(ARG_FILTER_TYPE) ?: "all"
        val value = arguments?.getString(ARG_FILTER_VALUE)
        val filter = when (type) {
            "favorites" -> NotesFilter.Favorites
            "category" -> NotesFilter.ByCategory(value ?: "")
            else -> NotesFilter.All
        }
        viewModel.setFilter(filter)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is NotesUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                            binding.tvEmpty.visibility = View.GONE
                        }
                        is NotesUiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.GONE
                            binding.tvEmpty.visibility = View.VISIBLE
                            binding.tvEmpty.text = getEmptyMessage()
                        }
                        is NotesUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvEmpty.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            adapter.submitList(state.notes)
                        }
                        is NotesUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun observeSyncState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isSyncing.collect { isSyncing ->
                    binding.swipeRefresh.isRefreshing = isSyncing
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.syncError.collect { errorMsg ->
                    Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun observeCardFontSize() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsStorage.cardFontSize.collect { preset ->
                    if (adapter.cardFontSize != preset) {
                        adapter.cardFontSize = preset
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchQuery
                    .debounce(300)
                    .collectLatest { query ->
                        if (query.isBlank()) {
                            viewModel.setFilter(NotesFilter.All)
                        } else {
                            viewModel.setFilter(NotesFilter.Search(query))
                        }
                    }
            }
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar_notes_menu, menu)

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem?.actionView as? SearchView
                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?) = false
                    override fun onQueryTextChange(newText: String?): Boolean {
                        searchQuery.value = newText ?: ""
                        return true
                    }
                })
                searchView?.setOnCloseListener {
                    searchQuery.value = ""
                    false
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_sync -> {
                        viewModel.syncNotes()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showDeleteDialog(noteId: Long, title: String) {
        if (settingsStorage.isConfirmDeleteNote()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_note_title)
                .setMessage(R.string.delete_note_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    viewModel.deleteNote(noteId)
                }
                .show()
        } else {
            viewModel.deleteNote(noteId)
        }
    }

    private fun getEmptyMessage(): String {
        return when (val filter = viewModel.currentFilter.value) {
            is NotesFilter.Favorites -> getString(R.string.notes_empty_favorites)
            is NotesFilter.ByCategory -> getString(R.string.notes_empty_category)
            is NotesFilter.Search -> getString(R.string.notes_empty_search)
            else -> getString(R.string.notes_empty)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // prevent memory leaks — binding holds a reference to the View
    }
}
