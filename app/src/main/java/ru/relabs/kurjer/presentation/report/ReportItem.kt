package ru.relabs.kurjer.presentation.report

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.models.TaskItemPhotoModel

sealed class ReportPhotoItem {
    data class Single(val required: Boolean, val hasPhoto: Boolean): ReportPhotoItem()
    data class Photo(val photo: TaskItemPhoto, val photoUri: Uri): ReportPhotoItem()
}

data class ReportEntranceItem(
    val taskItem: TaskItem.Common,
    val entranceNumber: EntranceNumber,
    val selection: ReportEntranceSelection,
    val coupleEnabled: Boolean,
    val hasPhoto: Boolean,
    val hasDescription: Boolean
)

data class ReportTaskItem(val task: Task, val taskItem: TaskItem, val active: Boolean)