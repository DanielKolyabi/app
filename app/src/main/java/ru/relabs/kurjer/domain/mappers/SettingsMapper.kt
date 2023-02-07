package ru.relabs.kurjer.domain.mappers

import ru.relabs.kurjer.data.models.common.SettingsResponse
import ru.relabs.kurjer.domain.models.AppSettings
import ru.relabs.kurjer.domain.models.GpsRefreshTimes

object SettingsMapper {
    fun fromRaw(raw: SettingsResponse) = AppSettings(
        isCloseRadiusRequired = !raw.radius.closeAnyDistance,
        isPhotoRadiusRequired = !raw.radius.photoAnyDistance,
        isStorageCloseRadiusRequired = !raw.radius.storageCloseAnyDistance,
        isStoragePhotoRadiusRequired = !raw.radius.storagePhotoAnyDistance,
        gpsRefreshTimes = GpsRefreshTimes(
            close = raw.gpsRefreshTimes.close,
            photo = raw.gpsRefreshTimes.photo
        ),
        canSkipUpdates = raw.userSettings.canSkipUpdates,
        canSkipUnfinishedTaskitem = raw.userSettings.canSkipUnfinishedTaskItem
    )
}
