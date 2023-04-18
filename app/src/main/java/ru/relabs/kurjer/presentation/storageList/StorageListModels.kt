package ru.relabs.kurjer.presentation.storageList

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.useCases.StorageReportUseCase
import ru.relabs.kurjer.domain.useCases.TaskUseCase
import ru.relabs.kurjer.presentation.base.tea.*

data class StorageListState(
    var tasks: List<TaskWrapper> = emptyList(),
    var loaders: Int = 0
) {
    data class TaskWrapper(val task: Task, val isStorageActuallyRequired: Boolean)
}

class StorageListContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {
    val taskUseCase: TaskUseCase by inject()
    val storageReportUseCase: StorageReportUseCase by inject()


}

typealias StorageListMessage = ElmMessage<StorageListContext, StorageListState>
typealias StorageListEffect = ElmEffect<StorageListContext, StorageListState>
typealias StorageListRender = ElmRender<StorageListState>