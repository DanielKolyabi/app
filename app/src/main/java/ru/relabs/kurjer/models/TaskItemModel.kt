package ru.relabs.kurjer.models

import android.os.Parcel
import android.os.Parcelable
import ru.relabs.kurjer.persistence.entities.TaskItemEntity

data class EntranceModel(
        val num: Int,
        var coupleEnabled: Boolean
)

data class TaskItemModel(
        var address: AddressModel,
        var state: Int,
        var id: Int,
        var notes: List<String>,
        var entrances: List<EntranceModel>,
        var subarea: Int,
        var bypass: Int,
        var copies: Int,
        var needPhoto: Boolean,
        var entrancesData: List<EntranceDataModel>
) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readParcelable(AddressModel::class.java.classLoader)!!,
            parcel.readInt(),
            parcel.readInt(),
            parcel.createStringArrayList()!!,
            parcel.createIntArray()!!.toList().map {
                EntranceModel(it, false)
            },
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readByte() != 0.toByte(),
            parcel.createTypedArrayList(EntranceDataModel)!!
    )

    fun toTaskItemEntity(parentTaskId: Int): TaskItemEntity {
        return TaskItemEntity(
                address.id, state, id, notes, entrances.map { it.num }, subarea, bypass, copies, parentTaskId, needPhoto
        )
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(address, flags)
        parcel.writeInt(state)
        parcel.writeInt(id)
        parcel.writeStringList(notes)
        parcel.writeIntArray(entrances.map { it.num }.toIntArray())
        parcel.writeInt(subarea)
        parcel.writeInt(bypass)
        parcel.writeInt(copies)
        parcel.writeByte(if (needPhoto) 1 else 0)
        parcel.writeTypedList(entrancesData)
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