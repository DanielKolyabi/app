package ru.relabs.kurjer.ui.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ui.models.DetailsListModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.DetailsTableInfoHolder

/**
 * Created by ProOrange on 29.08.2018.
 */

class TaskDetailsInfoDelegate : IAdapterDelegate<DetailsListModel> {
    override fun isForViewType(data: List<DetailsListModel>, position: Int): Boolean {
        return data[position] is DetailsListModel.Task
    }

    override fun onBindViewHolder(holder: BaseViewHolder<DetailsListModel>, data: List<DetailsListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<DetailsListModel> {
        return DetailsTableInfoHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_task_details_list_info, parent, false))
    }
}