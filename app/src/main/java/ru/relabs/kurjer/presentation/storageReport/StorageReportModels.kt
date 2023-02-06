package ru.relabs.kurjer.presentation.storageReport

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.useCases.TaskUseCase
import ru.relabs.kurjer.presentation.base.tea.*

data class StorageReportState(
    var tasks: List<Task> = listOf(),
    var loaders: Int = 0
)

class StorageReportContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {
    val taskUseCase: TaskUseCase by inject()


}

typealias StorageReportMessage = ElmMessage<StorageReportContext, StorageReportState>
typealias StorageReportEffect = ElmEffect<StorageReportContext, StorageReportState>
typealias StorageReportRender = ElmRender<StorageReportState>