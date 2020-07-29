package ru.relabs.kurjer.presentation.taskDetails

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem

sealed class TaskDetailsItem {
    data class PageHeader(val task: Task): TaskDetailsItem()
    object ListHeader: TaskDetailsItem()
    data class ListItem(val taskItem: TaskItem): TaskDetailsItem()
}