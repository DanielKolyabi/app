package ru.relabs.kurjer.domain.models.photo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ru.relabs.kurjer.domain.models.EntranceNumber
import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import java.util.Date


@Parcelize
data class PhotoId(val id: Int) : Parcelable

@Parcelize
data class TaskItemPhoto(
    val id: PhotoId,
    val uuid: String,
    val taskItemId: TaskItemId,
    val entranceNumber: EntranceNumber
)  : Parcelable

@Parcelize
data class StoragePhotoId(val id: Int) : Parcelable

data class StorageReportPhoto(
    val id: StoragePhotoId,
    val uuid: String,
    val storageReportId: StorageReportId,
    val gps: GPSCoordinatesModel,
    val time: Date
)
