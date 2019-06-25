package ru.relabs.kurjer.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import ru.relabs.kurjer.persistence.entities.EntranceDataEntity

data class EntranceDataModel(
        val number: Int,
        @SerializedName("apartments_count")
        val apartmentsCount: Int,
        @SerializedName("is_euro_boxes")
        val isEuroBoxes: Boolean,
        @SerializedName("has_lookout")
        val hasLookout: Boolean,
        @SerializedName("is_stacked")
        val isStacked: Boolean,
        @SerializedName("is_refused")
        val isRefused: Boolean
) : Parcelable {
    val state: Int
        get() {
            var state = 0x0000
            if(isEuroBoxes) state = state xor 0x0001
            if(hasLookout) state = state xor 0x0010
            if(isStacked) state = state xor 0x0100
            if(isRefused) state = state xor 0x1000
            return state
        }

    fun toEntity(taskItemId: Int): EntranceDataEntity {
        return EntranceDataEntity(0, taskItemId, number, apartmentsCount, isEuroBoxes, hasLookout, isStacked, isRefused)
    }

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readInt(),
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(number)
        parcel.writeInt(apartmentsCount)
        parcel.writeByte(if (isEuroBoxes) 1 else 0)
        parcel.writeByte(if (hasLookout) 1 else 0)
        parcel.writeByte(if (isStacked) 1 else 0)
        parcel.writeByte(if (isRefused) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EntranceDataModel> {
        override fun createFromParcel(parcel: Parcel): EntranceDataModel {
            return EntranceDataModel(parcel)
        }

        override fun newArray(size: Int): Array<EntranceDataModel?> {
            return arrayOfNulls(size)
        }
    }
}
