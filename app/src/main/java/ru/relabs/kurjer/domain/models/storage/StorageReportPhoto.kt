package ru.relabs.kurjer.domain.models.storage

import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import java.util.*

data class StorageReportPhoto(
    val id: StoragePhotoId,
    val UUID: String,
    val reportId: ReportId,
    val gps: GPSCoordinatesModel,
    val time: Date
)

data class StoragePhotoId(val id:Int)