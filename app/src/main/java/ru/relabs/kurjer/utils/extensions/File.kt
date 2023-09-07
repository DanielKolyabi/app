package ru.relabs.kurjer.utils.extensions

import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import java.io.File

@RequiresApi(Build.VERSION_CODES.R)
fun File.permittedMkDirs() {
    if (Environment.isExternalStorageManager())
        mkdirs()
}

@RequiresApi(Build.VERSION_CODES.R)
fun File.permittedCreateFile() {
    if (Environment.isExternalStorageManager())
        createNewFile()
}

fun File.checkedMkDirs() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        permittedMkDirs()
    else
        mkdirs()
}

fun File.checkedCreateFile() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        permittedCreateFile()
    else
        createNewFile()
}