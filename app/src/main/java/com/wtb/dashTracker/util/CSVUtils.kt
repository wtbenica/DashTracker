package com.wtb.dashTracker.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.DashEntry.Companion.asList
import com.wtb.dashTracker.database.models.DataModel
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


@ExperimentalCoroutinesApi
class CSVUtils {
    fun exportDatabaseToCSVFile(
        context: Context,
        entries: List<DashEntry>?,
        weeklies: List<Weekly>?
    ) {
        val dailyFile: File? = generateFile(context, FILE_ENTRIES)
        val weeklyFile: File? = generateFile(context, FILE_WEEKLIES)

        if (dailyFile != null && weeklyFile != null) {
            exportEntries(dailyFile, entries)
            exportWeeklies(weeklyFile, weeklies)

            val viewFileIntent = getSaveFilesIntent(context = context, dailyFile, weeklyFile)

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

    private fun getSaveFilesIntent(context: Context, vararg file: File): Intent {
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

    private fun generateFile(context: Context, fileName: String): File? {
        val csvFile = File(context.filesDir, fileName)
        csvFile.createNewFile()

        return if (csvFile.exists()) {
            csvFile
        } else {
            null
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
            writeRow(DashEntry.headerList)
            entries?.forEachIndexed { _, dashEntry ->
                writeRow(dashEntry.asList())
            }
        }
    }

    fun copyStreamToFile(inputStream: InputStream, outputFile: String): File {
        val res = File(outputFile)

        inputStream.use { input ->
            val outputStream = FileOutputStream(res)
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }

        return res
    }

    fun importFromCSV(
        entriesPath: InputStream? = null,
        weekliesPath: InputStream? = null
    ): Pair<List<DashEntry>?, List<Weekly>?> {
        val entries: List<DashEntry>? = entriesPath?.let { path ->
            csvReader().readAllWithHeader(path).map { DashEntry.fromCSV(it) }
        }

        val weeklies: List<Weekly>? = weekliesPath?.let { path ->
            csvReader().readAllWithHeader(path).map { Weekly.fromCSV(it) }
        }

        return Pair(entries, weeklies)
    }

    companion object {
        const val FILE_ENTRIES = "dash_tracker_daily.csv"
        const val FILE_WEEKLIES = "dash_tracker_weekly.csv"
    }
}

interface CSVConvertible<T : DataModel> {
    val headerList: List<String>

    fun fromCSV(row: Map<String, String>): T

    fun T.asList(): List<*>
}