package ru.relabs.kurjer.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import ru.relabs.kurjer.persistence.entities.AddressEntity

data class AddressModel(
        var id: Int,
        var street: String,
        var house: Int
) : Parcelable {

    val name: String
        get() = "ул. $street, д. $house"

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            //parcel.readString(),
            parcel.readString(),
            parcel.readInt()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        //parcel.writeString(name)
        parcel.writeString(street)
        parcel.writeInt(house)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun toAddressEntity(): AddressEntity {
        return AddressEntity(id, street, house)
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