package ru.relabs.kurjer.domain.mappers.database

import ru.relabs.kurjer.data.database.entities.storage.ReportCloseDataEntity
import ru.relabs.kurjer.data.database.entities.storage.StorageReportEntity
import ru.relabs.kurjer.domain.models.StorageId
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.storage.ReportCloseData
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import ru.relabs.kurjer.domain.models.storage.StorageReport

object StorageReportMapper {
    fun fromEntity(entity: StorageReportEntity): StorageReport = StorageReport(
        id = StorageReportId(entity.id),
        storageId = StorageId(entity.storageId),
        taskIds = entity.taskIds.map { TaskId(it) },
        gps = entity.gps,
        description = entity.description,
        closeData = dataFromEntity(entity.closeData)
    )


    fun toEntity(report: StorageReport): StorageReportEntity = StorageReportEntity(
        id = report.id.id,
        storageId = report.storageId.id,
        taskIds = report.taskIds.map { it.id },
        gps = report.gps,
        description = report.description,
        isClosed = report.closeData != null,
        closeData = dataToEntity(report.closeData)
    )

    private fun dataFromEntity(entity: ReportCloseDataEntity?) = when (entity) {
        null -> null
        else -> {
            ReportCloseData(
                entity.closeTime,
                entity.batteryLevel,
                entity.deviceRadius,
                entity.deviceCloseAnyDistance,
                entity.deviceAllowedDistance,
                entity.isPhotoRequired,
            )
        }
    }

    private fun dataToEntity(data: ReportCloseData?) = when (data) {
        null -> null
        else -> {
            ReportCloseDataEntity(
                data.closeTime,
                data.batteryLevel,
                data.deviceRadius,
                data.deviceCloseAnyDistance,
                data.deviceAllowedDistance,
                data.isPhotoRequired,
            )
        }
    }
}