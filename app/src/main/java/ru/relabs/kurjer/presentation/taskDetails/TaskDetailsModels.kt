package ru.relabs.kurjer.presentation.taskDetails

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.presentation.base.tea.*
import java.io.File

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class TaskDetailsState(
    val loaders: Int = 0,
    val task: Task? = null
)

class TaskDetailsContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

    var onExamine: (Task) -> Unit = {}
    val database: DatabaseRepository by inject()
    var showFatalError: suspend (String) -> Unit = {}
    var showSnackbar: suspend (Int) -> Unit = {}
    var showImagePreview: suspend (File) -> Unit = {}
}

typealias TaskDetailsMessage = ElmMessage<TaskDetailsContext, TaskDetailsState>
typealias TaskDetailsEffect = ElmEffect<TaskDetailsContext, TaskDetailsState>
typealias TaskDetailsRender = ElmRender<TaskDetailsState>