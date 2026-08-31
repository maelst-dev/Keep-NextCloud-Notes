package com.keepnc.ui.notes

/**
 * Describes which subset of notes to display.
 * Passed as a Bundle argument to [NotesFragment].
 */
sealed class NotesFilter {
    /** Show all non-deleted notes. */
    object All : NotesFilter()

    /** Show only pinned (favorite) notes. */
    object Favorites : NotesFilter()

    /** Show only notes in a specific category. */
    data class ByCategory(val category: String) : NotesFilter()

    /** Show notes matching a search query. */
    data class Search(val query: String) : NotesFilter()
}
