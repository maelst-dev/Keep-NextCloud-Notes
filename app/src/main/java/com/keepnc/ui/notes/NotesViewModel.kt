package com.keepnc.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepnc.data.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the notes list screen.
 */
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository
) : ViewModel() {

    private val _filter = MutableStateFlow<NotesFilter>(NotesFilter.All)
    val currentFilter: StateFlow<NotesFilter> = _filter.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableSharedFlow<String>()
    val syncError: SharedFlow<String> = _syncError.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NotesUiState> = _filter
        .flatMapLatest { filter ->
            when (filter) {
                is NotesFilter.All -> repository.getAllNotes()
                is NotesFilter.Favorites -> repository.getFavorites()
                is NotesFilter.ByCategory -> repository.getNotesByCategory(filter.category)
                is NotesFilter.Search -> repository.searchNotes(filter.query)
            }
        }
        .map { notes ->
            if (notes.isEmpty()) NotesUiState.Empty
            else NotesUiState.Success(notes)
        }
        .catch { e ->
            emit(NotesUiState.Error(e.message ?: "Unknown error"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotesUiState.Loading
        )

    fun setFilter(filter: NotesFilter) {
        _filter.value = filter
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            // Trigger sync to push deletion to server
            syncNotes()
        }
    }

    /**
     * Executes foreground sync directly via repository.
     * Updates [isSyncing] and emits to [syncError] on failure so the UI responds immediately.
     */
    fun syncNotes() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            repository.syncWithServer().fold(
                onSuccess = {
                    // Sync complete; Room Flow will auto-update the UI
                },
                onFailure = { error ->
                    _syncError.emit(error.message ?: "Sync error")
                }
            )
            _isSyncing.value = false
        }
    }

    init {
        // Automatically sync when the notes list screen is opened
        syncNotes()
    }
}
