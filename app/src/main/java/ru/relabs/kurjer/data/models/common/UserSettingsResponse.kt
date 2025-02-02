package ru.relabs.kurjer.data.models.common

import com.google.gson.annotations.SerializedName

data class UserSettingsResponse(
    @SerializedName("canSkipUpdate")
    val canSkipUpdates: Boolean,
    @SerializedName("canSkipUnfinishedTaskItem")
    val canSkipUnfinishedTaskItem: Boolean
)
