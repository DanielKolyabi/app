package ru.relabs.kurjer.domain.models.storage

import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import ru.relabs.kurjer.domain.models.StorageId
import ru.relabs.kurjer.domain.models.TaskId
import java.util.*

data class StorageReportId(val id: Int)

data class StorageReport(
    val id: StorageReportId,
    val storageId: StorageId,
    val taskIds: List<TaskId>,
    val closeTime: Date?,
    val gps: GPSCoordinatesModel,
    val description: String,
    val isClosed: Boolean
)