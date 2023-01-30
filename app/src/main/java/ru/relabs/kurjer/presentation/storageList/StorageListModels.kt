package ru.relabs.kurjer.presentation.storageList

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.presentation.base.tea.ErrorContext
import ru.relabs.kurjer.presentation.base.tea.ErrorContextImpl
import ru.relabs.kurjer.presentation.base.tea.RouterContext
import ru.relabs.kurjer.presentation.base.tea.RouterContextMainImpl

data class StorageListState(
    var tasks: List<Task>? = null
)

class StorageListContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {
    val databaseRepository: DatabaseRepository by inject()
}

