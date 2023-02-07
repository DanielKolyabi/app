package ru.relabs.kurjer.domain.models.storage

import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import ru.relabs.kurjer.domain.models.StorageId
import ru.relabs.kurjer.domain.models.TaskId
import java.util.*

data class ReportId(val id: Int)

data class StorageReport(
    val id: ReportId,
    val storageId: StorageId,
    val photo: StorageReportPhoto,
    val taskIds: List<TaskId>,
    val closeTime: Date?,
    val gps: GPSCoordinatesModel,
    val description: String,
    val isClosed: Boolean
)