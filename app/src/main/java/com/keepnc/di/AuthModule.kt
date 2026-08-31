package com.keepnc.di

import com.google.gson.Gson
import com.keepnc.data.auth.LoginFlowService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for authentication-related dependencies.
 *
 * Separated from [AppModule] for clarity — auth concerns are isolated here.
 * [TokenStorage] is provided via @Inject constructor so no explicit @Provides needed.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /**
     * LoginFlowService uses an unauthenticated HTTP client (no auth headers)
     * because it runs BEFORE we have credentials.
     */
    @Provides
    @Singleton
    fun provideLoginFlowService(
        @Named("unauthenticated") httpClient: OkHttpClient,
        gson: Gson
    ): LoginFlowService = LoginFlowService(httpClient, gson)
}
