package ru.relabs.kurjer

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by ProOrange on 02.10.2018.
 */
object CustomLog {
    fun getStacktraceAsString(e: Throwable): String{
        val stringBuffSync = StringWriter()
        val printWriter = PrintWriter(stringBuffSync)
        e.printStackTrace(printWriter)
        val stacktrace = stringBuffSync.toString()
        printWriter.close()
        return stacktrace
    }

    fun writeToFile(currentStacktrace: String)
    {
        try {

            //Gets the Android external storage directory & Create new folder Crash_Reports
            val dir = File(Environment.getExternalStorageDirectory(),
                    "deliveryman")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val dateFormat = SimpleDateFormat(
                    "yyyy_MM_dd_HH_mm_ss")
            val date = Date()
            val filename = "crash.log"

            // Write the file into the folder
            val reportFile = File(dir, filename)
            val fileWriter = FileWriter(reportFile, true)
            fileWriter.append("\n${dateFormat.format(date)}:\n")
            fileWriter.append(currentStacktrace)
            fileWriter.flush()
            fileWriter.close()

            if(reportFile.length() > 10*1024*1024){
                val writer = PrintWriter(reportFile)
                writer.print("")
                writer.close()
            }
        } catch (e: Exception) {
            Log.e("ExceptionHandler", e.message)
        }
    }
}