package ru.relabs.kurjer.presentation.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import androidx.core.net.toUri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.services.ReportService
import ru.relabs.kurjer.utils.*
import ru.relabs.kurjer.utils.extensions.isLocationExpired
import java.io.File
import java.util.*

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */
object ReportEffects {
    fun effectLoadData(itemIds: List<Pair<TaskId, TaskItemId>>, selectedTaskItemId: TaskItemId): ReportEffect = { c, s ->
        messages.send(ReportMessages.msgAddLoaders(1))
        val tasks = itemIds.mapNotNull { (taskId, taskItemId) ->
            val task = c.database.getTask(taskId)
            val item = task?.items?.firstOrNull { it.id == taskItemId }

            if (task != null && item != null) {
                TaskWithItem(task, item)
            } else {
                null
            }
        }

        if (tasks.size != itemIds.size) {
            c.showError("re:108", true)
        }

        val selectedTaskWithItem = tasks.firstOrNull { it.taskItem.id == selectedTaskItemId }

        if (selectedTaskWithItem == null) {
            c.showError("re:109", true)
        }

        //Default coupling
        val activeTaskWithItems = tasks.filter { it.taskItem.state == TaskItemState.CREATED }
        messages.send(
            ReportMessages.msgCouplingChanged(
                tasks
                    .distinctBy { it.task.coupleType }
                    .flatMap { taskWithItem ->
                        taskWithItem.taskItem.entrancesData.map {
                            val isCouplingEnabled = activeTaskWithItems.count { taskWithItem.task.coupleType == it.task.coupleType } > 1 &&
                                    c.database.getTaskItemResult(taskWithItem.taskItem) == null

                            (it.number to taskWithItem.task.coupleType) to isCouplingEnabled
                        }
                    }
                    .toMap()
            )
        )

        messages.send(ReportMessages.msgTasksLoaded(tasks))
        messages.send(ReportMessages.msgTaskSelected(selectedTaskItemId))
        messages.send(ReportMessages.msgAddLoaders(-1))
    }

    fun effectNavigateBack(exits: Int): ReportEffect = { c, s ->
        if(exits == 1){
            withContext(Dispatchers.Main) {
                c.router.exit()
            }
        }
    }

