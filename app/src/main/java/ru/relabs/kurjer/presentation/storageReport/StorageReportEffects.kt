package ru.relabs.kurjer.presentation.storageReport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.domain.models.TaskId


object StorageReportEffects {
    fun effectLoadTasks(taskIds: List<TaskId>): StorageReportEffect = { c, s ->
        messages.send(StorageReportMessages.msgAddLoaders(1))
        val tasks = c.taskUseCase.getTasksByIds(taskIds)
        messages.send(StorageReportMessages.msgTasksLoaded(tasks))
        messages.send(StorageReportMessages.msgAddLoaders(-1))
    }

    fun effectNavigateBack(): StorageReportEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.router.exit()
        }
    }
}