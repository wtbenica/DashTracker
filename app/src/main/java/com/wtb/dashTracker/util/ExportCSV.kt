package com.wtb.dashTracker.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File


@ExperimentalCoroutinesApi
class ExportCSV {
    fun exportDatabaseToCSVFile(
        context: Context,
        entries: List<DashEntry>?,
        weeklies: List<Weekly>?
    ) {
        val dailyFile: File? = generateFile(context, "dash_tracker_daily.csv")
        val weeklyFile: File? = generateFile(context, "dash_tracker_weekly.csv")

        if (dailyFile != null && weeklyFile != null) {
            exportEntries(dailyFile, entries)
            exportWeeklies(weeklyFile, weeklies)

            val viewFileIntent = viewFileIntent(
                context = context,
                dailyFile,
                weeklyFile
            )

            try {
                startActivity(context, Intent.createChooser(viewFileIntent, null), null)
            } catch (e: ActivityNotFoundException) {
                (context as MainActivity).runOnUiThread {
                    ConfirmationDialog(
                        text = R.string.no_spreadsheet,
                        requestKey = "confirmSearchPlay",
                        negButton = R.string.no,
                        message = "Open Google Play?",
                        posAction = {
                            val getSpreadsheetAppIntent = Intent(Intent.ACTION_VIEW)
                            getSpreadsheetAppIntent.data =
                                Uri.parse("market://search?q=spreadsheet")
                            startActivity(context, getSpreadsheetAppIntent, null)
                        },
                        negAction = { }
                    ).show(context.supportFragmentManager, null)
                }
            }
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

    private fun exportWeeklies(csvFile: File, weeklies: List<Weekly>?) {
        csvWriter().open(csvFile, append = false) {
            writeRow(
                listOf(
                    "Start of Week",
                    "Base Pay Adjustment",
                    "Week Number",
                    "isNew"
                )
            )
            weeklies?.forEach {
                writeRow(
                    listOf(
                        it.date,
                        it.basePayAdjustment,
                        it.weekNumber,
                        it.isNew
                    )
                )
            }
        }
    }

    private fun exportEntries(csvFile: File, entries: List<DashEntry>?) {
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
            entries?.forEachIndexed { _, dashEntry ->
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

    private fun viewFileIntent(
        context: Context,
        vararg file: File
    ): Intent {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        val contentUri = arrayListOf<Uri>()
        file.forEach {
            contentUri.add(
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
            )
        }
        intent.type = "text/csv"
        intent.putParcelableArrayListExtra(EXTRA_STREAM, contentUri)

        intent.flags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        return intent
    }
}