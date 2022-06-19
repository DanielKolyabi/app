package ru.relabs.kurjer.data.models.common

import com.google.gson.annotations.SerializedName

data class UserSettingsResponse(
    @SerializedName("canSkipUpdates")
    val canSkipUpdates: Boolean
)
