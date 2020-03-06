package ru.relabs.kurjer.files

import android.content.ContentResolver
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Created by ProOrange on 10.09.2018.
 */
object ImageUtils {
    fun resizeBitmap(b: Bitmap, width: Float, height: Float): Bitmap {
        var newWidth = width
        var newHeight = height
        if (b.width > width) {
            newWidth = width
            newHeight = b.height.toFloat() * (width/b.width.toFloat())
        }
        if (b.height > height) {
            newWidth = b.width.toFloat() * (height / b.height.toFloat())
            newHeight = height
        }
        Log.d("Resizer", "Calculated: $newWidth x $newHeight")
        return Bitmap.createScaledBitmap(b, newWidth.toInt(), newHeight.toInt(), false)
    }

    fun saveImage(b: Bitmap, f: File, contentResolver: ContentResolver? = null) {

        val fos = FileOutputStream(f)
        b.compress(Bitmap.CompressFormat.JPEG, 75, fos)
        fos.close()
    }
}