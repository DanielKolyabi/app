package ru.relabs.kurjer.presentation.storageList

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgState

object StorageListMessages {
    fun msgInit(taskIds: List<TaskId>): StorageListMessage =
        msgEffect(
            StorageListEffects.effectLoadTasks(taskIds)
        )

    fun msgStorageItemClicked(taskIds: List<TaskId>): StorageListMessage = msgEffect(
        StorageListEffects.navigateStorageScreen(taskIds)
    )

    fun msgNavigateBack(): StorageListMessage = msgEffect(
        StorageListEffects.effectNavigateBack()
    )

    fun msgTasksLoaded(tasks: List<StorageListState.TaskWrapper>): StorageListMessage =
        msgState { it.copy(tasks = tasks) }

    fun msgAddLoaders(i: Int): StorageListMessage =
        msgState { it.copy(loaders = it.loaders + i) }
}