package com.wtb.dashTracker.ui.extensions

import com.wtb.dashTracker.ui.entry_list.EntryListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalTime

@ExperimentalCoroutinesApi
fun CharSequence.toTimeOrNull() =
    if (this.isNotEmpty()) LocalTime.parse(this, EntryListFragment.dtfTime) else null

@ExperimentalCoroutinesApi
fun CharSequence.toDateOrNull() =
    if (this.isNotEmpty()) {
        try {
            val df = EntryListFragment.dtfDate
            LocalDate.parse(this, df)
        } catch (e: Exception) {
            try {
                val df = EntryListFragment.dtfDateThisYear
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