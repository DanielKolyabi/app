package ru.relabs.kurjer.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object IntentUtils {
    fun getImageViewIntent(file: File, ctx: Context): Intent {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        val uri = FileProvider.getUriForFile(ctx, "com.relabs.kurjer.file_provider", file)
        intent.setDataAndType(uri, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return intent
    }

    fun getShareTextIntent(title: String, text: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }

}