package com.keepnc.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for notes.
 *
 * Functions returning [Flow] are observed by the UI via ViewModel —
 * Room automatically re-emits whenever the underlying data changes.
 *
 * Functions marked `suspend` are called from coroutines on Dispatchers.IO.
 */
@Dao
interface NoteDao {

    /** All non-deleted notes, pinned first, then by last-modified descending. */
    @Query("SELECT * FROM notes WHERE syncStatus != 'PENDING_DELETE' ORDER BY favorite DESC, modified DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    /** Notes filtered by category. */
    @Query("SELECT * FROM notes WHERE syncStatus != 'PENDING_DELETE' AND category = :category ORDER BY favorite DESC, modified DESC")
    fun getNotesByCategory(category: String): Flow<List<NoteEntity>>

    /** Only pinned (favorite) notes. */
    @Query("SELECT * FROM notes WHERE syncStatus != 'PENDING_DELETE' AND favorite = 1 ORDER BY modified DESC")
    fun getFavorites(): Flow<List<NoteEntity>>

    /** Full-text search across title and content. */
    @Query("SELECT * FROM notes WHERE syncStatus != 'PENDING_DELETE' AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY modified DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    /** Distinct non-empty categories, sorted alphabetically — used to populate the drawer. */
    @Query("SELECT DISTINCT category FROM notes WHERE syncStatus != 'PENDING_DELETE' AND category != '' ORDER BY category")
    fun getCategories(): Flow<List<String>>

    /** Look up all local notes with a given server-side ID (used for deduplication during sync). */
    @Query("SELECT * FROM notes WHERE serverId = :serverId ORDER BY id ASC")
    suspend fun getNotesByServerId(serverId: Long): List<NoteEntity>

    /** Look up a note by its server-side ID (used during sync). */
    @Query("SELECT * FROM notes WHERE serverId = :serverId LIMIT 1")
    suspend fun getNoteByServerId(serverId: Long): NoteEntity?

    /** Look up a note by its local Room ID. */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    /** Notes that were edited locally and haven't been pushed yet. */
    @Query("SELECT * FROM notes WHERE syncStatus = 'DIRTY'")
    suspend fun getDirtyNotes(): List<NoteEntity>

    /** Notes marked for deletion that exist on the server. */
    @Query("SELECT * FROM notes WHERE syncStatus = 'PENDING_DELETE' AND serverId IS NOT NULL")
    suspend fun getPendingDeleteNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Clean up notes that were created locally, never pushed, and are now marked for deletion. */
    @Query("DELETE FROM notes WHERE serverId IS NULL AND syncStatus = 'PENDING_DELETE'")
    suspend fun deletePureLocalPendingDeletes()

    /**
     * Deletes all SYNCED notes whose [serverId] is NOT in [serverIds].
     * Called during sync to remove notes the user deleted on the server.
     *
     * We only touch SYNCED notes — DIRTY notes (locally edited) are intentionally
     * left alone; they'll be pushed to the server in the next sync cycle.
     *
     * Room supports IN/NOT IN with a List/Set parameter automatically.
     * The [serverIds] list must be non-empty; use [deleteAllSynced] when the
     * server returns an empty list.
     */
    @Query("DELETE FROM notes WHERE syncStatus = 'SYNCED' AND serverId IS NOT NULL AND serverId NOT IN (:serverIds)")
    suspend fun deleteSyncedNotesNotIn(serverIds: List<Long>)

    /**
     * Deletes ALL SYNCED notes. Used when the server returns an empty note list
     * (user deleted everything remotely). DIRTY notes are preserved so they can
     * still be pushed.
     */
    @Query("DELETE FROM notes WHERE syncStatus = 'SYNCED'")
    suspend fun deleteAllSynced()

    /**
     * Deletes ALL notes from the database. Used when logging out or switching accounts.
     */
    @Query("DELETE FROM notes")
    suspend fun clearAll()
}
