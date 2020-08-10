package ru.relabs.kurjer.presentation.report

import android.net.Uri
import ru.relabs.kurjer.domain.models.EntranceNumber
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemPhoto
import ru.relabs.kurjer.models.TaskItemPhotoModel

sealed class ReportPhotoItem {
    data class Single(val required: Boolean, val hasPhoto: Boolean): ReportPhotoItem()
    data class Photo(val photo: TaskItemPhoto): ReportPhotoItem()
}

data class ReportEntranceItem(
    val taskItem: TaskItem,
    val entranceNumber: EntranceNumber,
    val selection: ReportEntranceSelection,
    val coupleEnabled: Boolean,
    val hasPhoto: Boolean
)

data class ReportTaskItem(val task: Task, val taskItem: TaskItem, val active: Boolean)

data class ReportEntranceSelection(
    val isEuro: Boolean,
    val isWatch: Boolean,
    val isStacked: Boolean,
    val isRejected: Boolean
)