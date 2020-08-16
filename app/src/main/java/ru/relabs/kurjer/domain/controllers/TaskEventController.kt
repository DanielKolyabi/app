package ru.relabs.kurjer.domain.controllers

import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItemId

class TaskEventController: BaseEventController<TaskEvent>()

sealed class TaskEvent{
    data class TaskClosed(val taskId: TaskId): TaskEvent()
    data class TaskItemClosed(val taskItemId: TaskItemId): TaskEvent()
}