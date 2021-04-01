package ru.relabs.kurjer.domain.mappers

import ru.relabs.kurjer.data.models.common.SettingsResponse
import ru.relabs.kurjer.domain.mappers.network.RadiusMapper
import ru.relabs.kurjer.domain.models.AppSettings
import ru.relabs.kurjer.domain.models.GpsRefreshTimes

object SettingsMapper {
    fun fromRaw(raw: SettingsResponse) = AppSettings(
        radius = RadiusMapper.fromRaw(raw.radius),
        gpsRefreshTimes = GpsRefreshTimes(
            close = raw.gpsRefreshTimes.close,
            photo = raw.gpsRefreshTimes.photo
        )
    )
}
