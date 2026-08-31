package com.keepnc.ui.editor

import com.keepnc.data.auth.TokenStorage
import com.keepnc.data.local.NoteDao
import com.keepnc.data.local.NoteEntity
import com.keepnc.data.local.SyncStatus
import com.keepnc.data.remote.NotesApi
import com.keepnc.data.remote.dto.NoteCreateRequest
import com.keepnc.data.remote.dto.NoteDto
import com.keepnc.data.remote.dto.NoteUpdateRequest
import com.keepnc.data.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeNoteDao
    private lateinit var fakeApi: FakeNotesApi
    private lateinit var repository: NotesRepository
    private lateinit var viewModel: EditorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeNoteDao()
        fakeApi = FakeNotesApi()

        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val unsafeField = unsafeClass.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null)
        val allocateMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
        val dummyTokenStorage = allocateMethod.invoke(unsafe, TokenStorage::class.java) as TokenStorage

        repository = NotesRepository(fakeDao, fakeApi, dummyTokenStorage)
        viewModel = EditorViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when first line matches title, it is stripped and saved immediately`() = runTest {
        val note = NoteEntity(
            id = 1L,
            serverId = 101L,
            title = "My Shopping List",
            content = "My Shopping List\n- [ ] Milk\n- [ ] Bread",
            category = "Groceries",
            favorite = true,
            syncStatus = SyncStatus.SYNCED
        )
        fakeDao.notes[1L] = note

        viewModel.loadNote(1L)
        advanceUntilIdle()

        // Title and stripped content in ViewModel
        assertEquals("My Shopping List", viewModel.title.value)
        assertEquals("- [ ] Milk\n- [ ] Bread", viewModel.content.value)
        assertEquals("Groceries", viewModel.category.value)
        assertTrue(viewModel.isFavorite.value)

        // Database should be updated immediately with stripped content
        val updatedNote = fakeDao.notes[1L]!!
        assertEquals("- [ ] Milk\n- [ ] Bread", updatedNote.content)
        assertEquals(SyncStatus.DIRTY, updatedNote.syncStatus)

        // hasChanges should be false because no user edits have been made
        assertFalse(
            viewModel.hasChanges(
                currentTitle = viewModel.title.value,
                currentContent = viewModel.content.value,
                currentCategory = viewModel.category.value,
                currentFavorite = viewModel.isFavorite.value
            )
        )
    }

    @Test
    fun `when single line note matches title, content becomes empty and saved`() = runTest {
        val note = NoteEntity(
            id = 2L,
            serverId = 102L,
            title = "Quick Note",
            content = "Quick Note",
            category = "",
            favorite = false,
            syncStatus = SyncStatus.SYNCED
        )
        fakeDao.notes[2L] = note

        viewModel.loadNote(2L)
        advanceUntilIdle()

        assertEquals("Quick Note", viewModel.title.value)
        assertEquals("", viewModel.content.value)

        val updatedNote = fakeDao.notes[2L]!!
        assertEquals("", updatedNote.content)
        assertEquals(SyncStatus.DIRTY, updatedNote.syncStatus)

        assertFalse(
            viewModel.hasChanges(
                currentTitle = viewModel.title.value,
                currentContent = viewModel.content.value,
                currentCategory = viewModel.category.value,
                currentFavorite = viewModel.isFavorite.value
            )
        )
    }

    @Test
    fun `when first line does not match title, content is kept unchanged`() = runTest {
        val note = NoteEntity(
            id = 3L,
            serverId = 103L,
            title = "Different Title",
            content = "First line is different\nSecond line",
            category = "Work",
            favorite = false,
            syncStatus = SyncStatus.SYNCED
        )
        fakeDao.notes[3L] = note

        var updateCalled = false
        fakeDao.onUpdate = { updateCalled = true }

        viewModel.loadNote(3L)
        advanceUntilIdle()

        assertEquals("Different Title", viewModel.title.value)
        assertEquals("First line is different\nSecond line", viewModel.content.value)
        assertFalse("Repository update should not be called when content did not match title", updateCalled)
    }

    @Test
    fun `when title is blank, first line is not stripped`() = runTest {
        val note = NoteEntity(
            id = 4L,
            serverId = 104L,
            title = "",
            content = "First line\nSecond line",
            category = "",
            favorite = false,
            syncStatus = SyncStatus.SYNCED
        )
        fakeDao.notes[4L] = note

        var updateCalled = false
        fakeDao.onUpdate = { updateCalled = true }

        viewModel.loadNote(4L)
        advanceUntilIdle()

        assertEquals("", viewModel.title.value)
        assertEquals("First line\nSecond line", viewModel.content.value)
        assertFalse(updateCalled)
    }

    private class FakeNoteDao : NoteDao {
        val notes = mutableMapOf<Long, NoteEntity>()
        var onUpdate: (() -> Unit)? = null

        override fun getAllNotes(): Flow<List<NoteEntity>> = flowOf(notes.values.toList())
        override fun getNotesByCategory(category: String): Flow<List<NoteEntity>> = flowOf(notes.values.filter { it.category == category })
        override fun getFavorites(): Flow<List<NoteEntity>> = flowOf(notes.values.filter { it.favorite })
        override fun searchNotes(query: String): Flow<List<NoteEntity>> = flowOf(notes.values.filter { it.title.contains(query) || it.content.contains(query) })
        override fun getCategories(): Flow<List<String>> = flowOf(notes.values.map { it.category }.distinct())
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
            onUpdate?.invoke()
        }
        override suspend fun delete(note: NoteEntity) { notes.remove(note.id) }
        override suspend fun deleteById(id: Long) { notes.remove(id) }
        override suspend fun deletePureLocalPendingDeletes() {}
        override suspend fun deleteSyncedNotesNotIn(serverIds: List<Long>) {}
        override suspend fun deleteAllSynced() {}
    }

    private class FakeNotesApi : NotesApi {
        override suspend fun getNotes(): List<NoteDto> = emptyList()
        override suspend fun getNote(id: Long): NoteDto = NoteDto(id = id)
        override suspend fun createNote(note: NoteCreateRequest): NoteDto = NoteDto(id = 999L)
        override suspend fun updateNote(id: Long, note: NoteUpdateRequest): NoteDto = NoteDto(id = id)
        override suspend fun deleteNote(id: Long) {}
    }
}
