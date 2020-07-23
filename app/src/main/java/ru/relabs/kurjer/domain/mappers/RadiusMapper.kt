package ru.relabs.kurjer.domain.mappers

import ru.relabs.kurjer.data.models.radius.RadiusResponse
import ru.relabs.kurjer.domain.models.AllowedCloseRadius

object RadiusMapper {
    fun fromRaw(raw: RadiusResponse): AllowedCloseRadius = when (raw.locked) {
        true -> AllowedCloseRadius.Required(raw.radius)
        else -> AllowedCloseRadius.NotRequired(raw.radius)
    }
}