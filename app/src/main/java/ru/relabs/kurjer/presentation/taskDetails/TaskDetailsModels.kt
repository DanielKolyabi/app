package ru.relabs.kurjer.presentation.taskDetails

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.repositories.TaskRepository
import ru.relabs.kurjer.presentation.base.tea.*
import java.io.File

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class TaskDetailsState(
    val loaders: Int = 0,
    val task: Task? = null
) {
    val sortedTasks
        get() = if (task != null) {
            task.items.sortedWith(compareBy<TaskItem> { it.subarea }
                .thenBy { it.bypass }
                .thenBy { it.address.city }
                .thenBy { it.address.street }
                .thenBy { it.address.house }
                .thenBy { it.address.houseName }
                .thenBy { it.state }
            ).groupBy {
                it.address.id
            }.toList().sortedBy {
                !it.second.any { it.state != TaskItemState.CLOSED }
            }.toMap().flatMap {
                it.value
            }
        } else {
            listOf()
        }
    val photoRequired
        get() = task?.items?.any { it.needPhoto || (it is TaskItem.Common && it.entrancesData.any { it.photoRequired }) } ?: false
}

class TaskDetailsContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

    var onExamine: (Task) -> Unit = {}
    val taskRepository: TaskRepository by inject()
    val pathsProvider: PathsProvider by inject()

    var showFatalError: suspend (String) -> Unit = {}
    var showSnackbar: suspend (Int) -> Unit = {}
    var showImagePreview: suspend (File) -> Unit = {}
}

typealias TaskDetailsMessage = ElmMessage<TaskDetailsContext, TaskDetailsState>
typealias TaskDetailsEffect = ElmEffect<TaskDetailsContext, TaskDetailsState>