package ru.relabs.kurjer.domain.models

data class AppSettings(
    val isCloseRadiusRequired: Boolean,
    val isPhotoRadiusRequired: Boolean,
    val isStorageCloseRadiusRequired: Boolean,
    val isStoragePhotoRadiusRequired: Boolean,
    val gpsRefreshTimes: GpsRefreshTimes,
    val canSkipUpdates: Boolean,
    val canSkipUnfinishedTaskitem: Boolean
)
