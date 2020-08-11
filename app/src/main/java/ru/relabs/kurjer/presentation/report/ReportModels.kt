package ru.relabs.kurjer.presentation.report

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.presentation.base.tea.*

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class TaskWithItem(
    val task: Task,
    val taskItem: TaskItem
)

data class ReportState(
    val tasks: List<TaskWithItem> = emptyList(),
    val selectedTask: TaskWithItem? = null,
    val selectedTaskPhotos: List<TaskItemPhoto> = emptyList(),
    val selectedTaskReport: TaskItemResult? = null,
    val loaders: Int = 0
)

class ReportContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

    val database: DatabaseRepository by inject()
}

enum class EntranceSelectionButton {
    Euro, Watch, Stack, Reject
}

typealias ReportMessage = ElmMessage<ReportContext, ReportState>
typealias ReportEffect = ElmEffect<ReportContext, ReportState>
typealias ReportRender = ElmRender<ReportState>