package ru.relabs.kurjer.presentation.storageReport

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import ru.relabs.kurjer.presentation.base.DefaultListDiffCallback
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.renderT
import ru.relabs.kurjer.presentation.report.ReportRender
import ru.relabs.kurjer.uiOld.helpers.HintHelper
import ru.relabs.kurjer.utils.extensions.visible


object StorageReportRenders {
    fun renderLoading(view: View): StorageReportRender = renderT(
        { it.loaders > 0 },
        { loading ->
            view.visible = loading
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
                    task.storage.closes.map {
                        StorageReportItem.Closure(
                            task,
                            it
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

    )

    fun renderDescription() {

    }
}