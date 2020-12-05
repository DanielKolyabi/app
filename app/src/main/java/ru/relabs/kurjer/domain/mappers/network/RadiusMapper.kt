package ru.relabs.kurjer.domain.mappers.network

import ru.relabs.kurjer.data.models.radius.RadiusResponse
import ru.relabs.kurjer.domain.models.AllowedCloseRadius

object RadiusMapper {
    fun fromRaw(raw: RadiusResponse): AllowedCloseRadius = when (raw.closeAnyDistance) {
        true -> AllowedCloseRadius.NotRequired(raw.radius, raw.photoAnyDistance)
        else -> AllowedCloseRadius.Required(raw.radius, raw.photoAnyDistance)
    }
}