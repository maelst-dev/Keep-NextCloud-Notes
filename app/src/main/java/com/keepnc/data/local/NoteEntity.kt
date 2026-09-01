package com.keepnc.data.local

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

/**
 * Room database entity representing one note.
 *
 * [id]         — local database ID (auto-generated)
 * [serverId]   — Nextcloud server ID; null until the note is first pushed to the server
 * [title]      — note title (may be empty)
 * [content]    — full note body in Markdown
 * [category]   — Nextcloud category string (empty = uncategorized)
 * [favorite]   — pinned/starred flag (like Google Keep's pin)
 * [modified]   — Unix timestamp (seconds) of last modification
 * [etag]       — server ETag for conflict detection
 * [syncStatus] — tracks whether the note needs syncing (see [SyncStatus])
 */
@Entity(tableName = "notes")
@TypeConverters(Converters::class)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: Long? = null,
    val title: String = "",
    val content: String = "",
    val category: String = "",
    val favorite: Boolean = false,
    val modified: Long = 0L,
    val etag: String? = null,
    val syncStatus: SyncStatus = SyncStatus.DIRTY
) {
    /**
     * Short preview text for the card grid. Not stored in the database.
     * Takes up to 10 full lines without cutting lines mid-sentence or mid-tag.
     * Android TextView handles layout truncating (maxLines=8, ellipsize=end).
     */
    @get:Ignore
    val excerpt: String get() {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return ""

        // Strip duplicate title from first line if present
        val text = if (title.isNotBlank() && trimmed.substringBefore('\n').trim() == title.trim()) {
            trimmed.substringAfter('\n', missingDelimiterValue = "").trimStart('\r', '\n')
        } else {
            trimmed
        }

        // Take up to 10 complete lines
        val lines = text.lines().take(10)
        val candidate = lines.joinToString("\n")

        // Cap at 1000 characters for safety on unusually large paragraphs
        return if (candidate.length <= 1000) {
            candidate
        } else {
            val sub = candidate.substring(0, 1000)
            val lastNewline = sub.lastIndexOf('\n')
            if (lastNewline > 100) {
                sub.substring(0, lastNewline)
            } else {
                sub.substringBeforeLast(' ')
            }
        }
    }
}
