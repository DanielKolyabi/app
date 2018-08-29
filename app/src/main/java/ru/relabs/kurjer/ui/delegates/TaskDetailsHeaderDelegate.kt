package ru.relabs.kurjer.ui.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.AddressElement
import ru.relabs.kurjer.models.DataModels
import ru.relabs.kurjer.models.DetailsTableHeader
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.delegateAdapter.IAdapterDelegate
import ru.relabs.kurjer.ui.holders.AddressHolder
import ru.relabs.kurjer.ui.holders.DetailsTableHeaderHolder

/**
 * Created by ProOrange on 29.08.2018.
 */
class TaskDetailsHeaderDelegate : IAdapterDelegate<DataModels> {
    override fun isForViewType(data: List<DataModels>, position: Int): Boolean {
        return data[position] is DetailsTableHeader
    }

    override fun onBindViewHolder(holder: BaseViewHolder<DataModels>, data: List<DataModels>, position: Int) {
        holder.onBindViewHolder(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<DataModels> {
        return DetailsTableHeaderHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_task_details_list_header, parent, false))
    }
}