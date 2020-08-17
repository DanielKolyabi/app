package ru.relabs.kurjer.presentation.tasks

import ru.relabs.kurjer.domain.models.Task

sealed class TasksItem{
    data class TaskItem(
        val task: Task,
        val isTasksWithSameAddressPresented: Boolean,
        val isSelected: Boolean
    ): TasksItem()

    object Blank: TasksItem()
    data class Search(val filter: String): TasksItem()
    object Loader: TasksItem()
}