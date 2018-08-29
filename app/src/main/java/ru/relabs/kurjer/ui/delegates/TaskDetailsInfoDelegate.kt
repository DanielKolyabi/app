package ru.relabs.kurjer.ui.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.DataModels
import ru.relabs.kurjer.models.DetailsTableHeader
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.DetailsTableHeaderHolder
import ru.relabs.kurjer.ui.holders.DetailsTableInfoHolder

/**
 * Created by ProOrange on 29.08.2018.
 */

class TaskDetailsInfoDelegate : IAdapterDelegate<DataModels> {
    override fun isForViewType(data: List<DataModels>, position: Int): Boolean {
        return data[position] is TaskModel
    }

    override fun onBindViewHolder(holder: BaseViewHolder<DataModels>, data: List<DataModels>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<DataModels> {
        return DetailsTableInfoHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_task_details_list_info, parent, false))
    }
}