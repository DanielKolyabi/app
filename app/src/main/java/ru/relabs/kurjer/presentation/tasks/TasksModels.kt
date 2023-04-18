package ru.relabs.kurjer.presentation.tasks

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.repositories.TaskRepository
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.SettingsRepository
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

class TasksContext(val examinedConsumer: TasksFragment, val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

    val deliveryRepository: DeliveryRepository by inject()
    val taskRepository: TaskRepository by inject()
    val taskEventController: TaskEventController by inject()
    val settingsRepository: SettingsRepository by inject()
    val pathsProvider: PathsProvider by inject()

    var showSnackbar: suspend (Int) -> Unit = {}
    var showUpdateRequiredOnVisible: (canSkip: Boolean) -> Unit = {}
}

typealias TasksMessage = ElmMessage<TasksContext, TasksState>
typealias TasksEffect = ElmEffect<TasksContext, TasksState>
typealias TasksRender = ElmRender<TasksState>