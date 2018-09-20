package ru.relabs.kurjer.network.models

import com.google.gson.annotations.SerializedName

data class UpdateInfo(
        val version: Int,
        val url: String,
        @SerializedName("is_required")
        val isRequired: Boolean
)

data class UpdateInfoResponse(
        val last_required: UpdateInfo,
        val last_optional: UpdateInfo
)
