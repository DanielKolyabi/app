package ru.relabs.kurjer.uiOld.models

import ru.relabs.kurjer.models.TaskModel

/**
 * Created by ProOrange on 30.08.2018.
 */

sealed class ReportTasksListModel{
    data class TaskButton(val task: TaskModel, val pos: Int, var active: Boolean): ReportTasksListModel()
}