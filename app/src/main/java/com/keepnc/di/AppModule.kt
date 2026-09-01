package com.keepnc.di

import android.content.Context
import androidx.work.WorkManager
import com.google.gson.Gson
import com.keepnc.data.auth.TokenStorage
import com.keepnc.data.local.AppDatabase
import com.keepnc.data.local.NoteDao
import com.keepnc.data.remote.AuthInterceptor
import com.keepnc.data.remote.NotesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Main Hilt dependency injection module.
 *
 * BEGINNER NOTE: @InstallIn(SingletonComponent::class) means these bindings
 * live as long as the Application does — they are created once and shared everywhere.
 *
 * IMPORTANT — Dynamic base URL:
 * Retrofit requires a base URL at construction time, but we only know the Nextcloud
 * server URL after the user logs in. Our solution for v1:
 * - We build Retrofit with a placeholder URL if none is stored.
 * - After login, the app's process is restarted (via finishAffinity + startActivity)
 *   so Retrofit is reconstructed with the real URL.
 * TODO: Replace with a proper dynamic URL approach (e.g. a UrlProvider singleton
 *       that AuthInterceptor reads) to avoid needing a process restart.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    /** Unauthenticated OkHttpClient — used by LoginFlowService, which runs before credentials exist. */
    @Provides
    @Singleton
    @Named("unauthenticated")
    fun provideUnauthenticatedOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()

    /** Authenticated OkHttpClient — used by Retrofit to call Notes API. */
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // BODY-level logging in all builds for now; disable in release for production
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson,
        tokenStorage: TokenStorage
    ): Retrofit {
        // Build the Notes API base URL from stored server URL, or use a placeholder.
        // See the TODO above for a proper fix.
        val baseUrl = tokenStorage.getServerUrl()
            ?.trimEnd('/')
            ?.let { "$it/index.php/apps/notes/api/v1/" }
            ?: "https://placeholder.invalid/index.php/apps/notes/api/v1/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideNotesApi(retrofit: Retrofit): NotesApi =
        retrofit.create(NotesApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        androidx.room.Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()

    @Provides
    @Singleton
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()

    /**
     * Provide WorkManager so Hilt can inject it.
     *
     * We use WorkManager.getInstance() rather than letting Hilt construct it,
     * because WorkManager is already initialised by [KeepNcApp] via
     * [Configuration.Provider] with our custom HiltWorkerFactory.
     * Creating a second instance would break SyncWorker injection.
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
