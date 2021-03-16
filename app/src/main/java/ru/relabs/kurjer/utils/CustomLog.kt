package ru.relabs.kurjer.utils

import android.app.Activity
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import org.joda.time.DateTime
import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.domain.providers.PathsProvider
import java.io.*

/**
 * Created by ProOrange on 02.10.2018.
 */

object CustomLog: KoinComponent {
    private val pathsProvider: PathsProvider by inject()

    fun getStacktraceAsString(e: Throwable): String {
        val stringBuffSync = StringWriter()
        val printWriter = PrintWriter(stringBuffSync)
        e.printStackTrace(printWriter)
        val stacktrace = stringBuffSync.toString()
        printWriter.close()
        return stacktrace
    }

    fun share(context: Activity): Either<Exception, Unit> = Either.of {
        val f = pathsProvider.getCrashLogFile()
        if (!f.exists()) {
            throw FileNotFoundException()
        }

        val uri = FileProvider.getUriForFile(context, "com.relabs.kurjer.file_provider", f)
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "crash.log")
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "text/*"
        }
        context.startActivity(intent)
    }

    fun writeToFile(currentStacktrace: String) {
        try {
            // Write the file into the folder
            val reportFile = pathsProvider.getCrashLogFile()
            val fileWriter = FileWriter(reportFile, true)
            fileWriter.append("\n${DateTime().toString("yyyy-MM-dd'T'HH:mm:ss")} Ver.${BuildConfig.VERSION_NAME}:\n")
            fileWriter.append(currentStacktrace)
            fileWriter.flush()
            fileWriter.close()

            if (reportFile.length() > 5 * 1024 * 1024) {
                val writer = PrintWriter(reportFile)
                writer.print("")
                writer.close()
            }

            Log.d("CustomLog", currentStacktrace)
        } catch (e: Exception) {
            Log.e("ExceptionHandler", e.message)
        }
    }
}