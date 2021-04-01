package ru.relabs.kurjer.domain.models

import ru.relabs.kurjer.data.models.common.GpsRefreshTimesResponse

data class AppSettings(
    val radius: AllowedCloseRadius,
    val gpsRefreshTimes: GpsRefreshTimes
)
