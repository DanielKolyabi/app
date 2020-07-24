package ru.relabs.kurjer.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.relabs.kurjer.data.database.entities.AddressEntity

@Parcelize
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


    fun toAddressEntity(): AddressEntity {
        return AddressEntity(id, city, street, house, houseName, lat, long)
    }
}