package ru.relabs.kurjer.presentation.storageList

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskDeliveryType
import ru.relabs.kurjer.domain.useCases.StorageReportUseCase
import ru.relabs.kurjer.domain.useCases.TaskUseCase
import ru.relabs.kurjer.presentation.base.tea.ElmEffect
import ru.relabs.kurjer.presentation.base.tea.ElmMessage
import ru.relabs.kurjer.presentation.base.tea.ErrorContext
import ru.relabs.kurjer.presentation.base.tea.ErrorContextImpl
import ru.relabs.kurjer.presentation.base.tea.RouterContext
import ru.relabs.kurjer.presentation.base.tea.RouterContextMainImpl

data class StorageListState(
    val tasks: List<TaskWrapper> = emptyList(),
    val loaders: Int = 0
) {
    data class TaskWrapper(val task: Task, val isStorageActuallyRequired: Boolean)
    val storageWithTasksList = tasks.filter { it.task.deliveryType == TaskDeliveryType.Address }
    .groupBy { Triple(it.task.name, it.task.edition, it.isStorageActuallyRequired) to it.task.storage.id }
    .map {
       StorageWithTasks(
            it.value.first().task.storage,
            it.value
        )
    }
}
data class StorageWithTasks(val storage: Task.Storage, val tasks: List<StorageListState.TaskWrapper>)

class StorageListContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {
    val taskUseCase: TaskUseCase by inject()
    val storageReportUseCase: StorageReportUseCase by inject()


}

typealias StorageListMessage = ElmMessage<StorageListContext, StorageListState>
typealias StorageListEffect = ElmEffect<StorageListContext, StorageListState>