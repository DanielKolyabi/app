package ru.relabs.kurjer.ui.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ui.models.DetailsListModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.DetailsTableItemHolder

/**
 * Created by ProOrange on 29.08.2018.
 */
class TaskDetailsItemDelegate(val onInfoClicked: (item: TaskItemModel) -> Unit) : IAdapterDelegate<DetailsListModel> {
    override fun isForViewType(data: List<DetailsListModel>, position: Int): Boolean {
        return data[position] is DetailsListModel.TaskItem
    }

    override fun onBindViewHolder(holder: BaseViewHolder<DetailsListModel>, data: List<DetailsListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<DetailsListModel> {
        return DetailsTableItemHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_task_details_list_item, parent, false),
                onInfoClicked
        )
    }
}