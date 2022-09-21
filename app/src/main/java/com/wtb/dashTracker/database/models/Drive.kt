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

package com.wtb.dashTracker.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wtb.dashTracker.extensions.dtfTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DashEntry::class,
            parentColumns = ["entryId"],
            childColumns = ["entry"],
            onDelete = CASCADE
        )
    ],
    indices = [
        Index(value = ["entry"])
    ]
)
data class Drive(
    @PrimaryKey(autoGenerate = true) val driveId: Long = AUTO_ID,
    val entry: Long?,
    var start: LocalDateTime? = null,
    var end: LocalDateTime? = null,
    var startOdometer: Int? = null,
    var endOdometer: Int? = null
) : DataModel() {
    override val id: Long
        get() = driveId

    val duration: Long?
        get() = start?.let { st -> end?.let { e -> st.until(e, ChronoUnit.SECONDS) } }

    fun getTimeRange(): String {
        val startTime = start?.toLocalTime()?.format(dtfTime) ?: ""
        val endTime = end?.let { it.toLocalTime().format(dtfTime) } ?: ""

        val nextDay =
            if (end == null || end == start) {
                ""
            } else {
                " (next day)"
            }

        return "$startTime - $endTime$nextDay"
    }
}

