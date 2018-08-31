package ru.relabs.kurjer.models

/**
 * Created by ProOrange on 30.08.2018.
 */

sealed class ReportPhotosListModel{
    object BlankPhoto: ReportPhotosListModel()
    data class TaskItemPhoto(val taskItem: TaskItemModel, val photoId: Int): ReportPhotosListModel()
}