package ru.relabs.kurjer.models

import android.os.Parcel
import android.os.Parcelable
import ru.relabs.kurjer.persistence.entities.TaskEntity
import java.util.*

data class TaskModel(
        var id: Int,
        var name: String,
        var edition: Int,
        var copies: Int,
        var packs: Int,
        var remain: Int,
        var area: Int,
        var state: Int,
        var startTime: Date,
        var endTime: Date,
        var brigade: Int,
        var brigadier: String,
        var rastMapUrl: String,
        var userId: Int,
        var items: List<TaskItemModel>,
        var city: String,
        var storageAddress: String,
        var iteration: Int,
        //Temporary var, for some features in lists
        var selected: Boolean
) : Parcelable {

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
            parcel.readString(),
            parcel.readString(),
            parcel.readInt(),
            arrayListOf<TaskItemModel>().apply {
                parcel.readList(this, TaskItemModel::class.java.classLoader)
            },
            parcel.readString(),
            parcel.readString(),
            parcel.readInt(),
            parcel.readByte() != 0.toByte())

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
        parcel.writeInt(brigade)
        parcel.writeString(brigadier)
        parcel.writeString(rastMapUrl)
        parcel.writeInt(userId)
        parcel.writeList(items)
        parcel.writeString(city)
        parcel.writeString(storageAddress)
        parcel.writeInt(iteration)
        parcel.writeByte(if (selected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun toTaskEntity(): TaskEntity {
        return TaskEntity(
                id, name, edition, copies, packs, remain, area, state, startTime, endTime, brigade, brigadier, rastMapUrl, userId,
                city, storageAddress, iteration
        )
    }

    fun isAvailableByDate(date: Date): Boolean = (date >= startTime && date <= Date(endTime.time + 3 * 24 * 60 * 60 * 1000))


    companion object CREATOR : Parcelable.Creator<TaskModel> {
        override fun createFromParcel(parcel: Parcel): TaskModel {
            return TaskModel(parcel)
        }

        override fun newArray(size: Int): Array<TaskModel?> {
            return arrayOfNulls(size)
        }

        val CREATED = 0
        val EXAMINED = 1
        val STARTED = 2
        val COMPLETED = 4
        val BY_OTHER_USER = 8
    }
}