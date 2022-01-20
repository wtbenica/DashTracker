package com.wtb.dashTracker.util

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
import com.wtb.dashTracker.database.models.Weekly.Companion.asList
import com.wtb.dashTracker.repository.Repository
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.*
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


@ExperimentalCoroutinesApi
class CSVUtils(private val context: Context) {
    private val repository: Repository
        get() = Repository.get()

    fun export(entries: List<DashEntry>?, weeklies: List<Weekly>?) {
        fun generateFile(context: Context, fileName: String): File? {
            val csvFile = File(context.filesDir, fileName)
            csvFile.createNewFile()

            return if (csvFile.exists()) {
                csvFile
            } else {
                null
            }
        }

        fun exportEntries(csvFile: File, entries: List<DashEntry>?) {
            csvWriter().open(csvFile, append = false) {
                writeRow(DashEntry.headerList)
                entries?.forEach { dashEntry ->
                    writeRow(dashEntry.asList())
                }
            }
        }

        fun exportWeeklies(csvFile: File, weeklies: List<Weekly>?) {
            csvWriter().open(csvFile, append = false) {
                writeRow(Weekly.headerList)
                weeklies?.forEach { weekly ->
                    writeRow(weekly.asList())
                }
            }
        }

        fun zipFiles(context: Context, vararg file: File): File {
            val outFile = File(context.filesDir, getZipFileName())
            val zipOut = ZipOutputStream(BufferedOutputStream(outFile.outputStream()))

            file.forEach { f ->
                val fi = FileInputStream(f)
                val origin = BufferedInputStream(fi)
                val entry = ZipEntry(f.name)
                zipOut.putNextEntry(entry)
                origin.copyTo(zipOut, 1024)
                origin.close()
            }
            zipOut.close()

            return outFile
        }

        val dailyFile: File? = generateFile(context, getEntriesFileName())
        val weeklyFile: File? = generateFile(context, getWeekliesFileName())

        if (dailyFile != null && weeklyFile != null) {
            exportEntries(dailyFile, entries)
            exportWeeklies(weeklyFile, weeklies)

            val zipFile: File = zipFiles(context, dailyFile, weeklyFile)
            dailyFile.delete()
            weeklyFile.delete()

            (context as MainActivity).runOnUiThread {
                ConfirmationDialog(
                    text = R.string.confirm_export,
                    requestKey = "confirmExport",
                    message = "Confirm Export",
                    posButton = R.string.label_action_export_csv,
                    posAction = {
                        startActivity(
                            context,
                            Intent.createChooser(getSendFilesIntent(zipFile), null),
                            null
                        )
                    },
                    negButton = R.string.cancel,
                    negAction = { },
                ).show(context.supportFragmentManager, null)
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

    fun import() {
        (context as MainActivity).runOnUiThread {
            ConfirmationDialog(
                text = R.string.confirm_import,
                requestKey = "confirmImportEntry",
                message = "Confirm Import",
                posButton = R.string.label_action_import_csv,
                posAction = {
                    context.getContentZipLauncher.launch("application/zip")
                },
                negButton = R.string.cancel,
                negAction = { },
            ).show(context.supportFragmentManager, null)
        }
    }

    fun extractZip(uri: Uri) {
        ZipInputStream(context.contentResolver.openInputStream(uri)).use { zipIn ->
            var nextEntry: ZipEntry? = zipIn.nextEntry
            while (nextEntry != null) {
                val destFile = File(context.filesDir, nextEntry.name)
                FileOutputStream(destFile).use { t ->
                    zipIn.copyTo(t, 1024)
                }
                val inputStream = FileInputStream(destFile)

                nextEntry.name?.also { entryName ->
                    when {
                        entryName.startsWith(FILE_ENTRIES, false) -> {
                            repository.importStream(entries = getModelsFromCsv(inputStream) {
                                DashEntry.fromCSV(it)
                            })
                        }
                        entryName.startsWith(FILE_WEEKLIES, false) -> {
                            repository.importStream(weeklies = getModelsFromCsv(inputStream) {
                                Weekly.fromCSV(it)
                            })
                        }
                    }
                }

                nextEntry = zipIn.nextEntry
            }
            zipIn.closeEntry()
        }
    }

    private fun <T : DataModel> getModelsFromCsv(
        path: InputStream?,
        function: (Map<String, String>) -> T
    ): List<T>? =
        path?.let { inStream ->
            csvReader().readAllWithHeader(inStream).map { function(it) }
        }

    private fun getSendFilesIntent(file: File): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        val contentUri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        intent.data = contentUri
        intent.putExtra(EXTRA_STREAM, contentUri)
        intent.flags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        return intent
    }


    companion object {
        const val FILE_ZIP = "dash_tracker_"
        const val FILE_ENTRIES = "dash_tracker_entries_"
        const val FILE_WEEKLIES = "dash_tracker_weeklies_"

        private fun getEntriesFileName() =
            "$FILE_ENTRIES${LocalDate.now().toString().replace('-', '_')}.csv"

        private fun getZipFileName() =
            "$FILE_ZIP${LocalDate.now().toString().replace('-', '_')}.zip"

        private fun getWeekliesFileName() =
            "$FILE_WEEKLIES${LocalDate.now().toString().replace('-', '_')}.csv"
    }
}

interface CSVConvertible<T : DataModel> {
    val headerList: List<String>

    fun fromCSV(row: Map<String, String>): T

    fun T.asList(): List<*>
}