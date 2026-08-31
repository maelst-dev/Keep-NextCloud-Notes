package com.keepnc.ui.notes

import com.keepnc.data.local.NoteEntity

/**
 * UI state for the notes list screen.
 *
 * Using a sealed class ensures every possible state is handled explicitly in the UI.
 * This pattern eliminates a whole class of bugs where the UI shows stale content
 * because a developer forgot to reset a loading spinner or error message.
 */
sealed class NotesUiState {
    /** Loading from Room for the first time. */
    object Loading : NotesUiState()

    /** No notes match the current filter. */
    object Empty : NotesUiState()

    /** Notes loaded successfully. */
    data class Success(val notes: List<NoteEntity>) : NotesUiState()

    /** An error occurred (e.g., database error). */
    data class Error(val message: String) : NotesUiState()
}
