package com.keepnc.data.repository

import com.keepnc.data.auth.TokenStorage
import com.keepnc.data.local.NoteDao
import com.keepnc.data.local.NoteEntity
import com.keepnc.data.local.SyncStatus
import com.keepnc.data.remote.NotesApi
import com.keepnc.data.remote.dto.NoteCreateRequest
import com.keepnc.data.remote.dto.NoteDto
import com.keepnc.data.remote.dto.NoteUpdateRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesRepositoryTest {

    private lateinit var repository: NotesRepository
    private lateinit var fakeDao: FakeNoteDao
    private lateinit var fakeApi: FakeNotesApi
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        fakeDao = FakeNoteDao()
        fakeApi = FakeNotesApi()

        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val unsafeField = unsafeClass.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null)
        val allocateMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
        val dummyTokenStorage = allocateMethod.invoke(unsafe, TokenStorage::class.java) as TokenStorage

        repository = NotesRepository(
            noteDao = fakeDao,
            notesApi = fakeApi,
            tokenStorage = dummyTokenStorage
        ).apply {
            ioDispatcher = testDispatcher
        }
    }

    @Test
    fun `syncWithServer pulls new notes from server into empty database`() = runTest(testDispatcher) {
        fakeApi.remoteNotes.add(NoteDto(id = 101L, title = "Server Note 1", content = "Content 1"))
        fakeApi.remoteNotes.add(NoteDto(id = 102L, title = "Server Note 2", content = "Content 2"))

        val result = repository.syncWithServer()

        assertTrue(result.isSuccess)
        assertEquals(2, fakeDao.notes.size)
        val note1 = fakeDao.notes.values.find { it.serverId == 101L }
        val note2 = fakeDao.notes.values.find { it.serverId == 102L }
        assertEquals("Server Note 1", note1?.title)
        assertEquals("Server Note 2", note2?.title)
    }

    @Test
    fun `syncWithServer cleans up local duplicate notes for same serverId`() = runTest(testDispatcher) {
        // Prepare database with 2 duplicate entries having the same serverId
        fakeDao.notes[1L] = NoteEntity(id = 1L, serverId = 100L, title = "Duplicate 1", content = "Body", syncStatus = SyncStatus.SYNCED)
        fakeDao.notes[2L] = NoteEntity(id = 2L, serverId = 100L, title = "Duplicate 2", content = "Body", syncStatus = SyncStatus.SYNCED)
        fakeDao.notes[3L] = NoteEntity(id = 3L, serverId = 200L, title = "Other Note", content = "Body", syncStatus = SyncStatus.SYNCED)

        // Server returns the note with serverId 100 and serverId 200
        fakeApi.remoteNotes.add(NoteDto(id = 100L, title = "Updated Server Title", content = "Body", modified = 100L))
        fakeApi.remoteNotes.add(NoteDto(id = 200L, title = "Other Note", content = "Body"))

        val result = repository.syncWithServer()

        assertTrue(result.isSuccess)
        // Check that only 1 note exists for serverId 100
        val notes100 = fakeDao.notes.values.filter { it.serverId == 100L }
        assertEquals(1, notes100.size)
        assertEquals("Updated Server Title", notes100.first().title)
        assertEquals(2, fakeDao.notes.size)
    }

    @Test
    fun `clearAllLocalNotes wipes all notes from local database`() = runTest(testDispatcher) {
        fakeDao.notes[1L] = NoteEntity(id = 1L, serverId = 100L, title = "Note 1", content = "Body")
        fakeDao.notes[2L] = NoteEntity(id = 2L, serverId = 101L, title = "Note 2", content = "Body")

        repository.clearAllLocalNotes()

        assertEquals(0, fakeDao.notes.size)
    }

    private class FakeNoteDao : NoteDao {
        val notes = mutableMapOf<Long, NoteEntity>()

        override fun getAllNotes(): Flow<List<NoteEntity>> = flowOf(notes.values.toList())
        override fun getNotesByCategory(category: String): Flow<List<NoteEntity>> = flowOf(notes.values.filter { it.category == category })
        override fun getFavorites(): Flow<List<NoteEntity>> = flowOf(notes.values.filter { it.favorite })
        override fun searchNotes(query: String): Flow<List<NoteEntity>> = flowOf(notes.values.filter { it.title.contains(query) || it.content.contains(query) })
        override fun getCategories(): Flow<List<String>> = flowOf(notes.values.map { it.category }.distinct())
        override suspend fun getNotesByServerId(serverId: Long): List<NoteEntity> = notes.values.filter { it.serverId == serverId }
        override suspend fun getNoteByServerId(serverId: Long): NoteEntity? = notes.values.find { it.serverId == serverId }
        override suspend fun getNoteById(id: Long): NoteEntity? = notes[id]
        override suspend fun getDirtyNotes(): List<NoteEntity> = notes.values.filter { it.syncStatus == SyncStatus.DIRTY }
        override suspend fun getPendingDeleteNotes(): List<NoteEntity> = notes.values.filter { it.syncStatus == SyncStatus.PENDING_DELETE }
        override suspend fun insert(note: NoteEntity): Long {
            val newId = (notes.keys.maxOrNull() ?: 0L) + 1L
            val created = note.copy(id = newId)
            notes[newId] = created
            return newId
        }
        override suspend fun update(note: NoteEntity) {
            notes[note.id] = note
        }
        override suspend fun delete(note: NoteEntity) { notes.remove(note.id) }
        override suspend fun deleteById(id: Long) { notes.remove(id) }
        override suspend fun deletePureLocalPendingDeletes() {}
        override suspend fun deleteSyncedNotesNotIn(serverIds: List<Long>) {
            val toRemove = notes.values.filter { it.syncStatus == SyncStatus.SYNCED && it.serverId != null && it.serverId !in serverIds }
            toRemove.forEach { notes.remove(it.id) }
        }
        override suspend fun deleteAllSynced() {
            val toRemove = notes.values.filter { it.syncStatus == SyncStatus.SYNCED }
            toRemove.forEach { notes.remove(it.id) }
        }
        override suspend fun clearAll() { notes.clear() }
    }

    private class FakeNotesApi : NotesApi {
        val remoteNotes = mutableListOf<NoteDto>()

        override suspend fun getNotes(): List<NoteDto> = remoteNotes
        override suspend fun getNote(id: Long): NoteDto = remoteNotes.first { it.id == id }
        override suspend fun createNote(note: NoteCreateRequest): NoteDto = NoteDto(id = 999L, title = note.title, content = note.content)
        override suspend fun updateNote(id: Long, note: NoteUpdateRequest): NoteDto = NoteDto(id = id, title = note.title, content = note.content)
        override suspend fun deleteNote(id: Long) {
            remoteNotes.removeAll { it.id == id }
        }
    }
}
