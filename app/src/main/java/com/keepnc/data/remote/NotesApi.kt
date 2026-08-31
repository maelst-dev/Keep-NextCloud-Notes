package com.keepnc.data.remote

import com.keepnc.data.remote.dto.NoteCreateRequest
import com.keepnc.data.remote.dto.NoteDto
import com.keepnc.data.remote.dto.NoteUpdateRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit interface for the Nextcloud Notes API v1.
 * Full spec: https://github.com/nextcloud/notes/blob/main/docs/api/v1.md
 *
 * Base URL is set in AppModule and includes the server host + path prefix:
 *   https://{host}/index.php/apps/notes/api/v1/
 *
 * All calls use Basic Auth (loginName:appPassword), added by [AuthInterceptor].
 *
 * BEGINNER NOTE: The `suspend` keyword makes each function a coroutine —
 * Retrofit will automatically run it off the main thread when called from
 * a coroutine context.
 */
interface NotesApi {

    /** Fetch all notes. Returns a flat list — no pagination in the API. */
    @GET("notes")
    suspend fun getNotes(): List<NoteDto>

    /** Fetch a single note by server ID. */
    @GET("notes/{id}")
    suspend fun getNote(@Path("id") id: Long): NoteDto

    /** Create a new note. The server returns the created note with its ID and etag. */
    @POST("notes")
    suspend fun createNote(@Body note: NoteCreateRequest): NoteDto

    /** Update an existing note. Returns the updated note with a new etag. */
    @PUT("notes/{id}")
    suspend fun updateNote(
        @Path("id") id: Long,
        @Body note: NoteUpdateRequest
    ): NoteDto

    /** Delete a note permanently. Returns 200 with no body on success. */
    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: Long)
}
