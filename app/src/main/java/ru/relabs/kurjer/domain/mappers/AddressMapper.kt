package ru.relabs.kurjer.domain.mappers

import ru.relabs.kurjer.data.models.auth.AddressResponse
import ru.relabs.kurjer.domain.models.Address
import ru.relabs.kurjer.domain.models.AddressId

object AddressMapper {
    fun fromRaw(raw: AddressResponse) = Address(
        id = AddressId(raw.id),
        city = raw.city,
        street = raw.street,
        house = raw.house,
        houseName = raw.houseName,
        lat = raw.lat,
        long = raw.long
    )
}