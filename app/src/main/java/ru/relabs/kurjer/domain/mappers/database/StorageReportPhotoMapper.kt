package ru.relabs.kurjer.domain.mappers.database

import ru.relabs.kurjer.data.database.entities.storage.StorageReportPhotoEntity
import ru.relabs.kurjer.domain.models.storage.StoragePhotoId
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import ru.relabs.kurjer.domain.models.storage.StorageReportPhoto

object StorageReportPhotoMapper {
    fun fromEntity(entity: StorageReportPhotoEntity): StorageReportPhoto = StorageReportPhoto(
        id = StoragePhotoId(entity.id),
        uuid = entity.UUID,
        storageReportId = StorageReportId(entity.reportId),
        gps = entity.gps,
        time = entity.time
    )

    fun toEntity(photo: StorageReportPhoto): StorageReportPhotoEntity = StorageReportPhotoEntity(
        id = photo.id.id,
        UUID = photo.uuid,
        reportId = photo.storageReportId.id,
        gps = photo.gps,
        time = photo.time
    )

}