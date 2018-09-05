package ru.relabs.kurjer.network.models

import ru.relabs.kurjer.models.AddressModel

data class AddressResponseModel(
        val id: Int,
        val street: String,
        val house: Int
) {
    fun toAddressModel(): AddressModel{
        return AddressModel(id, street, house)
    }
}
