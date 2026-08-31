package com.keepnc.data.remote.dto

/**
 * Mirrors the note object returned by Nextcloud Notes API v1.
 * See: https://github.com/nextcloud/notes/blob/main/docs/api/v1.md
 *
 * [id]       — server-side note ID
 * [etag]     — used for conditional requests / conflict detection
 * [readonly] — true if the note is from a shared read-only source
 * [modified] — Unix timestamp (seconds) of last modification on the server
 * [title]    — note title (server may auto-generate from content first line)
 * [category] — string category / label; empty string means uncategorized
 * [content]  — full note body (Markdown)
 * [favorite] — pinned flag
 */
data class NoteDto(
    val id: Long = 0L,
    val etag: String? = null,
    val readonly: Boolean = false,
    val modified: Long = 0L,
    val title: String? = "",
    val category: String? = "",
    val content: String? = "",
    val favorite: Boolean = false
)

/**
 * Request body for POST /notes — create a new note.
 */
data class NoteCreateRequest(
    val title: String,
    val content: String,
    val category: String,
    val favorite: Boolean
)

/**
 * Request body for PUT /notes/{id} — update an existing note.
 */
data class NoteUpdateRequest(
    val title: String,
    val content: String,
    val category: String,
    val favorite: Boolean
)
