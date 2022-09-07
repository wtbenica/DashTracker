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
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity
data class StandardMileageDeduction(
    @PrimaryKey val year: Long,
    var amount: Float
) : DataModel() {
    override val id: Long
        get() = year

    companion object {
        val STANDARD_DEDUCTIONS: Map<Long, Float> = mapOf(2021L to 0.56f, 2022L to 0.585f)
    }
}

class StdMileageDeductionTable {
    operator fun get(date: LocalDate): Float {
        val month = RATES[date.year]?.keys?.sorted()?.reversed()?.last { it >= date.month.value }
        return RATES[date.year]?.get(month) ?: 0f
    }

    companion object {
        private val RATES: Map<Int, Map<Int, Float>> = mapOf(
            2011 to mapOf(
                6 to 0.51f,
                12 to 0.555f
            ),
            2012 to mapOf(
                12 to 0.555f
            ),
            2013 to mapOf(
                12 to 0.565f
            ),
            2014 to mapOf(
                12 to 0.56f
            ),
            2015 to mapOf(
                12 to 0.575f
            ),
            2016 to mapOf(
                12 to 0.54f
            ),
            2017 to mapOf(
                12 to 0.535f
            ),
            2018 to mapOf(
                12 to 0.545f
            ),
            2019 to mapOf(
                12 to 0.58f
            ),
            2020 to mapOf(
                12 to 0.575f
            ),
            2021 to mapOf(
                12 to 0.56f
            ),
            2022 to mapOf(
                6 to 0.585f,
                12 to 0.625f
            )
        )
    }
}