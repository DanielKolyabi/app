package ru.relabs.kurjer.domain.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.relabs.kurjer.files.PathHelper

@Parcelize
data class PhotoId(val id: Int): Parcelable

@Parcelize
data class TaskItemPhoto(
    val id: PhotoId,
    val UUID: String,
    val taskItemId: TaskItemId,
    val entranceNumber: EntranceNumber
) : Parcelable {
    val uri: Uri //TODO: Remove PathHelper
        get() = Uri.fromFile(PathHelper.getTaskItemPhotoFileByID(taskItemId.id, java.util.UUID.fromString(UUID)))
}