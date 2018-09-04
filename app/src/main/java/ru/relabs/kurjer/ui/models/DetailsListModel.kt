package ru.relabs.kurjer.ui.models

import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel

/**
 * Created by ProOrange on 29.08.2018.
 */
sealed class DetailsListModel {
    class Task(val task: TaskModel) : DetailsListModel()
    class TaskItem(val taskItem: TaskItemModel) : DetailsListModel()
    object DetailsTableHeader : DetailsListModel()
}