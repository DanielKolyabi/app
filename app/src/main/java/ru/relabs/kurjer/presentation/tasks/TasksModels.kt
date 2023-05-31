package ru.relabs.kurjer.presentation.tasks

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.address
import ru.relabs.kurjer.domain.models.canBeSelectedWith
import ru.relabs.kurjer.domain.models.id
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.SettingsRepository
import ru.relabs.kurjer.domain.repositories.TaskRepository
import ru.relabs.kurjer.presentation.base.tea.ElmEffect
import ru.relabs.kurjer.presentation.base.tea.ElmMessage
import ru.relabs.kurjer.presentation.base.tea.ElmRender
import ru.relabs.kurjer.presentation.base.tea.ErrorContext
import ru.relabs.kurjer.presentation.base.tea.ErrorContextImpl
import ru.relabs.kurjer.presentation.base.tea.RouterContext
import ru.relabs.kurjer.presentation.base.tea.RouterContextMainImpl
import ru.relabs.kurjer.utils.SearchUtils

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class TasksState(
    val tasks: List<Task> = emptyList(),
    val selectedTasks: List<Task> = emptyList(),
    val loaders: Int = 0,
    val searchFilter: String = ""
) {
    private val intersections = searchIntersections(tasks, selectedTasks)
    val sortedTasks = tasks.sortedBy { it.listSort }
        .filter {
            if (searchFilter.isNotEmpty()) {
                SearchUtils.isMatches(it.listName, searchFilter)
            } else {
                true
            }
        }.map {
            TaskListItem(it, intersections.getOrElse(it) { false }, selectedTasks.contains(it))
        }

    private fun searchIntersections(
        tasks: List<Task>,
        selectedTasks: List<Task>
    ): Map<Task, Boolean> {
        val result = mutableMapOf<Task, Boolean>()
        //Check if any on items has intersected address with other tasks
        tasks
            .filter { it.canBeSelectedWith(selectedTasks) } //Filter restricted by district rule
            .forEach { task -> //Search addresses intersections
                selectedTasks.forEach { selectedTask ->
                    val isSelectedTaskContainsTaskAddress = task.items.any { taskItem ->
                        selectedTask.items.any { selectedTaskItem -> selectedTaskItem.address.id == taskItem.address.id && selectedTaskItem.id != taskItem.id }
                    }

                    if (isSelectedTaskContainsTaskAddress) {
                        result[task] = true
                    }
                }
            }
        return result
    }
}

data class TaskListItem(
    val task: Task,
    val isTasksWithSameAddressPresented: Boolean,
    val isSelected: Boolean
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