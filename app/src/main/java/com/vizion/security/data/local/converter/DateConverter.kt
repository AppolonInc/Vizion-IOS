// ========================================

// Chemin: app/src/main/java/com/vizion/security/data/local/converter/DateConverter.kt

package com.vizion.security.data.local.converter

import androidx.room.TypeConverter
import java.util.Date

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}