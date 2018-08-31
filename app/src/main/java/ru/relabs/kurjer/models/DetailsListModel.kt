package ru.relabs.kurjer.models

/**
 * Created by ProOrange on 29.08.2018.
 */
sealed class DetailsListModel {
    class Task(val task: TaskModel) : DetailsListModel()
    class TaskItem(val taskItem: TaskItemModel) : DetailsListModel()
    object DetailsTableHeader : DetailsListModel()
}