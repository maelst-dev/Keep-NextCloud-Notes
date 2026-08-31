package com.keepnc.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepnc.data.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the note editor.
 *
 * Holds the current note data as mutable state so it survives rotation.
 * The Fragment reads from these fields when setting up the UI.
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Loading)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // Holds the local DB id of the note being edited; null means new note
    private var noteId: Long? = null

    // Initial snapshot of the note when loaded — used to detect unsaved changes
    private var initialTitle: String = ""
    private var initialContent: String = ""
    private var initialCategory: String = ""
    private var initialFavorite: Boolean = false

    // Mutable note fields — the Fragment reads these to populate its views
    val title = MutableStateFlow("")
    val content = MutableStateFlow("")
    val category = MutableStateFlow("")
    val isFavorite = MutableStateFlow(false)

    /** Live list of all distinct categories in the database. */
    val allCategories: StateFlow<List<String>> = repository.getCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Call once from [EditorFragment.onViewCreated].
     * If [id] is null or -1, sets up a blank new note.
     * Otherwise, loads the existing note from Room.
     */
    fun loadNote(id: Long?) {
        _uiState.value = EditorUiState.Loading

        if (id == null || id == -1L) {
            // New note — start editing immediately
            noteId = null
            initialTitle = ""
            initialContent = ""
            initialCategory = ""
            initialFavorite = false
            title.value = ""
            content.value = ""
            category.value = ""
            isFavorite.value = false
            _uiState.value = EditorUiState.Editing
            return
        }

        viewModelScope.launch {
            try {
                val note = repository.getNoteById(id)
                if (note != null) {
                    noteId = note.id

                    val titleTrimmed = note.title.trim()
                    val firstLine = note.content.substringBefore('\n').trim()
                    val shouldStripTitle = titleTrimmed.isNotBlank() && firstLine == titleTrimmed

                    val finalContent = if (shouldStripTitle) {
                        note.content.substringAfter('\n', missingDelimiterValue = "").trimStart('\r', '\n')
                    } else {
                        note.content
                    }

                    initialTitle = note.title
                    initialContent = finalContent
                    initialCategory = note.category
                    initialFavorite = note.favorite

                    title.value = note.title
                    content.value = finalContent
                    category.value = note.category
                    isFavorite.value = note.favorite

                    if (shouldStripTitle) {
                        repository.updateNote(note.id, note.title, finalContent, note.category, note.favorite)
                        repository.syncWithServer()
                    }

                    _uiState.value = EditorUiState.Editing
                } else {
                    _uiState.value = EditorUiState.Error(messageRes = com.keepnc.R.string.editor_note_not_found)
                }
            } catch (e: Exception) {
                _uiState.value = EditorUiState.Error(message = e.message)
            }
        }
    }

    /** Updates the selected category for this note. */
    fun setCategory(newCategory: String) {
        category.value = newCategory.trim()
    }

    /**
     * Checks if current field values differ from the initially loaded note state.
     */
    fun hasChanges(
        currentTitle: String,
        currentContent: String,
        currentCategory: String,
        currentFavorite: Boolean
    ): Boolean {
        // If it's a new blank note and nothing was entered, no changes
        if (noteId == null &&
            currentTitle.isBlank() &&
            currentContent.isBlank() &&
            currentCategory.isBlank() &&
            !currentFavorite
        ) {
            return false
        }
        return currentTitle != initialTitle ||
                currentContent != initialContent ||
                currentCategory != initialCategory ||
                currentFavorite != initialFavorite
    }

    /**
     * Saves the note (create or update), immediately launches server sync,
     * and transitions to [EditorUiState.Saved].
     */
    fun saveNote(
        titleText: String,
        contentText: String,
        categoryText: String,
        favorite: Boolean
    ) {
        // Don't save completely empty new notes
        if (titleText.isBlank() && contentText.isBlank()) {
            _uiState.value = EditorUiState.Saved
            return
        }

        viewModelScope.launch {
            try {
                val currentId = noteId
                if (currentId == null) {
                    repository.createNote(titleText, contentText, categoryText, favorite)
                } else {
                    repository.updateNote(currentId, titleText, contentText, categoryText, favorite)
                }
                // Immediately sync with the server after saving
                repository.syncWithServer()
                _uiState.value = EditorUiState.Saved
            } catch (e: Exception) {
                _uiState.value = EditorUiState.Error(e.message ?: "Failed to save note")
            }
        }
    }

    /**
     * Deletes the note (if already saved), launches server sync,
     * and transitions to [EditorUiState.Saved] to close the screen.
     */
    fun deleteNote() {
        val currentId = noteId
        if (currentId == null) {
            // Unsaved new note — simply exit
            _uiState.value = EditorUiState.Saved
            return
        }

        viewModelScope.launch {
            try {
                repository.deleteNote(currentId)
                repository.syncWithServer()
                _uiState.value = EditorUiState.Saved
            } catch (e: Exception) {
                _uiState.value = EditorUiState.Error(e.message ?: "Failed to delete note")
            }
        }
    }

    fun toggleFavorite() {
        isFavorite.value = !isFavorite.value
    }
}
