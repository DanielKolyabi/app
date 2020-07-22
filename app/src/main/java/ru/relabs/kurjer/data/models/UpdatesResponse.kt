package ru.relabs.kurjer.data.models

import com.google.gson.annotations.SerializedName

data class UpdateDataResponse(
    @SerializedName("version") val version: Int,
    @SerializedName("url") val url: String,
    @SerializedName("is_required") val isRequired: Boolean
)

data class UpdatesResponse(
    @SerializedName("last_required") val required: UpdateDataResponse?,
    @SerializedName("last_optional") val optional: UpdateDataResponse?
)
