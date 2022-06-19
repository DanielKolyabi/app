package ru.relabs.kurjer.domain.models

data class AppSettings(
    val isCloseRadiusRequired: Boolean,
    val isPhotoRadiusRequired: Boolean,
    val gpsRefreshTimes: GpsRefreshTimes,
    val canSkipUpdates: Boolean
)
