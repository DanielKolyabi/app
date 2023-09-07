package ru.relabs.kurjer.domain.models

import java.io.Serializable

data class GpsRefreshTimes(
    val close: Int,
    val photo: Int
) : Serializable
