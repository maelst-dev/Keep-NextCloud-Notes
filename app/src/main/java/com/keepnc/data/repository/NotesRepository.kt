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
                            throw e
                        }
                    }
                }

                // Step 2: Push pending deletes
                val pendingDeletes = noteDao.getPendingDeleteNotes()
                Log.d(tag, "Pushing ${pendingDeletes.size} pending deletes...")
                for (note in pendingDeletes) {
                    try {
                        note.serverId?.let { notesApi.deleteNote(it) }
                    } catch (e: HttpException) {
                        // 404 means already deleted on server, which is expected/safe
                        if (e.code() != 404) throw e
                    }
                    noteDao.delete(note)
                }
                // Remove local-only notes marked for deletion (serverId == null)
                noteDao.deletePureLocalPendingDeletes()

                // Step 3: Pull from server — insert new notes, update changed ones, clean up duplicates
                Log.d(tag, "Fetching all notes from server...")
                val serverNotes = notesApi.getNotes()
                Log.d(tag, "Received ${serverNotes.size} notes from server")
                for (dto in serverNotes) {
                    val localNotes = noteDao.getNotesByServerId(dto.id)
                    if (localNotes.isEmpty()) {
                        // Note exists on server but not locally — insert it
                        noteDao.insert(dto.toEntity())
                    } else {
                        val primary = localNotes.first()
                        // If duplicates exist in Room with the same serverId, clean them up
                        if (localNotes.size > 1) {
                            Log.w(tag, "Found ${localNotes.size} duplicate local notes for serverId ${dto.id}. Cleaning up...")
                            for (duplicate in localNotes.drop(1)) {
                                noteDao.delete(duplicate)
                            }
                        }
                        if (primary.syncStatus == SyncStatus.SYNCED && serverIsNewer(dto, primary)) {
                            noteDao.update(
                                primary.copy(
                                    title = dto.title ?: "",
                                    content = dto.content ?: "",
                                    category = dto.category ?: "",
                                    favorite = dto.favorite,
                                    modified = dto.modified,
                                    etag = dto.etag
                                )
                            )
                        }
                        // DIRTY: local edits win — do nothing; they'll be pushed in the next sync
                    }
                }

                // Step 4: Delete local notes that were removed on the server.
                // Any SYNCED note whose serverId is absent from the server response
                // was deleted remotely and should be removed from Room too.
                val serverIds = serverNotes.map { it.id }
                if (serverIds.isEmpty()) {
                    noteDao.deleteAllSynced()
                } else {
                    noteDao.deleteSyncedNotesNotIn(serverIds)
                }
                Log.d(tag, "Sync completed successfully.")
                Unit
            }.onFailure { e ->
                Log.e(tag, "Sync failed: ${e.message}", e)
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
