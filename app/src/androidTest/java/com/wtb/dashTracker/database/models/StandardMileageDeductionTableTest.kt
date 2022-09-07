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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
internal class StandardMileageDeductionTableTest {

    private lateinit var stdMileageTable: StandardMileageDeductionTable

    @org.junit.Before
    fun setUp() {
        stdMileageTable = StandardMileageDeductionTable()
    }

    @Test
    fun getDecember() {
        assertEquals(
            "StdDed December is wrong",
            0.54f,
            stdMileageTable[LocalDate.of(2016, 12, 1)],
        )
    }

    @Test
    fun getJanuary() {
        assertEquals(
            "StdDed January is wrong",
            0.535f,
            stdMileageTable[LocalDate.of(2017, 1, 1)],
        )
    }

    @Test
    fun getPreCusp() {
        assertEquals(
            "StdDed PreCusp is wrong",
            0.585f,
            stdMileageTable[LocalDate.of(2022, 6, 1)],
        )
    }

    @Test
    fun getPostCusp() {
        assertEquals(
            "StdDed PostCusp is wrong",
            0.625f,
            stdMileageTable[LocalDate.of(2022, 7, 1)],
        )
    }

    @Test
    fun getNotInTable() {
        assertEquals(
            "StdDed PostCusp is wrong",
            0f,
            stdMileageTable[LocalDate.of(1999, 7, 1)],
        )
    }
}