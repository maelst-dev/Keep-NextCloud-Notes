package com.keepnc.data.repository

import android.util.Log
import com.keepnc.data.local.NoteDao
import com.keepnc.data.local.NoteEntity
import com.keepnc.data.local.SyncStatus
import com.keepnc.data.remote.NotesApi
import com.keepnc.data.remote.dto.NoteCreateRequest
import com.keepnc.data.remote.dto.NoteDto
import com.keepnc.data.remote.dto.NoteUpdateRequest
import com.keepnc.data.auth.TokenStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for note data.
 *
 * ARCHITECTURE RULE: The UI layer NEVER calls Retrofit directly.
 * It always goes through this repository, which:
 * 1. Returns local (Room) data immediately as a [Flow] — UI is always responsive
 * 2. Syncs with the Nextcloud server in the background via [syncWithServer]
 * 3. Writes to Room immediately on local edits (optimistic updates)
 *
 * This is the "offline-first" pattern: the app works without internet;
 * changes queue up and sync when the network is available.
 */
@Singleton
class NotesRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val notesApi: NotesApi,
    private val tokenStorage: TokenStorage
) {
    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val syncMutex = Mutex()
    private val tag = "NotesRepository"

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // -----------------------------------------------------------------------
    // Read operations — return Flows that Room updates automatically
    // -----------------------------------------------------------------------

    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getNotesByCategory(category: String): Flow<List<NoteEntity>> =
        noteDao.getNotesByCategory(category)

    fun getFavorites(): Flow<List<NoteEntity>> = noteDao.getFavorites()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    fun getCategories(): Flow<List<String>> = noteDao.getCategories()

    suspend fun getNoteById(id: Long): NoteEntity? = withContext(ioDispatcher) {
        noteDao.getNoteById(id)
    }

    // -----------------------------------------------------------------------
    // Write operations — write to Room first (optimistic), sync later
    // -----------------------------------------------------------------------

    /**
     * Creates a note locally. The note is saved immediately with [SyncStatus.DIRTY]
     * so [SyncWorker] will push it to the server in the background.
     * Returns the new local database ID.
     */
    suspend fun createNote(
        title: String,
        content: String,
        category: String,
        favorite: Boolean
    ): Long = withContext(ioDispatcher) {
        val entity = NoteEntity(
            title = title,
            content = content,
            category = category,
            favorite = favorite,
            modified = System.currentTimeMillis() / 1000,
            syncStatus = SyncStatus.DIRTY
        )
        noteDao.insert(entity)
    }

    /**
     * Updates a note locally and marks it as [SyncStatus.DIRTY].
     */
    suspend fun updateNote(
        id: Long,
        title: String,
        content: String,
        category: String,
        favorite: Boolean
    ) = withContext(ioDispatcher) {
        val existing = noteDao.getNoteById(id) ?: return@withContext
        noteDao.update(
            existing.copy(
                title = title,
                content = content,
                category = category,
                favorite = favorite,
                modified = System.currentTimeMillis() / 1000,
                syncStatus = SyncStatus.DIRTY
            )
        )
    }

    /**
     * Marks a note for deletion.
     * - If the note was never synced (no serverId), delete it immediately.
     * - Otherwise, mark as [SyncStatus.PENDING_DELETE] so the worker can call DELETE on the API.
     */
    suspend fun deleteNote(id: Long) = withContext(ioDispatcher) {
        val existing = noteDao.getNoteById(id) ?: return@withContext
        if (existing.serverId == null) {
            noteDao.deleteById(id)
        } else {
            noteDao.update(existing.copy(syncStatus = SyncStatus.PENDING_DELETE))
        }
    }

    // -----------------------------------------------------------------------
    // Sync — called by ViewModel and SyncWorker
    // -----------------------------------------------------------------------

    /**
     * Full sync cycle:
     * 1. Push dirty (locally edited) notes to the server
     * 2. Push pending deletes
     * 3. Fetch all notes from the server and update local cache
     * 4. Delete local notes that were removed on the server
     *
     * Returns [Result.success] on completion, [Result.failure] on any error.
     */
    suspend fun syncWithServer(): Result<Unit> = withContext(ioDispatcher) {
        syncMutex.withLock {
            _isSyncing.value = true
            try {
                runCatching {
                    Log.d(tag, "Starting syncWithServer...")

                    // Step 1: Push dirty notes
                    val dirtyNotes = noteDao.getDirtyNotes()
                    Log.d(tag, "Pushing ${dirtyNotes.size} dirty notes...")
                    for (note in dirtyNotes) {
                        try {
                            if (note.serverId == null) {
                                // New note: create on server
                                val dto = notesApi.createNote(
                                    NoteCreateRequest(note.title, note.content, note.category, note.favorite)
                                )
                                noteDao.update(
                                    note.copy(
                                        serverId = dto.id,
                                        etag = dto.etag,
                                        modified = dto.modified,
                                        syncStatus = SyncStatus.SYNCED
                                    )
                                )
                            } else {
                                // Existing note: update on server
                                val dto = notesApi.updateNote(
                                    note.serverId,
                                    NoteUpdateRequest(note.title, note.content, note.category, note.favorite)
                                )
                                noteDao.update(
                                    note.copy(
                                        etag = dto.etag,
                                        modified = dto.modified,
                                        syncStatus = SyncStatus.SYNCED
                                    )
                                )
                            }
                        } catch (e: HttpException) {
                            if (e.code() == 404 && note.serverId != null) {
                                // Note was deleted on server while we had local edits. Re-create on server
                                val dto = notesApi.createNote(
                                    NoteCreateRequest(note.title, note.content, note.category, note.favorite)
                                )
                                noteDao.update(
                                    note.copy(
                                        serverId = dto.id,
                                        etag = dto.etag,
                                        modified = dto.modified,
                                        syncStatus = SyncStatus.SYNCED
                                    )
                                )
                            } else {
                                Log.e(tag, "Failed to push note ${note.id}: ${e.message}")
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to push note ${note.id}: ${e.message}")
                        }
                    }

                    // Step 2: Push pending deletes
                    val pendingDeletes = noteDao.getPendingDeleteNotes()
                    Log.d(tag, "Pushing ${pendingDeletes.size} pending deletes...")
                    for (note in pendingDeletes) {
                        try {
                            if (note.serverId != null) {
                                notesApi.deleteNote(note.serverId)
                            }
                            noteDao.delete(note)
                        } catch (e: HttpException) {
                            if (e.code() == 404) {
                                // Already gone from server; remove local row
                                noteDao.delete(note)
                            } else {
                                Log.e(tag, "Failed to delete note ${note.id} on server: ${e.message}")
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to delete note ${note.id} on server: ${e.message}")
                        }
                    }
                    // Clean up purely local notes marked for deletion that were never pushed to server
                    noteDao.deletePureLocalPendingDeletes()

                    // Step 3: Fetch all notes from server
                    Log.d(tag, "Fetching all notes from server...")
                    val serverNotes = notesApi.getNotes()
                    Log.d(tag, "Received ${serverNotes.size} notes from server.")

                    for (dto in serverNotes) {
                        val matchingNotes = noteDao.getNotesByServerId(dto.id)
                        if (matchingNotes.isNotEmpty()) {
                            // Local deduplication: keep the first note as primary, delete any duplicates
                            val primary = matchingNotes.first()
                            for (duplicate in matchingNotes.drop(1)) {
                                Log.w(tag, "Removing duplicate local note id=${duplicate.id} for serverId=${dto.id}")
                                noteDao.deleteById(duplicate.id)
                            }

                            if (primary.syncStatus == SyncStatus.SYNCED) {
                                // Server wins if newer
                                if (serverIsNewer(dto, primary)) {
                                    noteDao.update(
                                        primary.copy(
                                            title = dto.title ?: "",
                                            content = dto.content ?: "",
                                            category = dto.category ?: "",
                                            favorite = dto.favorite,
                                            etag = dto.etag,
                                            modified = dto.modified
                                        )
                                    )
                                }
                            }
                            // If local is DIRTY, we already pushed it in Step 1 or will push next time
                        } else {
                            // Note doesn't exist locally: insert it
                            noteDao.insert(
                                NoteEntity(
                                    serverId = dto.id,
                                    title = dto.title ?: "",
                                    content = dto.content ?: "",
                                    category = dto.category ?: "",
                                    favorite = dto.favorite,
                                    etag = dto.etag,
                                    modified = dto.modified,
                                    syncStatus = SyncStatus.SYNCED
                                )
                            )
                        }
                    }

                    // Step 4: Server-side deletions
                    // Delete local SYNCED notes that no longer exist on the server
                    val serverIds = serverNotes.map { it.id }
                    if (serverIds.isEmpty()) {
                        // Server is completely empty — remove all synced notes
                        noteDao.deleteAllSynced()
                    } else {
                        noteDao.deleteSyncedNotesNotIn(serverIds)
                    }
                    Log.d(tag, "Sync completed successfully.")
                    Unit
                }.onFailure { e ->
                    Log.e(tag, "Sync failed: ${e.message}", e)
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Clears all local notes from the Room database.
     * Used on logout or account switch.
     */
    suspend fun clearAllLocalNotes() = withContext(ioDispatcher) {
        syncMutex.withLock {
            noteDao.clearAll()
        }
    }

    /**
     * Returns `true` when the server version of a note differs from the local copy.
     *
     * Priority:
     * 1. Both sides have an etag → compare etags (changes for any field edit)
     * 2. Otherwise → fall back to `modified` timestamp
     */
    private fun serverIsNewer(dto: NoteDto, local: NoteEntity): Boolean =
        if (dto.etag != null && local.etag != null) {
            dto.etag != local.etag
        } else {
            dto.modified > local.modified
        }
}

// ---------------------------------------------------------------------------
// Extension function (defined at file level for clarity)
// ---------------------------------------------------------------------------

/** Converts a server [NoteDto] to a local [NoteEntity] with SYNCED status. */
fun NoteDto.toEntity() = NoteEntity(
    serverId = id,
    title = title ?: "",
    content = content ?: "",
    category = category ?: "",
    favorite = favorite,
    modified = modified,
    etag = etag,
    syncStatus = SyncStatus.SYNCED
)
