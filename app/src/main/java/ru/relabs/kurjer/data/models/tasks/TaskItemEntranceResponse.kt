package ru.relabs.kurjer.data.models.tasks

import com.google.gson.annotations.SerializedName

data class TaskItemEntranceResponse(
    @SerializedName("number") val number: Int,
    @SerializedName("apartments_count") val apartmentsCount: Int,
    @SerializedName("is_euro_boxes") val isEuroBoxes: Boolean,
    @SerializedName("has_lookout") val hasLookout: Boolean,
    @SerializedName("is_stacked") val isStacked: Boolean,
    @SerializedName("is_refused") val isRefused: Boolean,
    @SerializedName("photo_required") var photoRequired: Boolean,
    @SerializedName("flats_with_problems") val problemApartments: List<String>
)