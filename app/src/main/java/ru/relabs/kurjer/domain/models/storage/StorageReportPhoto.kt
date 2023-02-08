package ru.relabs.kurjer.domain.models.storage

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import java.util.*

data class StorageReportPhoto(
    val id: StoragePhotoId,
    val uuid: String,
    val storageReportId: StorageReportId,
    val gps: GPSCoordinatesModel,
    val time: Date
)
@Parcelize
data class StoragePhotoId(val id:Int): Parcelable