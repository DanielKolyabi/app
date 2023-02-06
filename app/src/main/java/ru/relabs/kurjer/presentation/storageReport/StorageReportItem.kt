package ru.relabs.kurjer.presentation.storageReport

import android.net.Uri
import ru.relabs.kurjer.domain.models.StorageClosure
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItemPhoto


sealed class StorageReportItem{
    data class Single(val hasPhoto: Boolean):  StorageReportItem()
    data class Photo(val photo: TaskItemPhoto, val photoUri: Uri):  StorageReportItem()
    data class Closure(val task: Task, val closure: StorageClosure):  StorageReportItem()
}
