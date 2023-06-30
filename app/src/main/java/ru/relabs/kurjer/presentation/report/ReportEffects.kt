package ru.relabs.kurjer.presentation.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.models.ENTRANCE_NUMBER_TASK_ITEM
import ru.relabs.kurjer.domain.models.EntranceNumber
import ru.relabs.kurjer.domain.models.ReportEntranceSelection
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.domain.models.TaskItemState
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.domain.models.address
import ru.relabs.kurjer.domain.models.id
import ru.relabs.kurjer.domain.models.needPhoto
import ru.relabs.kurjer.domain.models.photo.TaskItemPhoto
import ru.relabs.kurjer.domain.models.state
import ru.relabs.kurjer.domain.models.taskId
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.services.ReportService
import ru.relabs.kurjer.uiOld.helpers.formatedWithSecs
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.Either
import ru.relabs.kurjer.utils.ImageUtils
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.awaitFirst
import ru.relabs.kurjer.utils.calculateDistance
import ru.relabs.kurjer.utils.extensions.isLocationExpired
import java.io.File
import java.util.Date
import java.util.UUID

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */
object ReportEffects {
    fun effectLoadData(
        itemIds: List<Pair<TaskId, TaskItemId>>,
        selectedTaskItemId: TaskItemId
    ): ReportEffect = { c, s ->
        messages.send(ReportMessages.msgAddLoaders(1))
        val tasks = itemIds.mapNotNull { (taskId, taskItemId) ->
            val task = c.taskRepository.getTask(taskId)
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
                                            c.taskRepository.getTaskItemResult(taskWithItem.taskItem) == null

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
        val taskItem = c.taskRepository.getTaskItem(id)
        if (task == null || taskItem == null) {
            c.showError("re:110", true)
        } else {
            val photos = c.photoRepository.getTaskItemPhotos(taskItem).map {
                TaskPhotoWithUri(
                    it,
                    c.pathsProvider.getTaskItemPhotoFile(taskItem, UUID.fromString(it.uuid)).toUri()
                )
            }
            messages.send(
                ReportMessages.msgTaskSelectionLoaded(
                    TaskWithItem(task, taskItem),
                    photos
                )
            )

            val report = c.taskRepository.getTaskItemResult(taskItem)
            messages.send(ReportMessages.msgSavedResultLoaded(report))

            if (s.isEntranceSelectionChanged) {
                s.selectedTask?.task?.coupleType?.let {
                    messages.send(ReportMessages.msgDisableCouplingForType(it))
                }
            }
        }
        messages.send(ReportMessages.msgAddLoaders(-1))
    }

    fun effectValidateUnfinishedReportsAnd(
        msgFactory: () -> ReportMessage
    ): ReportEffect = { c, s ->
        if (!c.settingsRepository.canSkipUnfinishedTaskItem) {
            messages.send(ReportMessages.msgAddLoaders(1))
            val unfinishedTaskItem = c.photoRepository.getUnfinishedItemPhotos()
                .map { c.taskRepository.getTaskItem(it.taskItemId) }
                .firstOrNull()
                ?.takeIf {
                    it.id != s.selectedTask?.taskItem?.id
                            && it.address.id != s.selectedTask?.taskItem?.address?.id
                }

            when (unfinishedTaskItem) {
                null -> messages.send(msgFactory())
                else -> {
                    val unfinishedTask = c.taskRepository.getTask(unfinishedTaskItem.taskId)
                    if (unfinishedTask == null) {
                        messages.send(msgFactory())
                    } else {
                        withContext(Dispatchers.Main) {
                            c.showCloseError(
                                R.string.report_close_with_unfinished_item_warning,
                                false,
                                null,
                                null,
                                arrayOf(
                                    "${unfinishedTask.name} №${unfinishedTask.edition}",
                                    unfinishedTaskItem.address.name
                                )
                            )
                        }
                    }
                }
            }
            messages.send(ReportMessages.msgAddLoaders(-1))
        } else {
            messages.send(msgFactory())
        }
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
                        val delayJob =
                            async { delay(c.settingsRepository.closeGpsUpdateTime.photo * 1000L) }
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
                        messages.send(
                            msgEffect(
                                effectValidatePhotoRadiusAnd(
                                    msgFactory,
                                    withAnyRadiusWarning,
                                    false
                                )
                            )
                        )
                    }
                } else {
                    if (!c.settingsRepository.isPhotoRadiusRequired) {
                        //https://git.relabs.ru/kurier/app/-/issues/87 если юзер может делать фото и закрывать дома вне радиуса - ему нужно показывать диалог (он админ).
                        //Если же он может только делать фото, ему о диалоге знать не надо, что бы не особо пользовался этим
                        val shouldSuppressDialog = c.settingsRepository.isCloseRadiusRequired

                        if (distance > selected.taskItem.closeRadius && withAnyRadiusWarning && !shouldSuppressDialog) {
                            withContext(Dispatchers.Main) {
                                c.showCloseError(
                                    R.string.report_close_location_far_warning,
                                    false,
                                    null,
                                    null,
                                    emptyArray()
                                )
                            }
                        }
                        messages.send(msgFactory())
                    } else {
                        when {
                            locationNotValid -> withContext(Dispatchers.Main) {
                                c.showCloseError(
                                    R.string.report_close_location_null_error,
                                    false,
                                    null,
                                    null,
                                    emptyArray()
                                )
                            }

                            distance > selected.taskItem.closeRadius -> withContext(Dispatchers.Main) {
                                c.showCloseError(
                                    R.string.report_close_location_far_error,
                                    false,
                                    null,
                                    null,
                                    emptyArray()
                                )
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
        effectValidatePhotoRadiusAnd({
            msgEffect(
                effectRequestPhoto(
                    entranceNumber,
                    multiplePhoto
                )
            )
        }, false)(c, s)
    }

    fun effectRequestPhoto(entranceNumber: Int, multiplePhotos: Boolean): ReportEffect = { c, s ->
        when (val selectedTask = s.selectedTask) {
            null -> c.showError("re:100", true)
            else -> {
                val photoUUID = UUID.randomUUID()
                CustomLog.writeToFile("Request photo ${entranceNumber} ${photoUUID}")
                val photoFile =
                    c.pathsProvider.getTaskItemPhotoFile(selectedTask.taskItem, photoUUID)
                withContext(Dispatchers.Main) {
                    c.requestPhoto(entranceNumber, multiplePhotos, photoFile, photoUUID)
                }
            }
        }
    }

    fun effectRemovePhoto(it: TaskItemPhoto): ReportEffect = { c, s ->
        c.photoRepository.removePhoto(it)
    }

    fun effectEntranceSelectionChanged(
        entrance: EntranceNumber,
        button: EntranceSelectionButton
    ): ReportEffect = { c, s ->
        fun applyButtonClick(selection: ReportEntranceSelection): ReportEntranceSelection =
            when (button) {
                EntranceSelectionButton.Euro -> selection.copy(isEuro = !selection.isEuro)
                EntranceSelectionButton.Watch -> selection.copy(isWatch = !selection.isWatch)
                EntranceSelectionButton.Stack -> selection.copy(isStacked = !selection.isStacked)
                EntranceSelectionButton.Reject -> selection.copy(isRejected = !selection.isRejected)
            }

        when (val affectedTask = s.selectedTask) {
            null -> c.showError("re:101", true)
            else -> {
                val newResult = c.taskRepository.createOrUpdateTaskItemEntranceResultSelection(
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
                val newSelection =
                    newResult?.entrances?.firstOrNull { it.entranceNumber == entrance }?.selection
                if (newSelection != null && s.coupling.isCouplingEnabled(
                        affectedTask.task,
                        entrance
                    )
                ) {
                    s.tasks
                        .filter { it.task.coupleType == affectedTask.task.coupleType && it.taskItem.state == TaskItemState.CREATED }
                        .forEach {
                            c.taskRepository.createOrUpdateTaskItemEntranceResultSelection(
                                entrance,
                                it.taskItem,
                                newSelection
                            )
                        }
                }

                newResult?.let {
                    messages.send(ReportMessages.msgSavedResultLoaded(it))
                }
            }
        }
    }


    fun effectSavePhotoFromFile(
        entrance: Int,
        photoUri: Uri,
        targetFile: File,
        uuid: UUID
    ): ReportEffect = { c, s ->
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

    private fun effectSavePhotoFromBitmap(
        entrance: Int,
        bitmap: Bitmap,
        targetFile: File,
        uuid: UUID
    ): ReportEffect = { c, s ->
        CustomLog.writeToFile("Save photo ${entrance} ${uuid}")
        when (val task = s.selectedTask) {
            null -> c.showError("re:102", true)
            else -> {
                when (savePhotoFromBitmapToFile(bitmap, targetFile)) {
                    is Left -> messages.send(ReportMessages.msgPhotoError(6))
                    is Right -> {
                        val location = c.locationProvider.lastReceivedLocation()
                        val photo = c.photoRepository.savePhoto(entrance, task.taskItem, uuid, location)
                        val path = c.pathsProvider.getTaskItemPhotoFile(task.taskItem, uuid)
                        messages.send(ReportMessages.msgNewPhoto(TaskPhotoWithUri(photo, path.toUri())))
                    }
                }
            }
        }
    }

    fun effectUpdateDescription(text: String, task: TaskWithItem): ReportEffect = f@{ c, s ->
        when (val selectedTask = s.selectedTask) {
            null -> c.showError("re:103", true)
            else -> {
                if (task != selectedTask || task.taskItem.state != TaskItemState.CREATED) return@f
                s.tasks.filter { it.taskItem.state == TaskItemState.CREATED }.forEach { t ->
                    val result = c.taskRepository.getTaskItemResult(t.taskItem)
                        ?: c.taskRepository.createEmptyTaskResult(t.taskItem)
                    val updated = c.taskRepository.updateTaskItemResult(result.copy(description = text))
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
                    messages.send(
                        ReportMessages.msgCouplingChanged(
                            taskCoupleType,
                            entrance,
                            !currentCoupleState
                        )
                    )
                }
            }
        }
    }

    fun effectCloseCheck(withLocationLoading: Boolean, rejectReason: String?): ReportEffect =
        { c, s ->
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

                    val rejectionPhotoExists = s.selectedTaskReport?.entrances?.filter { it.selection.isRejected }
                        ?.all { entrance -> s.selectedTaskPhotos.any { it.photo.entranceNumber == entrance.entranceNumber } } == true

                    val location = c.locationProvider.lastReceivedLocation()
                    CustomLog.writeToFile(
                        "GPS LOG: Close check with location(${location?.latitude}, ${location?.longitude}, ${
                            Date(
                                location?.time ?: 0
                            ).formatedWithSecs()
                        })"
                    )

                    if (c.storageReportUseCase.isReportActuallyRequired(selected.task)) {
                        withContext(Dispatchers.Main) {
                            c.showCloseError(
                                R.string.storage_not_closed_error,
                                false,
                                null,
                                rejectReason,
                                emptyArray()
                            )
                        }
                    } else if (c.pauseRepository.isPaused) {
                        withContext(Dispatchers.Main) {
                            c.showPausedWarning()
                        }
                    } else if (!taskItemRequiredPhotoExists || !entrancesRequiredPhotoExists || !rejectionPhotoExists) {
                        withContext(Dispatchers.Main) {
                            c.showPhotosWarning()
                        }
                    } else if (withLocationLoading && (location == null || Date(location.time).isLocationExpired())) {
                        coroutineScope {
                            messages.send(ReportMessages.msgAddLoaders(1))
                            messages.send(ReportMessages.msgGPSLoading(true))
                            val delayJob =
                                async { delay(c.settingsRepository.closeGpsUpdateTime.close * 1000L) }
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
                                        c.showCloseError(
                                            R.string.report_close_location_null_error,
                                            false,
                                            null,
                                            rejectReason,
                                            emptyArray()
                                        )
                                        true
                                    }

                                    distance > selected.taskItem.closeRadius -> {
                                        c.showCloseError(
                                            R.string.report_close_location_far_error,
                                            false,
                                            null,
                                            rejectReason,
                                            emptyArray()
                                        )
                                        true
                                    }

                                    else -> {
                                        c.showPreCloseDialog(location, rejectReason)
                                        false
                                    }
                                }
                            } else {
                                when {
                                    location == null -> {
                                        c.showCloseError(
                                            R.string.report_close_location_null_warning,
                                            true,
                                            location,
                                            rejectReason,
                                            emptyArray()
                                        )
                                        false
                                    }

                                    distance > selected.taskItem.closeRadius -> {
                                        c.showCloseError(
                                            R.string.report_close_location_far_warning,
                                            true,
                                            location,
                                            rejectReason,
                                            emptyArray()
                                        )
                                        false
                                    }

                                    else -> {
                                        c.showPreCloseDialog(location, rejectReason)
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


    fun effectClosePerform(
        withRemove: Boolean,
        location: Location?,
        rejectReason: String?
    ): ReportEffect = { c, s ->
        messages.send(ReportMessages.msgAddLoaders(1))
        when (val selected = s.selectedTask) {
            null -> c.showError("re:107", true)
            else -> {
                effectInterruptPause()(c, s)
                if (selected.taskItem is TaskItem.Common) {
                    c.taskRepository.updateTaskItem(selected.taskItem.copy(closeTime = Date()))
                }
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

    private fun savePhotoFromBitmapToFile(
        bitmap: Bitmap,
        targetFile: File
    ): Either<Exception, File> = Either.of {
        val resized = ImageUtils.resizeBitmap(bitmap, 1024f, 768f)
        bitmap.recycle()
        ImageUtils.saveImage(resized, targetFile)
        targetFile
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

                        is TaskEvent.TasksUpdateRequired -> Unit
                        is TaskEvent.TaskStorageClosed -> Unit
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
        val currentDescription =
            s.selectedTaskReport?.entrances?.firstOrNull { it.entranceNumber == number }?.userDescription
                ?: ""
        val isTaskFinished =
            s.selectedTask?.task?.state?.state in listOf(TaskState.COMPLETED, TaskState.CANCELED)
        withContext(Dispatchers.Main) {
            c.showDescriptionInputDialog(
                number,
                currentDescription,
                !isTaskFinished
            )
        }
    }

    fun effectChangeEntranceDescription(
        entranceNumber: EntranceNumber,
        description: String
    ): ReportEffect = { c, s ->
        when (val affectedTask = s.selectedTask) {
            null -> c.showError("re:111", true)
            else -> {
                c.taskRepository.createOrUpdateTaskItemEntranceResult(
                    entranceNumber,
                    affectedTask.taskItem
                ) {
                    it.copy(userDescription = description)
                }?.let { messages.send(ReportMessages.msgSavedResultLoaded(it)) }
            }
        }
    }

    fun effectWarnProblemApartmentsAnd(
        taskItem: TaskItem?,
        entranceNumber: EntranceNumber?,
        problemApartments: List<Int>?,
        msgFactory: () -> ReportMessage
    ): ReportEffect = f@{ c, s ->
        if (entranceNumber == null || taskItem == null || problemApartments == null) {
            messages.send(msgFactory())
            return@f
        }

        if (c.taskRepository.getWarning(taskItem.id, entranceNumber) == null) {
            c.showProblemApartmentsWarning(problemApartments, entranceNumber, taskItem.id)
            c.taskRepository.createWarning(entranceNumber, taskItem.id, taskItem.taskId)
        } else {
            messages.send(msgFactory())
        }

    }
}