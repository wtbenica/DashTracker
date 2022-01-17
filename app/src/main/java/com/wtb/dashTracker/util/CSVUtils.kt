package com.wtb.dashTracker.util

import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.security.crypto.EncryptedFile
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.MainActivity.Companion.BACKUP_KEY
import com.wtb.dashTracker.MainActivity.Companion.getMasterKey
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.DashEntry.Companion.asList
import com.wtb.dashTracker.database.models.DataModel
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.database.models.Weekly.Companion.asList
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.*
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


@ExperimentalCoroutinesApi
class CSVUtils {
    companion object {
        private const val TAG = APP + "CSVUtils"

        private fun encrypt(context: Context, file: File): File {
            val masterKey = getMasterKey(context)

            val outFile = File(context.filesDir, getZipFileName().replace(".zip", ".bak"))
            if (outFile.exists()) {
                outFile.delete()
            }

            val encryptedFile = EncryptedFile.Builder(
                context,
                outFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            )
                .setKeysetPrefName(MainActivity.BACKUP_PREFS)
                .setKeysetAlias(BACKUP_KEY)
                .build()

            encryptedFile.openFileOutput().bufferedWriter().use { writer ->
                file.bufferedReader().use { reader ->
                    writer.write(reader.read())
                    writer.flush()
                }
            }

            return outFile
        }

        fun exportDb(
            context: Context,
            entries: List<DashEntry>?,
            weeklies: List<Weekly>?
        ) {
            val dailyFile: File? = generateFile(context, getEntriesFileName())
            val weeklyFile: File? = generateFile(context, getWeekliesFileName())

            if (dailyFile != null && weeklyFile != null) {
                exportEntries(dailyFile, entries)
                exportWeeklies(weeklyFile, weeklies)

                val zipFile: File = zipFiles(context, dailyFile, weeklyFile)
                dailyFile.delete()
                weeklyFile.delete()

                fun sendFilesIntent(encrypted: Boolean = false): Intent {
                    val encrypt = if (encrypted) encrypt(context, zipFile) else zipFile
                    return getSendFilesIntent(context, encrypted, encrypt)
                }

                (context as MainActivity).runOnUiThread {
                    ConfirmationDialog(
                        text = R.string.confirm_export,
                        requestKey = "confirmExport",
                        message = "Confirm Export",
                        posButton = R.string.label_action_export_csv,
                        posAction = {
                            startActivity(
                                context,
                                Intent.createChooser(sendFilesIntent(), null),
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

        private fun zipFiles(context: Context, vararg file: File): File {
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

        private fun unzipFiles() {

        }

        fun importCsv(
            entriesPath: InputStream? = null,
            weekliesPath: InputStream? = null
        ): Pair<List<DashEntry>?, List<Weekly>?> {
            val entries: List<DashEntry>? = getModelsFromCsv(entriesPath) { DashEntry.fromCSV(it) }
            val weeklies: List<Weekly>? = getModelsFromCsv(weekliesPath) { Weekly.fromCSV(it) }
            return Pair(entries, weeklies)
        }

        private fun <T : DataModel> getModelsFromCsv(
            path: InputStream?,
            function: (Map<String, String>) -> T
        ): List<T>? =
            path?.let {
                csvReader().readAllWithHeader(it).map { function(it) }
            }

        private fun getSendFilesIntent(
            context: Context,
            encrypted: Boolean,
            file: File
        ): Intent {
            val intent = Intent(Intent.ACTION_SEND)
            val contentUri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            intent.data = contentUri
//            intent.type = if (encrypted) "*/*" else "application/zip"
            intent.putExtra(EXTRA_STREAM, contentUri)

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
                writeRow(Weekly.headerList)
                weeklies?.forEach { weekly ->
                    writeRow(weekly.asList())
                }
            }
        }

        private fun exportEntries(csvFile: File, entries: List<DashEntry>?) {
            csvWriter().open(csvFile, append = false) {
                writeRow(DashEntry.headerList)
                entries?.forEach { dashEntry ->
                    writeRow(dashEntry.asList())
                }
            }
        }

        const val FILE_ZIP = "dash_tracker_"
        const val FILE_BACKUP = "dash_tracker_"
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