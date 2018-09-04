package ru.relabs.kurjer.ui.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.AddressListSortingHolder
import ru.relabs.kurjer.ui.holders.TaskListLoaderHolder
import ru.relabs.kurjer.ui.models.AddressListModel
import ru.relabs.kurjer.ui.models.TaskListModel

class TaskListLoaderDelegate : IAdapterDelegate<TaskListModel> {

    override fun isForViewType(data: List<TaskListModel>, position: Int): Boolean {
        return data[position] is TaskListModel.Loader
    }

    override fun onBindViewHolder(holder: BaseViewHolder<TaskListModel>, data: List<TaskListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<TaskListModel> {
        return TaskListLoaderHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_loading, parent, false))
    }
}
