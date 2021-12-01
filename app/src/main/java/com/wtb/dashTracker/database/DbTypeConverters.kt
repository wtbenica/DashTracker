package com.wtb.dashTracker.database

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

class DbTypeConverters {
    @TypeConverter
    fun toLocalDate(date: String?): LocalDate? {
        return if (date == null || date == "null") {
            null
        } else {
            LocalDate.parse(date)
        }
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalTime(date: String?): LocalTime? {
        return if (date == null || date == "null") {
            null
        } else {
            LocalTime.parse(date)
        }
    }

    @TypeConverter
    fun fromLocalTime(date: LocalTime?): String? {
        return date?.toString()
    }
}