package ru.relabs.kurjer.presentation.report

import android.graphics.Bitmap
import android.location.Location
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgEmpty
import ru.relabs.kurjer.presentation.base.tea.msgState
import java.io.File
import java.util.*

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

object ReportMessages {
    fun msgInit(itemIds: List<Pair<TaskId, TaskItemId>>, selectedTaskItemId: TaskItemId): ReportMessage = msgEffects(
        { it.copy() },
        {
            listOf(
                ReportEffects.effectLoadData(itemIds, selectedTaskItemId),
                ReportEffects.effectLaunchEventConsumers()
            )
        }
    )

    fun msgAddLoaders(i: Int): ReportMessage =
        msgState { it.copy(loaders = it.loaders + i) }

    fun msgBackClicked(): ReportMessage =
        msgEffect(ReportEffects.effectNavigateBack())

    fun msgTasksLoaded(tasks: List<TaskWithItem>): ReportMessage =
        msgState { it.copy(tasks = tasks) }

    fun msgTaskSelected(id: TaskItemId): ReportMessage = msgEffects(
        { it },
        { s ->
            listOfNotNull(
                ReportEffects.effectLoadSelection(id).takeIf { s.selectedTask?.taskItem?.id != id }
            )
        }
    )

    fun msgTaskSelectionLoaded(taskWithItem: TaskWithItem, photos: List<TaskItemPhoto>): ReportMessage =
        msgState { it.copy(selectedTask = taskWithItem, selectedTaskPhotos = photos) }

    fun msgPhotoClicked(entranceNumber: EntranceNumber? = null, multiplePhoto: Boolean): ReportMessage =
        msgEffect(ReportEffects.effectCreatePhoto(entranceNumber?.number ?: ENTRANCE_NUMBER_TASK_ITEM, multiplePhoto))

    fun msgRemovePhotoClicked(removedPhoto: TaskItemPhoto): ReportMessage = msgEffects(
        { state -> state.copy(selectedTaskPhotos = state.selectedTaskPhotos.filter { photo -> photo != removedPhoto }) },
        { listOf(ReportEffects.effectRemovePhoto(removedPhoto)) }
    )

    fun msgCoupleClicked(entrance: EntranceNumber): ReportMessage =
        msgEffect(ReportEffects.effectChangeCoupleState(entrance))

    fun msgEntranceSelectClicked(entrance: EntranceNumber, button: EntranceSelectionButton): ReportMessage =
        msgEffect(ReportEffects.effectEntranceSelectionChanged(entrance, button))

    fun msgSavedResultLoaded(report: TaskItemResult?): ReportMessage =
        msgState { it.copy(selectedTaskReport = report) }

    fun msgPhotoError(errorCode: Int): ReportMessage = msgEmpty() //TODO: Show error
    fun msgPhotoCaptured(entrance: Int, multiplePhoto: Boolean, targetFile: File, uuid: UUID): ReportMessage = msgEffects(
        { it },
        {
            listOfNotNull(
                ReportEffects.effectSavePhotoFromFile(entrance, targetFile, uuid),
                ReportEffects.effectCreatePhoto(entrance, multiplePhoto).takeIf { multiplePhoto }
            )
        }
    )

    fun msgPhotoCaptured(entrance: Int, multiplePhoto: Boolean, bitmap: Bitmap, targetFile: File, uuid: UUID): ReportMessage = msgEffects(
        { it },
        {
            listOfNotNull(
                ReportEffects.effectSavePhotoFromBitmap(entrance, bitmap, targetFile, uuid),
                ReportEffects.effectCreatePhoto(entrance, multiplePhoto).takeIf { multiplePhoto }
            )
        }
    )

    fun msgNewPhoto(newPhoto: TaskItemPhoto): ReportMessage =
        msgState { it.copy(selectedTaskPhotos = it.selectedTaskPhotos + listOf(newPhoto)) }

    fun msgDescriptionChanged(text: String): ReportMessage =
        msgEffect(ReportEffects.effectUpdateDescription(text))

    fun msgCouplingChanged(taskCoupleType: CoupleType, entrance: EntranceNumber, enabled: Boolean): ReportMessage =
        msgState { it.copy(coupling = it.coupling + mapOf(Pair(entrance, taskCoupleType) to enabled)) }

    fun msgCouplingChanged(coupling: ReportCoupling): ReportMessage =
        msgState { it.copy(coupling = coupling) }

    fun msgCloseClicked(): ReportMessage =
        msgEffect(ReportEffects.effectCloseCheck(true))

    fun msgPerformClose(location: Location?): ReportMessage =
        msgEffect(ReportEffects.effectClosePerform(true, location))

    fun msgInterruptPause(): ReportMessage =
        msgEffect(ReportEffects.effectInterruptPause())

    fun msgTaskItemClosed(task: TaskWithItem, withRemove: Boolean): ReportMessage = msgEffects(
        {
            if (withRemove) {
                it.copy(
                    tasks = it.tasks.map { t ->
                        if (t.taskItem.id == task.taskItem.id) {
                            t.copy(taskItem = t.taskItem.copy(state = TaskItemState.CLOSED))
                        } else {
                            t
                        }
                    },
                    selectedTask = it.selectedTask?.copy(
                        taskItem = it.selectedTask.taskItem.copy(
                            state = TaskItemState.CLOSED
                        )
                    )
                )
            } else {
                it
            }
        },
        { s ->
            listOfNotNull(
                s.tasks
                    .firstOrNull { it.taskItem.state == TaskItemState.CREATED && it.taskItem.id != task.taskItem.id }
                    ?.let { ReportEffects.effectLoadSelection(it.taskItem.id) }
                    ?.takeIf { withRemove },
                ReportEffects.effectNavigateBack()
                    .takeIf { s.tasks.none { it.taskItem.state == TaskItemState.CREATED && it.taskItem.id != task.taskItem.id } }
                    .takeIf { withRemove }
            )
        }
    )

    fun msgGPSLoading(enabled: Boolean): ReportMessage =
        msgState { it.copy(isGPSLoading = enabled) }

    fun msgTaskClosed(taskId: TaskId): ReportMessage = msgEffects(
        { s ->
            s.copy(
                tasks = s.tasks.map { t ->
                    if (t.task.id == taskId) {
                        t.copy(
                            task = t.task.copy(state = t.task.state.copy(state = TaskState.COMPLETED)),
                            taskItem = t.taskItem.copy(state = TaskItemState.CLOSED)
                        )
                    }else{
                        t
                    }
                }
            )
        },
        { s ->
            listOfNotNull(
                s.tasks
                    .firstOrNull { it.taskItem.state == TaskItemState.CREATED && it.task.id != taskId }
                    ?.let { ReportEffects.effectLoadSelection(it.taskItem.id) },
                ReportEffects.effectNavigateBack()
                    .takeIf { s.tasks.none { it.taskItem.state == TaskItemState.CREATED && it.task.id != taskId } }
            )
        }
    )

}