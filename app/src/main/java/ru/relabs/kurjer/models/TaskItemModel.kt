package ru.relabs.kurjer.models

import android.arch.persistence.room.*
import android.os.Parcel
import android.os.Parcelable

@Entity(tableName = "task_items", foreignKeys = [ForeignKey(
        entity = TaskModel::class,
        parentColumns = ["id"],
        childColumns = ["task_id"]
), ForeignKey(
        entity = AddressModel::class,
        parentColumns = ["id"],
        childColumns = ["address_id"]
)])

data class TaskItemModel(
        @Ignore
        var address: AddressModel,
        @ColumnInfo(name = "address_id")
        var addressId: Int,
        var state: Int,
        @PrimaryKey
        var id: Int,
        var notes: List<String>,
        var entrances: List<Int>,
        var subarea: Int,
        var bypass: Int,
        var copies: Int,
        @ColumnInfo(name = "task_id")
        var taskId: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readParcelable(AddressModel::class.java.classLoader),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.createStringArrayList(),
            parcel.createIntArray().toList(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt()) {
    }

    constructor(): this(AddressModel(), 0, 0, 0, listOf<String>(), listOf<Int>(), 0, 0, 0, 0)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(address, flags)
        parcel.writeInt(addressId)
        parcel.writeInt(state)
        parcel.writeInt(id)
        parcel.writeStringList(notes)
        parcel.writeIntArray(entrances.toIntArray())
        parcel.writeInt(subarea)
        parcel.writeInt(bypass)
        parcel.writeInt(copies)
        parcel.writeInt(taskId)
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