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
     * Strips leading/trailing whitespace and caps at 200 characters.
     */
    @Ignore
    val excerpt: String = content.trim().take(200)
}
