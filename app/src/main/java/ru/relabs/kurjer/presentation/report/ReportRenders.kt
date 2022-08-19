package ru.relabs.kurjer.presentation.report

import android.text.Html
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.presentation.base.DefaultListDiffCallback
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.renderT
import ru.relabs.kurjer.uiOld.helpers.HintHelper
import ru.relabs.kurjer.utils.extensions.renderText
import ru.relabs.kurjer.utils.extensions.visible
import java.util.*

/**
 * Created by Daniil Kurchanov on 06.04.2020.
 */
object ReportRenders {
    fun renderLoading(view: View, gpsLoadingLabel: View): ReportRender = renderT(
        { (it.loaders > 0) to it.isGPSLoading },
        { (loading, isGPSLoading) ->
            view.visible = loading
            gpsLoadingLabel.visible = isGPSLoading && loading
        }
    )

    fun renderTitle(view: TextView): ReportRender = renderT(
        { it.tasks.firstOrNull()?.taskItem?.address },
        {
            if (it == null) {
                view.text = "Неизвестно"
            } else {
                view.text = it.name
            }
        }
    )

    fun renderTasks(adapter: DelegateAdapter<ReportTaskItem>, view: View): ReportRender = renderT(
        { it.tasks to it.selectedTask },
        { (available, selected) ->
            adapter.items.clear()
            adapter.items.addAll(
                available
                    .sortedBy { it.taskItem.state }
                    .map {
                        ReportTaskItem(it.task, it.taskItem, it == selected)
                    }
            )
            adapter.notifyDataSetChanged()
            view.visible = available.size > 1
        }
    )

    fun renderPhotos(adapter: DelegateAdapter<ReportPhotoItem>): ReportRender = renderT(
        { it.selectedTaskPhotos to it.selectedTask },
        { (photos, task) ->
            val photoRequired = task?.taskItem?.needPhoto ?: false
            val hasPhoto = photos.any { it.photo.entranceNumber.number == ENTRANCE_NUMBER_TASK_ITEM }
            val newItems =
                listOf(ReportPhotoItem.Single(photoRequired, hasPhoto)) +
                        photos.map { ReportPhotoItem.Photo(it.photo, it.uri) }

            val diff = DiffUtil.calculateDiff(DefaultListDiffCallback(adapter.items, newItems))

            adapter.items.clear()
            adapter.items.addAll(newItems)
            diff.dispatchUpdatesTo(adapter)
        }
    )

    fun renderEntrances(adapter: DelegateAdapter<ReportEntranceItem>, view: RecyclerView): ReportRender = renderT(
        { Triple(it.selectedTask?.taskItem to it.selectedTask?.task, it.selectedTaskPhotos, it.selectedTaskReport) to it.coupling },
        { (data, coupling) ->
            val (taskData, photos, report) = data
            val (taskItem, task) = taskData
            view.visible = taskItem is TaskItem.Common
            adapter.items.clear()
            if (taskItem != null) {
                if (taskItem is TaskItem.Common) {
                    adapter.items.addAll(
                        taskItem.entrancesData.map { entrance ->
                            val reportEntrance = report?.entrances?.firstOrNull { it.entranceNumber == entrance.number }
                            ReportEntranceItem(
                                taskItem,
                                entrance.number,
                                reportEntrance?.selection ?: ReportEntranceSelection(false, false, false, false),
                                task?.let { coupling.isCouplingEnabled(task, entrance.number) } ?: false,
                                photos.any { photo -> photo.photo.entranceNumber == entrance.number },
                                reportEntrance?.userDescription.isNullOrEmpty()
                            )
                        }
                    )
                }
            }
            adapter.notifyDataSetChanged()
        }
    )

    fun renderDescription(view: EditText, watcher: TextWatcher): ReportRender = renderT(
        { it.selectedTaskReport },
        { view.renderText(it?.description ?: "", watcher) }
    )

    fun renderNotes(hintHelper: HintHelper): ReportRender = renderT(
        { it.selectedTask?.taskItem?.notes.orEmpty() },
        { notes ->
            hintHelper.text = Html.fromHtml(
                (3 downTo 1)
                    .map { notes.getOrElse(it - 1) { "" } }
                    .joinToString("<br/>")
                    .repeat(3)
            )
        }
    )

    fun renderTaskItemAvailability(
        listInterceptor: ListClickInterceptor,
        descriptionEdit: View,
        closeButton: View,
        rejectButton: View
    ): ReportRender = renderT(
        { it.selectedTask },
        {
            val available = it != null && it.taskItem.state == TaskItemState.CREATED && it.task.startTime < Date()
            listInterceptor.enabled = available
            descriptionEdit.isEnabled = available
            closeButton.isEnabled = available
            rejectButton.isEnabled = available
        }
    )

    fun renderRejectButton(button: View): ReportRender = renderT(
        { it.selectedTask },
        { button.visible = it?.taskItem is TaskItem.Firm }
    )

    fun renderFirmFullAddress(textView: TextView): ReportRender = renderT(
        {
            val ti = it.selectedTask
            if (ti != null && ti.taskItem is TaskItem.Firm) {
                listOf(ti.task.name, "№${ti.task.edition}", "${ti.taskItem.copies}экз.", ti.taskItem.firmName, ti.taskItem.office)
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
            } else {
                null
            }
        },
        {
            textView.visible = it != null
            textView.text = it
        }
    )
}