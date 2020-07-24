package ru.relabs.kurjer.presentation.tasks

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.presentation.base.tea.*

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class TasksState(
    val data: Any? = null
)

class TasksContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

}

typealias TasksMessage = ElmMessage<TasksContext, TasksState>
typealias TasksEffect = ElmEffect<TasksContext, TasksState>
typealias TasksRender = ElmRender<TasksState>