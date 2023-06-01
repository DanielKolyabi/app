package ru.relabs.kurjer.presentation.storageReport

import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.base.DefaultListDiffCallback
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.renderT
import ru.relabs.kurjer.uiOld.helpers.HintHelper
import ru.relabs.kurjer.utils.extensions.getColorCompat
import ru.relabs.kurjer.utils.extensions.renderText
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
        {
            view.text = it.firstOrNull()?.storage?.address
            if (it.firstOrNull()?.storageCloseFirstRequired == true) {
                view.setTextColor(view.resources.getColorCompat(R.color.colorFuchsia))
            }
        }
    )

    fun renderHint(hintHelper: HintHelper): StorageReportRender = renderT(
        { it.tasks },
        { hintHelper.text = it.firstOrNull()?.storage?.description.toString() }
    )

    fun renderClosesList(adapter: DelegateAdapter<StorageReportItem>): StorageReportRender =
        renderT(
            { it.tasks },
            { tasks ->
                val newItems =
                    tasks.flatMap { task ->
                        task.storage.closes.map {
                            StorageReportItem.Closure(0, task, it)
                        }
                    }.sortedBy { it.closure.closeDate }.mapIndexed { index, closure -> closure.copy(idx = index) }

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

    fun renderDescription(view: EditText, watcher: TextWatcher): StorageReportRender = renderT(
        { it.storageReport },
        { view.renderText(it?.description ?: "", watcher) }
    )
}