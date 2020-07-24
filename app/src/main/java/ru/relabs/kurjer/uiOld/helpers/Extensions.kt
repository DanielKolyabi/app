package ru.relabs.kurjer.uiOld.helpers

import android.view.View
import org.joda.time.DateTime
import java.util.*

/**
 * Created by ProOrange on 29.08.2018.
 */

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun Date.formated(): String {
    return DateTime(this).toString("dd.MM.yyyy")
}

fun Date.formatedWithSecs(): String {
    return DateTime(this).toString("dd.MM.yyyy HH:mm:ss")
}