package ru.relabs.kurjer.models

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * Created by ProOrange on 29.08.2018.
 */
sealed class DataModels

data class AddressModel(
        val id: Int,
        val name: String
) : Parcelable, DataModels() {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AddressModel> {
        override fun createFromParcel(parcel: Parcel): AddressModel {
            return AddressModel(parcel)
        }

        override fun newArray(size: Int): Array<AddressModel?> {
            return arrayOfNulls(size)
        }
    }
}

data class TaskItemModel(
        val address: AddressModel,
        val state: Int,
        val id: Int,
        val notes: List<String>,
        val entrances: Int
) : Parcelable, DataModels() {
    constructor(parcel: Parcel) : this(
            parcel.readParcelable(AddressModel::class.java.classLoader),
            parcel.readInt(),
            parcel.readInt(),
            parcel.createStringArrayList(),
            parcel.readInt()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(address, flags)
        parcel.writeInt(state)
        parcel.writeInt(id)
        parcel.writeStringList(notes)
        parcel.writeInt(entrances)
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
    }


}

data class TaskModel(
        val id: Int,
        val publisher: String,
        val copies: Int,
        val area: Int,
        val state: Int,
        val startTime: Date,
        val endTime: Date,
        val region: Int,
        val brigade: Int,
        val brigader: String,
        val rastMapUrl: String,
        val userId: Int,
        val items: List<TaskItemModel>,

        //Temporary, for some features in lists
        var selected: Boolean
) : Parcelable, DataModels() {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString(),
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
            parcel.readByte() != 0.toByte()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(publisher)
        parcel.writeInt(copies)
        parcel.writeInt(area)
        parcel.writeInt(state)
        parcel.writeSerializable(startTime)
        parcel.writeSerializable(endTime)
        parcel.writeInt(region)
        parcel.writeInt(brigade)
        parcel.writeString(brigader)
        parcel.writeString(rastMapUrl)
        parcel.writeInt(userId)
        parcel.writeList(items)
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

object DetailsTableHeader : DataModels()