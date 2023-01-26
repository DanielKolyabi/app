package ru.relabs.kurjer.data.models.common

import com.google.gson.annotations.SerializedName

data class SettingsResponse(
    @SerializedName("radius")
    val radius: RadiusResponse,
    @SerializedName("gpsRefreshTime")
    val gpsRefreshTimes: GpsRefreshTimesResponse,
    @SerializedName("userSettings")
    val userSettings: UserSettingsResponse
)
