package ru.relabs.kurjer.presentation.storageList

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.RootScreen

object StorageListEffects {
    fun effectLoadTasks(taskIds: List<TaskId>): StorageListEffect = { c, s ->
        messages.send(StorageListMessages.msgAddLoaders(1))
        val tasks = c.taskUseCase.getTasksByIds(taskIds)
        messages.send(StorageListMessages.msgTasksLoaded(tasks))
        messages.send(StorageListMessages.msgAddLoaders(-1))
    }

    fun effectNavigateBack(): StorageListEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.router.exit()
        }
    }

    fun navigateStorageScreen(taskIds: List<TaskId>): StorageListEffect = { c, s ->
        withContext(Dispatchers.Main) { c.router.navigateTo(RootScreen.StorageReportScreen(taskIds)) }
    }
}