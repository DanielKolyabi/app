package ru.relabs.kurjer.files

import android.graphics.Bitmap
import android.util.Log

/**
 * Created by ProOrange on 10.09.2018.
 */
object ImageUtils {
    fun resizeBitmap(b: Bitmap, width: Float, height: Float): Bitmap {
        Log.d("Resizer", "Target: $width x $height; Original: ${b.width} x ${b.height}")
        var newWidth = width
        var newHeight = height
        if (b.width > b.height) {
            newWidth = width
            newHeight = b.height.toFloat() * (height / b.width.toFloat())
        } else if (b.width < b.height) {
            newWidth = b.width.toFloat() * (width / b.height.toFloat())
            newHeight = height
        }
        Log.d("Resizer", "Calculated: $newWidth x $newHeight")
        return Bitmap.createScaledBitmap(b, newWidth.toInt(), newHeight.toInt(), false)
    }
}