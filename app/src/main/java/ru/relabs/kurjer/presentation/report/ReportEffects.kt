package ru.relabs.kurjer.presentation.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.files.ImageUtils
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.utils.Either
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
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
}