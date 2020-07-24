package ru.relabs.kurjer.domain.mappers

import android.net.Uri
import ru.relabs.kurjer.data.models.UpdatesResponse
import ru.relabs.kurjer.domain.models.AppUpdate
import ru.relabs.kurjer.domain.models.AppUpdatesInfo
import java.net.URL

object UpdatesMapper {
    fun fromRaw(raw: UpdatesResponse): AppUpdatesInfo = AppUpdatesInfo(
        required = raw.required?.let {
            AppUpdate(
                version = it.version,
                url = Uri.parse(it.url),
                isRequired = it.isRequired
            )
        },
        optional = raw.optional?.let {
            AppUpdate(
                version = it.version,
                url = Uri.parse(it.url),
                isRequired = it.isRequired
            )
        }
    )
}