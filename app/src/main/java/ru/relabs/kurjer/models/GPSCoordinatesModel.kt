package ru.relabs.kurjer.models

import java.util.*

data class GPSCoordinatesModel(
        val lat: Double,
        val long: Double,
        val time: Date
) {
    val isEmpty: Boolean
        get() = lat == 0.0 || long == 0.0

    val isOld: Boolean
        get() = time.time - Date().time > 3 * 60 * 1000
}