    fun effectLoadSelection(id: TaskItemId): ReportEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.hideKeyboard()
        }
        messages.send(ReportMessages.msgAddLoaders(1))
        val task = s.tasks.firstOrNull { it.taskItem.id == id }?.task
        val taskItem = c.database.getTaskItem(id)
        if (task == null || taskItem == null) {
            c.showError("re:110", true)
        } else {
            val photos = c.database.getTaskItemPhotos(taskItem).map {
                PhotoWithUri(it, c.pathsProvider.getTaskItemPhotoFile(taskItem, UUID.fromString(it.UUID)).toUri())
            }
            messages.send(ReportMessages.msgTaskSelectionLoaded(TaskWithItem(task, taskItem), photos))

            val report = c.database.getTaskItemResult(taskItem)
            messages.send(ReportMessages.msgSavedResultLoaded(report))

            if (s.isEntranceSelectionChanged) {
                s.selectedTask?.task?.coupleType?.let {
                    messages.send(ReportMessages.msgDisableCouplingForType(it))
                }
            }
        }
        messages.send(ReportMessages.msgAddLoaders(-1))
    }

    fun effectCreatePhoto(entranceNumber: Int, multiplePhotos: Boolean): ReportEffect = { c, s ->
        when (val selectedTask = s.selectedTask) {
            null -> c.showError("re:100", true)
            else -> {
                val photoUUID = UUID.randomUUID()
                val photoFile = c.pathsProvider.getTaskItemPhotoFile(selectedTask.taskItem, photoUUID)
                withContext(Dispatchers.Main) {
                    c.requestPhoto(entranceNumber, multiplePhotos, photoFile, photoUUID)
                }
            }
        }
    }

    fun effectRemovePhoto(it: TaskItemPhoto): ReportEffect = { c, s ->
        c.database.removePhoto(it)
    }

    fun effectEntranceSelectionChanged(entrance: EntranceNumber, button: EntranceSelectionButton): ReportEffect = { c, s ->
        fun applyButtonClick(selection: ReportEntranceSelection): ReportEntranceSelection = when (button) {
            EntranceSelectionButton.Euro -> selection.copy(isEuro = !selection.isEuro)
            EntranceSelectionButton.Watch -> selection.copy(isWatch = !selection.isWatch)
            EntranceSelectionButton.Stack -> selection.copy(isStacked = !selection.isStacked)
            EntranceSelectionButton.Reject -> selection.copy(isRejected = !selection.isRejected)
        }

        when (val affectedTask = s.selectedTask) {
            null -> c.showError("re:101", true)
            else -> {
                val newResult =
                    c.database.createOrUpdateTaskItemEntranceResultSelection(entrance, affectedTask.taskItem) { selection ->
                        applyButtonClick(selection).let { applied ->
                            if (button == EntranceSelectionButton.Euro && applied.isEuro) {
                                applied.copy(isStacked = true)
                            } else if (button == EntranceSelectionButton.Watch && applied.isWatch) {
                                applied.copy(isStacked = true)
                            } else if (
                                (button == EntranceSelectionButton.Watch || button == EntranceSelectionButton.Euro)
                                && (!applied.isEuro && !applied.isWatch)
                            ) {
                                applied.copy(isStacked = false)
                            } else {
                                applied
                            }
                        }
                    }

                //Coupling
                val newSelection = newResult?.entrances?.firstOrNull { it.entranceNumber == entrance }?.selection
                if (newSelection != null && s.coupling.isCouplingEnabled(affectedTask.task, entrance)) {
                    s.tasks
                        .filter { it.task.coupleType == affectedTask.task.coupleType && it.taskItem.state == TaskItemState.CREATED }
                        .forEach { c.database.createOrUpdateTaskItemEntranceResultSelection(entrance, it.taskItem, newSelection) }
                }

                newResult?.let {
                    messages.send(ReportMessages.msgSavedResultLoaded(it))
                }
            }
        }
    }


    fun effectSavePhotoFromFile(entrance: Int, targetFile: File, uuid: UUID): ReportEffect = { c, s ->
        val bmp = BitmapFactory.decodeFile(targetFile.path)
        effectSavePhotoFromBitmap(entrance, bmp, targetFile, uuid)(c, s)
    }

    fun effectSavePhotoFromBitmap(entrance: Int, bitmap: Bitmap, targetFile: File, uuid: UUID): ReportEffect = { c, s ->
        when (val task = s.selectedTask) {
            null -> c.showError("re:102", true)
            else -> {
                when (savePhotoFromBitmapToFile(bitmap, targetFile)) {
                    is Left -> messages.send(ReportMessages.msgPhotoError(6))
                    is Right -> {
                        val location = c.locationProvider.lastReceivedLocation()
                        val photo = c.database.savePhoto(entrance, task.taskItem, uuid, location)
                        val path = c.pathsProvider.getTaskItemPhotoFile(task.taskItem, uuid)
                        messages.send(ReportMessages.msgNewPhoto(PhotoWithUri(photo, path.toUri())))
                    }
                }
            }
        }
    }

    fun effectUpdateDescription(text: String): ReportEffect = { c, s ->
        when (val selectedTask = s.selectedTask) {
            null -> c.showError("re:103", true)
            else -> when (val r = s.selectedTaskReport ?: createEmptyTaskResult(c.database, selectedTask.taskItem)) {
                null -> c.showError("re:104", true)
                else -> c.database.updateTaskItemResult(r.copy(description = text))?.let {
                    messages.send(ReportMessages.msgSavedResultLoaded(it))
                }
            }
        }
    }

    fun effectChangeCoupleState(entrance: EntranceNumber): ReportEffect = { c, s ->
        when (val selected = s.selectedTask) {
            null -> c.showError("re:105", true)
            else -> {
                val taskCoupleType = selected.task.coupleType
                val currentCoupleState = s.coupling.isCouplingEnabled(selected.task, entrance)
                if (s.tasks.filter { it.taskItem.state == TaskItemState.CREATED && it.task.coupleType == taskCoupleType }.size > 1) {
                    messages.send(ReportMessages.msgCouplingChanged(taskCoupleType, entrance, !currentCoupleState))
                }
            }
        }
    }

    fun effectCloseCheck(withLocationLoading: Boolean): ReportEffect = { c, s ->
        messages.send(ReportMessages.msgAddLoaders(1))
        when (val selected = s.selectedTask) {
            null -> c.showError("re:106", true)
            else -> {
                val taskItemRequiredPhotoExists = if (selected.taskItem.needPhoto) {
                    s.selectedTaskPhotos.any { it.photo.entranceNumber.number == ENTRANCE_NUMBER_TASK_ITEM }
                } else {
                    true
                }

                val requiredEntrancesPhotos = selected.taskItem.entrancesData
                    .filter { it.photoRequired }
                    .map { it.number }

                val entrancesRequiredPhotoExists = if (requiredEntrancesPhotos.isNotEmpty()) {
                    requiredEntrancesPhotos.all { entranceNumber -> s.selectedTaskPhotos.any { it.photo.entranceNumber == entranceNumber } }
                } else {
                    true
                }

                val location = c.locationProvider.lastReceivedLocation()

                if (c.pauseRepository.isPaused) {
                    withContext(Dispatchers.Main) {
                        c.showPausedWarning()
                    }
                } else if (!taskItemRequiredPhotoExists || !entrancesRequiredPhotoExists) {
                    withContext(Dispatchers.Main) {
                        c.showPhotosWarning()
                    }
                } else if (withLocationLoading && (location == null || Date(location.time).isLocationExpired())) {
                    coroutineScope {
                        messages.send(ReportMessages.msgAddLoaders(1))
                        messages.send(ReportMessages.msgGPSLoading(true))
                        val delayJob = async { delay(40 * 1000) }
                        val gpsJob = async(Dispatchers.Default) {
                            c.locationProvider.updatesChannel().apply {
                                receive()
                                cancel()
                            }
                        }
                        listOf(delayJob, gpsJob).awaitFirst()
                        messages.send(ReportMessages.msgGPSLoading(false))
                        messages.send(ReportMessages.msgAddLoaders(-1))
                        messages.send(msgEffect(effectCloseCheck(false)))
                    }
                } else {
                    val distance = location?.let {
                        calculateDistance(
                            location.latitude,
                            location.longitude,
                            selected.taskItem.address.lat.toDouble(),
                            selected.taskItem.address.long.toDouble()
                        )
                    } ?: Int.MAX_VALUE.toDouble()

                    val shadowClose: Boolean = withContext(Dispatchers.Main) {
                        when (val radius = c.radiusRepository.allowedCloseRadius) {
                            is AllowedCloseRadius.Required -> when {
                                location == null || Date(location.time).isLocationExpired() -> {
                                    c.showCloseError(R.string.report_close_location_null_error, false, null)
                                    true
                                }
                                distance > radius.distance -> {
                                    c.showCloseError(R.string.report_close_location_far_error, false, null)
                                    true
                                }
                                else -> {
                                    withContext(Dispatchers.Main) {
                                        c.showPreCloseDialog(location)
                                    }
                                    false
                                }
                            }
                            is AllowedCloseRadius.NotRequired -> when {
                                location == null || Date(location.time).isLocationExpired() -> {
                                    c.showCloseError(R.string.report_close_location_null_warning, true, location)
                                    false
                                }
                                distance > radius.distance -> {
                                    c.showCloseError(R.string.report_close_location_far_warning, true, location)
                                    false
                                }
                                else -> {
                                    withContext(Dispatchers.Main) {
                                        c.showPreCloseDialog(location)
                                    }
                                    false
                                }
                            }
                        }
                    }
                    if (shadowClose) {
                        effectClosePerform(false, location)(c, s)
                    }
                }
            }
        }
        messages.send(ReportMessages.msgAddLoaders(-1))
    }

    fun effectClosePerform(withRemove: Boolean, location: Location?): ReportEffect = { c, s ->
        messages.send(ReportMessages.msgAddLoaders(1))
        when (val selected = s.selectedTask) {
            null -> c.showError("re:107", true)
            else -> {
                effectInterruptPause()(c, s)
                c.reportUseCase.createReport(selected.task, selected.taskItem, location, c.getBatteryLevel() ?: 0f, withRemove)
                if (withRemove) {
                    ReportService.restartTaskClosingTimer()
                }
            }
        }
        messages.send(ReportMessages.msgAddLoaders(-1))
    }

    private fun savePhotoFromBitmapToFile(bitmap: Bitmap, targetFile: File): Either<Exception, File> = Either.of {
        val resized = ImageUtils.resizeBitmap(bitmap, 1024f, 768f)
        bitmap.recycle()
        ImageUtils.saveImage(resized, targetFile)
        targetFile
    }

    private suspend fun createEmptyTaskResult(database: DatabaseRepository, taskItem: TaskItem): TaskItemResult? {
        val result = TaskItemResult(
            id = TaskItemResultId(0),
            taskItemId = taskItem.id,
            closeTime = null,
            description = "",
            entrances = emptyList(),
            gps = GPSCoordinatesModel(0.0, 0.0, Date())
        )
        return database.updateTaskItemResult(result)
    }

    fun effectInterruptPause(): ReportEffect = { c, s ->
        if (c.pauseRepository.isPaused) {
            c.pauseRepository.stopPause(withNotify = true)
        }
    }

    fun effectLaunchEventConsumers(): ReportEffect = { c, s ->
        coroutineScope {
            launch {
                c.taskEventController.subscribe().collect { event ->
                    when (event) {
                        is TaskEvent.TaskClosed ->
                            messages.send(ReportMessages.msgTaskClosed(event.taskId))
                        is TaskEvent.TaskItemClosed ->
                            messages.send(msgEffect(effectEventTaskItemClosed(event.taskItemId)))
                    }
                }
            }
        }
    }

    private fun effectEventTaskItemClosed(taskItemId: TaskItemId): ReportEffect = { c, s ->
        s.tasks
            .firstOrNull { t -> t.taskItem.id == taskItemId }
            ?.let { messages.send(ReportMessages.msgTaskItemClosed(it, true)) }
    }

    fun effectShowPhotoError(errorCode: Int): ReportEffect = { c, s ->
        c.showError("Не удалось сделать фотографию: re:photo:$errorCode", false)
    }
}