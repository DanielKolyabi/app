package ru.relabs.kurjer.data.models.pause

import com.google.gson.annotations.SerializedName

/**
 * Created by Daniil Kurchanov on 06.01.2020.
 */
data class PauseCheckResponse(
    @SerializedName("loading") val loading: Int,
    @SerializedName("lunch") val lunch: Int,
    @SerializedName("control") val control: Int
)