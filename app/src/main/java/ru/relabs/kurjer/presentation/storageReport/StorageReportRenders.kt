package ru.relabs.kurjer.presentation.storageReport

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import ru.relabs.kurjer.presentation.base.DefaultListDiffCallback
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.renderT
import ru.relabs.kurjer.uiOld.helpers.HintHelper
import ru.relabs.kurjer.utils.extensions.visible


object StorageReportRenders {
    fun renderLoading(view: View, gpsView: View): StorageReportRender = renderT(
        { (it.loaders > 0) to it.isGPSLoading },
        { (loading, gps) ->
            view.visible = loading
            gpsView.visible = gps
        }
    )

    fun renderTitle(view: TextView): StorageReportRender = renderT(
        { it.tasks },
        { view.text = it.firstOrNull()?.storage?.address }
    )

    fun renderHint(hintHelper: HintHelper): StorageReportRender = renderT(
        { it.tasks },
        { hintHelper.text = it.firstOrNull()?.storage?.description.toString() }
    )

    fun renderClosesList(adapter: DelegateAdapter<StorageReportItem>): StorageReportRender =
        renderT(
            { it.tasks },
            { tasks ->
                val newItems = tasks.flatMap { task ->
                    task.storage.closes.mapIndexed { index, storageClosure ->
                        StorageReportItem.Closure(
                            index,
                            task,
                            storageClosure
                        )
                    }
                }.sortedBy { it.closure.closeDate }

                val diff = DiffUtil.calculateDiff(DefaultListDiffCallback(adapter.items, newItems))
                adapter.items.clear()
                adapter.items.addAll(newItems)
                diff.dispatchUpdatesTo(adapter)
            }
        )

    fun renderPhotos(adapter: DelegateAdapter<StorageReportItem>): StorageReportRender = renderT(
        { it.storagePhotos to it.tasks },
        { (photos, tasks) ->
            val photosRequired = tasks.any { it.storage.photoRequired }
            val hasPhoto = photos.isNotEmpty()
            val newItems = listOf(
                StorageReportItem.Single(photosRequired, hasPhoto)
            ) + photos.map { StorageReportItem.Photo(it.photo, it.uri) }

            val diff = DiffUtil.calculateDiff(DefaultListDiffCallback(adapter.items, newItems))

            adapter.items.clear()
            adapter.items.addAll(newItems)
            diff.dispatchUpdatesTo(adapter)
        }
    )

    fun renderDescription(): StorageReportRender = renderT(
        {},
        {}
    )
}