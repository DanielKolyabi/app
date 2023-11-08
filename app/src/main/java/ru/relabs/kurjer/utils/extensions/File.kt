package ru.relabs.kurjer.utils.extensions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File


fun File.checkedMkDirs(ctx: Context) {
    if (checkStoragePermissions(ctx)) {
        mkdirs()
    }
}

fun File.checkedCreateFile(ctx: Context) {
    if (checkStoragePermissions(ctx)) {
        createNewFile()
    }
}

fun checkStoragePermissions(ctx: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        //Android is 11 (R) or above
        Environment.isExternalStorageManager()
    } else {
        //Below android 11
        val write = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val read = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
    }
}