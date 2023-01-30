package ru.relabs.kurjer.utils

import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Created by Daniil Kurchanov on 13.01.2020.
 */

fun Double.toRadians() = Math.toRadians(this)

fun calculateDistance(fromLat: Double, fromLong: Double, toLat: Double, toLong: Double): Double {
    return calculateDistance(
        GPSCoordinatesModel(fromLat, fromLong, Date()),
        GPSCoordinatesModel(toLat, toLong, Date())
    )
}

fun calculateDistance(from: GPSCoordinatesModel, to: GPSCoordinatesModel): Double {
    val r = 6371e3
    val f1 = from.lat.toRadians()
    val f2 = to.lat.toRadians()

    val dF = (to.lat - from.lat).toRadians()
    val dL = (to.long - from.long).toRadians()

    val a = sin(dF / 2) * sin(dF / 2) + cos(f1) * cos(f2) * sin(dL / 2) * sin(dL / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return r * c
}