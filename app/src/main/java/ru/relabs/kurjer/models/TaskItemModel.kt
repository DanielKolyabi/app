package ru.relabs.kurjer.models

import android.arch.persistence.room.*
import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class TaskItemModel(
        var address: AddressModel,
        var state: Int,
        var id: Int,
        var notes: List<String>,
        var entrances: List<Int>,
        var subarea: Int,
        var bypass: Int,
        var copies: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readParcelable(AddressModel::class.java.classLoader),
            parcel.readInt(),
            parcel.readInt(),
            parcel.createStringArrayList(),
            parcel.createIntArray().toList(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(address, flags)
        parcel.writeInt(state)
        parcel.writeInt(id)
        parcel.writeStringList(notes)
        parcel.writeIntArray(entrances.toIntArray())
        parcel.writeInt(subarea)
        parcel.writeInt(bypass)
        parcel.writeInt(copies)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TaskItemModel> {
        override fun createFromParcel(parcel: Parcel): TaskItemModel {
            return TaskItemModel(parcel)
        }

        override fun newArray(size: Int): Array<TaskItemModel?> {
            return arrayOfNulls(size)
        }

        val CREATED = 0
        val CLOSED = 1
    }
}