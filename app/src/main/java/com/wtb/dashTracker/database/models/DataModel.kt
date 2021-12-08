package com.wtb.dashTracker.database.models

import java.io.Serializable
import java.time.LocalDate

sealed class DataModel(var lastUpdated: LocalDate = LocalDate.now()) : Serializable {
    abstract val id: Int
}