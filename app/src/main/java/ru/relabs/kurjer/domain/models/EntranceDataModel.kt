package ru.relabs.kurjer.domain.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import ru.relabs.kurjer.data.database.entities.EntranceDataEntity

@Parcelize
data class EntranceDataModel(
    @SerializedName("number") val number: Int,
    @SerializedName("apartments_count") val apartmentsCount: Int,
    @SerializedName("is_euro_boxes") val isEuroBoxes: Boolean,
    @SerializedName("has_lookout") val hasLookout: Boolean,
    @SerializedName("is_stacked") val isStacked: Boolean,
    @SerializedName("is_refused") val isRefused: Boolean,
    @SerializedName("photo_required") var photoRequired: Boolean
) : Parcelable {
    val state: Int
        get() {
            var state = 0x0000
            if (isEuroBoxes) state = state xor 0x0001
            if (hasLookout) state = state xor 0x0010
            if (isStacked) state = state xor 0x0100
            if (isRefused) state = state xor 0x1000
            return state
        }

    fun toEntity(taskItemId: Int): EntranceDataEntity {
        return EntranceDataEntity(0, taskItemId, number, apartmentsCount, isEuroBoxes, hasLookout, isStacked, isRefused, photoRequired)
    }
}
