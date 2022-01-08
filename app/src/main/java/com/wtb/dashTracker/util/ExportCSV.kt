package com.wtb.dashTracker.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File

@ExperimentalCoroutinesApi
class ExportCSV {
    fun exportDatabaseToCSVFile(context: Context, value: List<DashEntry>?) {
        val dailyFile: File? = generateFile(context, "dash_tracker_daily.csv")
//        val weeklyFile: File? = generateFile(context, "dash_tracker_weekly.csv")
        Log.d(APP + "ExportCSV", "exporting ${value?.size} entries")
        if (dailyFile != null) {
            exportEntries(dailyFile, value)

            (context as MainActivity).runOnUiThread {
                Toast.makeText(
                    context,
                    context.getString(R.string.csv_generations_success),
                    Toast.LENGTH_LONG
                ).show()
            }
            val intent = goToFileIntent(context, dailyFile)
            startActivity(context, intent, null)
        } else {
            (context as MainActivity).runOnUiThread {
                Toast.makeText(
                    context,
                    context.getString(R.string.csv_generation_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun exportEntries(csvFile: File, value: List<DashEntry>?) {
        csvWriter().open(csvFile, append = false) {
            writeRow(
                listOf(
                    "Start Date",
                    "Start Time",
                    "End Date",
                    "End Time",
                    "Start Odometer",
                    "End Odometer",
                    "Base Pay",
                    "Cash Tips",
                    "Other Pay",
                )
            )
            value?.forEachIndexed { _, dashEntry ->
                writeRow(
                    listOf(
                        dashEntry.date,
                        dashEntry.startTime,
                        dashEntry.endDate,
                        dashEntry.endTime,
                        dashEntry.startOdometer,
                        dashEntry.endOdometer,
                        dashEntry.pay,
                        dashEntry.cashTips,
                        dashEntry.otherPay
                    )
                )
            }
        }
    }

    private fun generateFile(context: Context, fileName: String): File? {
        val csvFile = File(context.filesDir, fileName)
        csvFile.createNewFile()

        return if (csvFile.exists()) {
            csvFile
        } else {
            null
        }
    }

    private fun goToFileIntent(context: Context, file: File): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        val contentUri =
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mimeType = context.contentResolver.getType(contentUri)
        intent.setDataAndType(contentUri, mimeType)
        intent.flags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        return intent
    }
}