package com.wtb.dashTracker.extensions

import com.wtb.dashTracker.ui.entry_list.EntryListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalTime

@ExperimentalCoroutinesApi
fun CharSequence.toTimeOrNull() =
    if (this.isNotEmpty()) LocalTime.parse(this, dtfTime) else null

@ExperimentalCoroutinesApi
fun CharSequence.toDateOrNull() =
    if (this.isNotEmpty()) {
        try {
            val df = dtfDate
            LocalDate.parse(this, df)
        } catch (e: Exception) {
            try {
                val df = dtfDateThisYear
                LocalDate.parse(this, df)
            } catch (e: Exception) {
                null
            }
        }
    } else {
        null
    }

fun CharSequence.toFloatOrNull(): Float? = if (this.isNotEmpty()) this.toString().toFloatOrNull() else null
fun CharSequence.toIntOrNull(): Int? = if (this.isNotEmpty()) this.toString().toIntOrNull() else null