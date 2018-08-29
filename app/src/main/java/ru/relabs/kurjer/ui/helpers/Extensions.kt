package ru.relabs.kurjer.ui.helpers

import android.view.View

/**
 * Created by ProOrange on 29.08.2018.
 */

fun View.setVisible(visible: Boolean){
    this.visibility = if(visible) View.VISIBLE else View.GONE
}