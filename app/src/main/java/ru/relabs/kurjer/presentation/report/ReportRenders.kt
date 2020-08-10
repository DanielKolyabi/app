package ru.relabs.kurjer.presentation.report

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import ru.relabs.kurjer.domain.models.ENTRANCE_NUMBER_TASK_ITEM
import ru.relabs.kurjer.presentation.base.DefaultListDiffCallback
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.renderT
import ru.relabs.kurjer.utils.extensions.visible

/**
 * Created by Daniil Kurchanov on 06.04.2020.
 */
object ReportRenders {
    fun renderLoading(view: View): ReportRender = renderT(
        { it.loaders > 0 },
        { view.visible = it }
    )

    fun renderTitle(view: TextView): ReportRender = renderT(
        {it.tasks.firstOrNull()?.taskItem?.address},
        {
            if(it == null){
                view.text = "Неизвестно"
            }else{
                view.text = it.name
            }
        }
    )

    fun renderTasks(adapter: DelegateAdapter<ReportTaskItem>, view: View): ReportRender = renderT(
        { it.tasks to it.selectedTask },
        { (available, selected) ->
            adapter.items.clear()
            adapter.items.addAll(available.map {
                ReportTaskItem(it.task, it.taskItem, it == selected)
            })
            adapter.notifyDataSetChanged()
            view.visible = available.size > 1
        }
    )

    fun renderPhotos(adapter: DelegateAdapter<ReportPhotoItem>): ReportRender = renderT(
        { it.selectedTaskPhotos to it.selectedTask },
        { (photos, task) ->
            val photoRequired = task?.taskItem?.needPhoto ?: false
            val hasPhoto = photos.any { it.entranceNumber.number == ENTRANCE_NUMBER_TASK_ITEM }
            val newItems =
                listOf(ReportPhotoItem.Single(photoRequired, hasPhoto)) +
                        photos.map { ReportPhotoItem.Photo(it) }

            val diff = DiffUtil.calculateDiff(DefaultListDiffCallback(adapter.items, newItems))

            adapter.items.clear()
            adapter.items.addAll(newItems)
            diff.dispatchUpdatesTo(adapter)
        }
    )

    fun renderEntrances(adapter: DelegateAdapter<ReportEntranceItem>): ReportRender = renderT(
        { it.selectedTask?.taskItem to it.selectedTaskPhotos },
        { (taskItem, photos) ->
            adapter.items.clear()
            if (taskItem != null) {
                adapter.items.addAll(
                    taskItem.entrancesData.map {
                        ReportEntranceItem(
                            taskItem,
                            it.number,
                            ReportEntranceSelection(
                                it.isEuroBoxes,
                                it.hasLookout,
                                it.isStacked,
                                it.isRefused
                            ), //TODO: Use data from saved entrance
                            false, //TODO: Check if coupleEnabled
                            photos.any { photo -> photo.entranceNumber == it.number }
                        )
                    }
                )
            }
            adapter.notifyDataSetChanged()
        }
    )
}