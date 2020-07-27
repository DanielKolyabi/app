package ru.relabs.kurjer.domain.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class AddressId(val id: Int) : Parcelable

@Parcelize
data class Address(
    val id: AddressId,
    val city: String,
    val street: String,
    val house: Int,
    val houseName: String,
    val lat: Float,
    val long: Float
): Parcelable {

    val name: String
        get() = "$street, д. $houseName"

}