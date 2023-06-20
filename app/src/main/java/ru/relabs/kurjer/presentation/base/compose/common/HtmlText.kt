package ru.relabs.kurjer.presentation.base.compose.common

import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
fun HtmlText(
    html: String,
    textSize: Float,
    modifier: Modifier = Modifier,
    gravity: Int = Gravity.START,
    typeface: Typeface? = null,
    textColor: Int? = null
) {
    AndroidView(
        factory = { context -> TextView(context) },
        update = {
            it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            it.textSize = textSize
            textColor?.let { color -> it.setTextColor(color) }
            it.gravity = gravity
            typeface?.let { tf -> it.typeface = tf }
        },
        modifier = modifier
    )
}