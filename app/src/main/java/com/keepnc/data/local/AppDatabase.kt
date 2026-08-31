package com.keepnc.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The single Room database for the app.
 *
 * exportSchema = false is fine for a learning project; set to true in production
 * and commit the schema JSON to version control so you can track migrations.
 *
 * BEGINNER NOTE: Every time you change the database schema (add/remove columns,
 * rename tables) you MUST increment `version` and provide a Migration or use
 * fallbackToDestructiveMigration() (destroys all data — OK only in development).
 */
@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        const val DATABASE_NAME = "keepnc_db"
    }
}
