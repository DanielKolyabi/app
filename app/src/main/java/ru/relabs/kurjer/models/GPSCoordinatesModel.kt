package ru.relabs.kurjer.models

import java.util.*

data class GPSCoordinatesModel(
        val lat: Double,
        val long: Double,
        val time: Date
) {
    val isEmpty: Boolean
        get() = lat == 0.0 || long == 0.0
}
