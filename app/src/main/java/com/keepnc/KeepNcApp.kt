package com.keepnc

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class. @HiltAndroidApp triggers Hilt's code generation and sets up
 * the application-level dependency injection component.
 *
 * We also implement Configuration.Provider so WorkManager uses HiltWorkerFactory,
 * which allows @HiltWorker annotation on SyncWorker.
 *
 * BEGINNER NOTE: You must declare this class in AndroidManifest.xml via
 * android:name=".KeepNcApp" on the <application> tag, or Hilt won't work.
 */
@HiltAndroidApp
class KeepNcApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
