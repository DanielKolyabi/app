package ru.relabs.kurjer.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {
    fun copyToClipboard(context: Activity, text: String): Either<Exception, Unit> = Either.of {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Text", text))
    }
}
