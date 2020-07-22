package ru.relabs.kurjer.domain.mappers

import ru.relabs.kurjer.data.models.UpdatesResponse
import ru.relabs.kurjer.domain.models.AppUpdate
import ru.relabs.kurjer.domain.models.AppUpdatesInfo

object UpdatesMapper {
    fun fromRaw(raw: UpdatesResponse): AppUpdatesInfo = AppUpdatesInfo(
        required = raw.required?.let {
            AppUpdate(
                version = it.version,
                url = it.url,
                isRequired = it.isRequired
            )
        },
        optional = raw.optional?.let {
            AppUpdate(
                version = it.version,
                url = it.url,
                isRequired = it.isRequired
            )
        }
    )
}