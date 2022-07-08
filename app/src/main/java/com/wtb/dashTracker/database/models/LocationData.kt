package com.wtb.dashTracker.database.models

import android.location.Location
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wtb.dashTracker.extensions.dtfDate
import com.wtb.dashTracker.extensions.dtfDateTime
import dev.benica.csvutil.CSVConvertible
import dev.benica.csvutil.CSVConvertible.Column
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.reflect.KProperty1

fun Double.metersToMiles(): Double = this / 1000 * 0.621371

val Location.localDateTime: LocalDateTime
    get() = Instant.ofEpochSecond(this.time / 1000).atZone(ZoneId.systemDefault()).toLocalDateTime()

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DashEntry::class,
            parentColumns = arrayOf("entryId"),
            childColumns = arrayOf("entry"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entry"])
    ]
)
class LocationData constructor(
    @PrimaryKey(autoGenerate = true) val locationId: Long = AUTO_ID,
    val time: LocalDateTime?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val bearing: Float?,
    val bearingAccuracy: Float?,
    val entry: Long
) : DataModel() {
    constructor(loc: Location, entryId: Long) : this(
        time = loc.localDateTime,
        latitude = loc.latitude, longitude = loc.longitude, accuracy = loc.accuracy, bearing = loc
            .bearing, bearingAccuracy = loc.bearingAccuracyDegrees, entry = entryId
    ) {

    }

    override val id: Long
        get() = locationId

    val timeFmt: String?
        get() = time?.format(dtfDateTime)

    fun distanceTo(loc: LocationData): Double {
        val distResults = floatArrayOf(0f)
        if (latitude != null && longitude != null && loc.latitude != null && loc.longitude != null) {
            Location.distanceBetween(
                latitude,
                longitude,
                loc.latitude,
                loc.longitude,
                distResults
            )
        }
        return distResults[0].toDouble().metersToMiles()
    }

    companion object : CSVConvertible<LocationData> {
        override val saveFileName: String
            get() = "locations"

        enum class Columns(
            val headerName: String,
            val getValue: KProperty1<LocationData, *>
        ) {
            ID("LocationId", LocationData::locationId),
            TIME("Time", LocationData::timeFmt),
            LAT("Latitude", LocationData::latitude),
            LONG("Longitude", LocationData::longitude),
            ACCURACY("Accuracy", LocationData::accuracy),
            BEARING("Bearing", LocationData::bearing),
            BEARING_ACCURACY("Bearing Accuracy", LocationData::bearingAccuracy),
            ENTRY("Entry", LocationData::entry)
        }

        @Throws(IllegalStateException::class)
        override fun fromCSV(row: Map<String, String>): LocationData {
            val entryColumnValue = row[Columns.ENTRY.headerName]
            val entry = entryColumnValue?.toLongOrNull()
                ?: throw IllegalStateException("Trip column must be filled with a valid tripId, not: $entryColumnValue")
            return LocationData(
                locationId = row[Columns.ID.headerName]?.toLong() ?: AUTO_ID,
                time = row[Columns.TIME.headerName]?.let {
                    LocalDateTime.parse(it, dtfDate)
                },
                latitude = row[Columns.LAT.headerName]?.toDoubleOrNull(),
                longitude = row[Columns.LONG.headerName]?.toDoubleOrNull(),
                accuracy = row[Columns.ACCURACY.headerName]?.toFloatOrNull(),
                bearing = row[Columns.BEARING.headerName]?.toFloatOrNull(),
                bearingAccuracy = row[Columns.BEARING_ACCURACY.headerName]?.toFloatOrNull(),
                entry = entry
            )
        }

        override fun getColumns(): Array<Column<LocationData>> =
            Columns.values().map { Column(it.headerName, it.getValue) }.toTypedArray()
    }
}