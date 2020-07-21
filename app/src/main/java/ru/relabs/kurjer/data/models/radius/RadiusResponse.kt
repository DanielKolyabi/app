package ru.relabs.kurjer.data.models.radius

import com.google.gson.annotations.SerializedName

/**
 * Created by Daniil Kurchanov on 06.01.2020.
 */
data class RadiusResponse(
    @SerializedName("locked") val locked: Boolean,
    @SerializedName("radius") val radius: Int
)