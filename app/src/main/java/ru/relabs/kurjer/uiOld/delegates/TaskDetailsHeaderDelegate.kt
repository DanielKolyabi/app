package ru.relabs.kurjer.uiOld.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.uiOld.models.DetailsListModel
import ru.relabs.kurjer.uiOld.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.uiOld.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.uiOld.holders.DetailsTableHeaderHolder

/**
 * Created by ProOrange on 29.08.2018.
 */
class TaskDetailsHeaderDelegate : IAdapterDelegate<DetailsListModel> {
    override fun isForViewType(data: List<DetailsListModel>, position: Int): Boolean {
        return data[position] is DetailsListModel.DetailsTableHeader
    }

    override fun onBindViewHolder(holder: BaseViewHolder<DetailsListModel>, data: List<DetailsListModel>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<DetailsListModel> {
        return DetailsTableHeaderHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_task_details_list_header, parent, false))
    }
}