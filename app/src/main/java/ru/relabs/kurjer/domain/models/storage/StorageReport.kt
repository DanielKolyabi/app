package ru.relabs.kurjer.domain.models.storage

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import ru.relabs.kurjer.domain.models.StorageId
import ru.relabs.kurjer.domain.models.TaskId
import java.util.*

@Parcelize
data class StorageReportId(val id: Int) : Parcelable

data class StorageReport(
    val id: StorageReportId,
    val storageId: StorageId,
    val taskIds: List<TaskId>,
    val gps: GPSCoordinatesModel,
    val description: String,
    val closeData: ReportCloseData?
) {
    val isClosed: Boolean
        get() = closeData != null
}

data class ReportCloseData(
    val closeTime: Date,
    val batteryLevel: Int,
    val deviceRadius: Int,
    val deviceCloseAnyDistance: Boolean,
    val deviceAllowedDistance: Int,
    val isPhotoRequired: Boolean
)