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
        var city: String,
        var street: String,
        var house: Int,
        var houseName: String,
        var lat: Double,
        var long: Double
) : Parcelable {

    val name: String
        get() = "$street, д. $houseName"

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString(),
            parcel.readString(),
            parcel.readInt(),
            parcel.readString(),
            parcel.readDouble(),
            parcel.readDouble()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(city)
        parcel.writeString(street)
        parcel.writeInt(house)
        parcel.writeString(houseName)
        parcel.writeDouble(lat)
        parcel.writeDouble(long)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun toAddressEntity(): AddressEntity {
        return AddressEntity(id, city, street, house, houseName, lat, long)
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