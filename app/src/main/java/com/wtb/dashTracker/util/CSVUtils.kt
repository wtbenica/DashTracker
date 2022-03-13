/*
 * Copyright 2022 Wesley T. Benica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wtb.dashTracker.util

import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.widget.Toast
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
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialogExport
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialogImport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.*
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


@ExperimentalCoroutinesApi
class CSVUtils {
    private val repository: Repository
        get() = Repository.get()

    fun export(entries: List<DashEntry>?, weeklies: List<Weekly>?, ctx: Context) {
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

        fun getSendFilesIntent(file: File): Intent {
            val intent = Intent(Intent.ACTION_SEND)
            val contentUri = FileProvider.getUriForFile(
                ctx, "${ctx.packageName}.fileprovider", file
            )
            intent.data = contentUri
            intent.putExtra(EXTRA_STREAM, contentUri)
            intent.flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            return intent
        }

        val dailyFile: File? = generateFile(ctx, getEntriesFileName())
        val weeklyFile: File? = generateFile(ctx, getWeekliesFileName())

        if (dailyFile != null && weeklyFile != null) {
            exportEntries(dailyFile, entries)
            exportWeeklies(weeklyFile, weeklies)

            val zipFile: File = zipFiles(ctx, dailyFile, weeklyFile)
            dailyFile.delete()
            weeklyFile.delete()

            (ctx as MainActivity).runOnUiThread {
                ConfirmationDialogExport(
                    ctx,
                    getSendFilesIntent(zipFile)
                ).show(ctx.supportFragmentManager, null)
            }
        } else {
            (ctx as MainActivity).runOnUiThread {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.csv_generation_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun import(ctx: Context) {
        (ctx as MainActivity).runOnUiThread {
            ConfirmationDialogImport(ctx).show(ctx.supportFragmentManager, null)
        }
    }

    private fun <T : DataModel> getModelsFromCsv(
        path: InputStream?,
        function: (Map<String, String>) -> T
    ): List<T>? =
        path?.let { inStream ->
            csvReader().readAllWithHeader(inStream).map { function(it) }
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