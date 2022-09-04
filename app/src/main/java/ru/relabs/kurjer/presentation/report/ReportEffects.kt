package ru.relabs.kurjer.presentation.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.services.ReportService
import ru.relabs.kurjer.uiOld.helpers.formatedWithSecs
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
                        if (taskWithItem.taskItem is TaskItem.Common) {
                            taskWithItem.taskItem.entrancesData.map {
                                val isCouplingEnabled =
                                    activeTaskWithItems.count { taskWithItem.task.coupleType == it.task.coupleType } > 1 &&
                                            c.database.getTaskItemResult(taskWithItem.taskItem) == null

                                (it.number to taskWithItem.task.coupleType) to isCouplingEnabled
                            }
                        } else {
                            emptyList()
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
        if (exits == 1) {
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

    private fun effectValidatePhotoRadiusAnd(
        msgFactory: () -> ReportMessage,
        withAnyRadiusWarning: Boolean,
        withLocationLoading: Boolean = true
    ): ReportEffect = { c, s ->
        messages.send(ReportMessages.msgAddLoaders(1))
        when (val selected = s.selectedTask) {
            null -> c.showError("re:106", true)
            else -> {
                val location = c.locationProvider.lastReceivedLocation()
                val distance = location?.let {
                    calculateDistance(
                        location.latitude,
                        location.longitude,
                        selected.taskItem.address.lat.toDouble(),
                        selected.taskItem.address.long.toDouble()
                    )
                } ?: Int.MAX_VALUE.toDouble()

                val locationNotValid = location == null || Date(location.time).isLocationExpired()
                CustomLog.writeToFile(
                    "Validate photo radius (valid: ${!locationNotValid}): " +
                            "${location?.latitude}, ${location?.longitude}, ${location?.time}, " +
                            "photoRadiusRequired: ${c.settingsRepository.isPhotoRadiusRequired}, " +
                            "allowedDistance: ${selected.taskItem.closeRadius}, " +
                            "distance: $distance, " +
                            "targetTaskItem: ${selected.taskItem.id}"
                )

                if (locationNotValid && withLocationLoading) {
                    coroutineScope {
                        messages.send(ReportMessages.msgAddLoaders(1))
                        messages.send(ReportMessages.msgGPSLoading(true))
                        val delayJob = async { delay(c.settingsRepository.closeGpsUpdateTime.photo * 1000L) }
                        val gpsJob = async(Dispatchers.Default) {
                            c.locationProvider.updatesChannel().apply {
                                receive()
                                cancel()
                            }
                        }
                        listOf(delayJob, gpsJob).awaitFirst()
                        listOf(delayJob, gpsJob).forEach {
                            if (it.isActive) {
                                it.cancel()
                            }
                        }
                        messages.send(ReportMessages.msgGPSLoading(false))
                        messages.send(ReportMessages.msgAddLoaders(-1))
                        messages.send(msgEffect(effectValidatePhotoRadiusAnd(msgFactory, withAnyRadiusWarning, false)))
                    }
                } else {
                    if (!c.settingsRepository.isPhotoRadiusRequired) {
                        //https://git.relabs.ru/kurier/app/-/issues/87 если юзер может делать фото и закрывать дома вне радиуса - ему нужно показывать диалог (он админ).
                        //Если же он может только делать фото, ему о диалоге знать не надо, что бы не особо пользовался этим
                        val shouldSuppressDialog = c.settingsRepository.isCloseRadiusRequired

                        if (distance > selected.taskItem.closeRadius && withAnyRadiusWarning && !shouldSuppressDialog) {
                            withContext(Dispatchers.Main) {
                                c.showCloseError(R.string.report_close_location_far_warning, false, null, null)
                            }
                        }
                        messages.send(msgFactory())
                    } else {
                        when {
                            locationNotValid -> withContext(Dispatchers.Main) {
                                c.showCloseError(R.string.report_close_location_null_error, false, null, null)
                            }
                            distance > selected.taskItem.closeRadius -> withContext(Dispatchers.Main) {
                                c.showCloseError(R.string.report_close_location_far_error, false, null, null)
                            }
                            else ->
                                messages.send(msgFactory())

                        }
                    }
                }
            }
        }
        messages.send(ReportMessages.msgAddLoaders(-1))
    }

    fun effectValidateRadiusAndSavePhoto(
        entrance: Int,
        photoUri: Uri,
        targetFile: File,
        uuid: UUID,
        multiplePhoto: Boolean
    ): ReportEffect = { c, s ->
        CustomLog.writeToFile("Save photo ($uuid) with radius validation")
        effectValidatePhotoRadiusAnd(
            {
                msgEffects(
                    { it },
                    {
                        listOfNotNull(
                            effectSavePhotoFromFile(entrance, photoUri, targetFile, uuid),
                            effectRequestPhoto(entrance, multiplePhoto).takeIf { multiplePhoto }
                        )
                    }
                )
            },
            withAnyRadiusWarning = true
        )(c, s)
    }

    fun effectValidateRadiusAndRequestPhoto(
        entranceNumber: Int,
        multiplePhoto: Boolean
    ): ReportEffect = { c, s ->
        CustomLog.writeToFile("Request photo ($entranceNumber) with raidus validation")
        effectValidatePhotoRadiusAnd({ msgEffect(effectRequestPhoto(entranceNumber, multiplePhoto)) }, false)(c, s)
    }

    fun effectRequestPhoto(entranceNumber: Int, multiplePhotos: Boolean): ReportEffect = { c, s ->
        when (val selectedTask = s.selectedTask) {
            null -> c.showError("re:100", true)
            else -> {
                val photoUUID = UUID.randomUUID()
                CustomLog.writeToFile("Request photo ${entranceNumber} ${photoUUID}")
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
                val newResult = c.database.createOrUpdateTaskItemEntranceResultSelection(
                    entrance,
                    affectedTask.taskItem
                ) { selection ->
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


    fun effectSavePhotoFromFile(entrance: Int, photoUri: Uri, targetFile: File, uuid: UUID): ReportEffect = { c, s ->
        val contentResolver = c.contentResolver()
        if (contentResolver == null) {
            messages.send(msgEffect(effectShowPhotoError(8)))
        } else {
            val bmp = BitmapFactory.decodeStream(contentResolver.openInputStream(photoUri))
            if (bmp == null) {
                CustomLog.writeToFile("Photo creation failed. Uri: ${photoUri}, File: ${targetFile.path}")
                messages.send(msgEffect(effectShowPhotoError(7)))
            } else {
                effectSavePhotoFromBitmap(entrance, bmp, targetFile, uuid)(c, s)
            }
        }
    }

    fun effectSavePhotoFromBitmap(entrance: Int, bitmap: Bitmap, targetFile: File, uuid: UUID): ReportEffect = { c, s ->
        CustomLog.writeToFile("Save photo ${entrance} ${uuid}")
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
            else -> {
                s.tasks.filter { it.taskItem.state == TaskItemState.CREATED }.forEach { t ->
                    val result = c.database.getTaskItemResult(t.taskItem) ?: createEmptyTaskResult(c.database, t.taskItem)
                    val updated = c.database.updateTaskItemResult(result.copy(description = text))
                    if (updated.taskItemId == selectedTask.taskItem.id) {
                        messages.send(ReportMessages.msgSavedResultLoaded(updated))
                    }
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

    fun effectCloseCheck(withLocationLoading: Boolean, rejectReason: String?): ReportEffect = { c, s ->
        messages.send(ReportMessages.msgAddLoaders(1))
        when (val selected = s.selectedTask) {
            null -> c.showError("re:106", true)
            else -> {
                val taskItemRequiredPhotoExists = if (selected.taskItem.needPhoto) {
                    s.selectedTaskPhotos.any { it.photo.entranceNumber.number == ENTRANCE_NUMBER_TASK_ITEM }
                } else {
                    true
                }

                val requiredEntrancesPhotos = when (selected.taskItem) {
                    is TaskItem.Common -> selected.taskItem.entrancesData
                        .filter { it.photoRequired }
                        .map { it.number }
                    is TaskItem.Firm -> emptyList()
                }

                val entrancesRequiredPhotoExists = if (requiredEntrancesPhotos.isNotEmpty()) {
                    requiredEntrancesPhotos.all { entranceNumber -> s.selectedTaskPhotos.any { it.photo.entranceNumber == entranceNumber } }
                } else {
                    true
                }

                val location = c.locationProvider.lastReceivedLocation()
                CustomLog.writeToFile("GPS LOG: Close check with location(${location?.latitude}, ${location?.longitude}, ${Date(location?.time ?: 0).formatedWithSecs()})")

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
                        val delayJob = async { delay(c.settingsRepository.closeGpsUpdateTime.close * 1000L) }
                        val gpsJob = async(Dispatchers.Default) {
                            c.locationProvider.updatesChannel().apply {
                                receive()
                                CustomLog.writeToFile("GPS LOG: Received new location")
                                cancel()
                            }
                        }
                        listOf(delayJob, gpsJob).awaitFirst()
                        delayJob.cancel()
                        gpsJob.cancel()
                        CustomLog.writeToFile("GPS LOG: Got force coordinates")
                        messages.send(ReportMessages.msgGPSLoading(false))
                        messages.send(ReportMessages.msgAddLoaders(-1))
                        messages.send(msgEffect(effectCloseCheck(false, rejectReason)))
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
                        if (c.settingsRepository.isCloseRadiusRequired) {
                            when {
                                location == null -> {
                                    c.showCloseError(R.string.report_close_location_null_error, false, null, rejectReason)
                                    true
                                }
                                distance > selected.taskItem.closeRadius -> {
                                    c.showCloseError(R.string.report_close_location_far_error, false, null, rejectReason)
                                    true
                                }
                                else -> {
                                    withContext(Dispatchers.Main) {
                                        c.showPreCloseDialog(location, rejectReason)
                                    }
                                    false
                                }
                            }
                        } else {
                            when {
                                location == null -> {
                                    c.showCloseError(R.string.report_close_location_null_warning, true, location, rejectReason)
                                    false
                                }
                                distance > selected.taskItem.closeRadius -> {
                                    c.showCloseError(R.string.report_close_location_far_warning, true, location, rejectReason)
                                    false
                                }
                                else -> {
                                    withContext(Dispatchers.Main) {
                                        c.showPreCloseDialog(location, rejectReason)
                                    }
                                    false
                                }
                            }
                        }
                    }
                    if (shadowClose) {
                        effectClosePerform(false, location, rejectReason)(c, s)
                    }
                }
            }
        }
        messages.send(ReportMessages.msgAddLoaders(-1))
    }

    fun effectClosePerform(withRemove: Boolean, location: Location?, rejectReason: String?): ReportEffect = { c, s ->
        messages.send(ReportMessages.msgAddLoaders(1))
        when (val selected = s.selectedTask) {
            null -> c.showError("re:107", true)
            else -> {
                effectInterruptPause()(c, s)
                c.reportUseCase.createReport(
                    selected.task,
                    selected.taskItem,
                    location,
                    c.getBatteryLevel() ?: 0f,
                    withRemove,
                    rejectReason != null,
                    rejectReason ?: ""
                )
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

    private suspend fun createEmptyTaskResult(database: DatabaseRepository, taskItem: TaskItem): TaskItemResult {
        val result = TaskItemResult(
            id = TaskItemResultId(0),
            taskItemId = taskItem.id,
            closeTime = null,
            description = "",
            entrances = emptyList(),
            gps = GPSCoordinatesModel(0.0, 0.0, Date()),
            isPhotoRequired = taskItem.needPhoto
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

    fun effectRejectClicked(): ReportEffect = { c, s ->
        val reasons = when (val r = c.deliveryRepository.getFirmRejectReasons()) {
            is Right -> r.value
            is Left -> emptyList()
        }
        withContext(Dispatchers.Main) {
            c.showRejectDialog(reasons)
        }
    }

    fun effectShowDescriptionInput(number: EntranceNumber): ReportEffect = { c, s ->
        val currentDescription = s.selectedTaskReport?.entrances?.firstOrNull { it.entranceNumber == number }?.userDescription ?: ""
        val isTaskFinished = s.selectedTask?.task?.state?.state in listOf(TaskState.COMPLETED, TaskState.CANCELED)
        withContext(Dispatchers.Main) {
            c.showDescriptionInputDialog(
                number,
                currentDescription,
                !isTaskFinished
            )
        }
    }

    fun effectChangeEntranceDescription(entranceNumber: EntranceNumber, description: String): ReportEffect = { c, s ->
        when (val affectedTask = s.selectedTask) {
            null -> c.showError("re:111", true)
            else -> {
                c.database.createOrUpdateTaskItemEntranceResult(
                    entranceNumber,
                    affectedTask.taskItem
                ) {
                    it.copy(userDescription = description)
                }?.let { messages.send(ReportMessages.msgSavedResultLoaded(it)) }
            }
        }
    }
}