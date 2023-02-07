package ru.relabs.kurjer.presentation.storageReport

import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgState

object StorageReportMessages {
    fun msgInit(taskIds: List<TaskId>): StorageReportMessage = msgEffect(
        StorageReportEffects.effectLoadTasks(taskIds)
    )

    fun msgNavigateBack(): StorageReportMessage = msgEffect(
        StorageReportEffects.effectNavigateBack()
    )

    fun msgTasksLoaded(tasks: List<Task>): StorageReportMessage =
        msgState { it.copy(tasks = tasks) }

    fun msgAddLoaders(i: Int): StorageReportMessage =
        msgState { it.copy(loaders = it.loaders + i) }

    fun msgPhotoClicked(): StorageReportMessage = {
        TODO("Not yet implemented")
    }

    fun msgRemovePhotoClicked(): StorageReportMessage = {
        TODO("Not yet implemented")
    }
}