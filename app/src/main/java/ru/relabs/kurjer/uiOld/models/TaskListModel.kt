package ru.relabs.kurjer.uiOld.models

import ru.relabs.kurjer.models.TaskModel

/**
 * Created by ProOrange on 31.08.2018.
 */
sealed class TaskListModel {
    object Loader: TaskListModel()
    data class Task(val task: TaskModel, var hasSelectedTasksWithSimilarAddress: Boolean = false): TaskListModel()
}