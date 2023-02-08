package ru.relabs.kurjer.data.models.common

import com.google.gson.annotations.SerializedName

/**
 * Created by Daniil Kurchanov on 06.01.2020.
 */
data class RadiusResponse(
    @SerializedName("closeAnyRadius") val closeAnyDistance: Boolean,
    @SerializedName("photoAnyDistance") val photoAnyDistance: Boolean,
    @SerializedName("storage_close_any_radius") val storageCloseAnyDistance: Boolean,
    @SerializedName("storage_photo_any_radius") val storagePhotoAnyDistance: Boolean
)