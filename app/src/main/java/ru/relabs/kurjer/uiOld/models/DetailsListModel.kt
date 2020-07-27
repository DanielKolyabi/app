package ru.relabs.kurjer.uiOld.models

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel

/**
 * Created by ProOrange on 29.08.2018.
 */
sealed class DetailsListModel {
    class Task(val task: ru.relabs.kurjer.domain.models.Task) : DetailsListModel()
    class TaskItem(val taskItem: ru.relabs.kurjer.domain.models.TaskItem) : DetailsListModel()
    object DetailsTableHeader : DetailsListModel()
}