package com.wtb.dashTracker.database.models

import android.location.Location
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wtb.dashTracker.extensions.dtfDateTime
import dev.benica.csvutil.CSVConvertible
import dev.benica.csvutil.CSVConvertible.Column
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Instant
import java.time.LocalDate
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
    val entry: Long,
    val still: Int? = null,
    val car: Int? = null,
    val foot: Int? = null,
    val unknown: Int? = null
) : DataModel() {
    constructor(
        loc: Location,
        entryId: Long,
        still: Int,
        car: Int,
        foot: Int,
        unknown: Int
    ) : this(
        time = loc.localDateTime,
        latitude = loc.latitude,
        longitude = loc.longitude,
        accuracy = loc.accuracy,
        bearing = loc.bearing,
        bearingAccuracy = loc.bearingAccuracyDegrees,
        entry = entryId,
        still = still,
        car = car,
        foot = foot,
        unknown = unknown
    )

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
            ENTRY("Entry", LocationData::entry),
            STILL("Still", LocationData::still),
            CAR("Car", LocationData::car),
            FOOT("Foot", LocationData::foot),
            UNKNOWN("Unknown", LocationData::unknown),
            LAST_UPDATED("Last Updated", LocationData::lastUpdated)
        }

        @Throws(IllegalStateException::class)
        override fun fromCSV(row: Map<String, String>): LocationData {
            val entryColumnValue = row[Columns.ENTRY.headerName]
            val entry = entryColumnValue?.toLongOrNull()
                ?: throw IllegalStateException("Trip column must be filled with a valid tripId, not: $entryColumnValue")

            return LocationData(
                locationId = row[Columns.ID.headerName]?.toLong() ?: AUTO_ID,
                time = row[Columns.TIME.headerName]?.let {
                    LocalDateTime.parse(it, dtfDateTime)
                },
                latitude = row[Columns.LAT.headerName]?.toDoubleOrNull(),
                longitude = row[Columns.LONG.headerName]?.toDoubleOrNull(),
                accuracy = row[Columns.ACCURACY.headerName]?.toFloatOrNull(),
                bearing = row[Columns.BEARING.headerName]?.toFloatOrNull(),
                bearingAccuracy = row[Columns.BEARING_ACCURACY.headerName]?.toFloatOrNull(),
                entry = entry,
                still = row[Columns.STILL.headerName]?.toIntOrNull(),
                car = row[Columns.CAR.headerName]?.toIntOrNull(),
                foot = row[Columns.FOOT.headerName]?.toIntOrNull(),
                unknown = row[Columns.UNKNOWN.headerName]?.toIntOrNull()
            ).apply {
                lastUpdated = LocalDate.parse(row[Columns.LAST_UPDATED.headerName])
            }
        }

        override fun getColumns(): Array<Column<LocationData>> =
            Columns.values().map { Column(it.headerName, it.getValue) }.toTypedArray()
    }
}

/**
 * Distance calculates the distance traveled, excluding when still or moving slowly
 */
@ExperimentalCoroutinesApi
val List<LocationData>.distance: Double
    get() {
        var prevLoc: LocationData? = null

        val res = sortedBy { it.time }.fold(0.0) { f, loc ->
            val tempPrev = prevLoc
            prevLoc = loc

            val dist = tempPrev?.let { loc.distanceTo(it) }

            f + if (dist != null && (loc.still != 100 || dist > 0.002)) {
                dist
            } else {
                0.0
            }
        }

        return res
    }