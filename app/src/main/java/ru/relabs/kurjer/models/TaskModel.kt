package ru.relabs.kurjer.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import java.util.*

@Entity(tableName = "tasks")
data class TaskModel(
        @PrimaryKey
        var id: Int,
        var name: String,
        var edition: Int,
        var copies: Int,
        var packs: Int,
        var remain: Int,
        var area: Int,
        var state: Int,
        @ColumnInfo(name = "start_time")
        var startTime: Date,
        @ColumnInfo(name = "end_time")
        var endTime: Date,
        var region: Int,
        var brigade: Int,
        var brigadier: String,
        @ColumnInfo(name = "rast_map_url")
        var rastMapUrl: String,
        @ColumnInfo(name = "user_id")
        var userId: Int,
        @Ignore
        var items: List<TaskItemModel>,
        var city: String,
        @ColumnInfo(name = "storage_address")
        var storageAddress: String,

        //Temporary var, for some features in lists
        var selected: Boolean
) : Parcelable {

    constructor() : this(0, "", 0, 0, 0, 0, 0, 0, Date(), Date(), 0, 0, "", "", 0, listOf<TaskItemModel>(), "", "", false)

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readSerializable() as Date,
            parcel.readSerializable() as Date,
            parcel.readInt(),
            parcel.readInt(),
            parcel.readString(),
            parcel.readString(),
            parcel.readInt(),
            arrayListOf<TaskItemModel>().apply {
                parcel.readList(this, TaskItemModel::class.java.classLoader)
            },
            parcel.readString(),
            parcel.readString(),
            parcel.readByte() != 0.toByte()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeInt(edition)
        parcel.writeInt(copies)
        parcel.writeInt(packs)
        parcel.writeInt(remain)
        parcel.writeInt(area)
        parcel.writeInt(state)
        parcel.writeSerializable(startTime)
        parcel.writeSerializable(endTime)
        parcel.writeInt(region)
        parcel.writeInt(brigade)
        parcel.writeString(brigadier)
        parcel.writeString(rastMapUrl)
        parcel.writeInt(userId)
        parcel.writeList(items)
        parcel.writeString(city)
        parcel.writeString(storageAddress)
        parcel.writeByte(if (selected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TaskModel> {
        override fun createFromParcel(parcel: Parcel): TaskModel {
            return TaskModel(parcel)
        }

        override fun newArray(size: Int): Array<TaskModel?> {
            return arrayOfNulls(size)
        }
    }
}