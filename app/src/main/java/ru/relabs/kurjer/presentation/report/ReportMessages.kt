package ru.relabs.kurjer.presentation.report

import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgEmpty
import ru.relabs.kurjer.presentation.base.tea.msgState

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

object ReportMessages {
    fun msgInit(itemIds: List<Pair<TaskId, TaskItemId>>, selectedTaskItemId: TaskItemId): ReportMessage = msgEffects(
        { it.copy() },
        { listOf(ReportEffects.effectLoadData(itemIds, selectedTaskItemId)) }
    )

    fun msgAddLoaders(i: Int): ReportMessage =
        msgState { it.copy(loaders = it.loaders + i) }

    fun msgBackClicked(): ReportMessage =
        msgEffect(ReportEffects.effectNavigateBack())

    fun msgTasksLoaded(tasks: List<TaskWithItem>): ReportMessage =
        msgState { it.copy(tasks = tasks) }

    fun msgTaskSelected(id: TaskItemId): ReportMessage =
        msgEffect(ReportEffects.effectLoadSelection(id))

    fun msgTaskSelectionLoaded(taskWithItem: TaskWithItem, photos: List<TaskItemPhoto>): ReportMessage =
        msgState { it.copy(selectedTask = taskWithItem, selectedTaskPhotos = photos) }

    fun msgPhotoClicked(entranceNumber: EntranceNumber? = null): ReportMessage =
        msgEffect(ReportEffects.effectCreatePhoto(entranceNumber?.number ?: ENTRANCE_NUMBER_TASK_ITEM))

    fun msgRemovePhotoClicked(removedPhoto: TaskItemPhoto): ReportMessage = msgEffects(
        { state -> state.copy(selectedTaskPhotos = state.selectedTaskPhotos.filter { photo -> photo != removedPhoto }) },
        { listOf(ReportEffects.effectRemovePhoto(removedPhoto)) }
    )

    fun msgCoupleClicked(entrance: EntranceNumber): ReportMessage = msgEmpty() //TODO: Implement

    fun msgEntranceSelectClicked(entrance: EntranceNumber, button: EntranceSelectionButton): ReportMessage =
        msgEffect(ReportEffects.effectEntranceSelectionChanged(entrance, button))

    fun msgSavedResultLoaded(report: TaskItemResult): ReportMessage =
        msgState { it.copy(selectedTaskReport = report) }
}