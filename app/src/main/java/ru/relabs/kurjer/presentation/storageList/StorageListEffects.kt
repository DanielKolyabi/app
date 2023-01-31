package ru.relabs.kurjer.presentation.storageList

import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.RootScreen

object StorageListEffects {
    fun effectLoadTasks(taskIds: List<TaskId>): StorageListEffect = { c, s ->
        messages.send(StorageListMessages.msgAddLoaders(1))
        val tasks = c.storageUseCase.getTasksByIds(taskIds)
        messages.send(StorageListMessages.msgTasksLoaded(tasks))
        messages.send(StorageListMessages.msgAddLoaders(-1))
    }

    fun effectNavigateBack(): StorageListEffect = { c, s ->
        c.router.exit()
    }

    fun navigateStorageScreen(taskId: TaskId): StorageListEffect = { c, s ->
        c.router.navigateTo(RootScreen.StorageScreen(taskId))
    }
}