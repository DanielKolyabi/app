package ru.relabs.kurjer.network.models

import com.google.gson.annotations.SerializedName
import ru.relabs.kurjer.models.AddressModel

data class AddressResponseModel(
        val id: Int,
        @SerializedName("city")
        val city: String,
        val street: String,
        val house: Int,
        @SerializedName("house_name")
        val houseName: String,
        @SerializedName("lat")
        val lat: Float,
        @SerializedName("long")
        val long: Float
) {
    fun toAddressModel(): AddressModel{
        return AddressModel(id, city, street, house, houseName, lat.toDouble(), long.toDouble())
    }
}
