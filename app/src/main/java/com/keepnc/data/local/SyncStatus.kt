package com.keepnc.data.local

/**
 * Tracks whether a note has been synced with the Nextcloud server.
 *
 * - SYNCED: note is identical to what the server has
 * - DIRTY: note was edited locally and needs to be pushed (optimistic update)
 * - PENDING_DELETE: note was deleted locally; if it has a serverId, delete it on server too
 */
enum class SyncStatus {
    SYNCED,
    DIRTY,
    PENDING_DELETE
}
