package ru.relabs.kurjer.data.models.radius

import com.google.gson.annotations.SerializedName

/**
 * Created by Daniil Kurchanov on 06.01.2020.
 */
data class RadiusResponse(
    @SerializedName("closeAnyRadius") val closeAnyDistance: Boolean,
    @SerializedName("photoAnyDistance") val photoAnyDistance: Boolean,
    @SerializedName("radius") val radius: Int
)