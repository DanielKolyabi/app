package ru.relabs.kurjer.ui.helpers

import android.view.View
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.SimpleFormatter

/**
 * Created by ProOrange on 29.08.2018.
 */

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun Date.formated(): String {
    val formatter = SimpleDateFormat("dd.MM.YYYY", Locale("ru", "RU"))
    return formatter.format(this)
}