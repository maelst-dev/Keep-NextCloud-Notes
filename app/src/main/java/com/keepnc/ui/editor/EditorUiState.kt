package com.keepnc.ui.editor

import androidx.annotation.StringRes

/**
 * UI state for the note editor screen.
 */
sealed class EditorUiState {
    /** Loading the note from Room (only for existing notes). */
    object Loading : EditorUiState()

    /** Note data is loaded and the editor is active. */
    object Editing : EditorUiState()

    /** Note was saved — fragment should navigate back. */
    object Saved : EditorUiState()

    /** An error occurred during load or save. */
    data class Error(
        val message: String? = null,
        @StringRes val messageRes: Int? = null
    ) : EditorUiState()
}
