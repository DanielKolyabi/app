package ru.relabs.kurjer.domain.controllers

import ru.relabs.kurjer.domain.models.StorageId
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItemId
import java.util.Date

class TaskEventController: BaseEventController<TaskEvent>()

sealed class TaskEvent{
    data class TaskClosed(val taskId: TaskId): TaskEvent()
    data class TaskItemClosed(val taskItemId: TaskItemId): TaskEvent()
    data class TasksUpdateRequired(val showDialogInTasks: Boolean = false): TaskEvent()
    data class TaskStorageClosed(val taskId: TaskId, val storageId: StorageId, val closeTime: Date): TaskEvent()
}