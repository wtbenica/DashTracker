/*
 * Copyright 2022 Wesley T. Benica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wtb.dashTracker.database

import androidx.room.TypeConverter
import com.wtb.dashTracker.extensions.dtfDateTime
import com.wtb.dashTracker.extensions.dtfDateTimeOld
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException

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
    fun toLocalDateTime(date: String?): LocalDateTime? {
        return if (date == null || date == "null") {
            null
        } else {
            try {
                LocalDateTime.parse(date, dtfDateTime)
            } catch (e: DateTimeParseException) {
                LocalDateTime.parse(date, dtfDateTimeOld)
            }
        }
    }

    @TypeConverter
    fun fromLocalDateTime(date: LocalDateTime?): String? {
        return date?.format(dtfDateTime)
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