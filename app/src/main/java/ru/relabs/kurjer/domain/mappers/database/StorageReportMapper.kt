package ru.relabs.kurjer.domain.mappers.database

import ru.relabs.kurjer.data.database.entities.storage.StorageReportEntity
import ru.relabs.kurjer.domain.models.StorageId
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import ru.relabs.kurjer.domain.models.storage.StorageReport

object StorageReportMapper {
    fun fromEntity(entity: StorageReportEntity): StorageReport = StorageReport(
        id = StorageReportId(entity.id),
        storageId = StorageId(entity.storageId),
        taskIds = entity.taskIds.map { TaskId(it) },
        closeTime = entity.closeTime,
        gps = entity.gps,
        description = entity.description,
        isClosed = entity.isClosed
    )


    fun toEntity(report: StorageReport): StorageReportEntity = StorageReportEntity(
        id = report.id.id,
        storageId = report.storageId.id,
        taskIds = report.taskIds.map { it.id },
        closeTime = report.closeTime,
        gps = report.gps,
        description = report.description,
        isClosed = report.isClosed,
    )
}