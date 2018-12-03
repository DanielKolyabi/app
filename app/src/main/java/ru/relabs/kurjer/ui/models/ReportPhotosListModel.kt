package ru.relabs.kurjer.ui.models

import android.net.Uri
import ru.relabs.kurjer.models.TaskItemPhotoModel

/**
 * Created by ProOrange on 30.08.2018.
 */

sealed class ReportPhotosListModel{
    object BlankPhoto: ReportPhotosListModel()
    object BlankMultiPhoto: ReportPhotosListModel()
    data class TaskItemPhoto(val taskItem: TaskItemPhotoModel, val photoURI: Uri): ReportPhotosListModel()
}