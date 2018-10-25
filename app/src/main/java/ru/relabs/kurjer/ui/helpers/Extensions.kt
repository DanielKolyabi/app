package ru.relabs.kurjer.ui.helpers

import android.view.View
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by ProOrange on 29.08.2018.
 */

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun Date.formated(): String {
    try {
        return SimpleDateFormat("dd.MM.YYYY", Locale("ru", "RU")).format(this)
    }catch (e: Throwable){
        e.printStackTrace()
        val cal = Calendar.getInstance().apply {
            time = this@formated
        }
        return "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH)}.${cal.get(Calendar.YEAR)}"
    }
}