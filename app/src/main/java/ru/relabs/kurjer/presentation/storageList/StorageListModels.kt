package ru.relabs.kurjer.presentation.storageList

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.useCases.StorageUseCase
import ru.relabs.kurjer.presentation.base.tea.*

data class StorageListState(
    var tasks: List<Task> = emptyList(),
    var loaders: Int = 0
)

class StorageListContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {
    val storageUseCase: StorageUseCase by inject()


}

typealias StorageListMessage = ElmMessage<StorageListContext, StorageListState>
typealias StorageListEffect = ElmEffect<StorageListContext, StorageListState>
typealias StorageListRender = ElmRender<StorageListState>