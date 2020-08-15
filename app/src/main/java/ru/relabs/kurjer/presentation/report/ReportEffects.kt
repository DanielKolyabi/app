package ru.relabs.kurjer.presentation.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import kotlinx.coroutines.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.files.ImageUtils
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.presentation.base.tea.msgEffect
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
            //TODO: Not all tasks found, report somehow
        }

        val selectedTaskWithItem = tasks.firstOrNull { it.taskItem.id == selectedTaskItemId }

        if (selectedTaskWithItem == null) {
            //TODO: Selected taskItem not found, report
        }

        //Default coupling
        val activeTaskWithItems = tasks.filter { it.taskItem.state == TaskItemState.CREATED }
        messages.send(
            ReportMessages.msgCouplingChanged(
                tasks
                    .distinctBy { it.task.coupleType }
                    .flatMap { taskWithItem ->
                        taskWithItem.taskItem.entrancesData.map {
                            (it.number to taskWithItem.task.coupleType) to (activeTaskWithItems.count { taskWithItem.task.coupleType == it.task.coupleType } > 1)
                        }
                    }
                    .toMap()
            )
        )

        messages.send(ReportMessages.msgTasksLoaded(tasks))
        messages.send(ReportMessages.msgTaskSelected(selectedTaskItemId))
        messages.send(ReportMessages.msgAddLoaders(-1))
    }

    fun effectNavigateBack(): ReportEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.router.exit()
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
            //TODO: Show error
        } else {
            val photos = c.database.getTaskItemPhotos(taskItem)
            messages.send(ReportMessages.msgTaskSelectionLoaded(TaskWithItem(task, taskItem), photos))

            val report = c.database.getTaskItemResult(taskItem)
            messages.send(ReportMessages.msgSavedResultLoaded(report))
        }
        messages.send(ReportMessages.msgAddLoaders(-1))
    }

    fun effectCreatePhoto(entranceNumber: Int, multiplePhotos: Boolean): ReportEffect = { c, s ->
        when (val selectedTask = s.selectedTask) {
            null -> Unit //TODO: Show error
            else -> {
                val photoUUID = UUID.randomUUID()
                val photoFile = PathHelper.getTaskItemPhotoFile(selectedTask.taskItem, photoUUID)
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
            null -> Unit //TODO: Show error
            else -> {
                val newResult =
                    c.database.createOrUpdateTaskItemEntranceResultSelection(entrance, affectedTask.taskItem, ::applyButtonClick)

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
            null -> Unit //TODO: Show error
            else -> {
                when (savePhotoFromBitmapToFile(bitmap, targetFile)) {
                    is Left -> messages.send(ReportMessages.msgPhotoError(6))
                    is Right -> {
                        val location = c.locationProvider.lastReceivedLocation()
                        messages.send(ReportMessages.msgNewPhoto(c.database.savePhoto(entrance, task.taskItem, uuid, location)))
                    }
                }
            }
        }
    }

    fun effectUpdateDescription(text: String): ReportEffect = { c, s ->
        when (val selectedTask = s.selectedTask) {
            null -> Unit //TODO: Show error
            else -> when (val r = s.selectedTaskReport ?: createEmptyTaskResult(c.database, selectedTask.taskItem)) {
                null -> Unit //TODO: Show error
                else -> c.database.updateTaskItemResult(r.copy(description = text))?.let {
                    messages.send(ReportMessages.msgSavedResultLoaded(it))
                }
            }
        }
    }

    fun effectChangeCoupleState(entrance: EntranceNumber): ReportEffect = { c, s ->
        when (val selected = s.selectedTask) {
            null -> Unit //TODO: Show error
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
            null -> Unit //TODO: Show error
            else -> {
                val taskItemRequiredPhotoExists = if(selected.taskItem.needPhoto){
                    s.selectedTaskPhotos.any { it.entranceNumber.number == ENTRANCE_NUMBER_TASK_ITEM }
                }else{
                    true
                }

                val requiredEntrancesPhotos = selected.taskItem.entrancesData
                        .filter { it.photoRequired }
                        .map { false to it.number }

                val entrancesRequiredPhotoExists = if(requiredEntrancesPhotos.isNotEmpty()){
                    requiredEntrancesPhotos
                        .reduce { entranceData, acc -> acc.copy(first = acc.first && s.selectedTaskPhotos.any { it.entranceNumber == entranceData.second }) }
                        .first
                }else{
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
                        messages.send(ReportMessages.msgGPSLoading(true))
                        val delayJob = async { delay(40 * 1000) }
                        val gpsJob = async(Dispatchers.Default) {
                            c.locationProvider.updatesChannel().apply{
                                receive()
                                cancel()
                            }
                        }
                        listOf(delayJob, gpsJob).awaitFirst()
                        messages.send(ReportMessages.msgGPSLoading(false))
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
            null -> Unit //TODO: Show error
            else -> {
                effectInterruptPause()(c,s)
                if(withRemove){
                    c.database.closeTaskItem(selected.taskItem)
                }
                c.reportUseCase.createReport(selected.task, selected.taskItem, location, c.getBatteryLevel() ?: 0f, withRemove)

                messages.send(ReportMessages.msgTaskItemClosed(selected, withRemove))
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
        if(c.pauseRepository.isPaused){
            c.pauseRepository.stopPause(withNotify = true, withUpdate = true)
        }
    }
}