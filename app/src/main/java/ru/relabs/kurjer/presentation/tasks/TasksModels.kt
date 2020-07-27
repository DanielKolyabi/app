package ru.relabs.kurjer.presentation.tasks

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.presentation.base.tea.*

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class TasksState(
    val tasks: List<Task> = emptyList(),
    val selectedTasks: List<Task> = emptyList(),
    val loaders: Int = 0,
    val searchFilter: String = ""
)

class TasksContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

    val deliveryRepository: DeliveryRepository by inject()
    val databaseRepository: DatabaseRepository by inject()

    var showSnackbar: suspend (Int) -> Unit = {}
}

typealias TasksMessage = ElmMessage<TasksContext, TasksState>
typealias TasksEffect = ElmEffect<TasksContext, TasksState>
typealias TasksRender = ElmRender<TasksState>