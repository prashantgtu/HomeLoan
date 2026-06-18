package com.example

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

fun exportScheduleToCsv(context: Context, summary: LoanSummary) {
    val csvContent = buildString {
        append("Month,Open Balance,EMI,Principal,Interest,Prepayment,Close Balance,Rate (%)\n")
        summary.schedule.forEach { row ->
            append("${row.month},")
            append("%.2f,".format(row.openBalance))
            append("%.2f,".format(row.emi))
            append("%.2f,".format(row.principalPaid))
            append("%.2f,".format(row.interestPaid))
            append("%.2f,".format(row.prepayment))
            append("%.2f,".format(row.closeBalance))
            append("%.2f\n".format(row.rate))
        }
    }

    try {
        val fileName = "LoanSchedule_${System.currentTimeMillis()}.csv"
        var outStream: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                outStream = resolver.openOutputStream(uri)
            }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, fileName)
            outStream = FileOutputStream(file)
        }

        outStream?.use {
            it.write(csvContent.toByteArray())
        }
        Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to export", Toast.LENGTH_SHORT).show()
    }
}
