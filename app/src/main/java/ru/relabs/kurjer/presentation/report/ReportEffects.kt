package ru.relabs.kurjer.presentation.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
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

        messages.send(ReportMessages.msgTasksLoaded(tasks))
        messages.send(ReportMessages.msgTaskSelected(selectedTaskItemId))
        messages.send(ReportMessages.msgAddLoaders(-1))
    }

    fun effectNavigateBack(): ReportEffect = { c, s ->
        //TODO: Save report
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

        when (val selectedTask = s.selectedTask) {
            null -> Unit //TODO: Show error
            else -> {
                val report = if (s.selectedTaskReport == null) {
                    val result = createEmptyTaskResult(c.database, selectedTask.taskItem)
                    result?.let { createEmptyEntranceResult(c.database, it, selectedTask.taskItem, entrance) }
                } else {
                    if (s.selectedTaskReport.entrances.none { it.entranceNumber == entrance }) {
                        createEmptyEntranceResult(c.database, s.selectedTaskReport, selectedTask.taskItem, entrance)
                    } else {
                        s.selectedTaskReport
                    }
                }

                when (val reportEntrance = report?.entrances?.firstOrNull { it.entranceNumber == entrance }) {
                    null -> Unit //TODO: Show error
                    else -> {
                        val updatedEntranceSelection = applyButtonClick(reportEntrance.selection).let {
                            if (button == EntranceSelectionButton.Euro && it.isEuro) {
                                it.copy(isStacked = true)
                            } else if (button == EntranceSelectionButton.Watch && it.isWatch) {
                                it.copy(isStacked = true)
                            } else if (
                                (button == EntranceSelectionButton.Watch || button == EntranceSelectionButton.Euro)
                                && (!it.isEuro && !it.isWatch)
                            ) {
                                it.copy(isStacked = false)
                            } else {
                                it
                            }
                        }
                        val updatedEntrance = reportEntrance.copy(selection = updatedEntranceSelection)
                        val updatedReport = report.copy(
                            entrances = report.entrances.map {
                                if (it.id == updatedEntrance.id) {
                                    updatedEntrance
                                } else {
                                    it
                                }
                            }
                        )

                        c.database.updateTaskItemResult(updatedReport)?.let {
                            messages.send(ReportMessages.msgSavedResultLoaded(it))
                        }
                    }
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

    private suspend fun createEmptyEntranceResult(
        database: DatabaseRepository,
        taskItemResult: TaskItemResult,
        taskItem: TaskItem,
        entrance: EntranceNumber
    ): TaskItemResult? {
        if (taskItemResult.entrances.any { it.entranceNumber == entrance }) {
            return taskItemResult
        }
        val defaultEntranceData = taskItem.entrancesData.firstOrNull { it.number == entrance }
        val updatedResult = taskItemResult.copy(
            entrances = taskItemResult.entrances + listOf(
                TaskItemEntranceResult(
                    id = TaskItemEntranceId(0),
                    taskItemResultId = taskItemResult.id,
                    entranceNumber = entrance,
                    selection = ReportEntranceSelection(
                        isEuro = defaultEntranceData?.isEuroBoxes ?: false,
                        isWatch = defaultEntranceData?.hasLookout ?: false,
                        isStacked = defaultEntranceData?.isStacked ?: false,
                        isRejected = defaultEntranceData?.isRefused ?: false
                    )
                )
            )
        )
        return database.updateTaskItemResult(updatedResult)
    }
}