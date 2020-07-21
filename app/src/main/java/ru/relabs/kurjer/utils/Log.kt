package ru.relabs.kurjer.utils

import android.util.Log

/**
 * Created by Daniil Kurchanov on 05.11.2019.
 */

fun Any.debug(message: String, throwable: Throwable? = null) {
    Log.d(
        if (javaClass.simpleName.isNotEmpty()) javaClass.simpleName else javaClass.name,
        message + " [${Thread.currentThread()}][${this.hashCode().toString(16)}]",
        throwable
    )
}

fun Throwable.debug(message: String) {
    this.debug(message, this)
}