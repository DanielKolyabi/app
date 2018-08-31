package ru.relabs.kurjer.models

/**
 * Created by ProOrange on 30.08.2018.
 */

sealed class ReportTasksListModel{
    data class TaskButton(val task: TaskModel, val pos: Int, var active: Boolean): ReportTasksListModel()
}