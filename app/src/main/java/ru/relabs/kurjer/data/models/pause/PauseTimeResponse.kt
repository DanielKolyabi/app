package ru.relabs.kurjer.data.models.pause

import com.google.gson.annotations.SerializedName

/**
 * Created by Daniil Kurchanov on 06.01.2020.
 */

data class PauseTimeResponse(
    @SerializedName("start") val start: PauseTimesResponse,
    @SerializedName("end") val end: PauseTimesResponse
)

data class PauseTimesResponse(
    @SerializedName("loading") val loading: Long,
    @SerializedName("lunch") val lunch: Long
)

