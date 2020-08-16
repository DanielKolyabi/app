package ru.relabs.kurjer.presentation.report

import android.location.Location
import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.providers.LocationProvider
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.RadiusRepository
import ru.relabs.kurjer.domain.useCases.ReportUseCase
import ru.relabs.kurjer.presentation.base.tea.*
import java.io.File
import java.util.*

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
    val loaders: Int = 0,
    val isGPSLoading: Boolean = false,

    val coupling: ReportCoupling = emptyMap()
)

class ReportContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

    val database: DatabaseRepository by inject()
    val locationProvider: LocationProvider by inject()
    val pauseRepository: PauseRepository by inject()
    val radiusRepository: RadiusRepository by inject()
    val reportUseCase: ReportUseCase by inject()
    val taskEventController: TaskEventController by inject()

    var requestPhoto: (entrance: Int, multiplePhoto: Boolean, targetFile: File, uuid: UUID) -> Unit = { _, _, _, _ -> }
    var hideKeyboard: () -> Unit = {}
    var showCloseError: (msgRes: Int, showNext: Boolean, location: Location?) -> Unit = { _, _, _ -> }
    var showPausedWarning: () -> Unit = {}
    var showPhotosWarning: () -> Unit = {}
    var showPreCloseDialog: (location: Location?) -> Unit = {}
    var getBatteryLevel: () -> Float? = { null }
}

enum class EntranceSelectionButton {
    Euro, Watch, Stack, Reject
}

typealias ReportCoupling = Map<Pair<EntranceNumber, CoupleType>, Boolean>
typealias ReportMessage = ElmMessage<ReportContext, ReportState>
typealias ReportEffect = ElmEffect<ReportContext, ReportState>
typealias ReportRender = ElmRender<ReportState>

fun ReportCoupling.isCouplingEnabled(task: Task, entranceNumber: EntranceNumber): Boolean {
    return this.getOrElse(entranceNumber to task.coupleType) { false }
}