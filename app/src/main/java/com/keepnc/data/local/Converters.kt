package com.keepnc.data.local

import androidx.room.TypeConverter

/**
 * Room TypeConverters for custom types that Room can't store natively.
 * Registered on [AppDatabase] via @TypeConverters(Converters::class).
 */
class Converters {
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
